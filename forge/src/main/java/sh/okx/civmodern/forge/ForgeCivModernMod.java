package sh.okx.civmodern.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.ArrayUtils;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.EventBus;

public class ForgeCivModernMod extends AbstractCivModernMod {

    @Override
    public EventBus provideEventBus() {
        return new ForgeEventBus();
    }

    @Override
    public void registerKeyBinding(KeyMapping mapping) {
        ArrayUtils.add(Minecraft.getInstance().options.keyMappings, mapping);
    }
}
