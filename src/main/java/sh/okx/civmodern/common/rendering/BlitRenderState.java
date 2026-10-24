package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public record BlitRenderState(
    GuiGraphicsExtractor graphics,
    int x,
    int y,
    int sizeX,
    int sizeY,
    ScreenRectangle bounds,
	@Nullable ScreenRectangle scissorArea,
    Renderer renderer
) implements PictureInPictureRenderState {
	public BlitRenderState(
		GuiGraphicsExtractor graphics,
        int x,
        int y,
        int sizeX,
        int sizeY,
        Matrix3x2fStack pose,
        Renderer renderer
	) {
        // The scissor stack is no longer exposed by GuiGraphicsExtractor in 26.1,
        // so states are submitted without a scissor area (they were previously
        // only ever clipped by the active screen-level scissor).
		this(graphics, x, y, sizeX, sizeY, getBounds(null, pose, x, y, sizeX, sizeY), null, renderer);
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
