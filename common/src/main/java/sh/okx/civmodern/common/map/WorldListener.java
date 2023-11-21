package sh.okx.civmodern.common.map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.StorageSourceAccessor;

import java.io.File;
import java.nio.file.Path;

public class WorldListener {

  private final CivMapConfig config;
  private final ColourProvider provider;

  private MapCache cache;
  private MapFile file;
  private Minimap minimap;
  private Waypoints waypoints;
  private Thread converter;

  public WorldListener(CivMapConfig config, ColourProvider colourProvider) {
    this.config = config;
    this.provider = colourProvider;
  }

  public void onLoad() {
    System.out.println("respawn");

    String type;
    String name;
    if (Minecraft.getInstance().isLocalServer()) {
      type = "c";
      name = ((StorageSourceAccessor) Minecraft.getInstance().getSingleplayerServer()).getStorageSource().getLevelId();
    } else {
      type = "s";
      name = Minecraft.getInstance().getCurrentServer().ip;
    }

    String dimension = Minecraft.getInstance().level.dimension().location().getPath();

    Path config = Minecraft.getInstance().gameDirectory.toPath().resolve("civmap");

    File mapFile = config.resolve(type).resolve(name.replace(":", "_")).resolve(dimension).toFile();
    mapFile.mkdirs();
    this.file = new MapFile(mapFile);
    VoxelMapConverter voxelMapConverter = new VoxelMapConverter(this.file, name, dimension);
    if (!voxelMapConverter.voxelMapFileExists()) {
      converter = new Thread(() -> {
        try {
          voxelMapConverter.convert();
          this.cache = new MapCache(this.file);
          this.minimap = new Minimap(this.cache, this.config, this.provider);
        } catch (RuntimeException ex) {
          ex.printStackTrace();
        }
      }, "VoxelMap converter");
      converter.start();
    } else {
      converter = null;
      this.cache = new MapCache(this.file);
      this.minimap = new Minimap(this.cache, this.config, this.provider);
    }
    this.waypoints = new Waypoints(mapFile);
  }

  public void onUnload() {
    System.out.println("unload");

    if (converter != null && converter.isAlive()) {
      converter.interrupt();
      try {
        converter.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    converter = null;
    if (this.cache != null) {
      this.cache.save();
      this.cache.free();
    }
    this.minimap = null;
    this.file = null;
    this.cache = null;
    if (this.waypoints != null) {
      this.waypoints.save();
    }
    this.waypoints = null;
  }

  public void onRespawn() {
    this.onUnload();
    this.onLoad();
  }

  public MapCache getCache() {
    return this.cache;
  }

  public void onChunkLoad(LevelChunk chunk) {
    if (this.cache != null) {
      this.cache.updateChunk(chunk);
    }
  }

  public void onRender(PostRenderGameOverlayEvent event) {
    if (this.minimap != null) {
      this.minimap.onRender(event);
    }
  }

  public void onRender(WorldRenderLastEvent event) {
    if (this.waypoints != null) {
      this.waypoints.onRender(event);
    }
  }

  public void cycleMinimapZoom() {
    if (this.minimap != null) {
      this.minimap.cycleZoom();
    }
  }

  public Waypoints getWaypoints() {
    return this.waypoints;
  }
}
