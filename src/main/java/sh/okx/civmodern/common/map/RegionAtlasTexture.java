package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import sh.okx.civmodern.common.rendering.BlitRenderState;
import sh.okx.civmodern.common.rendering.CivModernPipelines;
import sh.okx.civmodern.common.rendering.RegionAbstractTexture;
import sh.okx.civmodern.common.rendering.RegionTileStateShard;

import java.util.OptionalDouble;

import static org.lwjgl.opengl.GL44.*;

public class RegionAtlasTexture {
    public static final int SIZE = 4096;

    private int indexTexture;
    private RenderType type;
    private RegionAbstractTexture texture;

    public int getIndexTexture() {
        return indexTexture;
    }

    public void init() {
        if (this.indexTexture != 0) {
            return;
        }
        this.indexTexture = GlStateManager._genTexture();

        GlStateManager._bindTexture(this.indexTexture);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        clear();
    }

    public void clear() {
        GlStateManager._bindTexture(this.indexTexture);
        GlStateManager._pixelStore(GL_UNPACK_SWAP_BYTES, 0);
        GlStateManager._pixelStore(GL_UNPACK_LSB_FIRST, 0);
        GlStateManager._pixelStore(GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, 2);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB565, SIZE, SIZE, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, (short[]) null);
        glClearTexImage(this.indexTexture, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, new short[]{0}); // black
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public void update(short[] colours, int x, int z, int minX, int maxX, int minZ, int maxZ) {
        glBindTexture(GL_TEXTURE_2D, this.indexTexture);
        glPixelStorei(GL_UNPACK_SWAP_BYTES, 0);
        glPixelStorei(GL_UNPACK_LSB_FIRST, 0);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 2);
        glTexSubImage2D(GL_TEXTURE_2D, 0, x * 512 + minX, z * 512 + minZ, maxX - minX, maxZ - minZ, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, colours);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public BlitRenderState.Renderer draw(GuiGraphics graphics, float x, float y, float scale) {
        return draw(graphics, x, y, false, scale, 0, 0, SIZE, SIZE, SIZE, SIZE, 0, 0);
    }

    public BlitRenderState.Renderer drawLinear(GuiGraphics graphics, float x, float y, float scale, float xOff, float yOff, float xSize, float ySize, float width, float height, int translateX, int translateY) {
        return draw(graphics, x, y, true, scale, xOff, yOff, xSize, ySize, width, height, translateX, translateY);
    }

    private BlitRenderState.Renderer draw(GuiGraphics graphics, float x, float y, boolean linear, float scale, float xOff, float yOff, float xSize, float ySize, float width, float height, int translateX, int translateY) {
        return blit(graphics, (x / scale), (y / scale), linear, xOff / scale, yOff / scale, (width / scale), (height / scale), xSize / scale, ySize / scale, translateX, translateY);
    }

    public void delete() {
        RenderQueue.queue(() -> {
            GlStateManager._deleteTexture(this.indexTexture);
        });
    }

    private BlitRenderState.Renderer blit(GuiGraphics graphics, float renderX, float renderY, boolean linear, float textureXoffset, float texureYoffset, float renderWidth, float renderHeight, float textureWidth, float textureHeight, int translateX, int translateY) {
        return innerBlit(graphics, renderX, renderX + renderWidth, renderY, renderY + renderHeight, linear, renderWidth, renderHeight, textureXoffset, texureYoffset, textureWidth, textureHeight, translateX, translateY);
    }

    private BlitRenderState.Renderer innerBlit(GuiGraphics graphics, float i, float j, float k, float l, boolean linear, float n, float o, float f, float g, float p, float q, int translateX, int translateY) {
        return innerBlit(graphics, i, j, k, l, linear, (f + 0.0f) / p, (f + n) / p, (g + 0.0f) / q, (g + o) / q, translateX, translateY);
    }

    private BlitRenderState.Renderer innerBlit(GuiGraphics graphics, float i, float j, float k, float l, boolean linear, float f, float g, float h, float n, int translateX, int translateY) {
        return (source, stack) -> {
            if (type == null) {
                this.texture = new RegionAbstractTexture(this);
                this.texture.bindRegionTexture();
                type = RenderType.create("region_tile" + this.indexTexture, 4, false, false, CivModernPipelines.REGION_DEFAULT_RENDER_PIPELINE,
                    RenderType.CompositeState.builder()
                        .setTextureState(new RegionTileStateShard(this.texture))
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                        .setLayeringState(RenderStateShard.NO_LAYERING)
                        .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                        .createCompositeState(false));
            }
            VertexConsumer bufferBuilder = source.getBuffer(this.type);
            stack.pushPose();
            stack.setIdentity();
            int v = Minecraft.getInstance().getWindow().getGuiScale();
            this.texture.getTexture().setTextureFilter(linear ? FilterMode.LINEAR : FilterMode.NEAREST, FilterMode.NEAREST, true);
            this.texture.getTexture().setAddressMode(AddressMode.CLAMP_TO_EDGE);
            stack.scale(v, v, 1);
            stack.translate(translateX, translateY, 0);
            bufferBuilder.addVertex(stack.last(), i, l, 0).setUv(f, n).setColor(0xffffffff).setLight(0xff);
            bufferBuilder.addVertex(stack.last(), j, l, 0).setUv(g, n).setColor(0xffffffff).setLight(0xff);
            bufferBuilder.addVertex(stack.last(), j, k, 0).setUv(g, h).setColor(0xffffffff).setLight(0xff);
            bufferBuilder.addVertex(stack.last(), i, k, 0).setUv(f, h).setColor(0xffffffff).setLight(0xff);
            stack.popPose();
        };
    }
}
