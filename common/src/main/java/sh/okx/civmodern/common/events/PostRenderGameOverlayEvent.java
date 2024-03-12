package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;

public class PostRenderGameOverlayEvent implements Event {
    private final GuiGraphics guiGraphics;
    private final float delta;

    public PostRenderGameOverlayEvent(GuiGraphics guiGraphics, float delta) {
        this.guiGraphics = guiGraphics;
        this.delta = delta;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public float getDelta() {
        return delta;
    }
}
