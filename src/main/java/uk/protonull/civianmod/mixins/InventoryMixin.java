package uk.protonull.civianmod.mixins;

import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.protonull.civianmod.CivianMod;
import uk.protonull.civianmod.events.HotbarSlotChangedEvent;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    public int selected;

    @Inject(
        method = "setSelectedHotbarSlot",
        at = @At("HEAD")
    )
    protected void civianmod$emitHotbarSlotChangedEvent(
        final int slot,
        final @NotNull CallbackInfo ci
    ) {
        final int previousSlot = this.selected;
        if (slot != previousSlot) {
            CivianMod.EVENTS.post(new HotbarSlotChangedEvent(
                previousSlot,
                slot
            ));
        }
    }
}
