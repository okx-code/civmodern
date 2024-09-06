package sh.okx.civmodern.mod.mixins;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor
    int getClickCount();

    @Accessor("clickCount")
    void setClickCount(int clickCount);
}
