package sh.okx.civmodern.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.events.ChatReceivedEvent;
import sh.okx.civmodern.common.events.ChunkLoadEvent;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.CommandRegistration;
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
        HudRenderCallback.EVENT.register(((matrixStack, tickDelta) -> mod.eventBus.post(new PostRenderGameOverlayEvent(matrixStack, tickDelta.getGameTimeDeltaPartialTick(true)))));
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> mod.eventBus.post(new ChunkLoadEvent(level, chunk)));
        WorldRenderEvents.LAST.register(context -> {
            mod.eventBus.post(new WorldRenderLastEvent(context.matrixStack(), context.consumers(), context.tickCounter().getGameTimeDeltaPartialTick(true)));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> mod.eventBus.post(new CommandRegistration((CommandDispatcher<ClientSuggestionProvider>) (CommandDispatcher<?>) dispatcher, registryAccess)));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                mod.eventBus.post(new ChatReceivedEvent(message));
            }
        });
    }

    public static FabricCivModernMod getMod() {
        return FabricCivModernBootstrap.mod;
    }
}
