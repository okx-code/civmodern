package sh.okx.civmodern.forge;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.EventBus;

public class ForgeCivModernMod extends AbstractCivModernMod {

  @Override
  public EventBus provideEventBus() {
    return new ForgeEventBus();
  }

  @Override
  public void registerKeyBinding(KeyMapping mapping) {
    ClientRegistry.registerKeyBinding(mapping);
  }
}
