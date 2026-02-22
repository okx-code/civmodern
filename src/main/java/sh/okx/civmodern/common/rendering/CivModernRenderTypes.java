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
                .createRenderSetup()
        )
    );

    public static final Function<Identifier, RenderType> TEXT2 = Util.memoize(
        resourceLocation -> RenderType.create(
            "text2",
            RenderSetup.builder(CivModernPipelines.TEXT2)
                .withTexture("Sampler0", resourceLocation)
                .createRenderSetup()
        )
    );
}
