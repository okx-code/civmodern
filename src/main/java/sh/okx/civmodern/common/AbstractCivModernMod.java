package sh.okx.civmodern.common;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.common.boat.BoatNavigation;
import sh.okx.civmodern.common.events.*;
import sh.okx.civmodern.common.gui.screen.MainConfigScreen;
import sh.okx.civmodern.common.macro.AttackMacro;
import sh.okx.civmodern.common.macro.HoldKeyMacro;
import sh.okx.civmodern.common.macro.IceRoadMacro;
import sh.okx.civmodern.common.map.*;
//import sh.okx.civmodern.common.map.screen.MapScreen;
import sh.okx.civmodern.common.map.screen.MapScreen;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.parser.ParsedWaypoint;
import sh.okx.civmodern.common.radar.Radar;
import sh.okx.civmodern.common.rendering.BlitRenderState;
import sh.okx.civmodern.common.rendering.BlitRenderer;
import sh.okx.civmodern.common.rendering.CivModernPipelines;
import sh.okx.civmodern.common.rendering.RegionTileStateShard;

public abstract class AbstractCivModernMod {

    private static AbstractCivModernMod INSTANCE;
    public static final Logger LOGGER = LogManager.getLogger();

    private final KeyMapping configBinding;
    private final KeyMapping holdLeftBinding;
    private final KeyMapping holdRightBinding;
    private final KeyMapping iceRoadBinding;
    private final KeyMapping attackBinding;

    private final KeyMapping mapBinding;
    private final KeyMapping minimapZoomBinding;

    private CivMapConfig config;
    private ColourProvider colourProvider;
    private Radar radar;

    private HoldKeyMacro leftMacro;
    private HoldKeyMacro rightMacro;
    private IceRoadMacro iceRoadMacro;
    private AttackMacro attackMacro;

    private WorldListener worlds;
    private BoatNavigation boatNavigation;

    public final EventBus eventBus = new EventBus("CivModernEvents");

    public AbstractCivModernMod() {
        this.configBinding = new KeyMapping(
            "key.civmodern.config",
            Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.civmodern"
        );
        this.holdLeftBinding = new KeyMapping(
            "key.civmodern.left",
            Type.KEYSYM,
            GLFW.GLFW_KEY_MINUS,
            "category.civmodern"
        );
        this.holdRightBinding = new KeyMapping(
            "key.civmodern.right",
            Type.KEYSYM,
            GLFW.GLFW_KEY_EQUAL,
            "category.civmodern"
        );
        this.iceRoadBinding = new KeyMapping(
            "key.civmodern.ice",
            Type.KEYSYM,
            GLFW.GLFW_KEY_BACKSPACE,
            "category.civmodern"
        );
        this.attackBinding = new KeyMapping(
            "key.civmodern.attack",
            Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            "category.civmodern"
        );
        this.mapBinding = new KeyMapping(
            "key.civmodern.map",
            Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.civmodern"
        );
        this.minimapZoomBinding = new KeyMapping(
            "key.civmodern.minimapzoom",
            Type.KEYSYM,
            GLFW.GLFW_KEY_KP_DIVIDE,
            "category.civmodern"
        );


        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("AbstractCivModernMod initialised twice");
        }
    }

    public final void init() {
        SpecialGuiElementRegistry.register(ctx -> new BlitRenderer(ctx.vertexConsumers()));
        CivModernPipelines.register();

        registerKeyBinding(this.configBinding);
        registerKeyBinding(this.holdLeftBinding);
        registerKeyBinding(this.holdRightBinding);
        registerKeyBinding(this.attackBinding);
        registerKeyBinding(this.iceRoadBinding);
        registerKeyBinding(this.mapBinding);
        registerKeyBinding(this.minimapZoomBinding);
    }

    public final void enable() {
        loadConfig();
        loadRadar();

        this.worlds = new WorldListener(config, colourProvider);

        this.eventBus.register(this);

        this.eventBus.register(this.worlds);

        this.eventBus.register(this.radar);

        Options options = Minecraft.getInstance().options;
        this.leftMacro = new HoldKeyMacro(this, this.holdLeftBinding, options.keyAttack);
        this.rightMacro = new HoldKeyMacro(this, this.holdRightBinding, options.keyUse);
        this.iceRoadMacro = new IceRoadMacro(this, config, this.iceRoadBinding);
        this.attackMacro = new AttackMacro(this, this.attackBinding, options.keyAttack);

        this.boatNavigation = new BoatNavigation(this);
    }

    public abstract void registerKeyBinding(KeyMapping mapping);

    @Subscribe
    private void registerCommands(CommandRegistration registration) {
        registration.dispatcher().register(LiteralArgumentBuilder.<ClientSuggestionProvider>literal("civmodern_openwaypoint").then(RequiredArgumentBuilder.<ClientSuggestionProvider, String>argument("data", StringArgumentType.greedyString()).executes(context -> {
            ParsedWaypoint parsed = ParsedWaypoint.parseWaypoints(StringArgumentType.getString(context, "data")).getFirst();
            if (parsed == null) {
                return 0;
            }
            Waypoint waypoint = new Waypoint(
                parsed.name(),
                parsed.x(),
                parsed.y(),
                parsed.z(),
                "target",
                0xFF0000
            );
            if (!Screen.hasControlDown()) {
                this.worlds.getWaypoints().setTarget(waypoint);
            } else {
                MapScreen screen = new MapScreen(this, this.mapBinding, config, worlds.getCache(), boatNavigation, worlds.getWaypoints(), worlds.getPlayerWaypoints());
                screen.setNewWaypoint(waypoint);
                Minecraft.getInstance().setScreen(screen);
            }
            return 0;
        })));
    }

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        while (this.configBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(newConfigGui(null));
        }
        while (mapBinding.consumeClick()) {
            if (worlds.getCache() != null) {
                Minecraft.getInstance().setScreen(new MapScreen(this, this.mapBinding, config, worlds.getCache(), boatNavigation, worlds.getWaypoints(), worlds.getPlayerWaypoints()));
            }
        }
        while (minimapZoomBinding.consumeClick()) {
            worlds.cycleMinimapZoom();
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        File configFile = gameDir.resolve("config").resolve("civmodern.properties").toFile();
        try {
            if (!configFile.exists()) {
                InputStream resource = AbstractCivModernMod.class
                    .getResourceAsStream("/civmodern.properties");
                byte[] buffer = new byte[resource.available()];
                resource.read(buffer);
                FileOutputStream fos = new FileOutputStream(configFile);
                fos.write(buffer);
            }

            FileInputStream input = new FileInputStream(configFile);
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.config = new CivMapConfig(configFile, properties);

    }

    private void loadRadar() {
        this.colourProvider = new ColourProvider(config);
        this.radar = new Radar(config, colourProvider);
    }

    public ColourProvider getColourProvider() {
        return colourProvider;
    }

    public @NotNull Screen newConfigGui(
        final Screen previousScreen
    ) {
        return new MainConfigScreen(this.config, this.colourProvider, previousScreen);
    }

    public WorldListener getWorldListener() {
        return worlds;
    }

    public CivMapConfig getConfig() {
        return config;
    }

    public static AbstractCivModernMod getInstance() {
        return INSTANCE;
    }
}
