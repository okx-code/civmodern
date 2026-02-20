package sh.okx.civmodern.common.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageButton extends AbstractWidget {

    private Identifier image;
    private final OnPress onPress;

    public ImageButton(int x, int y, int width, int height, Identifier image, OnPress onPress) {
        super(x, y, width, height, Component.empty());
        this.image = image;
        this.onPress = onPress;
    }

    public void setImage(Identifier image) {
        this.image = image;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        int k = this.isHoveredOrFocused() ? 1 : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED,  image, this.getX(), this.getY(), 0, k * 20, this.width, this.height, 20, 40, -1);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean bl) {
        this.onPress.onPress(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public interface OnPress {
        void onPress(ImageButton button);
    }
}
