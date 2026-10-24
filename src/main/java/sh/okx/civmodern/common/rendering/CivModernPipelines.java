package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

public class CivModernPipelines {
    // Equivalent of the now-private RenderPipelines#MATRICES_PROJECTION_SNIPPET
    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).buildSnippet();

    // Equivalent of the now-private RenderPipelines#GUI_SNIPPET
    private static final RenderPipeline.Snippet GUI_SHAPE_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("minecraft", "snippet/gui"))
        .withVertexShader("core/gui")
        .withFragmentShader("core/gui")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .buildSnippet();

    // Equivalent of the now-private RenderPipelines#GUI_TEXTURED_SNIPPET
    private static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("minecraft", "snippet/gui_textured"))
        .withVertexShader("core/position_tex_color")
        .withFragmentShader("core/position_tex_color")
        .withSampler("Sampler0")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .buildSnippet();

    public static final RenderPipeline GUI_TRIANGLE_STRIP_BLEND = RenderPipeline.builder(GUI_SHAPE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("owo", "pipeline/gui_triangle_strip2"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
        .build();
    public static final RenderPipeline GUI_QUADS = RenderPipeline.builder(GUI_SHAPE_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/gui_quads2"))
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .build();

    public static final RenderPipeline TEXT2 = RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)))
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();
    public static final RenderPipeline TEXT = RenderPipeline.builder(GUI_TEXTURED_SNIPPET)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
        .withColorTargetState(new ColorTargetState(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)))
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();

    public static final RenderPipeline.Snippet COLOR_WRITE = RenderPipeline.builder()
        .withColorTargetState(new ColorTargetState(java.util.Optional.empty(), ColorTargetState.WRITE_RED | ColorTargetState.WRITE_GREEN | ColorTargetState.WRITE_BLUE))
        .withDepthStencilState(java.util.Optional.empty())
        .buildSnippet();
    public static final RenderPipeline.Snippet POSITION_TEX_COLOR_SHADER = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET).withLocation("cm/pipeline/position_tex_color").withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color").withSampler("Sampler0").buildSnippet();
    public static final RenderPipeline REGION_DEFAULT_RENDER_PIPELINE = RenderPipeline.builder(POSITION_TEX_COLOR_SHADER, COLOR_WRITE).withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/pos_tex_color")).withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)).withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withCull(false).build();

    public static void register() {
        // RenderPipelines#register is private in 26.1; pipelines are self-contained after build()
    }
}
