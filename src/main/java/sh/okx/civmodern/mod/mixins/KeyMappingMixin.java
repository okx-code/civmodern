package sh.okx.civmodern.mod.mixins;

import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.mod.CivModernMod;
import sh.okx.civmodern.mod.events.AllKeysReleasedEvent;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(
        method = "releaseAll",
        at = @At("TAIL")
    )
    private static void civmodern$inject$releaseAll$emitEvent(
        final @NotNull CallbackInfo ci
    ) {
        CivModernMod.EVENTS.post(AllKeysReleasedEvent.INSTANCE);
    }

    @Inject(
        method = "setAll",
        at = @At("TAIL")
    )
    private static void civmodern$inject$setAll$emitEvent(
        final @NotNull CallbackInfo ci
    ) {
        CivModernMod.EVENTS.post(AllKeysReleasedEvent.INSTANCE);
    }
}
