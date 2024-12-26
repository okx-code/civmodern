package sh.okx.civmodern.mod.mixins;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import sh.okx.civmodern.mod.features.macros.HoldForwardMacro;

@Mixin(KeyboardInput.class)
public abstract class HoldForwardMixin {
    @ModifyArg(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Input;<init>(ZZZZZZZ)V"
        ),
        index = 0
    )
    protected boolean civmodern$pretendForwardIsDownIfMacroEnabled(
        final boolean forward
    ) {
        return forward || HoldForwardMacro.enabled;
    }
}
