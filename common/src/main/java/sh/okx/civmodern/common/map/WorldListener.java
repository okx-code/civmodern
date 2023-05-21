package sh.okx.civmodern.common.map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import sh.okx.civmodern.common.mixins.StorageSourceAccessor;

import java.io.File;
import java.nio.file.Path;

public class WorldListener {

  private MapCache cache;
  private MapFile file;
  private Thread converter;

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
        } catch (RuntimeException ex) {
          ex.printStackTrace();
        }
      }, "VoxelMap converter");
      converter.start();
    } else {
      converter = null;
      this.cache = new MapCache(this.file);
    }
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
    this.file = null;
    this.cache = null;
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
}
