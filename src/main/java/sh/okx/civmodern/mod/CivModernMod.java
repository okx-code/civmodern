package sh.okx.civmodern.mod;

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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.mod.events.ClientTickEvent;
import sh.okx.civmodern.mod.events.EventBus;
import sh.okx.civmodern.mod.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.mod.gui.screen.MainConfigScreen;
import sh.okx.civmodern.mod.macro.AttackMacro;
import sh.okx.civmodern.mod.macro.HoldKeyMacro;
import sh.okx.civmodern.mod.macro.IceRoadMacro;
import sh.okx.civmodern.mod.radar.Radar;

public final class CivModernMod implements ClientModInitializer {

    private static CivModernMod INSTANCE;
    private static final Logger LOGGER = LogManager.getLogger();

    private final KeyMapping configBinding;
    private final KeyMapping holdLeftBinding;
    private final KeyMapping holdRightBinding;
    private final KeyMapping iceRoadBinding;
    private final KeyMapping attackBinding;
    private CivMapConfig config;
    private ColourProvider colourProvider;
    private Radar radar;

    private HoldKeyMacro leftMacro;
    private HoldKeyMacro rightMacro;
    private IceRoadMacro iceRoadMacro;
    private AttackMacro attackMacro;

    public final EventBus eventBus = new EventBus("CivModernEvents");

    public CivModernMod() {
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

        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("CivModernMod initialised twice");
        }
    }

    @Override
    public void onInitializeClient() {
        init();
        ClientLifecycleEvents.CLIENT_STARTED.register((e) -> enable());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            this.eventBus.post(new ClientTickEvent());
        });
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            this.eventBus.post(new PostRenderGameOverlayEvent(guiGraphics, tickDelta.getGameTimeDeltaPartialTick(true)));
        });
    }

    public final void init() {
        KeyBindingHelper.registerKeyBinding(this.configBinding);
        KeyBindingHelper.registerKeyBinding(this.holdLeftBinding);
        KeyBindingHelper.registerKeyBinding(this.holdRightBinding);
        KeyBindingHelper.registerKeyBinding(attackBinding);
        KeyBindingHelper.registerKeyBinding(this.iceRoadBinding);
    }

    public final void enable() {
        loadConfig();
        loadRadar();

        this.eventBus.register(this);

        Options options = Minecraft.getInstance().options;
        this.leftMacro = new HoldKeyMacro(this, this.holdLeftBinding, options.keyAttack);
        this.rightMacro = new HoldKeyMacro(this, this.holdRightBinding, options.keyUse);
        this.iceRoadMacro = new IceRoadMacro(this, config, this.iceRoadBinding);
        this.attackMacro = new AttackMacro(this, this.attackBinding, options.keyAttack);
    }

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        while (configBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(new MainConfigScreen(this, config));
        }
    }

    private void loadConfig() {
        Properties properties = new Properties();
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        File configFile = gameDir.resolve("config").resolve("civmodern.properties").toFile();
        try {
            if (!configFile.exists()) {
                InputStream resource = CivModernMod.class
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
        return new MainConfigScreen(this, this.config);
    }

    public static CivModernMod getInstance() {
        return INSTANCE;
    }
}
