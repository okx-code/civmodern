package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.netty.util.internal.ObjectUtil;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public record Waypoint(String name, int x, int y, int z, String icon, int colour) {

	public void render(VertexConsumer buffer, Matrix4f pose, int f, int k) {
		RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("civmodern", "map/" + this.icon + ".png"));
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		int colour = this.colour | k;
		buffer.addVertex(pose, -f, f, 0).setUv(0, 1).setColor(colour);
		buffer.addVertex(pose, f, f, 0).setUv(1, 1).setColor(colour);
		buffer.addVertex(pose, f, -f, 0).setUv(1, 0).setColor(colour);
		buffer.addVertex(pose, -f, -f, 0).setUv(0, 0).setColor(colour);
	}

    public ResourceLocation resourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("civmodern", "map/" + this.icon + ".png");
    }

	public void renderFocus(BufferBuilder buffer, Matrix4f pose, float f) {
		RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath("civmodern", "map/focus.png"));
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		int colour = 0xFFFFFF00;
		buffer.addVertex(pose, -f, f, 0).setUv(0, 1).setColor(colour);
		buffer.addVertex(pose, f, f, 0).setUv(1, 1).setColor(colour);
		buffer.addVertex(pose, f, -f, 0).setUv(1, 0).setColor(colour);
		buffer.addVertex(pose, -f, -f, 0).setUv(0, 0).setColor(colour);
	}
}
