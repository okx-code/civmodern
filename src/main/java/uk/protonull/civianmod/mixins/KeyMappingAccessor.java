package uk.protonull.civianmod.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int civianmod$getClickCount();

    @Accessor("clickCount")
    void civianmod$setClickCount(
        int clickCount
    );
    
    @Accessor("key")
    @NotNull InputConstants.Key civianmod$getKey();
}
