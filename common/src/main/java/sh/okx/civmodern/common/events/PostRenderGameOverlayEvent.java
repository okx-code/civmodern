package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;

public class PostRenderGameOverlayEvent implements Event {
  private final PoseStack poseStack;
  private final float delta;

  public PostRenderGameOverlayEvent(PoseStack poseStack, float delta) {
    this.poseStack = poseStack;
    this.delta = delta;
  }

  public PoseStack getPoseStack() {
    return poseStack;
  }

  public float getDelta() {
    return delta;
  }
}
