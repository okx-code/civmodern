package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.MultiBufferSource;

public class BlitRenderer extends PictureInPictureRenderer<BlitRenderState> {

    public BlitRenderer(MultiBufferSource.BufferSource vertexConsumers) {
        super(vertexConsumers);
    }

    @Override
    public Class<BlitRenderState> getRenderStateClass() {
        return BlitRenderState.class;
    }

    @Override
    public void prepare(BlitRenderState pictureInPictureRenderState, GuiRenderState guiRenderState, int i) {
        super.prepare(pictureInPictureRenderState, guiRenderState, i);
    }

    @Override
    protected void renderToTexture(BlitRenderState pictureInPictureRenderState, PoseStack poseStack) {
        pictureInPictureRenderState.renderer().render(this.bufferSource, poseStack);
    }

    @Override
    protected String getTextureLabel() {
        return "civmodern-atlas";
    }
}
