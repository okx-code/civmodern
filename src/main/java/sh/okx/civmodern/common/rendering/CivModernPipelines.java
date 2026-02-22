package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthWrite(true)
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();
    public static final RenderPipeline TEXT = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA))
        .withDepthWrite(true)
        .withLocation(Identifier.fromNamespaceAndPath("civmodern", "pipeline/text"))
        .build();

    public static final RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).buildSnippet();
    public static final RenderPipeline.Snippet COLOR_WRITE = RenderPipeline.builder().withColorWrite(true).withDepthWrite(false).buildSnippet();
    public static final RenderPipeline.Snippet POSITION_TEX_COLOR_SHADER = RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET).withLocation("cm/pipeline/position_tex_color").withVertexShader("core/position_tex_color").withFragmentShader("core/position_tex_color").withSampler("Sampler0").buildSnippet();
    public static final RenderPipeline REGION_DEFAULT_RENDER_PIPELINE = RenderPipeline.builder(POSITION_TEX_COLOR_SHADER, COLOR_WRITE).withLocation("cm/pipeline/pos_tex_color").withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withBlend(BlendFunction.TRANSLUCENT).withCull(false).build();

    public static void register() {
        RenderPipelines.register(GUI_QUADS);
        RenderPipelines.register(TEXT);
        RenderPipelines.register(TEXT2);
        RenderPipelines.register(REGION_DEFAULT_RENDER_PIPELINE);
    }
}
