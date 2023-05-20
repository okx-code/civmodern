package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;

import static org.lwjgl.opengl.GL33.*;

public class RegionAtlasTexture {
    public static final int SIZE = 4096;
    private static final short[] BLACK = new short[SIZE * SIZE];

    private int indexTexture;

    public void init() {
        this.indexTexture = TextureUtil.generateTextureId();

        RenderSystem.bindTextureForSetup(this.indexTexture);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        clear();
    }

    private void clear() {
        RenderSystem.bindTexture(this.indexTexture);
        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, SIZE, SIZE, 0, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, BLACK);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public void update(short[] colours, int x, int z) {
        RenderSystem.bindTexture(this.indexTexture);
        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);
        glTexSubImage2D(GL_TEXTURE_2D, 0, x * 512, z * 512, 512, 512, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, colours);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public void draw(PoseStack poseStack, float x, float y, float scale) {
        RenderSystem.setShaderTexture(0, this.indexTexture);
        RenderSystem.bindTexture(this.indexTexture);
        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);

        blit(poseStack, x / scale, y / scale, 0, 0, 0, SIZE / scale, SIZE / scale, SIZE / scale, SIZE / scale);
    }

    public void delete() {
        RenderSystem.deleteTexture(this.indexTexture);
    }

    private static void blit(PoseStack poseStack, float renderX, float renderY, int z, float textureXoffset, float texureYoffset, float renderWidth, float renderHeight, float textureWidth, float textureHeight) {

        innerBlit(poseStack, renderX, renderX + renderWidth, renderY, renderY + renderHeight, z, renderWidth, renderHeight, textureXoffset, texureYoffset, textureWidth, textureHeight);
    }
    private  static void innerBlit(PoseStack poseStack, float i, float j, float k, float l, int m, float n, float o, float f, float g, float p, float q) {
        innerBlit(poseStack.last().pose(), i, j, k, l, m, (f + 0.0f) / p, (f + n) / p, (g + 0.0f) / q, (g + o) / q);
    }
    private static void innerBlit(Matrix4f matrix4f, float i, float j, float k, float l, int m, float f, float g, float h, float n) {
//        RenderSystem.setShader(ShaderManager::getMapShader);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, i, l, m).uv(f, n).endVertex();
        bufferBuilder.vertex(matrix4f, j, l, m).uv(g, n).endVertex();
        bufferBuilder.vertex(matrix4f, j, k, m).uv(g, h).endVertex();
        bufferBuilder.vertex(matrix4f, i, k, m).uv(f, h).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }
}
