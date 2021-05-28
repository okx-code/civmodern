package sh.okx.civmodern.common.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class ImageButton extends AbstractWidget {

  private final ResourceLocation image;
  private final OnPress onPress;

  public ImageButton(int x, int y, int width, int height, ResourceLocation image, OnPress onPress) {
    super(x, y, width, height, TextComponent.EMPTY);
    this.image = image;
    this.onPress = onPress;
  }

  @Override
  public void renderButton(PoseStack poseStack, int i, int j, float f) {
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.getTextureManager().bind(image);
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
    int k = this.isHovered() ? 1 : 0;
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();
    Gui.blit(poseStack, this.x, this.y, this.getBlitOffset(), 0, k * 20, this.width, this.height, 40, 20);
    this.renderBg(poseStack, minecraft, i, j);
  }

  @Override
  public void onClick(double d, double e) {
    this.onPress.onPress(this);
  }

  public interface OnPress {
    void onPress(ImageButton button);
  }
}
