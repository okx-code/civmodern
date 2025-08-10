package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoints;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.rendering.BlitRenderState;
import sh.okx.civmodern.common.rendering.ChevronRenderState;
import sh.okx.civmodern.common.rendering.CivModernPipelines;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class Minimap {

    private final Waypoints waypoints;
    private final PlayerWaypoints playerWaypoints;
    private final MapCache cache;
    private final CivMapConfig config;
    private final ColourProvider provider;

    private static final RegionAtlasTexture blank = new RegionAtlasTexture();

    static {
        RenderSystem.queueFencedTask(blank::init);
    }


    public Minimap(Waypoints waypoints, PlayerWaypoints playerWaypoints, MapCache cache, CivMapConfig config, ColourProvider provider) {
        this.waypoints = waypoints;
        this.playerWaypoints = playerWaypoints;
        this.cache = cache;
        this.config = config;
        this.provider = provider;
    }

    public void onRender(PostRenderGameOverlayEvent event) {
        if (!config.isMinimapEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen() || !(!mc.options.keyPlayerList.isDown() || mc.isLocalServer() && mc.player.connection.getListedOnlinePlayers().size() <= 1 && objective == null)) {
            return;
        }

        float zoom = config.getMinimapZoom();

        float size = config.getMinimapSize();

        GuiGraphics graphics = event.guiGraphics();
        Matrix3x2fStack matrices = graphics.pose();

        matrices.pushMatrix();

        int offsetX = config.getMinimapX();
        int offsetY = config.getMinimapY();

        int translateX;
        int translateY;

        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        switch (config.getMinimapAlignment()) {
            case TOP_LEFT -> {
                translateX = offsetX;
                translateY = offsetY;
            }
            case TOP_RIGHT -> {
                translateX = width - offsetX - config.getMinimapSize();
                translateY = offsetY;
            }
            case BOTTOM_RIGHT -> {
                translateX = width - offsetX - config.getMinimapSize();
                translateY = height - offsetY - config.getMinimapSize();
            }
            default -> {
                translateX = offsetX;
                translateY = height - offsetY - config.getMinimapSize();
            }
        }

        matrices.translate(translateX, translateY);

        LocalPlayer player = Minecraft.getInstance().player;
        float px = (float) Mth.lerp(event.deltaTick(), player.xo, player.getX());
        float pz = (float) Mth.lerp(event.deltaTick(), player.zo, player.getZ());
        float x = px - (size * zoom) / 2;
        float y = pz - (size * zoom) / 2;

        float drawnX = 0;
        float drawnY = 0;
        List<BlitRenderState.Renderer> renderers = new ArrayList<>();
        for (float screenX = 0; screenX < (size * zoom) + SIZE; screenX += SIZE) {
            float tmp = 0;
            for (float screenY = 0; screenY < (size * zoom) + SIZE; screenY += SIZE) {
                float realX = x + screenX;
                float realY = y + screenY;

                float renderX = realX - floatMod(realX, SIZE);
                float renderY = realY - floatMod(realY, SIZE);

                RegionKey key = new RegionKey(Math.floorDiv((int) renderX, SIZE), Math.floorDiv((int) renderY, SIZE));
                RegionAtlasTexture texture = cache.getTexture(key);
                float xOff = (renderX - x) + 4096;
                float yOff = (renderY - y) + 4096;

                texture = texture == null ? blank : texture;
                renderers.add(texture.drawLinear(graphics, drawnX, drawnY, zoom, screenX == 0 ? SIZE - xOff : 0, screenY == 0 ? SIZE - yOff : 0, SIZE, SIZE, Math.max(0, size * zoom - drawnX), Math.max(0, size * zoom - drawnY), translateX, translateY));
                drawnY += screenY == 0 ? yOff : SIZE;
                tmp += xOff;
            }
            drawnY = 0;
            drawnX += screenX == 0 ? tmp / 2 : SIZE;
        }
        graphics.guiRenderState.submitPicturesInPictureState(new BlitRenderState(graphics,
            ((source, stack) -> renderers.forEach(r -> r.render(source, stack)))));

        if (config.isPlayerWaypointsEnabled()) {
            // TODO fix the player rendering above the chevron
            // todo fading
            for (PlayerWaypoint waypoint : this.playerWaypoints.getWaypoints()) {
                // TODO cycle between players on the same snitch
                double wx = waypoint.x() + 0.5;
                double wz = waypoint.z() + 0.5;
                double tx = (wx - x) / zoom;
                double ty = (wz - y) / zoom;
                if (tx < 0 || ty < 0 || tx > size || ty > size) {
                    continue;
                }
                matrices.pushMatrix();
                matrices.translate((float) tx, (float) ty);

                boolean old = waypoint.timestamp().until(Instant.now(), ChronoUnit.MINUTES) >= 10;
                int colour = (old ? 0x77 : 0xFF) << 24 | 0xFFFFFF;
                waypoint.render(graphics, colour);
                matrices.popMatrix();
            }
        }

        List<Waypoint> waypointList = waypoints.getWaypoints();
        Map<String, List<Waypoint>> waypointByIcon = new HashMap<>();
        for (Waypoint waypoint : waypointList) {
            waypointByIcon.computeIfAbsent(waypoint.icon(), k -> new ArrayList<>()).add(waypoint);
        }
        matrices.pushMatrix();
        for (List<Waypoint> waypointGroup : waypointByIcon.values()) {
            for (Waypoint waypoint : waypointGroup) {
                double wx = waypoint.x() + 0.5;
                double wz = waypoint.z() + 0.5;
                double tx = (wx - x) / zoom;
                double ty = (wz - y) / zoom;
                if (tx < 0 || ty < 0 || tx > size || ty > size) {
                    continue;
                }
                matrices.pushMatrix();
                matrices.translate((float) tx, (float) ty);

                waypoint.render2D(graphics);
                matrices.popMatrix();
            }
        }
        matrices.popMatrix();

        matrices.pushMatrix();
        matrices.translate(size / 2, size / 2);
        matrices.rotate((float) Math.toRadians(player.getViewYRot(event.deltaTick()) % 360f));
        matrices.scale(4, 4);
        int chevronColour = provider.getChevronColour() | 0xFF000000;
        matrices.translate(0, 0.75f);
        graphics.guiRenderState.submitGuiElement(new ChevronRenderState(
            CivModernPipelines.GUI_TRIANGLE_STRIP_BLEND,
            new Matrix3x2f(graphics.pose()),
            graphics.scissorStack.peek(),
            chevronColour));
        matrices.popMatrix();

        matrices.popMatrix();
    }

    private float floatMod(float x, float y) {
        // x mod y behaving the same way as Math.floorMod but with floats
        return (x - (float) Math.floor(x / y) * y);
    }

    public void cycleZoom() {
        float zoom = config.getMinimapZoom();
        zoom /= 2;
        if (zoom < 0.5f) {
            zoom = 16f;
        }
        config.setMinimapZoom(zoom);
    }
}
