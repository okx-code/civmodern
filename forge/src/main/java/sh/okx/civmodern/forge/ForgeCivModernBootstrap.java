package sh.okx.civmodern.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  }
}
