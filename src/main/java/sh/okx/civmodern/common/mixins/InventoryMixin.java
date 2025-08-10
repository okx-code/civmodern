package sh.okx.civmodern.common.mixins;

import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ScrollEvent;

@Mixin(Inventory.class)
public class InventoryMixin {
  @Shadow
  public int selected;

  @Inject(method = "setSelectedSlot", at = @At("HEAD"))
  protected void setSelectedHotbarSlot(int slot, CallbackInfo info) {
    if (slot != this.selected) {
      AbstractCivModernMod.getInstance().eventBus.post(new ScrollEvent(this.selected == (slot + 1) % Inventory.getSelectionSize()));
    }
  }
}
