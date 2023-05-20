package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapCache {

  private final Set<RegionKey> getting = new HashSet<>();

  private final Map<RegionKey, RegionTexture> textureCache = new ConcurrentHashMap<>(); // region -> texture ~60ms EDIT: now 4ms
  private final Map<RegionKey, RegionData> cache = new ConcurrentHashMap<>(); // chunk -> data ~1ms EDIT: now 200us

  private final Set<RegionKey> dirtyRenderRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Set<RegionKey> dirtySaveRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
    this.textureCache.computeIfAbsent(region, k -> {
      RegionTexture texture1 = new RegionTexture();
      texture1.init();
      return texture1;
    });

    // Pretty sketchy in terms of race conditions but it works
    executor.submit(() -> {
      if (data.updateChunk(chunk)) {
        dirtyRenderRegions.add(region);
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
      dirtyRenderRegions.add(key);
    }
    return data;
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
              // don't add region data to cache - saves memory
              //this.cache.put(key, data);
            });
            data.render(newTexture);
          }
        });
      }
      return null;
    } else if (dirtyRenderRegions.remove(key)) {
      executor.submit(() -> {
        RegionData data = cache.get(key);
        if (data != null) {
          //System.out.println("dirty " + key);
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
    // todo save not just on gui close but on world unload or 60 second interval
    executor.submit(() -> {
      Map<RegionKey, RegionData> updated = new HashMap<>();
      for (Iterator<RegionKey> iterator = dirtySaveRegions.iterator(); iterator.hasNext(); ) {
        RegionKey dirtySave = iterator.next();
        RegionData cached = this.cache.get(dirtySave);
        if (cached != null) {
          updated.put(dirtySave, cached);
          iterator.remove();
        }
      }
      if (!updated.isEmpty()) {
        this.mapFile.save(updated);
      }
    });
  }
}
