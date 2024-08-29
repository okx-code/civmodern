package sh.okx.civmodern.common;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.gui.screen.MainConfigScreen;
import sh.okx.civmodern.common.macro.AttackMacro;
import sh.okx.civmodern.common.macro.HoldKeyMacro;
import sh.okx.civmodern.common.macro.IceRoadMacro;
import sh.okx.civmodern.common.radar.Radar;

public abstract class AbstractCivModernMod {

    private static AbstractCivModernMod INSTANCE;
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
        registerKeyBinding(attackBinding);
        registerKeyBinding(this.iceRoadBinding);
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

    public abstract void registerKeyBinding(KeyMapping mapping);

    /**
     * Even though this will almost certainly be `.minecraft/config/` for all mod loaders, it's best practice
     * nonetheless to use what the mod loader gives you.
     */
    protected abstract @NotNull File getConfigFolder();

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        while (configBinding.consumeClick()) {
            Minecraft.getInstance().setScreen(new MainConfigScreen(this, config));
        }
    }

    private void loadConfig() {
        final File configDir = getConfigFolder();
        final var configFile = new File(configDir, "civmodern.properties");
        InputStream configReadStream;
        try {
            configReadStream = new FileInputStream(configFile);
        }
        catch (final FileNotFoundException ignored) {
            final byte[] raw;
            try (final InputStream defaultConfigResource = AbstractCivModernMod.class.getResourceAsStream("/civmodern.properties")) {
                raw = defaultConfigResource.readAllBytes(); // Ignore highlighter
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not read CivModern's default config resource!", e);
            }
            configDir.mkdirs(); // Just in case
            try {
                FileUtils.writeByteArrayToFile(configFile, raw);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not save CivModern's default config resource!", e);
            }
            configReadStream = new ByteArrayInputStream(raw);
        }
        final var properties = new Properties();
        try {
            properties.load(configReadStream);
        }
        catch (final IOException e) {
            throw new IllegalStateException("Could not parse CivModern's default config resource!", e);
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

    public static AbstractCivModernMod getInstance() {
        return INSTANCE;
    }
}
