package sh.okx.civmodern.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.events.ChunkLoadEvent;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.JoinEvent;
import sh.okx.civmodern.common.events.LeaveEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;

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
            mod.eventBus.post(new PostRenderGameOverlayEvent(guiGraphics, tickDelta.getGameTimeDeltaPartialTick(true)));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> mod.eventBus.post(new JoinEvent()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> mod.eventBus.post(new LeaveEvent()));
        ClientTickEvents.START_CLIENT_TICK.register(client -> mod.eventBus.post(new ClientTickEvent()));
        HudRenderCallback.EVENT.register(((matrixStack, tickDelta) -> mod.eventBus.post(new PostRenderGameOverlayEvent(matrixStack, tickDelta.getGameTimeDeltaPartialTick(true)))));
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> mod.eventBus.post(new ChunkLoadEvent(level, chunk)));
        WorldRenderEvents.LAST.register(context -> mod.eventBus.post(new WorldRenderLastEvent(context.matrixStack(), context.consumers(), context.tickCounter().getGameTimeDeltaPartialTick(true)))); // TODO forge
    }

    public static FabricCivModernMod getMod() {
        return FabricCivModernBootstrap.mod;
    }
}
