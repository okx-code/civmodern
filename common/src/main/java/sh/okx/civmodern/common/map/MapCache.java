package sh.okx.civmodern.common.map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import sh.okx.civmodern.common.map.data.RegionLoader;
import sh.okx.civmodern.common.map.data.RegionMapUpdater;
import sh.okx.civmodern.common.map.data.RegionRenderer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapCache {
    private static final int ATLAS_LENGTH = RegionAtlasTexture.SIZE / RegionMapUpdater.SIZE;
    private static final int ATLAS_BITS = Integer.numberOfTrailingZeros(RegionAtlasTexture.SIZE / RegionMapUpdater.SIZE);

    private final Set<RegionKey> gettingAtlas = new HashSet<>();

    // maybe downsample texture atlases? rendering a full civmc map is >1GB of VRAM
    private final Map<RegionKey, RegionAtlasTexture> textureCache = new ConcurrentHashMap<>();
    private final Map<RegionKey, RegionLoader> cache = new ConcurrentHashMap<>();

    private final Set<RegionKey> dirtySaveRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Queue<RegionKey> queue = new PriorityBlockingQueue<>(11, (r1, r2) -> {
        int px = Minecraft.getInstance().player.getBlockX();
        int pz = Minecraft.getInstance().player.getBlockZ();
        return Double.compare(
            Mth.lengthSquared(r1.x() * 512 + 256 - px, r1.z() * 512 + 256 - pz),
            Mth.lengthSquared(r2.x() * 512 + 256 - px, r2.z() * 512 + 256 - pz));
    });
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()) - 1);

    private final Map<ChunkPos, ScheduledFuture<?>> debounced = new ConcurrentHashMap<>();

    private final MapFolder mapFile;

    private final Set<RegionKey> availableRegions;
    private final IdLookup blockLookup;
    private final IdLookup biomeLookup;

    private final ReadWriteLock evictionLock = new ReentrantReadWriteLock();

    public MapCache(MapFolder mapFile) {
        this.mapFile = mapFile;
        this.availableRegions = mapFile.listRegions();
        this.blockLookup = new IdLookup(mapFile.blockIds(), "minecraft:air");
        this.biomeLookup = new IdLookup(mapFile.biomeIds(), "minecraft:void");
    }

    private void primeHeightmaps(ChunkAccess chunk) {
        if (chunk != null) {
            if (!chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE)) {
                Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE));
            }
            if (!chunk.hasPrimedHeightmap(Heightmap.Types.OCEAN_FLOOR_WG)) {
                Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));
            }
        }
    }

    public RegionKey getRegionKey(int x, int z) {
        return new RegionKey(x >> 9, z >> 9);
    }

    public void addInterest(RegionKey key, RegionDataType type) {
        try {
            this.evictionLock.readLock().lock();
            RegionLoader loader = cache.computeIfAbsent(key, k -> new RegionLoader(k, mapFile));
            loader.addInterest(type);
        } finally {
            this.evictionLock.readLock().lock();
        }
    }

    public void removeInterest(RegionKey key, RegionDataType type) {
        try {
            this.evictionLock.readLock().lock();
            RegionLoader loader = cache.computeIfAbsent(key, k -> new RegionLoader(k, mapFile));
            loader.removeInterest(type);
        } finally {
            this.evictionLock.readLock().lock();
        }
    }

    public Short getYLevel(int x, int z) {
        RegionKey key = getRegionKey(x, z);
        RegionLoader loader;
        try {
            this.evictionLock.readLock().lock();
            loader = cache.computeIfAbsent(key, k -> new RegionLoader(k, mapFile));
        } finally {
            this.evictionLock.readLock().lock();
        }
        // TODO make async
        short[] ylevels = loader.getOrLoadYLevels();
        if (ylevels == null) {
            return null;
        }
        short ylevel = ylevels[Math.floorMod(z, 512) + Math.floorMod(x, 512) * 512];

        if (ylevel > 0) {
            return (short) (ylevel - 1);
        } else if (ylevel == 0) {
            return null;
        } else {
            return ylevel;
        }
    }

    public void updateChunk(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int regionX = pos.getRegionX();
        int regionZ = pos.getRegionZ();
        RegionKey region = new RegionKey(regionX, regionZ);


        // TODO post process neighbouring north and west chunks (if loaded) to finish height shading
        RegionKey atlas = new RegionKey(regionX >> ATLAS_BITS, regionZ >> ATLAS_BITS);
        RegionAtlasTexture tex = this.textureCache.computeIfAbsent(atlas, k -> {
            RegionAtlasTexture texture1 = new RegionAtlasTexture();
            texture1.init();
            return texture1;
        });

        ChunkAccess north = chunk.getLevel().getChunk(pos.x, pos.z - 1, ChunkStatus.FULL, false);
        ChunkAccess west = chunk.getLevel().getChunk(pos.x - 1, pos.z, ChunkStatus.FULL, false);

        primeHeightmaps(chunk);
        primeHeightmaps(north);
        primeHeightmaps(west);

        boolean addedAtlas = gettingAtlas.add(atlas);
        executor.submit(() -> {
            try {
                this.evictionLock.readLock().lock();
                RegionLoader loader = cache.computeIfAbsent(region, k -> new RegionLoader(k, mapFile));
                loader.addInterest(RegionDataType.MAP);
                try {
                    // TODO fully get rid of banding, this is only a partial solution
                    RegionMapUpdater updater = new RegionMapUpdater(loader, blockLookup, biomeLookup);
                    boolean updated = updater.updateChunk(chunk.getLevel().registryAccess(), chunk, north, west);

                    // todo check this if the full screen map is loading y level data
                    // todo put this on a 60 second loop
                    // also test with while true to make sure there are no race conditions
//            cacheEvict(cache); // todo don't cache evict if gui is open?

                    boolean shouldRender = loader.render();
                    if (shouldRender) {
                        RegionRenderer renderer = new RegionRenderer(loader, blockLookup, biomeLookup);
                        renderer.render(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1);
                    }
                    if (updated) {
                        dirtySaveRegions.add(region);
                        if (!shouldRender) {
                            int regionLocalX = pos.getRegionLocalX();
                            int regionLocalZ = pos.getRegionLocalZ();
                            RegionRenderer renderer = new RegionRenderer(loader, blockLookup, biomeLookup);
                            renderer.renderChunk(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1, regionLocalX, regionLocalZ);
                        }
                    }
                } finally {
                    loader.removeInterest(RegionDataType.MAP);
                }
            } finally {
                this.evictionLock.readLock().unlock();
            }
            if (addedAtlas) {
                for (int x = 0; x < ATLAS_LENGTH; x++) {
                    for (int z = 0; z < ATLAS_LENGTH; z++) {
                        if ((x != (regionX & ATLAS_LENGTH - 1) || z != (regionZ & (ATLAS_LENGTH - 1)))) {
                            enqueue(new RegionKey(atlas.x() << ATLAS_BITS | x, atlas.z() << ATLAS_BITS | z));
                        }
                    }
                }
            }
        });
    }

    private void cacheEvict(Map<RegionKey, RegionLoader> cache) {
        try {
            this.evictionLock.writeLock().lock();
            Iterator<Map.Entry<RegionKey, RegionLoader>> iterator = cache.entrySet().iterator();
            Map<RegionKey, RegionLoader> toSave = new HashMap<>();
            while (iterator.hasNext()) {
                Map.Entry<RegionKey, RegionLoader> entry = iterator.next();
                // TODO save specific interests instead of checking for all and then cancelling
                if (entry.getValue().hasInterests()) {
                    continue;
                }

                int px = Minecraft.getInstance().player.getBlockX();
                int pz = Minecraft.getInstance().player.getBlockZ();
                double dist = Mth.lengthSquared(entry.getKey().x() * 512 + 256 - px, entry.getKey().z() * 512 + 256 - pz);
                if (dist > 96 * 16 * 96 * 16) {
                    iterator.remove();
                    if (this.dirtySaveRegions.remove(entry.getKey())) {
                        toSave.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            this.mapFile.saveBlockIds(this.blockLookup.getNames());
            this.mapFile.saveBiomeIds(this.biomeLookup.getNames());
            this.mapFile.saveBulk(toSave);
        } finally {
            this.evictionLock.writeLock().unlock();
        }
    }

    public RegionAtlasTexture getTexture(RegionKey atlas) {
        RegionAtlasTexture texture = this.textureCache.get(atlas);
        if (texture == null) {
            if (gettingAtlas.add(atlas)) {
                for (int x = 0; x < ATLAS_LENGTH; x++) {
                    for (int z = 0; z < ATLAS_LENGTH; z++) {
                        RegionKey region = new RegionKey(atlas.x() << ATLAS_BITS | x, atlas.z() << ATLAS_BITS | z);
                        if (availableRegions.contains(region)) {
                            this.textureCache.computeIfAbsent(atlas, k -> {
                                RegionAtlasTexture tex = new RegionAtlasTexture();
                                tex.init();
                                return tex;
                            });
                            enqueue(region);
                        }
                    }
                }
            }
            return null;
        }

        return texture;
    }

    private void enqueue(RegionKey region) {
        queue.add(region);
        executor.submit(() -> {
            RegionKey poll = queue.poll();
            if (poll == null) {
                return;
            }
            try {
                this.evictionLock.readLock().lock(); // TODO make lock less coarse
                RegionLoader loader = cache.computeIfAbsent(poll, k -> new RegionLoader(k, mapFile));

                loader.addInterest(RegionDataType.MAP);
                try {
                    // TODO fully get rid of banding, this is only a partial solution
                    RegionRenderer renderer = new RegionRenderer(loader, blockLookup, biomeLookup);
                    renderer.render(this.textureCache.get(new RegionKey(poll.x() >> ATLAS_BITS, poll.z() >> ATLAS_BITS)), poll.x() & ATLAS_LENGTH - 1, poll.z() & ATLAS_LENGTH - 1);
                } finally {
                    loader.removeInterest(RegionDataType.MAP);
                }
            } finally {
                this.evictionLock.readLock().unlock();
            }
        });
    }

    public void save() {
        // todo save not just on gui close but on world unload or 60 second interval
        executor.submit(() -> {
            Map<RegionKey, RegionLoader> toSave = new HashMap<>();
            for (Iterator<RegionKey> iterator = dirtySaveRegions.iterator(); iterator.hasNext(); ) {
                RegionKey dirtySave = iterator.next();
                RegionLoader cached = this.cache.get(dirtySave);
                if (cached != null) {
                    toSave.put(dirtySave, cached);
                    iterator.remove();
                }
            }
            this.mapFile.saveBlockIds(this.blockLookup.getNames());
            this.mapFile.saveBiomeIds(this.biomeLookup.getNames());
            this.mapFile.saveBulk(toSave);
        });
        executor.close();
    }

    public void free() {
        for (RegionAtlasTexture atlas : this.textureCache.values()) {
            atlas.delete();
        }
    }
}
