package sh.okx.civmodern.common.map;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;

public class MapCache {

  private final Map<RegionKey, RegionTexture> textureCache = new HashMap<>(); // region -> texture ~60ms
  private final Map<RegionKey, RegionData> cache = new HashMap<>(); // chunk -> data ~1ms

  public void updateChunk(LevelChunk chunk) {
    int regionX = chunk.getPos().getRegionX();
    int regionZ = chunk.getPos().getRegionZ();
    RegionKey region = new RegionKey(regionX, regionZ);

    RegionData data = cache.computeIfAbsent(region, k -> new RegionData());
    long s = System.nanoTime();
    data.updateChunk(chunk);
    long n = System.nanoTime();
    System.out.println((n-s)/1000 + "us");

    // TODO post process neighbouring north and west chunks (if loaded) to finish height shading

    RegionTexture texture = textureCache.computeIfAbsent(region, k -> new RegionTexture());
  }

  public RegionTexture lol() {
    return textureCache.get(new RegionKey(0, 0));
  }
  public RegionData lol2() {
    return cache.get(new RegionKey(0, 0));
  }
}
