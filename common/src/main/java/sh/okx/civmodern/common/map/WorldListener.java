package sh.okx.civmodern.common.map;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.*;
import sh.okx.civmodern.common.map.converters.JourneymapConverter;
import sh.okx.civmodern.common.map.converters.VoxelMapConverter;
import sh.okx.civmodern.common.map.screen.ImportAvailable;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.StorageSourceAccessor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class WorldListener {

    private final CivMapConfig config;
    private final ColourProvider provider;

    private MapCache cache;
    private MapFolder file;
    private Minimap minimap;
    private Waypoints waypoints;
    private Thread converter = null;

    private long seed = -1;

    public WorldListener(CivMapConfig config, ColourProvider colourProvider) {
        this.config = config;
        this.provider = colourProvider;
    }

    @Subscribe
    public void onLoad(JoinEvent event) {
        var tempSeed = seed;
        this.onUnload(null);
        setSeed(tempSeed);

        if (seed == -1) {
            AbstractCivModernMod.LOGGER.warn("World seed is not set");
            return;
        }

        String type;
        String name;
        if (Minecraft.getInstance().isLocalServer()) {
            type = "sp";
            name = ((StorageSourceAccessor) Minecraft.getInstance().getSingleplayerServer()).getStorageSource().getLevelId();
        } else {
            type = "mp";
            name = Minecraft.getInstance().getCurrentServer().ip;
        }

        ClientLevel level = Minecraft.getInstance().level;
        String dimension = level.dimension().location().getPath();

        Path civmapFolder = Minecraft.getInstance().gameDirectory.toPath().resolve("civmap");
        File mapFile = civmapFolder.resolve(type).resolve(name.replace(":", "_")).resolve(dimension).resolve(String.valueOf(seed)).toFile();
        mapFile.mkdirs();

        this.file = new MapFolder(mapFile);
        this.waypoints = new Waypoints(this.file.getConnection()); // TODO: import waypoints

        ArrayList<String> importableMapMods = new ArrayList<>();

        if (file.getHistory().settings.enableImportPrompt) {
            VoxelMapConverter voxelMapConverter = new VoxelMapConverter(this.file, name, dimension, level.registryAccess());
            JourneymapConverter journeymapConverter = new JourneymapConverter(this.file, name, dimension, level.registryAccess());

            if (!voxelMapConverter.hasAlreadyConverted() && voxelMapConverter.filesAvailable()) {
                importableMapMods.add("VoxelMap");
            }
            if (!journeymapConverter.hasAlreadyConverted() && journeymapConverter.filesAvailable()) {
                importableMapMods.add("Journeymap");
            }

            // if there is something to import
            if (!importableMapMods.isEmpty()) {
                Minecraft.getInstance().setScreen(new ImportAvailable(importableMapMods.toArray(new String[0]), mod -> {
                    converter = new Thread(() -> {
                        try {
                            switch (mod) {
                                case "VoxelMap" -> voxelMapConverter.convert();
                                case "Journeymap" -> journeymapConverter.convert();
                                case "close" -> {
                                    return;
                                }
                                case "neverShowAgain" -> {
                                    file.getHistory().settings.enableImportPrompt = false;
                                    file.saveHistory();
                                    return;
                                }
                                default -> {
                                    AbstractCivModernMod.LOGGER.warn("Unknown mod for import: " + mod);
                                    return;
                                }
                            }

                            Minecraft.getInstance().getToastManager().addToast(new SystemToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                    Component.literal("Import Done"),
                                    Component.literal("CivMap has finished importing " + mod)
                            ));
                        } catch (RuntimeException ex) {
                            ex.printStackTrace();
                        } finally {
                            this.cache = new MapCache(this.file);
                            this.minimap = new Minimap(this.waypoints, this.cache, this.config, this.provider);
                        }
                    }, "Map converter");
                    converter.start();
                }));
            }
        }


        AbstractCivModernMod.LOGGER.info("No mods available for import, using existing map data");
        this.cache = new MapCache(this.file);
        this.minimap = new Minimap(this.waypoints, this.cache, this.config, this.provider);
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
        this.cache = null;
        if (this.waypoints != null) {
            this.waypoints.save();
        }
        this.waypoints = null;
        if (this.file != null) {
            this.file.close();
            this.file = null;
        }
        setSeed(-1);
    }

    @Subscribe
    public void onRespawn(RespawnEvent event) {
        this.onLoad(null);
    }

    public MapCache getCache() {
        return this.cache;
    }

    @Subscribe
    public void onChunkLoad(ChunkLoadEvent event) {
        if (this.cache != null && config.isMappingEnabled()) {
            this.cache.updateChunk(event.chunk());
        }
    }

    @Subscribe
    public void onChunkLoad(BlockStateChangeEvent event) {
        if (this.cache != null && config.isMappingEnabled()) {
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

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return this.seed;
    }
}
