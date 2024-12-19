package sh.okx.civmodern.common.gui;

import static org.lwjgl.opengl.GL11.*;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * OpenGL interface for a texture binding
 */
public class Texture {
    private final int id;
    private int width;
    private int height;
    private int[] pixels;

    public Texture(int width, int height) {
        this.id = TextureUtil.generateTextureId();
        RenderSystem.pixelStore(GL_UNPACK_ALIGNMENT, 1);
        bind();

        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        resize(width, height);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8, this.pixels);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public void setPixels(int[] pixels) {
        if (Objects.requireNonNull(pixels).length != width * height) {
            throw new IllegalArgumentException("Pixels length array incorrect, should be " + (width * height) + ", is " + pixels.length);
        }
        this.pixels = pixels;
    }

    public void update() {
        bind();

        RenderSystem.pixelStore(0xcf0, 0);
        RenderSystem.pixelStore(0xcf1, 0);
        RenderSystem.pixelStore(0xcf2, 0);
        RenderSystem.pixelStore(0xcf3, 0);
        RenderSystem.pixelStore(0xcf4, 0);
        RenderSystem.pixelStore(0xcf5, 4);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8, this.pixels);
    }

    public void bind() {
        RenderSystem.setShaderTexture(0, id);
        RenderSystem.bindTexture(id);
    }

    public void unbind() {
        GlStateManager._bindTexture(0);
    }

    public void delete() {
        GlStateManager._deleteTexture(id);
    }

    public int getWidth() {
        return width;
    }

    public int getId() {
        return id;
    }
}
