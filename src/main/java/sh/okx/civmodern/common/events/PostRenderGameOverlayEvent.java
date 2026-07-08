package sh.okx.civmodern.common.events;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;

public record PostRenderGameOverlayEvent(
    @NotNull GuiGraphicsExtractor guiGraphics,
    float deltaTick
) {
    public PostRenderGameOverlayEvent {
        Objects.requireNonNull(guiGraphics);
    }
}
