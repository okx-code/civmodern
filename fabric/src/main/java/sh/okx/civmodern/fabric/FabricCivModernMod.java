package sh.okx.civmodern.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.EventBus;

public class FabricCivModernMod extends AbstractCivModernMod {

  @Override
  public EventBus provideEventBus() {
    return new FabricEventBus();
  }

  @Override
  public void registerKeyBinding(KeyMapping mapping) {
    KeyBindingHelper.registerKeyBinding(mapping);
  }
}
