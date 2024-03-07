package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record WorldRenderEvent(
    @NotNull PoseStack poseStack,
    float deltaTick
) {
    public WorldRenderEvent {
        Objects.requireNonNull(poseStack);
    }
}
