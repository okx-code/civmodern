package sh.okx.civmodern.mod.events;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

public record PostRenderGameOverlayEvent(
    @NotNull GuiGraphics guiGraphics,
    float deltaTick
) {
    public PostRenderGameOverlayEvent {
        Objects.requireNonNull(guiGraphics);
    }
}
