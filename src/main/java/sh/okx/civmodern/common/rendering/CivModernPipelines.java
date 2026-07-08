package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class CivModernPipelines {
    public static final RenderPipeline GUI_TRIANGLE_STRIP_BLEND = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("owo", "pipeline/gui_triangle_strip2"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
        .build());
    public static final RenderPipeline GUI_QUADS = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/gui_quads2"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .build();

    public static final RenderPipeline TEXT2 = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)))
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();
    public static final RenderPipeline TEXT = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)))
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();

    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .buildSnippet();

    public static final RenderPipeline.Snippet COLOR_WRITE = RenderPipeline.builder()
        .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
        .withDepthStencilState(Optional.empty())
        .buildSnippet();

    public static final RenderPipeline.Snippet POSITION_TEX_COLOR_SHADER = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation("cm/pipeline/position_tex_color")
        .withVertexShader("core/position_tex_color")
        .withFragmentShader("core/position_tex_color")
        .withSampler("Sampler0")
        .buildSnippet();

    public static final RenderPipeline REGION_DEFAULT_RENDER_PIPELINE = RenderPipeline.builder(POSITION_TEX_COLOR_SHADER, COLOR_WRITE)
        .withLocation("cm/pipeline/pos_tex_color")
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build();

    public static void register() {
        RenderPipelines.register(GUI_QUADS);
        RenderPipelines.register(TEXT);
        RenderPipelines.register(TEXT2);
        RenderPipelines.register(REGION_DEFAULT_RENDER_PIPELINE);
    }
}
