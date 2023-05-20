package sh.okx.civmodern.common.map;

import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapCache {

  private final Set<RegionKey> getting = new HashSet<>(); // todo remove this map; when update chunk should render entire atlas
  private final Set<RegionKey> gettingAtlas = new HashSet<>();

  private final Map<RegionKey, RegionAtlasTexture> textureCache = new ConcurrentHashMap<>(); // region -> texture ~60ms EDIT: now 4ms
  private final Map<RegionKey, RegionData> cache = new ConcurrentHashMap<>(); // chunk -> data ~1ms EDIT: now 200us

  private final Set<RegionKey> dirtyRenderAltases = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final Set<RegionKey> dirtySaveRegions = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    RegionKey atlas = new RegionKey(regionX >> 3, regionZ >> 3);
    this.textureCache.computeIfAbsent(atlas, k -> {
      RegionAtlasTexture texture1 = new RegionAtlasTexture();
      texture1.init();
      return texture1;
    });


    getting.add(region);
    // Pretty sketchy in terms of race conditions but it works
    executor.submit(() -> {
      RegionData data = getData(region);
      if (data.updateChunk(chunk)) {
        dirtyRenderAltases.add(atlas);
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
      dirtyRenderAltases.add(new RegionKey(key.x() >> 3, key.z() >> 3));
    }
    return data;
  }

  public RegionAtlasTexture getTexture(RegionKey atlas) {
    RegionAtlasTexture texture = this.textureCache.get(atlas);
    if (texture == null) {
      if (gettingAtlas.add(atlas)) {
        for (int x = 0; x < 8; x++) {
          for (int z = 0; z < 8; z++) {
            RegionKey region = new RegionKey(atlas.x() << 3 | x, atlas.z() << 3 | z);
            if (availableRegions.contains(region) && getting.add(region)) {
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
    } else if (dirtyRenderAltases.remove(atlas)) {
      executor.submit(() -> {
        for (int x = 0; x < 8; x++) {
          for (int z = 0; z < 8; z++) {
            RegionData data = cache.get(new RegionKey(atlas.x() << 3 | x, atlas.z() << 3 | z));
            if (data != null) {
              //System.out.println("dirty " + key);
              // TODO debouncer 100ms
              data.render(texture, x, z);
            }
          }
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
