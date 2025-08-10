package sh.okx.civmodern.common.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import sh.okx.civmodern.common.map.RegionAtlasTexture;

public record BlitRenderState(
    GuiGraphics graphics,
	@Nullable ScreenRectangle scissorArea,
    Renderer renderer
) implements PictureInPictureRenderState {
	public BlitRenderState(
		GuiGraphics graphics,
        Renderer renderer
	) {
		this(graphics, graphics.scissorStack.peek(), renderer);
	}

    @Override
    public int x0() {
        return 0;
    }

    @Override
    public int x1() {
        return RegionAtlasTexture.SIZE;
    }

    @Override
    public int y0() {
        return 0;
    }

    @Override
    public int y1() {
        return RegionAtlasTexture.SIZE;
    }

    @Override
    public float scale() {
        return 1;
    }

    @Override
    public ScreenRectangle bounds() {
        return PictureInPictureRenderState.getBounds(0, 0, RegionAtlasTexture.SIZE, RegionAtlasTexture.SIZE, null);
    }

    public interface Renderer {
        void render(MultiBufferSource.BufferSource source, PoseStack stack);
    }
}
