package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import sh.okx.civmodern.common.map.RegionAtlasTexture;

public record BlitRenderState(
    GuiGraphics graphics,
    int x,
    int y,
    int sizeX,
    int sizeY,
    ScreenRectangle bounds,
	@Nullable ScreenRectangle scissorArea,
    Renderer renderer
) implements PictureInPictureRenderState {
	public BlitRenderState(
		GuiGraphics graphics,
        int x,
        int y,
        int sizeX,
        int sizeY,
        Matrix3x2fStack pose,
        Renderer renderer
	) {
		this(graphics, x, y, sizeX, sizeY, getBounds(graphics.scissorStack.peek(), pose, x, y, sizeX, sizeY), graphics.scissorStack.peek(), renderer);
	}

    @Override
    public int x0() {
        return 0;
    }

    @Override
    public int x1() {
        return sizeX;
    }

    @Override
    public int y0() {
        return y;
    }

    @Override
    public int y1() {
        return sizeY;
    }

    @Override
    public float scale() {
        return 1;
    }

    private static ScreenRectangle getBounds(ScreenRectangle scissorArea, Matrix3x2fStack stack, int x, int y, int sizeX, int sizeY) {
        ScreenRectangle bounds = new ScreenRectangle(x, y, sizeX - x, sizeY - y).transformMaxBounds(stack);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    public interface Renderer {
        void render(MultiBufferSource.BufferSource source, PoseStack stack);
    }
}
