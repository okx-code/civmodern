package sh.okx.civmodern.fabric;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import sh.okx.civmodern.common.AbstractCivModernMod;

public class FabricCivModernMod extends AbstractCivModernMod {
    @Override
    public void registerKeyBinding(KeyMapping mapping) {
        KeyMappingHelper.registerKeyMapping(mapping);
    }
}
