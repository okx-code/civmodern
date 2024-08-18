package sh.okx.civmodern.forge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.WorldRenderEvent;

@Mod("civmodern")
public class ForgeCivModernBootstrap {
    private static final Logger LOGGER = LogManager.getLogger();

    private final ForgeCivModernMod mod;

    public ForgeCivModernBootstrap() {
        this.mod = new ForgeCivModernMod();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        this.mod.init();
        this.mod.enable();

        // Register hook for Forge's native mod list
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, previousScreen) -> AbstractCivModernMod.getInstance().newConfigGui(previousScreen)
            )
        );
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            mod.eventBus.post(new ClientTickEvent());
        }
    }

    @SubscribeEvent
    public void onRender(ScreenEvent.Render event) {
        mod.eventBus.post(new PostRenderGameOverlayEvent(event.getGuiGraphics(), event.getPartialTick()));
    }
}
