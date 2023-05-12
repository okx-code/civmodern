package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.world.level.material.MaterialColor;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL33.*;

public class RegionTexture {
    private static final int SIZE = 512;

    private int indexTexture;

    private int[] colours = new int[SIZE * SIZE];

    public void init() {
        this.indexTexture = TextureUtil.generateTextureId();

//        for (int i = 0; i < 512; i++) {
//            for (int j = 0; j < 512; j++) {
//                this.colorIndexes[i * 512 + j] = MaterialColor.byId(ThreadLocalRandom.current().nextInt(62)).col << 8;
//            }
//        }

        RenderSystem.bindTextureForSetup(this.indexTexture);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        update();
    }

    public void update() {
        RenderSystem.bindTexture(this.indexTexture);
        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, colours);
    }

    public int[] getColours() {
        return colours;
    }

    public synchronized void draw(PoseStack poseStack, float x, float y) {
        RenderSystem.setShaderTexture(0, this.indexTexture);
        RenderSystem.bindTexture(this.indexTexture);
        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);

//        Matrix4f matrix4f = poseStack.last().pose();

        // TODO GL_TEXTURE0?
//        RenderSystem.setShader(ShaderManager::getMapShader);
//        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
//        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        bufferBuilder.vertex(matrix4f, (float)0, (float)0, (float)100).uv(0, 0).endVertex();
//        bufferBuilder.vertex(matrix4f, (float)100, (float)0, (float)100).uv(0, 1).endVertex();
//        bufferBuilder.vertex(matrix4f, (float)100, (float)0, (float)0).uv(1, 1).endVertex();
//        bufferBuilder.vertex(matrix4f, (float)0, (float)0, (float)0).uv(1, 0).endVertex();
//        bufferBuilder.end();
//        BufferUploader.end(bufferBuilder);

        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        blit(poseStack, x, y, 0, 0, 0, 512 / scale, 512 / scale, 512 / scale, 512 / scale);
    }

    public void delete() {
        RenderSystem.deleteTexture(this.indexTexture);
    }

    private static void blit(PoseStack poseStack, float renderX, float renderY, int z, float textureXoffset, float texureYoffset, int renderWidth, int renderHeight, int textureWidth, int textureHeight) {

        innerBlit(poseStack, renderX, renderX + renderWidth, renderY, renderY + renderHeight, z, renderWidth, renderHeight, textureXoffset, texureYoffset, textureWidth, textureHeight);
    }
    private static void innerBlit(PoseStack poseStack, float i, float j, float k, float l, int m, int n, int o, float f, float g, int p, int q) {
        innerBlit(poseStack.last().pose(), i, j, k, l, m, (f + 0.0f) / (float)p, (f + (float)n) / (float)p, (g + 0.0f) / (float)q, (g + (float)o) / (float)q);
    }
    private static void innerBlit(Matrix4f matrix4f, float i, float j, float k, float l, int m, float f, float g, float h, float n) {
        RenderSystem.setShader(ShaderManager::getMapShader);
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
