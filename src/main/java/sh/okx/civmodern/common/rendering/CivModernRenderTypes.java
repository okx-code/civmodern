package sh.okx.civmodern.common.rendering;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class CivModernRenderTypes {
    public static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
        resourceLocation -> RenderType.create(
            "text",
            786432,
            false,
            false,
            CivModernPipelines.TEXT,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false))
                .setLightmapState(RenderType.LIGHTMAP)
                .createCompositeState(false)
        )
    );
}
