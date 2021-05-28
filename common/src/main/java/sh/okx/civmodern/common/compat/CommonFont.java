package sh.okx.civmodern.common.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

/**
 * For some reason the Font class changed some method signatures between 1.16.1 and 1.16.5
 */
public interface CommonFont {
  void draw(PoseStack poseStack, Component component, float x, float y, int colour);
  void drawShadow(PoseStack poseStack, Component component, float x, float y, int colour);
  void drawShadowCentred(PoseStack poseStack, Component component, float x, float y, int colour);
}
