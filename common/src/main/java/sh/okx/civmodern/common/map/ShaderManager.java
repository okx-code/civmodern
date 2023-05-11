package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ShaderManager {

    private static ShaderInstance positionTexMapShader;

    public static void registerShaders(ResourceProvider provider, List<Pair<ShaderInstance, Consumer<ShaderInstance>>> shaders) throws IOException {
        shaders.add(Pair.of(new FabricShaderProgram(provider, new ResourceLocation("civmodern", "position_tex_map"), DefaultVertexFormat.POSITION_TEX),
            shader -> positionTexMapShader = shader));
    }

    public static ShaderInstance getMapShader() {
        return positionTexMapShader;
    }

}
