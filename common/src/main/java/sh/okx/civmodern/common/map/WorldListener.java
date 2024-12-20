package sh.okx.civmodern.common.map;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.BlockStateChangeEvent;
import sh.okx.civmodern.common.events.ChunkLoadEvent;
import sh.okx.civmodern.common.events.JoinEvent;
import sh.okx.civmodern.common.events.LeaveEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.RespawnEvent;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.StorageSourceAccessor;

import java.io.File;
import java.nio.file.Path;

public class WorldListener {

    private final CivMapConfig config;
    private final ColourProvider provider;

    private MapCache cache;
    private MapFolder file;
    private Minimap minimap;
    private Waypoints waypoints;
    private Thread converter;

    public WorldListener(CivMapConfig config, ColourProvider colourProvider) {
        this.config = config;
        this.provider = colourProvider;
    }

    @Subscribe
    public void onLoad(JoinEvent event) {
        String type;
        String name;
        if (Minecraft.getInstance().isLocalServer()) {
            type = "c";
            name = ((StorageSourceAccessor) Minecraft.getInstance().getSingleplayerServer()).getStorageSource().getLevelId();
        } else {
            type = "s";
            name = Minecraft.getInstance().getCurrentServer().ip;
        }

        ClientLevel level = Minecraft.getInstance().level;
        String dimension = level.dimension().location().getPath();

        Path config = Minecraft.getInstance().gameDirectory.toPath().resolve("civmap");

        File mapFile = config.resolve(type).resolve(name.replace(":", "_")).resolve(dimension).toFile();
        mapFile.mkdirs();
        this.file = new MapFolder(mapFile);
        VoxelMapConverter voxelMapConverter = new VoxelMapConverter(this.file, name, dimension, level.registryAccess());
        if (!voxelMapConverter.voxelMapFileExists() && false) { // todo fix
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

    @Subscribe
    public void onUnload(LeaveEvent event) {
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

    @Subscribe
    public void onRespawn(RespawnEvent event) {
        this.onUnload(null);
        this.onLoad(null);
    }

    public MapCache getCache() {
        return this.cache;
    }

    @Subscribe
    public void onChunkLoad(ChunkLoadEvent event) {
        if (this.cache != null) {
            this.cache.updateChunk(event.chunk());
        }
    }

    @Subscribe
    public void onChunkLoad(BlockStateChangeEvent event) {
        if (this.cache != null) {
            this.cache.updateChunk(event.level().getChunkAt(event.pos()));
        }
    }

    @Subscribe
    public void onRender(PostRenderGameOverlayEvent event) {
        if (this.minimap != null) {
            this.minimap.onRender(event);
        }
    }

    @Subscribe
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
