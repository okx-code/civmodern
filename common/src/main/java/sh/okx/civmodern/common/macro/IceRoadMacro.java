package sh.okx.civmodern.common.macro;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.events.ClientTickEvent;

public class IceRoadMacro {
  private final CivMapConfig config;
  private final KeyMapping key;
  // alternate between true and false for maximum jumpage
  private boolean enabled = false;
  private boolean jump = false;

  public IceRoadMacro(AbstractCivModernMod mod, CivMapConfig config, KeyMapping key) {
    // Options for ice road macro:
    // Auto cardinal
    // Auto eat

    mod.getEventBus().listen(ClientTickEvent.class, e -> tick());
    this.config = config;
    this.key = key;
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();

    while (this.key.consumeClick()) {
      if (enabled) {
        mc.options.keySprint.setDown(false);
        mc.options.keyUp.setDown(false);
        if (jump) {
          jump = false;
          mc.options.keyJump.setDown(false);
        }
        enabled = false;
      } else {
        if (config.isIceRoadCardinalEnabled()) {
          float rot = Math.round(mc.player.yRot / 45);
          mc.player.yRot = rot * 45;
        }

        enabled = true;
      }
    }

    if (enabled) {
      if (!jump) {
        mc.options.keyJump.setDown(true);
        jump = true;
      } else {
        mc.options.keyJump.setDown(false);
        jump = false;
      }
      mc.options.keySprint.setDown(true);
      mc.options.keyUp.setDown(true);
    }
  }
}
