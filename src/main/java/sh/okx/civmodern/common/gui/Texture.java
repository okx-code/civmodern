package sh.okx.civmodern.common.gui;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL12;

import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * OpenGL interface for a texture binding
 */
public class Texture {
    private final int id;
    private int width;
    private int height;
    private int[] pixels;

    public Texture(int id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
//
//        resize(width, height);
//
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8, this.pixels);
    }

    public void setPixels(int[] pixels) {
        this.pixels = pixels;
    }

    public void update() {
        GlStateManager._bindTexture(this.id);
        GlStateManager._pixelStore(GL_UNPACK_ALIGNMENT, 1);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._pixelStore(0xcf0, 0);
        GlStateManager._pixelStore(0xcf1, 0);
        GlStateManager._pixelStore(0xcf2, 0);
        GlStateManager._pixelStore(0xcf3, 0);
        GlStateManager._pixelStore(0xcf4, 0);
        GlStateManager._pixelStore(0xcf5, 4);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8, this.pixels);
        glGenerateMipmap(GL_TEXTURE_2D);
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
