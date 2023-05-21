package sh.okx.civmodern.common.map;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MapCache {
  private static final ExecutorService SAVE = Executors.newSingleThreadExecutor();

  private static final int ATLAS_LENGTH = RegionAtlasTexture.SIZE / RegionData.SIZE;
  private static final int ATLAS_BITS = Integer.numberOfTrailingZeros(RegionAtlasTexture.SIZE / RegionData.SIZE);

  private final Set<RegionKey> gettingAtlas = new HashSet<>();

  private final Map<RegionKey, RegionAtlasTexture> textureCache = new ConcurrentHashMap<>();
  // todo fix memory leak here if someone just goes to every region, maybe just clear it when >100 regions?
  private final Map<RegionKey, RegionData> cache = new ConcurrentHashMap<>();

  private final Set<RegionKey> dirtyRenderAtlases = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Set<RegionKey> dirtySaveRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final ExecutorService executor = Executors.newFixedThreadPool(Math.min(16, Runtime.getRuntime().availableProcessors()));

  private final MapFile mapFile;

  private final Set<RegionKey> availableRegions;

  public MapCache(MapFile mapFile) {
    this.mapFile = mapFile;
    this.availableRegions = mapFile.listRegions();
  }

  public void updateChunk(LevelChunk chunk) {
    int regionX = chunk.getPos().getRegionX();
    int regionZ = chunk.getPos().getRegionZ();
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
      if (addedAtlas) {
        for (int x = 0; x < ATLAS_LENGTH; x++) {
          for (int z = 0; z < ATLAS_LENGTH; z++) {
            if ((x != (regionX & 1 << ATLAS_BITS) || z != (regionZ & 1 << ATLAS_BITS))) {
              RegionData rgd = mapFile.getRegion(new RegionKey(atlas.x() << ATLAS_BITS | x, atlas.z() << ATLAS_BITS | z));
              if (rgd != null) {
                rgd.render(tex, x, z);
              }
            }
          }
        }
      }
      RegionData data = getData(region);
      if (data.updateChunk(chunk)) {
        dirtyRenderAtlases.add(atlas);
        dirtySaveRegions.add(region);
      }
    });
  }

  public RegionData getData(RegionKey key) {
    boolean[] created = new boolean[] {false};
    RegionData data = cache.computeIfAbsent(key, k -> {
      created[0] = true;
      RegionData region1 = mapFile.getRegion(k);
      return Objects.requireNonNullElseGet(region1, RegionData::new);
    });
    if (created[0]) {
      dirtyRenderAtlases.add(new RegionKey(key.x() >> ATLAS_BITS, key.z() >> ATLAS_BITS));
    }
    return data;
  }

  public RegionAtlasTexture getTexture(RegionKey atlas) {
    RegionAtlasTexture texture = this.textureCache.get(atlas);
    if (texture == null) {
      if (gettingAtlas.add(atlas)) {
        for (int x = 0; x < ATLAS_LENGTH; x++) {
          for (int z = 0; z < ATLAS_LENGTH; z++) {
            RegionKey region = new RegionKey(atlas.x() << ATLAS_BITS | x, atlas.z() << ATLAS_BITS | z);
            if (availableRegions.contains(region)) {
              int fx = x;
              int fz = z;
              RegionAtlasTexture newTexture = this.textureCache.computeIfAbsent(atlas, k -> {
                RegionAtlasTexture tex = new RegionAtlasTexture();
                tex.init();
                return tex;
              });
              executor.submit(() -> {
                RegionData data = mapFile.getRegion(region);
                if (data != null) {
                  data.render(newTexture, fx, fz);
                }
              });
            }
          }
        }
      }
      return null;
    } else if (dirtyRenderAtlases.remove(atlas)) {
      executor.submit(() -> {
        for (int x = 0; x < ATLAS_LENGTH; x++) {
          for (int z = 0; z < ATLAS_LENGTH; z++) {
            RegionData data = cache.get(new RegionKey(atlas.x() << ATLAS_BITS | x, atlas.z() << ATLAS_BITS | z));
            if (data != null) {
              data.render(texture, x, z);
            }
          }
        }
      });
    }

    // TODO reset texture on world unload
    return texture;
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
