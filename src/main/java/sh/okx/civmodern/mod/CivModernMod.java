package sh.okx.civmodern.mod;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.ApiStatus;
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

public final class CivModernMod {
    public static final EventBus EVENTS = new EventBus("CivModernEvents");

    private static final KeyMapping CONFIG_BINDING = new KeyMapping(
        "key.civmodern.config",
        Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        "category.civmodern"
    );
    private static final KeyMapping HOLD_LEFT_BINDING = new KeyMapping(
        "key.civmodern.left",
        Type.KEYSYM,
        GLFW.GLFW_KEY_MINUS,
        "category.civmodern"
    );
    private static final KeyMapping HOLD_RIGHT_BINDING = new KeyMapping(
        "key.civmodern.right",
        Type.KEYSYM,
        GLFW.GLFW_KEY_EQUAL,
        "category.civmodern"
    );
    private static final KeyMapping ICE_ROAD_BINDING = new KeyMapping(
        "key.civmodern.ice",
        Type.KEYSYM,
        GLFW.GLFW_KEY_BACKSPACE,
        "category.civmodern"
    );
    private static final KeyMapping ATTACK_BINDING = new KeyMapping(
        "key.civmodern.attack",
        Type.KEYSYM,
        GLFW.GLFW_KEY_0,
        "category.civmodern"
    );

    @ApiStatus.Internal
    public static void bootstrap() {
        KeyBindingHelper.registerKeyBinding(CONFIG_BINDING);
        KeyBindingHelper.registerKeyBinding(HOLD_LEFT_BINDING);
        KeyBindingHelper.registerKeyBinding(HOLD_RIGHT_BINDING);
        KeyBindingHelper.registerKeyBinding(ATTACK_BINDING);
        KeyBindingHelper.registerKeyBinding(ICE_ROAD_BINDING);

        ClientLifecycleEvents.CLIENT_STARTED.register((e) -> enable());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            EVENTS.post(new ClientTickEvent());
        });
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            EVENTS.post(new PostRenderGameOverlayEvent(guiGraphics, tickDelta.getGameTimeDeltaPartialTick(true)));
        });
    }

    private static CivMapConfig config;
    private static ColourProvider colourProvider;

    private static void enable() {
        loadConfig();

        colourProvider = new ColourProvider(config);
        EVENTS.register(new Radar(config, colourProvider));

        EVENTS.register(new Listener());

        final Options options = Minecraft.getInstance().options;
        EVENTS.register(new HoldKeyMacro(HOLD_LEFT_BINDING, options.keyAttack));
        EVENTS.register(new HoldKeyMacro(HOLD_RIGHT_BINDING, options.keyUse));
        EVENTS.register(new IceRoadMacro(config, ICE_ROAD_BINDING));
        EVENTS.register(new AttackMacro(ATTACK_BINDING, options.keyAttack));
    }

    private static final class Listener {
        @Subscribe
        private void tick(
            final @NotNull ClientTickEvent event
        ) {
            while (CONFIG_BINDING.consumeClick()) {
                Minecraft.getInstance().setScreen(newConfigGui(null));
            }
        }
    }

    private static void loadConfig() {
        final File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        final var configFile = new File(configDir, "civmodern.properties");
        InputStream configReadStream;
        try {
            configReadStream = new FileInputStream(configFile);
        }
        catch (final FileNotFoundException ignored) {
            final byte[] raw;
            try (final InputStream defaultConfigResource = CivModernMod.class.getResourceAsStream("/civmodern.properties")) {
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
        config = new CivMapConfig(configFile, properties);
    }

    public static @NotNull ColourProvider getColourProvider() {
        return colourProvider;
    }

    public static @NotNull Screen newConfigGui(
        final Screen previousScreen
    ) {
        return new MainConfigScreen(config);
    }
}
