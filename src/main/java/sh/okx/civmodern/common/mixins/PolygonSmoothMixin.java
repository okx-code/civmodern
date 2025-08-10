package sh.okx.civmodern.common.mixins;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.rendering.CivModernPipelines;

import static org.lwjgl.opengl.GL11.*;

@Mixin(GlCommandEncoder.class)
public abstract class PolygonSmoothMixin {
    @Inject(method = "applyPipelineState(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V", at = @At(value = "TAIL"))
    public void modify(RenderPipeline renderPipeline, CallbackInfo info) {
        if (renderPipeline == CivModernPipelines.GUI_TRIANGLE_STRIP_BLEND || renderPipeline == CivModernPipelines.GUI_QUADS) {
            glEnable(GL_POLYGON_SMOOTH);
        } else {
            glDisable(GL_POLYGON_SMOOTH);
        }
    }
}
