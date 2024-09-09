package sh.okx.civmodern.mod.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int civmodern$accessor$getClickCount();

    @Accessor("clickCount")
    void civmodern$accessor$setClickCount(
        int clickCount
    );
    
    @Accessor("key")
    @NotNull InputConstants.Key civmodern$accessor$getKey();

    @Invoker("release")
    void civmodern$invoker$release();
}
