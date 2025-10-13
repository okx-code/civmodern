package sh.okx.civmodern.common.map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.logging.log4j.Level;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.data.RegionLoader;
import sh.okx.civmodern.common.map.data.RegionMapUpdater;
import sh.okx.civmodern.common.map.data.RegionReference;
import sh.okx.civmodern.common.map.data.RegionRenderer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MapCache {
    private static final int ATLAS_LENGTH = RegionAtlasTexture.SIZE / RegionMapUpdater.SIZE;
    private static final int ATLAS_BITS = Integer.numberOfTrailingZeros(RegionAtlasTexture.SIZE / RegionMapUpdater.SIZE);

    private final Set<RegionKey> gettingAtlas = new HashSet<>();

    // maybe downsample texture atlases? rendering a full civmc map is >1GB of VRAM
    private final Map<RegionKey, RegionAtlasTexture> textureCache = new ConcurrentHashMap<>();
    private final Map<RegionKey, RegionReference> cache = new ConcurrentHashMap<>();

    // Prevents regions that have recently been updated from being GCed
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<RegionKey, RegionLoader> nearbyRegions = new ConcurrentHashMap<>();

    private final Queue<RegionKey> queue = new PriorityBlockingQueue<>(11, (r1, r2) -> {
        int px = Minecraft.getInstance().player.getBlockX();
        int pz = Minecraft.getInstance().player.getBlockZ();
        return Double.compare(
            Mth.lengthSquared(r1.x() * 512 + 256 - px, r1.z() * 512 + 256 - pz),
            Mth.lengthSquared(r2.x() * 512 + 256 - px, r2.z() * 512 + 256 - pz));
    });
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()) - 1);

    private final MapFolder mapFile;

    private final Set<RegionKey> availableRegions;
    private final IdLookup blockLookup;
    private final IdLookup biomeLookup;

    public MapCache(MapFolder mapFile) {
        this.mapFile = mapFile;
        this.availableRegions = mapFile.listRegions();
        this.blockLookup = new IdLookup(mapFile.blockIds(), "minecraft:air");
        this.biomeLookup = new IdLookup(mapFile.biomeIds(), "minecraft:void");

        this.executor.scheduleAtFixedRate(this::cacheEvict, 2, 2, TimeUnit.MINUTES);
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

    public RegionReference addReference(RegionKey key) {
        return cache.compute(key, (k, v) -> {
            RegionLoader loader;
            if (v == null || (loader = v.getLoader()) == null) {
                return new RegionReference(new RegionLoader(k, mapFile), 1);
            } else {
                v.addReference(loader);
                return v;
            }
        });
    }

    public void removeReference(RegionKey key) {
        cache.computeIfPresent(key, (k, v) -> {
            v.removeReference();
            return v;
        });
    }

    public Short getYLevel(int x, int z) {
        RegionKey key = getRegionKey(x, z);
        RegionReference ref = addReference(key);
        try {
            // TODO make async
            short[] ylevels = ref.getLoader().getOrLoadYLevels();
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
        } finally {
            ref.removeReference();
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

        primeHeightmaps(chunk);

        boolean addedAtlas = gettingAtlas.add(atlas);
        executor.submit(() -> {
            try {
                RegionReference reference = addReference(region);
                try {
                    RegionLoader loader = reference.getLoader();
                    this.nearbyRegions.put(region, loader);
                    // TODO fully get rid of banding, this is only a partial solution
                    RegionMapUpdater updater = new RegionMapUpdater(loader, blockLookup, biomeLookup);
                    boolean updated = updater.updateChunk(chunk.getLevel().registryAccess(), chunk);

                    boolean shouldRender = loader.render();
                    if (shouldRender) {
                        RegionRenderer renderer = new RegionRenderer(loader, blockLookup, biomeLookup);
                        renderer.render(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1);
                    }
                    if (updated) {
                        reference.markDirty();
                        if (!shouldRender) {
                            int regionLocalX = pos.getRegionLocalX();
                            int regionLocalZ = pos.getRegionLocalZ();
                            RegionRenderer renderer = new RegionRenderer(loader, blockLookup, biomeLookup);
                            renderer.renderChunk(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1, regionLocalX, regionLocalZ);
                        }
                    }
                } finally {
                    reference.removeReference();
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
            } catch (RuntimeException ex) {
                AbstractCivModernMod.LOGGER.log(Level.WARN, "Rendering chunk", ex);
            }
        });
    }

    private void cacheEvict() {
        Iterator<Map.Entry<RegionKey, RegionReference>> iterator = cache.entrySet().iterator();
        Map<RegionKey, RegionLoader> toSave = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<RegionKey, RegionReference> entry = iterator.next();

            int px = Minecraft.getInstance().player.getBlockX();
            int pz = Minecraft.getInstance().player.getBlockZ();
            double dist = Mth.lengthSquared(entry.getKey().x() * 512 + 256 - px, entry.getKey().z() * 512 + 256 - pz);

            boolean far = dist > 96 * 16 * 96 * 16;

            RegionLoader loader = entry.getValue().getLoader();
            if (!entry.getValue().isReferenced()) {
                if (far) {
                    iterator.remove();
                }
            }
            if (entry.getValue().clearDirty()) {
                toSave.put(entry.getKey(), loader);
            }

            if (loader == null || far) {
                nearbyRegions.remove(entry.getKey());
            }
        }
        if (toSave.isEmpty()) {
            return;
        }
        this.mapFile.saveBlockIds(this.blockLookup.getNames());
        this.mapFile.saveBiomeIds(this.biomeLookup.getNames());
        this.mapFile.saveBulk(toSave, executor);
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
        this.addReference(region);
        queue.add(region);
        executor.submit(() -> {
            RegionKey poll = queue.poll();
            if (poll == null) {
                return;
            }
            RegionReference reference = Objects.requireNonNull(cache.get(poll));
            try {
                // TODO fully get rid of banding, this is only a partial solution
                RegionRenderer renderer = new RegionRenderer(reference.getLoader(), blockLookup, biomeLookup);
                renderer.render(this.textureCache.get(new RegionKey(poll.x() >> ATLAS_BITS, poll.z() >> ATLAS_BITS)), poll.x() & ATLAS_LENGTH - 1, poll.z() & ATLAS_LENGTH - 1);
            } finally {
                reference.removeReference();
            }
        });
    }

    public void save() {
        // todo save not just on gui close but on world unload or 60 second interval
        CountDownLatch latch = new CountDownLatch(1);
        executor.submit(() -> {
            try {
                Map<RegionKey, RegionLoader> toSave = new HashMap<>();
                for (Map.Entry<RegionKey, RegionReference> cacheEntry : this.cache.entrySet()) {
                    RegionReference loader = cacheEntry.getValue();
                    RegionLoader cached = loader.getLoader();
                    if (loader.clearDirty()) {
                        toSave.put(cacheEntry.getKey(), cached);
                    }
                }
                this.mapFile.saveBlockIds(this.blockLookup.getNames());
                this.mapFile.saveBiomeIds(this.biomeLookup.getNames());
                this.mapFile.saveBulk(toSave, executor);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executor.close();
    }

    public void free() {
        for (RegionAtlasTexture atlas : this.textureCache.values()) {
            atlas.delete();
        }
    }
}
