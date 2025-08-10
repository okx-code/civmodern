package uk.protonull.civianmod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.protonull.civianmod.CivianMod;
import uk.protonull.civianmod.events.BeforeHotbarSlotChangedEvent;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
	private int selected;

    @Inject(
        method = "setSelectedSlot",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Inventory;selected:I",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.BEFORE
        )
    )
    protected void civianmod$emitHotbarSlotChangedEvent(
        final @NotNull CallbackInfo ci,
        final @Local(argsOnly = true) int slot
    ) {
        final int previousSlot = this.selected;
        if (slot != previousSlot) {
            CivianMod.EVENTS.post(new BeforeHotbarSlotChangedEvent(
                previousSlot,
                slot
            ));
        }
    }
}
