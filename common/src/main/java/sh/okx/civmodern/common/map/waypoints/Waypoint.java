package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public record Waypoint(String name, int x, int y, int z, String icon) {

	public void render(BufferBuilder buffer, Matrix4f last, int f) {
		RenderSystem.enableTexture();
		RenderSystem.setShaderTexture(0, new ResourceLocation("civmodern", "map/" + this.icon + ".png"));
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		int colour = 0xFFFF0000;
		buffer.vertex(last, -f, f, 0).uv(0, 1).color(colour).endVertex();
		buffer.vertex(last, f, f, 0).uv(1, 1).color(colour).endVertex();
		buffer.vertex(last, f, -f, 0).uv(1, 0).color(colour).endVertex();
		buffer.vertex(last, -f, -f, 0).uv(0, 0).color(colour).endVertex();
	}

	public void renderFocus(BufferBuilder buffer, Matrix4f last, int f) {
		RenderSystem.enableTexture();
		RenderSystem.setShaderTexture(0, new ResourceLocation("civmodern", "map/focus.png"));
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		int colour = 0xFFFFFF00;
		buffer.vertex(last, -f, f, 0).uv(0, 1).color(colour).endVertex();
		buffer.vertex(last, f, f, 0).uv(1, 1).color(colour).endVertex();
		buffer.vertex(last, f, -f, 0).uv(1, 0).color(colour).endVertex();
		buffer.vertex(last, -f, -f, 0).uv(0, 0).color(colour).endVertex();
	}
}
