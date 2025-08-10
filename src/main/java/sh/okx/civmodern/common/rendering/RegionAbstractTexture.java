package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import sh.okx.civmodern.common.map.RegionAtlasTexture;

public class RegionAbstractTexture extends AbstractTexture {
    private final RegionAtlasTexture atlasTexture;

    public RegionAbstractTexture(RegionAtlasTexture atlasTexture) {
        this.atlasTexture = atlasTexture;
    }

    public void bindRegionTexture() {
        if (this.texture == null) {
            atlasTexture.init();
            this.texture = new RegionGlTexture(5, "region" + atlasTexture.getIndexTexture(), TextureFormat.RGBA8, RegionAtlasTexture.SIZE, RegionAtlasTexture.SIZE, 1, 4, atlasTexture.getIndexTexture());
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture, 0, 1);
        }
    }
}
