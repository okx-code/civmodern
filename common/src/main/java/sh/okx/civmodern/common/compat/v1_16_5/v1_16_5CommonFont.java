package sh.okx.civmodern.common.compat.v1_16_5;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import sh.okx.civmodern.common.compat.CommonFont;

public class v1_16_5CommonFont implements CommonFont {

  private final Font font;

  public v1_16_5CommonFont(Font font) {
    this.font = font;
  }

  @Override
  public void draw(PoseStack poseStack, Component component, float x, float y, int colour) {
    font.draw(poseStack, component, x, y, colour);
  }

  @Override
  public void drawShadow(PoseStack poseStack, Component component, float x, float y, int colour) {
    font.drawShadow(poseStack, component, x, y, colour);
  }

  @Override
  public void drawShadowCentred(PoseStack poseStack, Component component, float x, float y,
      int colour) {
    font.drawShadow(poseStack, component, x - font.width(component) / 2f, y, colour);
  }
}