package sh.okx.civmodern.mod.mixins;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.mod.CivModernMod;
import sh.okx.civmodern.mod.events.ScrollEvent;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Inject(at = @At("HEAD"), method = "swapPaint(D)V")
    private void swapPaint(double direction, CallbackInfo info) {
        CivModernMod.EVENTS.post(new ScrollEvent(direction > 0));
    }
}
