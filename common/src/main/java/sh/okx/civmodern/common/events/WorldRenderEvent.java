package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;

public record WorldRenderEvent(PoseStack poseStack, float delta) implements Event {
}
