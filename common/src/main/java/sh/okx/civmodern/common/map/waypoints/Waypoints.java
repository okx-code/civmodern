package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Waypoints {

  private Int2ObjectMap<Int2ObjectMap<Waypoint>> waypoints;

  public void addWaypoint(Waypoint waypoint) {
    this.waypoints.computeIfAbsent(waypoint.x(), k -> new Int2ObjectOpenHashMap<>())
        .put(waypoint.z(), waypoint);
  }

  public List<Waypoint> getWaypoints(int x, int y, int z, int distance) {
    List<Waypoint> nearbyWaypoints = new ArrayList<>();

    for (Int2ObjectMap.Entry<Int2ObjectMap<Waypoint>> xEntry : waypoints.int2ObjectEntrySet()) {
      int dx = Math.abs(xEntry.getIntKey() - x);
      if (dx > distance) {
        continue;
      }

      for (Int2ObjectMap.Entry<Waypoint> zEntry : xEntry.getValue().int2ObjectEntrySet()) {
        int dz = Math.abs(zEntry.getIntKey() - z);
        if (dz > distance) {
          continue;
        }

        if (dx * dx + dz * dz < distance * distance) {
          nearbyWaypoints.add(zEntry.getValue());
        }
      }
    }

    return nearbyWaypoints;
  }

  public void onRender(WorldRenderLastEvent event) {
    LocalPlayer player = Minecraft.getInstance().player;
//    List<Waypoint> nearbyWaypoints = waypoints.getWaypoints(player.getBlockX(), player.getBlockY(), player.getBlockZ(), 2000);
    List<Waypoint> nearbyWaypoints = List.of(new Waypoint(0, 100, 0));

    Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
    PoseStack matricies = new PoseStack();
    Vec3 pos = camera.getPosition();
    for (Waypoint waypoint : nearbyWaypoints) {
      matricies.pushPose();
      double x = waypoint.x() - pos.x;
      double y = waypoint.y() - pos.y;
      double z = waypoint.z() - pos.z;
      float distance = (float) Mth.length(x, y, z);
      float maxDistance = (Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4) - 2;
      float adjustedDistance = distance;
      if (distance > maxDistance) {
        x = x / distance * maxDistance;
        y = y / distance * maxDistance;
        z = z / distance * maxDistance;
        adjustedDistance = maxDistance;
      }
      matricies.translate(x, y, z);

      adjustedDistance = (adjustedDistance * 0.1f + 1) * 0.0266f;
      matricies.mulPose(Vector3f.YP.rotationDegrees(-camera.getYRot()));
      matricies.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
      matricies.scale(-adjustedDistance, -adjustedDistance, -adjustedDistance);


      Font font = Minecraft.getInstance().font;
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder buffer = tessellator.getBuilder();
//
      RenderSystem.depthFunc(GL_ALWAYS);
      MultiBufferSource.BufferSource source = MultiBufferSource.immediate(buffer);
      String str = "Waypoint";

      Matrix4f last = matricies.last().pose();
      font.drawInBatch(str, -font.width(str) / 2f, (float)0, 0xFFFFFFFF, false, last, source, true, 1056964608, 15728640);
      font.drawInBatch(str, -font.width(str) / 2f, (float)0, -1, false, last, source, false, 0, 15728640);
      tessellator.end();

      RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
      RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      RenderSystem.enableTexture();
      RenderSystem.setShaderTexture(0, new ResourceLocation("civmodern", "map/waypoint.png"));
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
//      RenderSystem.blendFuncSeparate(770, 771, 1, 771);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      int colour = 0xFFFF0000;
//      bufferBuilder.vertex(last, -16, 40, 0).uv(0, 1).color(colour).endVertex();
//      bufferBuilder.vertex(last, 16, 40, 0).uv(1, 1).color(colour).endVertex();
//      bufferBuilder.vertex(last, 16, 8, 0).uv(1, 0).color(colour).endVertex();
//      bufferBuilder.vertex(last, -16, 8, 0).uv(0, 0).color(colour).endVertex();
      matricies.translate(0, 20, 0);
      buffer.vertex(last, -10, 10, 0).uv(0, 1).color(colour).endVertex();
      buffer.vertex(last, 10, 10, 0).uv(1, 1).color(colour).endVertex();
      buffer.vertex(last, 10, -10, 0).uv(1, 0).color(colour).endVertex();
      buffer.vertex(last, -10, -10, 0).uv(0, 0).color(colour).endVertex();
      tessellator.end();
      RenderSystem.disableBlend();

      RenderSystem.depthFunc(GL_LEQUAL);

      matricies.popPose();
    }
  }
}
