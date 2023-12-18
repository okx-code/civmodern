package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.gui.widget.CyclicButton;

public class IceRoadConfigScreen extends Screen {
  private final AbstractCivModernMod mod;
  private final CivMapConfig config;
  private final Screen parent;

  protected IceRoadConfigScreen(AbstractCivModernMod mod, CivMapConfig config, Screen parent) {
    super(new TranslatableComponent("civmodern.screen.ice.title"));
    this.mod = mod;
    this.config = config;
    this.parent = parent;
  }

  @Override
  protected void init() {
    addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 24, 165, 20, config.iceRoadPitchCardinalEnabled() ? 0 : 1, cycl -> {
      config.setIceRoadPitchCardinalEnabled(cycl.getIndex() == 0);
    }, new TranslatableComponent("civmodern.screen.ice.cardinal.pitch.enable"), new TranslatableComponent("civmodern.screen.ice.cardinal.pitch.disable")));

    addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 48, 165, 20, config.iceRoadYawCardinalEnabled() ? 0 : 1, cycl -> {
      config.setIceRoadYawCardinalEnabled(cycl.getIndex() == 0);
    }, new TranslatableComponent("civmodern.screen.ice.cardinal.yaw.enable"), new TranslatableComponent("civmodern.screen.ice.cardinal.yaw.disable")));

    addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 72, 165, 20, config.isIceRoadAutoEat() ? 0 : 1, cycl -> {
      config.setIceRoadAutoEat(cycl.getIndex() == 0);
    }, new TranslatableComponent("civmodern.screen.ice.eat.enable"), new TranslatableComponent("civmodern.screen.ice.eat.disable")));

    addRenderableWidget(new CyclicButton(this.width / 2 - 85, this.height / 6 + 96, 165, 20, config.isIceRoadStop() ? 0 : 1, cycl -> {
      config.setIceRoadStop(cycl.getIndex() == 0);
    }, new TranslatableComponent("civmodern.screen.ice.stop.enable"), new TranslatableComponent("civmodern.screen.ice.stop.disable")));

    addRenderableWidget(new Button(this.width / 2 - 50, this.height / 6 + 169, 98, 20, CommonComponents.GUI_DONE, button -> {
      config.save();
      minecraft.setScreen(parent);
    }));
  }

  @Override
  public void render(PoseStack poseStack, int i, int j, float f) {
    super.renderBackground(poseStack);
    super.render(poseStack, i, j, f);

    font.drawShadow(poseStack, this.title, this.width / 2f - font.width(this.title) / 2f, 40, 0xffffff);
  }

  @Override
  public void onClose() {
    super.onClose();
    config.save();
  }
}
