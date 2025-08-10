package sh.okx.civmodern.common.rendering;


import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class RegionTileStateShard extends RenderStateShard.EmptyTextureStateShard {
    private final int textureId;

    public RegionTileStateShard(AbstractTexture texture) {
        super(() -> {
            GlStateManager._bindTexture(((GlTexture) (texture.getTexture())).glId());
            RenderSystem.setShaderTexture(0, texture.getTextureView());
        }, () -> {});
        this.textureId = ((GlTexture)texture.getTexture()).glId();
    }

    public String toString() {
        return "RegionTileStateShard[" + this.textureId + ")]";
    }
}
