package sh.okx.civmodern.common.macro;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ClientTickEvent;

public class HoldKeyMacro {

  private final KeyMapping holdBinding;
  private final KeyMapping defaultBinding;
  private boolean down = false;

  public HoldKeyMacro(AbstractCivModernMod mod, KeyMapping holdBinding, KeyMapping defaultBinding) {
    mod.getEventBus().listen(ClientTickEvent.class, e -> tick());
    this.holdBinding = holdBinding;
    this.defaultBinding = defaultBinding;
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) {
      return;
    }

    if (down && (mc.screen != null || !mc.mouseHandler.isMouseGrabbed())) {
      set(false);
      return;
    }

    for (KeyMapping hotbar : mc.options.keyHotbarSlots) {
      if (hotbar.isDown()) {
        set(false);
        return;
      }
    }

    while (holdBinding.consumeClick()) {
      if (this.down) {
        set(false);
      } else if (!mc.player.isUsingItem()) {
        set(true);
      }
    }
  }

  public void onScroll() {
    if (this.down) {
      set(false);
    }
  }

  private void set(boolean down) {
    this.down = down;
    this.defaultBinding.setDown(down);
  }
}
