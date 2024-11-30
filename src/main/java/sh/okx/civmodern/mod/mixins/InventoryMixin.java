package sh.okx.civmodern.mod.mixins;

import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.mod.CivModernMod;
import sh.okx.civmodern.mod.events.HotbarSlotChangedEvent;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow
    public int selected;

    @Inject(
        method = "setSelectedHotbarSlot",
        at = @At("HEAD")
    )
    protected void civmodern$emitHotbarSlotChangedEvent(
        final int slot,
        final @NotNull CallbackInfo ci
    ) {
        final int previousSlot = this.selected;
        if (slot != previousSlot) {
            CivModernMod.EVENTS.post(new HotbarSlotChangedEvent(
                previousSlot,
                slot
            ));
        }
    }
}
