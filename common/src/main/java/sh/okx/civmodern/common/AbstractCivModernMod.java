package sh.okx.civmodern.common;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
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
import sh.okx.civmodern.common.map.screen.MapScreen;
import sh.okx.civmodern.common.map.waypoints.WaypointTexture;
import sh.okx.civmodern.common.radar.Radar;

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

        // todo check if forge and fabric versions of these are the same
        this.eventBus.register(this.worlds);

        Options options = Minecraft.getInstance().options;
        this.leftMacro = new HoldKeyMacro(this, this.holdLeftBinding, options.keyAttack);
        this.rightMacro = new HoldKeyMacro(this, this.holdRightBinding, options.keyUse);
        this.iceRoadMacro = new IceRoadMacro(this, config, this.iceRoadBinding);
        this.attackMacro = new AttackMacro(this, this.attackBinding, options.keyAttack);

        this.boatNavigation = new BoatNavigation(this);
    }

    public abstract void registerKeyBinding(KeyMapping mapping);

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        while (this.configBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(newConfigGui(null));
        }
        while (mapBinding.consumeClick()) {
            if (worlds.getCache() != null) {
                Minecraft.getInstance().setScreen(new MapScreen(this, worlds.getCache(), boatNavigation, worlds.getWaypoints()));
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
        this.radar = new Radar(config, eventBus, colourProvider);
    }

    public ColourProvider getColourProvider() {
        return colourProvider;
    }

    public @NotNull Screen newConfigGui(
        final Screen previousScreen
    ) {
        return new MainConfigScreen(this.config, this.colourProvider, previousScreen);
    }

    public static AbstractCivModernMod getInstance() {
        return INSTANCE;
    }
}
