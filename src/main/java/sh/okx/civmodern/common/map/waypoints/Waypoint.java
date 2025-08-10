package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import sh.okx.civmodern.common.rendering.CivModernRenderTypes;

public record Waypoint(String name, int x, int y, int z, String icon, int colour) {

    public void render(MultiBufferSource source, Matrix4f pose, int f, int k) {
        int colour = this.colour | k;
        VertexConsumer buffer = source.getBuffer(CivModernRenderTypes.TEXT.apply(this.resourceLocation()));
        buffer.addVertex(pose, -f, f, 0).setLight(0xff).setUv(0, 1).setColor(colour);
        buffer.addVertex(pose, f, f, 0).setLight(0xff).setUv(1, 1).setColor(colour);
        buffer.addVertex(pose, f, -f, 0).setLight(0xff).setUv(1, 0).setColor(colour);
        buffer.addVertex(pose, -f, -f, 0).setLight(0xff).setUv(0, 0).setColor(colour);
    }

    public void render2D(GuiGraphics guiGraphics) {
        render2D(guiGraphics, 0xff);
    }

    public void render2D(GuiGraphics guiGraphics, int transparency) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resourceLocation(), -8, -8, 0, 0, 16, 16, 16, 16, transparency << 24 | colour());
    }

    public ResourceLocation resourceLocation() {
        return ResourceLocation.fromNamespaceAndPath("civmodern", "map/" + this.icon + ".png");
    }
}
