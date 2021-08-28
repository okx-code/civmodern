package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;

public class WorldRenderEvent implements Event {
  private final PoseStack poseStack;
  private final float delta;

  public WorldRenderEvent(PoseStack poseStack, float delta) {
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
