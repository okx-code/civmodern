package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;

public final class FabricShaderProgram extends ShaderInstance {
    public FabricShaderProgram(ResourceProvider factory, ResourceLocation name, VertexFormat format) throws IOException {
        super(factory, name.toString(), format);
    }

    /**
     * Rewrites the input string containing an identifier
     * with the namespace of the id in the front instead of in the middle.
     *
     * <p>Example: {@code shaders/core/my_mod:xyz} -> {@code my_mod:shaders/core/xyz}
     *
     * @param input       the raw input string
     * @param containedId the ID contained within the input string
     * @return the corrected full ID string
     */
    public static String rewriteAsId(String input, String containedId) {
        ResourceLocation contained = new ResourceLocation(containedId);
        return contained.getNamespace() + ResourceLocation.NAMESPACE_SEPARATOR + input.replace(containedId, contained.getPath());
    }
}