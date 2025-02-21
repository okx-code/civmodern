package sh.okx.civmodern.common.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageButton extends AbstractWidget {

    private final ResourceLocation image;
    private final OnPress onPress;

    public ImageButton(int x, int y, int width, int height, ResourceLocation image, OnPress onPress) {
        super(x, y, width, height, Component.empty());
        this.image = image;
        this.onPress = onPress;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        RenderSystem.setShaderTexture(0, image);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = this.isHoveredOrFocused() ? 1 : 0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        guiGraphics.blit(RenderType::guiTextured, image, this.getX(), this.getY(), 0, k * 20, this.width, this.height, 20, 40);
//    this.renderBg(poseStack, minecraft, i, j);
    }

    @Override
    public void onClick(double d, double e) {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public interface OnPress {
        void onPress(ImageButton button);
    }
}
