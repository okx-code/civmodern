package sh.okx.civmodern.common.map;

import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapCache {

  private final Map<RegionKey, RegionTexture> textureCache = new HashMap<>(); // region -> texture ~60ms
  private final Map<RegionKey, RegionData> cache = new HashMap<>(); // chunk -> data ~1ms

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
    RegionTexture texture = textureCache.computeIfAbsent(region, k -> new RegionTexture());

    // Pretty sketchy in terms of race conditions but it works
    executor.submit(() -> {
      data.updateChunk(chunk);
      dirtyRenderRegions.add(region);
    });
  }

  public RegionData getData(RegionKey key) {
    return cache.computeIfAbsent(key, k -> {
      RegionData region1 = mapFile.getRegion(k);
      return Objects.requireNonNullElseGet(region1, RegionData::new);
    });
  }

  public RegionTexture getTexture(RegionKey key) {
    RegionTexture texture = this.textureCache.get(key);
    if (texture == null) {
      RegionData data = mapFile.getRegion(key);
      if (data != null) {
        RegionTexture newTexture = new RegionTexture();
        this.textureCache.put(key, newTexture);
        this.cache.put(key, data);
        executor.submit(() -> data.render(newTexture));
        texture = newTexture;
      }
    } else if (dirtyRenderRegions.remove(key)) {
      RegionTexture renderTexture = texture;
      executor.submit(() -> {
        RegionData data = getData(key);
        if (data != null) {
          // TODO debouncer 100ms
          data.render(renderTexture);
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
