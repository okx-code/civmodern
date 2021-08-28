package sh.okx.civmodern.common.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

public interface CommonFont {
  void draw(PoseStack poseStack, Component component, float x, float y, int colour);
  void drawShadow(PoseStack poseStack, Component component, float x, float y, int colour);
  void drawShadowCentred(PoseStack poseStack, Component component, float x, float y, int colour);
}
