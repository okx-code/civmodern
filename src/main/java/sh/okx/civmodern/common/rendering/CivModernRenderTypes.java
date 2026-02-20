package sh.okx.civmodern.common.rendering;

import net.minecraft.util.Util;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public class CivModernRenderTypes {
    public static final Function<Identifier, RenderType> TEXT = Util.memoize(
        resourceLocation -> RenderType.create(
            "text",
            RenderSetup.builder(CivModernPipelines.TEXT)
                .withTexture("Sampler0", resourceLocation)
                .useLightmap()
                .bufferSize(786432)
                .createRenderSetup()
        )
    );
}
