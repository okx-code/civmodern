package sh.okx.civmodern.common.map.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.boat.BoatNavigation;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.MapCache;
import sh.okx.civmodern.common.map.RegionAtlasTexture;
import sh.okx.civmodern.common.map.RegionDataType;
import sh.okx.civmodern.common.map.RegionKey;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.ScreenAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class MapScreen extends Screen {

    private static final int BOAT_PREVIEW_LINE_COLOUR = 0xFFFF0000;
    private static float zoom = 1; // blocks per pixel

    private final AbstractCivModernMod mod;
    private final MapCache mapCache;
    private final BoatNavigation navigation;
    private final Waypoints waypoints;


    private NewWaypointModal newWaypointModal;
    private EditWaypointModal editWaypointModal;
    private ImageButton openWaypointButton;

    private double x;
    private double y;

    private Waypoint highlightedWaypoint;

    private boolean targeting = false;

    private boolean boating = false;

    private Waypoint newWaypoint;

    private final Set<RegionKey> yLevelInterests = new HashSet<>();

    public MapScreen(AbstractCivModernMod mod, MapCache mapCache, BoatNavigation navigation, Waypoints waypoints) {
        super(Component.translatable("civmodern.screen.map.title"));

        this.mod = mod;
        this.mapCache = mapCache;
        this.waypoints = waypoints;
        Window window = Minecraft.getInstance().getWindow();

        x = Minecraft.getInstance().player.getX() - (window.getWidth() * zoom) / 2;
        y = Minecraft.getInstance().player.getZ() - (window.getHeight() * zoom) / 2;
        this.navigation = navigation;
    }

    @Override
    protected void init() {
        ImageButton boatButton = new ImageButton(10, 10, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/boat.png"), imbg -> {
            this.boating = !boating;
        });
        boatButton.setTooltip(Tooltip.create(Component.translatable("civmodern.map.boat.tooltip")));
        addRenderableWidget(boatButton);
        newWaypointModal = new NewWaypointModal(waypoints);
        if (newWaypoint == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            Short yLevel = mapCache.getYLevel(player.getBlockX(), player.getBlockZ());
            newWaypointModal.open("", player.getBlockX(), yLevel != null ? yLevel + 2 : player.getBlockY(), player.getBlockZ());
        } else {
            newWaypointModal.open(newWaypoint.name(), newWaypoint.x(), newWaypoint.y(), newWaypoint.z());
            newWaypointModal.setVisible(true);
        }
        editWaypointModal = new EditWaypointModal(waypoints);

        openWaypointButton = new ImageButton(this.width / 2 - 22, 10, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/new.png"), imbg -> {
            newWaypointModal.setVisible(!newWaypointModal.isVisible());
            if (newWaypointModal.isVisible()) {
                editWaypointModal.setVisible(false);
                editWaypointModal.setWaypoint(null);
            }
        });
        addRenderableWidget(openWaypointButton);

        ImageButton targetButton = new ImageButton(this.width / 2 + 2, 10, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.waypoints.setTarget(null);
            targeting = !targeting;
        });
        addRenderableWidget(targetButton);

        addRenderableWidget(newWaypointModal);
        addRenderableWidget(editWaypointModal);
    }

    public void setNewWaypoint(Waypoint waypoint) {
        this.newWaypoint = waypoint;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        PoseStack matrices = guiGraphics.pose();

        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale() * zoom;
        Window window = Minecraft.getInstance().getWindow();

        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);

        float renderY;
        for (int screenX = 0; screenX < (window.getWidth() * zoom) + SIZE; screenX += SIZE) {
            for (int screenY = 0; screenY < (window.getHeight() * zoom) + SIZE; screenY += SIZE) {
                float realX = (float) this.x + screenX;
                float realY = (float) this.y + screenY;

                float renderX = realX - floatMod(realX, SIZE);
                renderY = realY - floatMod(realY, SIZE);

                RegionKey key = new RegionKey(Math.floorDiv((int) renderX, SIZE), Math.floorDiv((int) renderY, SIZE));
                // todo if loading at low zoom, only render downsampled version to save memory
                RegionAtlasTexture texture = mapCache.getTexture(key);
                if (texture != null) {
                    texture.draw(matrices, renderX - (float) this.x, renderY - (float) this.y, scale);
                }
            }
        }

        matrices.pushPose();
        matrices.translate(0, -1, 0);
        Tesselator tesselator = Tesselator.getInstance();

        List<Waypoint> waypointList = waypoints.getWaypoints();
        Map<String, List<Waypoint>> waypointByIcon = new HashMap<>();
        for (Waypoint waypoint : waypointList) {
            if (editWaypointModal.getWaypoint() == waypoint && editWaypointModal.hasChanged()) {
                continue;
            }
            waypointByIcon.computeIfAbsent(waypoint.icon(), k -> new ArrayList<>()).add(waypoint);
        }

        for (List<Waypoint> waypointGroup : waypointByIcon.values()) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (Waypoint waypoint : waypointGroup) {
                matrices.pushPose();
                double x = waypoint.x() + 0.5;
                double z = waypoint.z() + 0.5;
                matrices.translate((x - this.x) / scale, (z - this.y) / scale, 0);

                waypoint.render(buffer, matrices.last().pose(), 7, 0xFF << 24);
                matrices.popPose();
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());

            for (Waypoint waypoint : waypointGroup) {
                if (waypoint.name().isBlank()) {
                    continue;
                }
                matrices.pushPose();
                double x = waypoint.x() + 0.5;
                double z = waypoint.z() + 0.5;
                matrices.translate((x - this.x) / scale, (z - this.y) / scale, 0);

                Font font = Minecraft.getInstance().font;

                String str = waypoint.name();

                matrices.translate(0, -15, -10);
                Matrix4f last = matrices.last().pose();
                RenderSystem.enableBlend();
                guiGraphics.drawSpecial(source -> {
                    MutableComponent comp = Component.literal(str);
                    font.drawInBatch(comp, -font.width(comp) / 2f, (float) 0, 0xFFFFFFFF, false, last, source, Font.DisplayMode.SEE_THROUGH, 1056964608, 15728640, false);
                    font.drawInBatch(comp, -font.width(comp) / 2f, (float) 0, 0xCCCCCC, false, last, source, Font.DisplayMode.NORMAL, 0, 15728880, true);
                });
                matrices.popPose();
            }
        }

        RenderSystem.depthFunc(GL_LEQUAL);

        if (targeting || newWaypointModal.isTargeting()) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrices.pushPose();
            matrices.translate(mouseX, mouseY, 0);

            Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, targeting ? "target" : "waypoint", 0xFF0000);
            int transparency = newWaypointModal.isTargeting() ? 0x7F : 0xFF;
            targetWaypoint.render(buffer, matrices.last().pose(), 7, transparency << 24);

            matrices.popPose();
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        if (newWaypointModal.isVisible()) {
            try {
                double x = newWaypointModal.getX() + 0.5;
                double z = newWaypointModal.getZ() + 0.5;

                BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                matrices.pushPose();
                matrices.translate((x - this.x) / scale, (z - this.y) / scale, 0);

                Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, "waypoint", newWaypointModal.getPreviewColour());
                targetWaypoint.render(buffer, matrices.last().pose(), 7, 0x7F << 24);

                matrices.popPose();
                RenderType.guiTextured(targetWaypoint.resourceLocation()).draw(buffer.buildOrThrow());
            } catch (NumberFormatException ignored) {
            }
        }

        if (highlightedWaypoint != null) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrices.pushPose();
            double x = highlightedWaypoint.x() + 0.5;
            double z = highlightedWaypoint.z() + 0.5;
            matrices.translate((x - this.x) / scale, (z - this.y) / scale, 0);

            highlightedWaypoint.renderFocus(buffer, matrices.last().pose(), 7);

            matrices.popPose();
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        if (editWaypointModal.isTargeting() || (editWaypointModal.getWaypoint() != null && editWaypointModal.hasChanged())) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            matrices.pushPose();
            if (editWaypointModal.isTargeting()) {
                matrices.translate(mouseX, mouseY, 0);
            } else {
                double x = editWaypointModal.getX() + 0.5;
                double z = editWaypointModal.getZ() + 0.5;
                matrices.translate((x - this.x) / scale, (z - this.y) / scale, 0);
            }

            Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, editWaypointModal.getWaypoint().icon(), editWaypointModal.getPreviewColour());
            if (editWaypointModal.getPreviewColour() != editWaypointModal.getColour()) {
                targetWaypoint.render(buffer, matrices.last().pose(), 7, 0xFF << 24);
            } else {
                targetWaypoint.render(buffer, matrices.last().pose(), 7, 0x7F << 24);
            }
            RenderType.guiTextured(targetWaypoint.resourceLocation()).draw(buffer.buildOrThrow());

            Font font = Minecraft.getInstance().font;

            String str = editWaypointModal.getName();
            if (!str.isBlank()) {
                matrices.translate(0, -15, -10);
                Matrix4f last = matrices.last().pose();
                RenderSystem.enableBlend();
                guiGraphics.drawSpecial(source -> {
                    Component comp = Component.literal(str);
                    font.drawInBatch(comp, -font.width(comp) / 2f, (float) 0, 0xFFFFFFFF, false, last, source, Font.DisplayMode.SEE_THROUGH, 1056964608, 15728640, false);
                    font.drawInBatch(comp, -font.width(comp) / 2f, (float) 0, 0xCCCCCC, false, last, source, Font.DisplayMode.NORMAL, 0, 15728880, true);
                });
            }
            matrices.popPose();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        float prx = (float) (player.getX() - this.x) / scale;
        float pry = (float) (player.getZ() - this.y) / scale;
        matrices.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        glEnable(GL_POLYGON_SMOOTH);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        matrices.translate(prx, pry, 0);
        matrices.scale(4, 4, 0);
        matrices.mulPose(Axis.ZP.rotationDegrees(player.getViewYRot(delta) % 360f));
        int chevron = 0xFF000000 | mod.getColourProvider().getChevronColour();
        Matrix4f pose = matrices.last().pose();
        bufferBuilder.addVertex(pose, -1, -1.5f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, -1, -1f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, 0, -0.5f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, 0, 0f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, 0, -0.5f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, 1, -1f, 0).setColor(chevron);
        bufferBuilder.addVertex(pose, 1, -1.5f, 0).setColor(chevron);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        glDisable(GL_POLYGON_SMOOTH);
        RenderSystem.disableBlend();
        matrices.popPose();


        Queue<Vec2> dests = navigation.getDestinations();
        if (boating || !dests.isEmpty()) {
            RenderSystem.enableBlend();
            glEnable(GL_POLYGON_SMOOTH);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);

            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            List<Vec2> points = new ArrayList<>();
            float px;
            float pz;
            if (player.getVehicle() != null) {
                px = (float) Mth.lerp(delta, player.getVehicle().xOld, player.getVehicle().getX());
                pz = (float) Mth.lerp(delta, player.getVehicle().zOld, player.getVehicle().getZ());
            } else {
                px = (float) player.getX();
                pz = (float) player.getZ();
            }
            points.add(new Vec2(px, pz));
            points.addAll(dests);
            if (boating) {
                points.add(new Vec2(mouseX * scale + (float) x, mouseY * scale + (float) y));
            }

            for (int i = 0; i < points.size() - 1; i++) {
                Vec2 from = points.get(i);
                Vec2 to = points.get(i + 1);

                double dx = (to.x - x) / scale - (from.x - x) / scale;
                double dy = (to.y - y) / scale - (from.y - y) / scale;
                float dist = (float) Mth.length(dx, dy) + 0.5f;

                matrices.pushPose();
                Matrix4f last = matrices.last().pose();
                matrices.translate((to.x - x) / scale, (to.y - y) / scale, 0);
                last.rotate(Axis.ZP.rotation((float) Mth.atan2(dx, -dy)));
                buffer.addVertex(last, -0.5f, 0, i / 255f).setColor(BOAT_PREVIEW_LINE_COLOUR);
                buffer.addVertex(last, -0.5f, dist, i / 255f).setColor(BOAT_PREVIEW_LINE_COLOUR);
                buffer.addVertex(last, 0.5f, dist, i / 255f).setColor(BOAT_PREVIEW_LINE_COLOUR);
                buffer.addVertex(last, 0.5f, 0, i / 255f).setColor(BOAT_PREVIEW_LINE_COLOUR);
                matrices.popPose();
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            glDisable(GL_POLYGON_SMOOTH);
            RenderSystem.disableBlend();
        }

        matrices.popPose();

        matrices.pushPose();
        float textScale = 1.5f;
        matrices.scale(textScale, textScale, 1);
        int px = (int) Math.floor(mouseX * scale + (float) x);
        int pz = (int) Math.floor(mouseY * scale + (float) y);
        RegionKey key = mapCache.getRegionKey(px, pz);
        mapCache.addInterest(key, RegionDataType.Y_LEVELS);
        this.yLevelInterests.add(key);
        Short y = mapCache.getYLevel(px, pz);
        guiGraphics.drawCenteredString(font, "(%d, %s, %d)".formatted(px, y == null ? "?" : Short.toString(y), pz), (int) (this.width / 2 / textScale), (int) ((this.height - 16) / textScale), -1);

        matrices.popPose();

        for (Renderable renderable : ((ScreenAccessor) this).civmodern$getRenderables()) {
            renderable.render(guiGraphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // 0 = left click
        // 1 = right click
        // 2 = middle click

        Window window = Minecraft.getInstance().getWindow();
        float scale = (float) window.getGuiScale() * zoom;
        if ((targeting || newWaypointModal.isTargeting() || editWaypointModal.isTargeting()) && button == 0) {
            double mouseWorldX = (mouseX * scale + x) - 0.5;
            double mouseWorldY = (mouseY * scale + y) - 0.5;

            int x = (int) Math.round(mouseWorldX);
            int z = (int) Math.round(mouseWorldY);
            Short yLevel = mapCache.getYLevel(x, z);
            if (newWaypointModal.isTargeting()) {
                newWaypointModal.setTargetResult(x, yLevel == null ? newWaypointModal.getY() : yLevel + 2, z);
            } else if (editWaypointModal.isTargeting()) {
                editWaypointModal.setTargetResult(x, yLevel == null ? newWaypointModal.getY() : yLevel + 2, z);
            } else {
                this.waypoints.setTarget(new Waypoint("", x, yLevel == null ? 64 : yLevel + 2, z, "target", 0xFF0000));
                targeting = false;
            }
            return true;
        }

        if (newWaypointModal.isVisible() && button == 1) {
            newWaypointModal.setVisible(false);
            return true;
        } else if (editWaypointModal.isVisible() && button == 1) {
            editWaypointModal.setVisible(false);
            editWaypointModal.setWaypoint(null);
            return true;
        }

        if (boating && button == 1) {
            double mouseWorldX = (mouseX * scale + x);
            double mouseWorldY = (mouseY * scale + y);
            if (Screen.hasShiftDown()) {
                this.navigation.getDestinations().pollLast();
            } else if (Screen.hasControlDown()) {
                this.navigation.reset();
            } else {
                this.navigation.addDestination(new Vec2((float) mouseWorldX, (float) mouseWorldY));
            }
            return true;
        }

        if (highlightedWaypoint != null && button == 0) {
            if (editWaypointModal.getWaypoint() != highlightedWaypoint) {
                editWaypointModal.setWaypoint(highlightedWaypoint);
                editWaypointModal.setVisible(true);
                newWaypointModal.setVisible(false);
            }
        }

        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        Window window = Minecraft.getInstance().getWindow();
        float scale = (float) window.getGuiScale() * zoom;

        List<Waypoint> waypointList = waypoints.getWaypoints();
        Waypoint closest = null;
        double mouseWorldX = (mouseX * scale + x);
        double mouseWorldY = (mouseY * scale + y);
        highlightedWaypoint = null;
        for (Waypoint waypoint : waypointList) {
            if (waypoint.equals(waypoints.getTarget())) {
                continue;
            }
            if (closest == null) {
                closest = waypoint;
            } else if (Math.abs(waypoint.x() - mouseWorldX) + Math.abs(waypoint.z() - mouseWorldY) < Math.abs(closest.x() - mouseWorldX) + Math.abs(closest.z() - mouseWorldY)) {
                closest = waypoint;
            }
        }
        if (closest != null) {
            double offsetX = (closest.x() + 0.5 - mouseWorldX) / scale;
            double offsetY = (closest.z() + 0.5 - mouseWorldY) / scale;
            if (Math.abs(offsetX) < 8 && Math.abs(offsetY) < 8) {
                highlightedWaypoint = closest;
            }
        }

        editWaypointModal.mouseMoved(mouseX, mouseY);
        newWaypointModal.mouseMoved(mouseX, mouseY);

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDirY, double scrollDir) {
        if (super.mouseScrolled(mouseX, mouseY, scrollDirY, scrollDir)) {
            return true;
        }

        if (scrollDir < 0 && zoom < 32) {
            // zoom out
            Window window = Minecraft.getInstance().getWindow();
            float scale = (float) window.getGuiScale() * zoom;

            double centreX = x + window.getWidth() * zoom * 0.5;
            double centreY = y + window.getHeight() * zoom * 0.5;

            double mouseWorldX = (mouseX * scale + x);
            double mouseWorldY = (mouseY * scale + y);

            zoom *= 2;

            x = (mouseWorldX - (mouseWorldX - centreX) / 0.5 - (window.getWidth() * zoom) / 2);
            y = (mouseWorldY - (mouseWorldY - centreY) / 0.5 - (window.getHeight() * zoom) / 2);
        } else if (scrollDir > 0 && zoom > 0.03125) {
            // zoom in
            Window window = Minecraft.getInstance().getWindow();
            float scale = (float) window.getGuiScale() * zoom;

            double centreX = x + window.getWidth() * zoom * 0.5;
            double centreY = y + window.getHeight() * zoom * 0.5;

            double mouseWorldX = (mouseX * scale + x);
            double mouseWorldY = (mouseY * scale + y);

            zoom /= 2;

            x = (mouseWorldX - (mouseWorldX - centreX) * 0.5 - (window.getWidth() * zoom) / 2);
            y = (mouseWorldY - (mouseWorldY - centreY) * 0.5 - (window.getHeight() * zoom) / 2);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double changeX, double changeY) {
        if (super.mouseDragged(x, y, button, changeX, changeY)) {
            return true;
        }

        if (button == 0 || button == 1) {
            double scale = Minecraft.getInstance().getWindow().getGuiScale() * zoom;
            this.x -= changeX * scale;
            this.y -= changeY * scale;
            return true;
        }
        return false;
        // 0 = left
        // 1 = right
        // 2 = middle
    }

    private float floatMod(float x, float y) {
        // x mod y behaving the same way as Math.floorMod but with floats
        return (x - (float) Math.floor(x / y) * y);
    }

    @Override
    public void added() {
        this.yLevelInterests.clear();
    }

    @Override
    public void removed() {
        for (Iterator<RegionKey> iterator = this.yLevelInterests.iterator(); iterator.hasNext(); ) {
            RegionKey key = iterator.next();
            mapCache.removeInterest(key, RegionDataType.Y_LEVELS);
            iterator.remove();
        }
    }
}
