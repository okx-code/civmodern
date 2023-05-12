package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapCache {

  private static RegionTexture EMPTY_TEXTURE;

  private final Set<RegionKey> getting = new HashSet<>();

  private final Map<RegionKey, RegionTexture> textureCache = new ConcurrentHashMap<>(); // region -> texture ~60ms EDIT: now 4ms
  private final Map<RegionKey, RegionData> cache = new ConcurrentHashMap<>(); // chunk -> data ~1ms EDIT: now 200us

  private final Set<RegionKey> dirtyRenderRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());
  // TODO dirty save regions

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final MapFile mapFile;

  public MapCache(MapFile mapFile) {
    this.mapFile = mapFile;
  }

  public void updateChunk(LevelChunk chunk) {
    int regionX = chunk.getPos().getRegionX();
    int regionZ = chunk.getPos().getRegionZ();
    RegionKey region = new RegionKey(regionX, regionZ);

    RegionData data = getData(region);

    // TODO post process neighbouring north and west chunks (if loaded) to finish height shading
    RegionTexture texture = textureCache.computeIfAbsent(region, k -> {
      RegionTexture texture1 = new RegionTexture();
      texture1.init();
      return texture1;
    });

    // Pretty sketchy in terms of race conditions but it works
    executor.submit(() -> {
      data.updateChunk(chunk);
      dirtyRenderRegions.add(region);
    });
  }

  public RegionData getData(RegionKey key) { // todo fix bug here
    return cache.computeIfAbsent(key, k -> {
      RegionData region1 = mapFile.getRegion(k);
      return Objects.requireNonNullElseGet(region1, RegionData::new);
    });
  }

  public RegionTexture getTexture(RegionKey key) {
    RegionTexture texture = this.textureCache.get(key);
    if (texture == null) {
      if (getting.add(key)) {
        executor.submit(() -> {
          RegionData data = mapFile.getRegion(key);
          if (data != null) {
            RegionTexture newTexture = new RegionTexture();
            RenderSystem.recordRenderCall(() -> {
              newTexture.init();
              this.textureCache.put(key, newTexture);
              this.cache.put(key, data);
            });
            data.render(newTexture);
          }
        });
      }
      if (EMPTY_TEXTURE == null) {
        EMPTY_TEXTURE = new RegionTexture();
        EMPTY_TEXTURE.init();
      }
      return EMPTY_TEXTURE;
    } else if (dirtyRenderRegions.remove(key)) {
      executor.submit(() -> {
        RegionData data = getData(key);
        if (data != null) {
          // TODO debouncer 100ms
          data.render(texture);
        }
      });
    }

    // TODO texture pool
    // TODO reset texture on world unload
    return texture;
  }

  public void save() {
    this.mapFile.save(this.cache);
  }
}
