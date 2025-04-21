package uk.protonull.civianmod.mixins;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import uk.protonull.civianmod.features.macros.HoldForwardMacro;
import uk.protonull.civianmod.features.macros.ToggleSneakMacro;

@Mixin(KeyboardInput.class)
public abstract class HoldInputsMixin {
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

    @ModifyArg(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Input;<init>(ZZZZZZZ)V"
        ),
        index = 5
    )
    protected boolean civianmod$pretendShiftIsDownIfMacroEnabled(
        final boolean shift
    ) {
        return shift || ToggleSneakMacro.enabled;
    }
}
