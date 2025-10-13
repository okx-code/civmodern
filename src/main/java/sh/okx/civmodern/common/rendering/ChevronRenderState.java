package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;


public record ChevronRenderState(
    RenderPipeline pipeline,
    Matrix3x2f pose,
    ScreenRectangle scissorArea,
    int colour
) implements GuiElementRenderState {
    @Override
    public void buildVertices(VertexConsumer vertices, float depth) {
        vertices.addVertexWith2DPose(pose, -1, -1.5f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, -1, -1f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 0, -0.5f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 0, 0f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 0, -0.5f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 1, -1f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 1, -1.5f, depth).setColor(colour);
        vertices.addVertexWith2DPose(pose, 1, -1.5f, depth).setColor(colour);
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle bounds() {
        return new ScreenRectangle(-2, -2, 4, 4).transformMaxBounds(this.pose);
    }
}
