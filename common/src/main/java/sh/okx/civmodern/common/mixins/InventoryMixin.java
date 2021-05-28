package sh.okx.civmodern.common.mixins;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;

@Mixin(Inventory.class)
public class InventoryMixin {
  @Inject(at = @At("HEAD"), method = "swapPaint(D)V")
  private void swapPaint(CallbackInfo info) {
    AbstractCivModernMod.staticOnScroll();
  }
}
