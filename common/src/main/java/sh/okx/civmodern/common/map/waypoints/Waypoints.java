package sh.okx.civmodern.common.map.waypoints;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sh.okx.civmodern.common.events.WorldRenderLastEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.opengl.GL11.*;

public class Waypoints {

  static {
    new WaypointTexture(new ResourceLocation("civmodern", "map/waypoint.png")).register();
    new WaypointTexture(new ResourceLocation("civmodern", "map/target.png")).register();
    new WaypointTexture(new ResourceLocation("civmodern", "map/focus.png")).register();
  }

  private final Int2ObjectMap<Int2ObjectMap<Waypoint>> waypoints = new Int2ObjectOpenHashMap<>();
  private Waypoint target;
  private final File waypointsFile;

  public Waypoints(File mapFile) {
    this.waypointsFile = new File(mapFile, "waypoints.txt");
    load();
  }

  private void load() {
    try {
      for (String line : Files.readAllLines(this.waypointsFile.toPath())) {
        String[] parts = line.split("\0");
        if (parts.length < 4) continue;

        String name = parts[0];
        int x;
        int y;
        int z;
        try {
          x = Integer.parseInt(parts[1]);
          y = Integer.parseInt(parts[2]);
          z = Integer.parseInt(parts[3]);
        } catch (NumberFormatException ex) {
          continue;
        }

        this.addWaypoint(new Waypoint(name, x, y, z, "waypoint"));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void save() {
    StringBuilder waypointString = new StringBuilder();
    for (Int2ObjectMap<Waypoint> zEntry : waypoints.values()) {
      for (Waypoint waypoint : zEntry.values()) {
        waypointString.append(waypoint.name()).append("\0")
            .append(waypoint.x()).append("\0")
            .append(waypoint.y()).append("\0")
            .append(waypoint.z()).append("\0")
            .append("\n");
      }
    }
    try {
      Files.writeString(this.waypointsFile.toPath(), waypointString);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void addWaypoint(Waypoint waypoint) {
    this.waypoints.computeIfAbsent(waypoint.x(), k -> new Int2ObjectOpenHashMap<>())
        .put(waypoint.z(), waypoint);
  }

  public Waypoint getTarget() {
    return target;
  }

  public void setTarget(Waypoint target) {
    this.target = target;
  }

  public List<Waypoint> getWaypoints() {
    List<Waypoint> list = new ArrayList<>();
    for (Int2ObjectMap<Waypoint> map : waypoints.values()) {
      list.addAll(map.values());
    }
    if (target != null) {
      list.add(target);
    }
    return list;
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

  public void onRender(WorldRenderLastEvent event) { // TODO separate this into its own class
    LocalPlayer player = Minecraft.getInstance().player;
    List<Waypoint> nearbyWaypoints = getWaypoints(player.getBlockX(), player.getBlockY(), player.getBlockZ(), 2000);
    if (getTarget() != null) {
      nearbyWaypoints.add(getTarget());
    }
    Collections.sort(nearbyWaypoints, (c, r) ->
        Integer.compare(((r.x() - player.getBlockX()) * (r.x() - player.getBlockX())) + ((r.z() - player.getBlockZ()) * (r.z() - player.getBlockZ())),
            ((c.x() - player.getBlockX()) * (c.x() - player.getBlockX())) + ((c.z() - player.getBlockZ()) * (c.z() - player.getBlockZ()))));

    Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
    PoseStack matrices = event.stack();//new PoseStack();
    Vec3 pos = camera.getPosition();
    Tesselator tessellator = Tesselator.getInstance();
    BufferBuilder buffer = tessellator.getBuilder();
    RenderSystem.depthFunc(GL_ALWAYS);
    for (Waypoint waypoint : nearbyWaypoints) {
      double x = waypoint.x() + 0.5 - pos.x;
      double y = waypoint.y() + 0.5 - pos.y;
      double z = waypoint.z() + 0.5 - pos.z;
      float distance = (float) Mth.length(x, y, z);
      if (distance < 3) {
        continue;
      }
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      matrices.pushPose();
      float maxDistance = (Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4) - 2;
      float adjustedDistance = distance;
      if (distance > maxDistance) {
        x = x / distance * maxDistance;
        y = y / distance * maxDistance;
        z = z / distance * maxDistance;
        adjustedDistance = maxDistance;
      }
      matrices.translate(x, y, z);

      adjustedDistance = (adjustedDistance * 0.1f + 1) * 0.0266f;
      matrices.mulPose(Vector3f.YP.rotationDegrees(-camera.getYRot()));
      matrices.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
      matrices.scale(-adjustedDistance, -adjustedDistance, -adjustedDistance);

      waypoint.render(buffer, matrices.last().pose(), 10);
      matrices.popPose();
      tessellator.end();
    }
    RenderSystem.disableBlend();
    MultiBufferSource.BufferSource source = MultiBufferSource.immediate(buffer);
    int rendered = 0;
    Collections.reverse(nearbyWaypoints);
    for (int i = 0; i < nearbyWaypoints.size() && rendered < 100; i++, rendered++) {
      Waypoint waypoint = nearbyWaypoints.get(i);
      double waypointDistance = Mth.length(waypoint.x() - player.getBlockX(), waypoint.y() - player.getBlockY(), waypoint.z() - player.getBlockZ());
      if (!isPointedAt(waypoint, waypointDistance, Minecraft.getInstance().getCameraEntity(), event.tickDelta())) {
        continue;
      }
      double x = waypoint.x() + 0.5 - pos.x;
      double y = waypoint.y() + 0.5 - pos.y;
      double z = waypoint.z() + 0.5 - pos.z;
      float distance = (float) Mth.length(x, y, z);
      if (distance < 3) {
        continue;
      }
      matrices.pushPose();
      float maxDistance = (Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 * 4) - 2;
      float adjustedDistance = distance;
      if (distance > maxDistance) {
        x = x / distance * maxDistance;
        y = y / distance * maxDistance;
        z = z / distance * maxDistance;
        adjustedDistance = maxDistance;
      }
      matrices.translate(x, y, z);

      adjustedDistance = (adjustedDistance * 0.1f + 1) * 0.0266f;
      matrices.mulPose(Vector3f.YP.rotationDegrees(-camera.getYRot()));
      matrices.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
      matrices.scale(-adjustedDistance, -adjustedDistance, -adjustedDistance);

      Font font = Minecraft.getInstance().font;

      String str = waypoint.name() + (waypoint.name().isBlank() ? "" : " ") + "(" + (int)waypointDistance + ")";

      matrices.translate(0, -20, 0);
      Matrix4f last = matrices.last().pose();
      RenderSystem.enableBlend();
      font.drawInBatch(str, -font.width(str) / 2f, (float) 0, 0xFFFFFFFF, false, last, source, false, 1056964608, 15728640);
      font.drawInBatch(str, -font.width(str) / 2f, (float) 0, 0xCCCCCC, false, last, source, true, 0, 15728880);
      matrices.popPose();
    }
    source.endBatch();
    RenderSystem.depthFunc(GL_LEQUAL);
  }

  private boolean isPointedAt(Waypoint waypoint, double distance, Entity cameraEntity, float partialTicks) {
    Vec3 cameraPos = cameraEntity.getEyePosition(partialTicks);
    double degrees = 4D + Math.min(4D / distance, 4D);
    double angle = degrees * 0.0174533D;
    double size = Math.sin(angle) * distance * 2;
    Vec3 cameraPosPlusDirection = cameraEntity.getViewVector(partialTicks);
    Vec3 cameraPosPlusDirectionTimesDistance = cameraPos.add(cameraPosPlusDirection.x * distance, cameraPosPlusDirection.y * distance, cameraPosPlusDirection.z * distance);
    AABB axisalignedbb = new AABB((waypoint.x() + 0.5F) - size, (waypoint.y() + 0.5F) - size, (waypoint.z() + 0.5F) - size, (waypoint.x() + 0.5F) + size, (waypoint.y() + 0.5F) + size, (waypoint.z() + 0.5F) + size);
    Optional<Vec3> raytraceresult = axisalignedbb.clip(cameraPos, cameraPosPlusDirectionTimesDistance);
    if (axisalignedbb.contains(cameraPos))
      return distance >= 1.0D;
    return raytraceresult.isPresent();
  }
}
