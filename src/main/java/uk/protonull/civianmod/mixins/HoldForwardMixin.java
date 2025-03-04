package uk.protonull.civianmod.mixins;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import uk.protonull.civianmod.features.macros.HoldForwardMacro;

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
    protected boolean civianmod$pretendForwardIsDownIfMacroEnabled(
        final boolean forward
    ) {
        return forward || HoldForwardMacro.enabled;
    }
}
