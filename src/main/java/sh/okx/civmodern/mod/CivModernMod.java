package sh.okx.civmodern.mod;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.mod.config.CivModernConfig;
import sh.okx.civmodern.mod.events.ClientTickEvent;
import sh.okx.civmodern.mod.events.EventBus;
import sh.okx.civmodern.mod.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.mod.macro.AttackMacro;
import sh.okx.civmodern.mod.macro.HoldForwardMacro;
import sh.okx.civmodern.mod.macro.HoldKeyMacro;
import sh.okx.civmodern.mod.macro.IceRoadMacro;
import sh.okx.civmodern.mod.macro.ToggleSneakMacro;

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
    private static final KeyMapping HOLD_FORWARD_BINDING = new KeyMapping(
        "key.civmodern.autorun",
        Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_BRACKET,
        "category.civmodern"
    );
    private static final KeyMapping HOLD_SNEAK_BINDING = new KeyMapping(
        "key.civmodern.holdsneak",
        Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_BRACKET,
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
        KeyBindingHelper.registerKeyBinding(HOLD_FORWARD_BINDING);
        KeyBindingHelper.registerKeyBinding(HOLD_SNEAK_BINDING);
        KeyBindingHelper.registerKeyBinding(ATTACK_BINDING);
        KeyBindingHelper.registerKeyBinding(ICE_ROAD_BINDING);

        ClientLifecycleEvents.CLIENT_STARTED.register(CivModernMod::enable);
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            EVENTS.post(new ClientTickEvent());
        });
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            EVENTS.post(new PostRenderGameOverlayEvent(guiGraphics, tickDelta.getGameTimeDeltaPartialTick(true)));
        });
    }

    private static void enable(
        final @NotNull Minecraft minecraft
    ) {
        CivModernConfig.HANDLER.load();
        CivModernConfig.apply(CivModernConfig.HANDLER.instance());

        EVENTS.register(new Listener());

        EVENTS.register(new HoldKeyMacro(HOLD_LEFT_BINDING, minecraft.options.keyAttack));
        EVENTS.register(new HoldKeyMacro(HOLD_RIGHT_BINDING, minecraft.options.keyUse));
        EVENTS.register(new HoldForwardMacro(minecraft, HOLD_FORWARD_BINDING));
        EVENTS.register(new ToggleSneakMacro(minecraft, HOLD_SNEAK_BINDING));
        EVENTS.register(new IceRoadMacro(ICE_ROAD_BINDING));
        EVENTS.register(new AttackMacro(ATTACK_BINDING, minecraft.options.keyAttack));
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

    public static @NotNull Screen newConfigGui(
        final Screen previousScreen
    ) {
        return CivModernConfig.generateScreenGenerator(CivModernConfig.HANDLER.instance()).generateScreen(previousScreen);
    }
}
