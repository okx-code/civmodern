package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.compat.CommonFont;
import sh.okx.civmodern.common.gui.widget.CyclicButton;

public class IceRoadConfigScreen extends Screen {
  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private final Screen parent;
  private CommonFont cFont;

  protected IceRoadConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
    super(new TranslatableComponent("civmodern.screen.ice.title"));
    this.mod = mod;
    this.config = config;
    this.parent = parent;
  }

  @Override
  protected void init() {
    this.cFont = mod.getCompat().provideFont(this.font);

    addButton(new CyclicButton(this.width / 2 - 75, this.height / 6 + 24, 150, 20, config.isIceRoadCardinalEnabled() ? 0 : 1, cycl -> {
      config.setIceRoadCardinalEnabled(cycl.getIndex() == 0);
    }, new TranslatableComponent("civmodern.screen.ice.cardinal.enable"), new TranslatableComponent("civmodern.screen.ice.cardinal.disable")));

    addButton(new Button(this.width / 2 - 49, this.height / 6 + 169, 98, 20, CommonComponents.GUI_DONE, button -> {
      Minecraft.getInstance().setScreen(parent);
    }));
  }

  @Override
  public void render(PoseStack poseStack, int i, int j, float f) {
    super.renderBackground(poseStack);
    super.render(poseStack, i, j, f);

    cFont.drawShadowCentred(poseStack, this.title, this.width / 2f, 40, 0xffffff);
  }

  @Override
  public void onClose() {
    super.onClose();
    config.save();
  }
}
