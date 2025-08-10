package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;

import static org.lwjgl.opengl.GL11.*;

public class RegionGlTexture extends GlTexture {
    public RegionGlTexture(int i, String string, TextureFormat textureFormat, int j, int k, int l, int m, int n) {
        super(i, string, textureFormat, j, k, l, m, n);
    }

    @Override
    public void flushModeChanges(int i) {
        if (this.modesDirty) {
            GlStateManager._texParameter(i, 10242, GlConst.toGl(this.addressModeU));
            GlStateManager._texParameter(i, 10243, GlConst.toGl(this.addressModeV));
            switch (this.minFilter) {
                case NEAREST:
                    GlStateManager._texParameter(i, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
                    break;
                case LINEAR:
                    GlStateManager._texParameter(i, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            }
            GlStateManager._texParameter(i, GL_TEXTURE_MAG_FILTER, GL_NEAREST);


            this.modesDirty = false;
        }
    }
}
