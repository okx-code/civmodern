package sh.okx.civmodern.common.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sh.okx.civmodern.common.map.RegionAtlasTexture;
import sh.okx.civmodern.common.rendering.RegionAbstractTexture;

import java.util.HashMap;
import java.util.Map;

@Mixin(RenderSetup.class)
public abstract class RenderSetupMixin {
    @ModifyReturnValue(method = "getTextures", at = @At("RETURN"))
    public Map<String, RenderSetup.TextureAndSampler> modify(Map<String, RenderSetup.TextureAndSampler> map) {
        RenderSetup setup = (RenderSetup) (Object) this;
        RegionAbstractTexture tex = RegionAtlasTexture.TEXTURES.get(setup);
        if (tex == null) {
            return map;
        } else {
            Map<String, RenderSetup.TextureAndSampler> mut = new HashMap<>(map);
            boolean linear = RegionAtlasTexture.LINEAR.get(setup);
            GpuSampler sampler = linear
                ? RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.LINEAR, false)
                : RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, false);
            mut.put("Sampler0", new RenderSetup.TextureAndSampler(tex.getTextureView(), sampler));
            return mut;
        }
    }
}
