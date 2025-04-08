package uk.protonull.civianmod;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.events.StartOfClientTickEvent;
import uk.protonull.civianmod.events.EventBus;
import uk.protonull.civianmod.features.ClickRailDest;
import uk.protonull.civianmod.features.macros.AttackMacro;
import uk.protonull.civianmod.features.macros.HoldForwardMacro;
import uk.protonull.civianmod.features.macros.HoldKeyMacro;
import uk.protonull.civianmod.features.macros.IceRoadMacro;
import uk.protonull.civianmod.features.macros.ToggleSneakMacro;

public final class CivianMod {
    public static final Logger LOGGER = LoggerFactory.getLogger(CivianMod.class);
    public static final EventBus EVENTS = new EventBus("CivianModEvents");

    private static final KeyMapping CONFIG_BINDING = new KeyMapping(
        "key.civianmod.config",
        Type.KEYSYM,
        GLFW.GLFW_KEY_R,
        "category.civianmod"
    );
    private static final KeyMapping HOLD_LEFT_BINDING = new KeyMapping(
        "key.civianmod.left",
        Type.KEYSYM,
        GLFW.GLFW_KEY_MINUS,
        "category.civianmod"
    );
    private static final KeyMapping HOLD_RIGHT_BINDING = new KeyMapping(
        "key.civianmod.right",
        Type.KEYSYM,
        GLFW.GLFW_KEY_EQUAL,
        "category.civianmod"
    );
    private static final KeyMapping HOLD_FORWARD_BINDING = new KeyMapping(
        "key.civianmod.autorun",
        Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_BRACKET,
        "category.civianmod"
    );
    private static final KeyMapping HOLD_SNEAK_BINDING = new KeyMapping(
        "key.civianmod.holdsneak",
        Type.KEYSYM,
        GLFW.GLFW_KEY_RIGHT_BRACKET,
        "category.civianmod"
    );
    private static final KeyMapping ICE_ROAD_BINDING = new KeyMapping(
        "key.civianmod.ice",
        Type.KEYSYM,
        GLFW.GLFW_KEY_BACKSPACE,
        "category.civianmod"
    );
    private static final KeyMapping ATTACK_BINDING = new KeyMapping(
        "key.civianmod.attack",
        Type.KEYSYM,
        GLFW.GLFW_KEY_0,
        "category.civianmod"
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

        ClientLifecycleEvents.CLIENT_STARTED.register(CivianMod::enable);
        ClientTickEvents.START_CLIENT_TICK.register(EVENTS::emitStartOfClientTickEvent);
        AttackBlockCallback.EVENT.register(ClickRailDest::handleBlockClick);
    }

    private static void enable(
        final @NotNull Minecraft minecraft
    ) {
        CivianModConfig.migrate();
        CivianModConfig.HANDLER.load();
        CivianModConfig.HANDLER.instance().apply();

        EVENTS.register(new Object() {
            @Subscribe
            private void handleTickEvent(
                final @NotNull StartOfClientTickEvent event
            ) {
                while (CONFIG_BINDING.consumeClick()) {
                    event.minecraft().setScreen(newConfigGui(null));
                }
            }
        });

        EVENTS.register(new HoldKeyMacro(HOLD_LEFT_BINDING, minecraft.options.keyAttack));
        EVENTS.register(new HoldKeyMacro(HOLD_RIGHT_BINDING, minecraft.options.keyUse));
        EVENTS.register(new HoldForwardMacro(minecraft, HOLD_FORWARD_BINDING));
        EVENTS.register(new ToggleSneakMacro(minecraft, HOLD_SNEAK_BINDING));
        EVENTS.register(new IceRoadMacro(ICE_ROAD_BINDING));
        EVENTS.register(new AttackMacro(ATTACK_BINDING, minecraft.options.keyAttack));
    }

    public static @NotNull Screen newConfigGui(
        final Screen previousScreen
    ) {
        return CivianModConfig.generateScreenGenerator(CivianModConfig.HANDLER.instance()).generateScreen(previousScreen);
    }
}
