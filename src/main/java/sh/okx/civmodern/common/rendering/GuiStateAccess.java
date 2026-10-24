package sh.okx.civmodern.common.rendering;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import sh.okx.civmodern.common.mixins.GuiGraphicsExtractorAccessor;

/**
 * GuiGraphicsExtractor no longer exposes its GuiRenderState in 26.1;
 * this reaches it through a mixin accessor so custom gui/pip render
 * states can still be submitted.
 */
public final class GuiStateAccess {
    private GuiStateAccess() {
    }

    public static GuiRenderState of(GuiGraphicsExtractor graphics) {
        return ((GuiGraphicsExtractorAccessor) graphics).civmodern$guiRenderState();
    }
}
