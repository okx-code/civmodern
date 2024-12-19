package sh.okx.civmodern.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import sh.okx.civmodern.common.AbstractCivModernMod;

public class FabricCivModernMod extends AbstractCivModernMod {
    @Override
    public void registerKeyBinding(KeyMapping mapping) {
        KeyBindingHelper.registerKeyBinding(mapping);
    }
}
