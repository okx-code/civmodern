package sh.okx.civmodern.common.map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class MapCache {
  private static final ExecutorService SAVE = Executors.newSingleThreadExecutor();

  private static final int ATLAS_LENGTH = RegionAtlasTexture.SIZE / RegionData.SIZE;
  private static final int ATLAS_BITS = Integer.numberOfTrailingZeros(RegionAtlasTexture.SIZE / RegionData.SIZE);

  private final Set<RegionKey> gettingAtlas = new HashSet<>();

  // maybe downsample texture atlases? rendering a full civmc map is >1GB of VRAM
  private final Map<RegionKey, RegionAtlasTexture> textureCache = new ConcurrentHashMap<>();
  // todo fix memory leak here if someone just goes to every region in one session
  // not technically a leak but it only gets cleared on world unload so it can get big
  private final Map<RegionKey, RegionData> cache = new ConcurrentHashMap<>();

  private final Set<RegionKey> dirtySaveRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final Queue<RegionKey> queue = new PriorityBlockingQueue<>(11, (r1, r2) -> {
    int px = Minecraft.getInstance().player.getBlockX();
    int pz = Minecraft.getInstance().player.getBlockZ();
    return Double.compare(
        Mth.lengthSquared(r1.x() * 512 + 256 - px, r1.z() * 512 + 256 - pz),
        Mth.lengthSquared(r2.x() * 512 + 256 - px, r2.z() * 512 + 256 - pz));
  });
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.min(16, Runtime.getRuntime().availableProcessors()) - 1);

  private final Map<ChunkPos, ScheduledFuture<?>> debounced = new ConcurrentHashMap<>();

  private final MapFile mapFile;

  private final Set<RegionKey> availableRegions;

  public MapCache(MapFile mapFile) {
    this.mapFile = mapFile;
    this.availableRegions = mapFile.listRegions();
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


    boolean addedAtlas = gettingAtlas.add(atlas);
    executor.submit(() -> {
      boolean[] created = new boolean[] {false};
      RegionData data = cache.computeIfAbsent(region, k -> {
        created[0] = true;
        RegionData region1 = mapFile.getRegion(k);
        return Objects.requireNonNullElseGet(region1, RegionData::new);
      });
      boolean updated = data.updateChunk(chunk);
      if (created[0]) {
        cacheEvict(cache);
        data.render(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1);
      }
      if (updated) {
        dirtySaveRegions.add(region);
        if (!created[0]) {

          int regionLocalX = pos.getRegionLocalX();
          int regionLocalZ = pos.getRegionLocalZ();
          // Delay chunk updates by 1 second, so we can capture all the changes during that time instead of just one.
          debounced.compute(pos, (k, scheduledFuture) -> {
            if (scheduledFuture == null || scheduledFuture.isDone()) {
              AtomicReference<ScheduledFuture<?>> ref = new AtomicReference<>();
              ref.set(executor.schedule(() -> {
                data.renderChunk(tex, region.x() & ATLAS_LENGTH - 1, region.z() & ATLAS_LENGTH - 1, regionLocalX, regionLocalZ);
                ScheduledFuture<?> scheduled = ref.get();
                if (scheduled != null) {
                  debounced.remove(pos, scheduled);
                }
              }, 100, TimeUnit.MILLISECONDS));
              return ref.get();
            } else {
              return scheduledFuture;
            }
          });
        }
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

  private void cacheEvict(Map<RegionKey, RegionData> cache) {
    Iterator<Map.Entry<RegionKey, RegionData>> iterator = cache.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<RegionKey, RegionData> entry = iterator.next();
      int px = Minecraft.getInstance().player.getBlockX();
      int pz = Minecraft.getInstance().player.getBlockZ();
      double dist = Mth.lengthSquared(entry.getKey().x() * 512 + 256 - px, entry.getKey().z() * 512 + 256 - pz);
      if (dist > 96 * 16 * 96 * 16) {
        iterator.remove();
      }
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

    // TODO reset texture on world unload
    return texture;
  }

  private void enqueue(RegionKey region) {
    queue.add(region);
    executor.submit(() -> {
      RegionKey poll = queue.poll();
      if (poll == null) {
        return;
      }
      RegionData data = mapFile.getRegion(poll);
      if (data != null) {
        data.render(this.textureCache.get(new RegionKey(poll.x() >> ATLAS_BITS, poll.z() >> ATLAS_BITS)), poll.x() & ATLAS_LENGTH - 1, poll.z() & ATLAS_LENGTH - 1);
      }
    });
  }

  public void save() {
    // todo save not just on gui close but on world unload or 60 second interval
    SAVE.submit(() -> {
      for (Iterator<RegionKey> iterator = dirtySaveRegions.iterator(); iterator.hasNext(); ) {
        RegionKey dirtySave = iterator.next();
        RegionData cached = this.cache.get(dirtySave);
        if (cached != null) {
          this.mapFile.save(dirtySave, cached);
          iterator.remove();
        }
      }
    });
    executor.shutdownNow();
    try {
      executor.awaitTermination(1000, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void free() {
    for (RegionAtlasTexture atlas : this.textureCache.values()) {
      atlas.delete();
    }
  }
}
