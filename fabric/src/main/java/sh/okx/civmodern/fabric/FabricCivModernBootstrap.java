package sh.okx.civmodern.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;

public class FabricCivModernBootstrap implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static FabricCivModernMod mod;

    public FabricCivModernBootstrap() {
        FabricCivModernBootstrap.mod = new FabricCivModernMod();
    }

    @Override
    public void onInitializeClient() {
        FabricCivModernBootstrap.mod.init();
        ClientLifecycleEvents.CLIENT_STARTED.register(e -> mod.enable());
        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            mod.eventBus.post(new ClientTickEvent());
        });
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            mod.eventBus.post(new PostRenderGameOverlayEvent(guiGraphics, tickDelta));
        });
    }

    public static FabricCivModernMod getMod() {
        return FabricCivModernBootstrap.mod;
    }
}
