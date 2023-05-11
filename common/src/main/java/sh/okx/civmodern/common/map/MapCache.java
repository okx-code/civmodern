package sh.okx.civmodern.common.map;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.Map;

public class MapCache {

  private final Map<RegionKey, RegionTexture> textureCache = new HashMap<>();
  private final Map<RegionKey, RegionData> cache = new HashMap<>();

  public void updateChunk(LevelChunk chunk) {
    int regionX = chunk.getPos().getRegionX();
    int regionZ = chunk.getPos().getRegionZ();
    RegionKey region = new RegionKey(regionX, regionZ);

    RegionData data = cache.computeIfAbsent(region, k -> new RegionData());
    data.updateChunk(chunk);
    RegionTexture texture = textureCache.computeIfAbsent(region, k -> new RegionTexture());
  }

  public RegionTexture lol() {
    return textureCache.get(new RegionKey(0, 0));
  }
  public RegionData lol2() {
    return cache.get(new RegionKey(0, 0));
  }
}
