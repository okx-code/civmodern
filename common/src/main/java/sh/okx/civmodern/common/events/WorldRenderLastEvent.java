package sh.okx.civmodern.common.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

// todo rename to End
public record WorldRenderLastEvent(PoseStack stack, MultiBufferSource source, float tickDelta) implements Event {}
