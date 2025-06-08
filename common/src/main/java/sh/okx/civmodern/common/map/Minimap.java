package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class Minimap {

    private final Waypoints waypoints;
    private final MapCache cache;
    private final CivMapConfig config;
    private final ColourProvider provider;
    private float zoom = 4;

    private static final RegionAtlasTexture blank = new RegionAtlasTexture();

    static {
        RenderSystem.recordRenderCall(blank::init);
    }


    public Minimap(Waypoints waypoints, MapCache cache, CivMapConfig config, ColourProvider provider) {
        this.waypoints = waypoints;
        this.cache = cache;
        this.config = config;
        this.provider = provider;
    }

    public void onRender(PostRenderGameOverlayEvent event) {
        if (!config.isMinimapEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()) {
            return;
        }

        float size = config.getMinimapSize();

        PoseStack matrices = event.guiGraphics().pose();

        matrices.pushPose();

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

        matrices.translate(translateX, translateY, 0);

        LocalPlayer player = Minecraft.getInstance().player;
        float px = (float) Mth.lerp(event.deltaTick(), player.xo, player.getX());
        float pz = (float) Mth.lerp(event.deltaTick(), player.zo, player.getZ());
        float x = px - (size * zoom) / 2;
        float y = pz - (size * zoom) / 2;

        float drawnX = 0;
        float drawnY = 0;
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
                texture.drawLinear(matrices, drawnX, drawnY, zoom, screenX == 0 ? SIZE - xOff : 0, screenY == 0 ? SIZE - yOff : 0, SIZE, SIZE, Math.max(0, size * zoom - drawnX), Math.max(0, size * zoom - drawnY));
                drawnY += screenY == 0 ? yOff : SIZE;
                tmp += xOff;
            }
            drawnY = 0;
            drawnX += screenX == 0 ? tmp / 2 : SIZE;
        }


        List<Waypoint> waypointList = waypoints.getWaypoints();
        Map<String, List<Waypoint>> waypointByIcon = new HashMap<>();
        for (Waypoint waypoint : waypointList) {
            waypointByIcon.computeIfAbsent(waypoint.icon(), k -> new ArrayList<>()).add(waypoint);
        }
        matrices.pushPose();
        Tesselator tesselator = Tesselator.getInstance();
        for (List<Waypoint> waypointGroup : waypointByIcon.values()) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            boolean waypointRendered = false;
            for (Waypoint waypoint : waypointGroup) {
                double wx = waypoint.x() + 0.5;
                double wz = waypoint.z() + 0.5;
                double tx = (wx - x) / zoom;
                double ty = (wz - y) / zoom;
                if (tx < 0 || ty < 0 || tx > size || ty > size) {
                    continue;
                }
                matrices.pushPose();
                matrices.translate(tx, ty, 0);

                waypoint.render(buffer, matrices.last().pose(), 7, 0xFF << 24);
                matrices.popPose();
                waypointRendered = true;
            }
            if (waypointRendered) {
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
        }
        matrices.popPose();

        matrices.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        glEnable(GL_POLYGON_SMOOTH);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        matrices.translate(size / 2, size / 2, 0);
        matrices.mulPose(Axis.ZP.rotationDegrees(player.getViewYRot(event.deltaTick()) % 360f));
        matrices.scale(4, 4, 1);
        int chevronColour = provider.getChevronColour() | 0xFF000000;
        Matrix4f pose = matrices.last().pose();
        bufferBuilder.addVertex(pose, -1, -0.75f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, -1, -0.25f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, 0, 0.25f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, 0, 0.75f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, 0, 0.25f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, 1, -0.25f, 0).setColor(chevronColour);
        bufferBuilder.addVertex(pose, 1, -0.75f, 0).setColor(chevronColour);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        glDisable(GL_POLYGON_SMOOTH);
        RenderSystem.disableBlend();
        matrices.popPose();

        matrices.popPose();
    }

    private float floatMod(float x, float y) {
        // x mod y behaving the same way as Math.floorMod but with floats
        return (x - (float) Math.floor(x / y) * y);
    }

    public void cycleZoom() {
        zoom *= 2;
        if (zoom >= 32) {
            zoom = 0.5f;
        }
    }
}
