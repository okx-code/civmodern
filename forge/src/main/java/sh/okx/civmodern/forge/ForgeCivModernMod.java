package sh.okx.civmodern.forge;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.EventBus;

public class ForgeCivModernMod extends AbstractCivModernMod {

  @Override
  public EventBus provideEventBus() {
    return new ForgeEventBus();
  }

  @Override
  public void registerKeyBinding(net.minecraft.client.KeyMapping mapping) {
    ClientRegistry.registerKeyBinding(mapping);
  }
}
