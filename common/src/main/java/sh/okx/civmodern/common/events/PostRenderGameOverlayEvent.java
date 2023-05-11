package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;

public record PostRenderGameOverlayEvent(PoseStack poseStack, float delta) implements Event {
}
