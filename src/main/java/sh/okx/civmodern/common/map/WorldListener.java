package sh.okx.civmodern.common.map;

import com.google.common.eventbus.Subscribe;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.BlockStateChangeEvent;
import sh.okx.civmodern.common.events.ChatReceivedEvent;
import sh.okx.civmodern.common.events.ChunkLoadEvent;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.JoinEvent;
import sh.okx.civmodern.common.events.LeaveEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.RespawnEvent;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;
import sh.okx.civmodern.common.map.converters.JourneymapConverter;
import sh.okx.civmodern.common.map.converters.VoxelMapConverter;
import sh.okx.civmodern.common.map.screen.ImportAvailable;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoints;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.StorageSourceAccessor;

import java.io.File;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;

public class WorldListener {

    private final CivMapConfig config;
    private final ColourProvider provider;

    private MapCache cache;
    private MapFolder file;
    private Minimap minimap;
    private Waypoints waypoints;
    private Thread converter = null;

    private long seed = -1;
    private PlayerWaypoints playerWaypoints;

    private final List<ChunkPos> loadedChunks = new ArrayList<>();

    public WorldListener(CivMapConfig config, ColourProvider colourProvider) {
        this.config = config;
        this.provider = colourProvider;

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ResourceLocation.fromNamespaceAndPath("civmodern", "minimap"), (context, tickCounter) -> {
            if (this.minimap != null) {
                this.minimap.onRender(new PostRenderGameOverlayEvent(context, tickCounter.getGameTimeDeltaPartialTick(true)));
            }
        });
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
        File mapDirectory = civmapFolder.resolve(type).resolve(name.replace(":", "_")).resolve(dimension).resolve(String.valueOf(seed)).toFile();

        // attempt to migrate from old map folder structure
        String oldType = type.equals("sp") ? "c" : "s";
        File oldMapDirectory = civmapFolder.resolve(oldType).resolve(name.replace(":", "_")).resolve(dimension).toFile();
        if (!mapDirectory.exists() && oldMapDirectory.exists()) {
            AbstractCivModernMod.LOGGER.info("Migrating map folder from old structure: " + oldMapDirectory.getAbsolutePath() + " to " + mapDirectory.getAbsolutePath());

            // create root folder so files can be moved
            mapDirectory.mkdirs();

            // move sqlite file
            File oldSqliteFile = oldMapDirectory.toPath().resolve("map.sqlite").toFile();
            File newSqliteFile = mapDirectory.toPath().resolve("map.sqlite").toFile();
            boolean sqliteResult = oldSqliteFile.renameTo(newSqliteFile);
            if (!sqliteResult) {
                AbstractCivModernMod.LOGGER.warn("Failed to move sqlite file from " + oldSqliteFile.getAbsolutePath() + " to " + newSqliteFile.getAbsolutePath());
            } else {
                AbstractCivModernMod.LOGGER.info("Moved sqlite file from " + oldSqliteFile.getAbsolutePath() + " to " + newSqliteFile.getAbsolutePath());
            }

            AbstractCivModernMod.LOGGER.info("Finished migrating map folder from old structure");
        } else {
            // attempt to create root since migration would have created it otherwise
            mapDirectory.mkdirs();
        }

        this.file = new MapFolder(mapDirectory);
        this.waypoints = new Waypoints(this.file.getConnection());
        this.playerWaypoints = new PlayerWaypoints();

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
                        } finally {
                            Minecraft.getInstance().execute(() -> {
                                this.cache = new MapCache(this.file);
                                this.minimap = new Minimap(this.waypoints, this.playerWaypoints, this.cache, this.config, this.provider);

                                for (ChunkPos chunk : this.loadedChunks) {
                                    LevelChunk levelChunk = level.getChunk(chunk.x, chunk.z);
                                    if (levelChunk != null) {
                                        this.cache.updateChunk(levelChunk);
                                    }
                                }
                                this.loadedChunks.clear();
                            });
                        }
                    }, "Map converter");
                    converter.start();
                }));
                return;
            }
        }

        AbstractCivModernMod.LOGGER.info("No mods available for import, using existing map data");
        converter = null;
        this.cache = new MapCache(this.file);
        this.minimap = new Minimap(this.waypoints, this.playerWaypoints, this.cache, this.config, this.provider);
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
        this.playerWaypoints = null;
        this.waypoints = null;
        if (this.file != null) {
            this.file.close();
        }
        this.file = null;
        this.loadedChunks.clear();
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
        if (config.isMappingEnabled()) {
            if (this.cache != null) {
                this.cache.updateChunk(event.chunk());
            } else {
                this.loadedChunks.add(event.chunk().getPos());
            }
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
        RenderQueue.runAll();
    }

    @Subscribe
    public void onRender(WorldRenderLastEvent event) {
        if (this.waypoints != null) {
            this.waypoints.onRender(event);
        }
    }

    @Subscribe
    public void onTick(ClientTickEvent event) {
        if (this.playerWaypoints != null) {
            this.playerWaypoints.tick();
        }
    }

    @Subscribe
    public void onChat(ChatReceivedEvent event) {
        if (this.playerWaypoints != null) {
            this.playerWaypoints.acceptSnitchHit(event.message());
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

    public PlayerWaypoints getPlayerWaypoints() {
        return this.playerWaypoints;
    }
}
