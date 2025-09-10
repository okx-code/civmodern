package sh.okx.civmodern.common.map.screen;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.renderstate.LineElementRenderState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.navigation.AutoNavigation;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.MapCache;
import sh.okx.civmodern.common.map.RegionAtlasTexture;
import sh.okx.civmodern.common.map.RegionKey;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoint;
import sh.okx.civmodern.common.map.waypoints.PlayerWaypoints;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;
import sh.okx.civmodern.common.mixins.ScreenAccessor;
import sh.okx.civmodern.common.rendering.BlitRenderState;
import sh.okx.civmodern.common.rendering.ChevronRenderState;
import sh.okx.civmodern.common.rendering.CivModernPipelines;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static sh.okx.civmodern.common.map.RegionAtlasTexture.SIZE;

public class MapScreen extends Screen {

    private static final int BOAT_PREVIEW_LINE_COLOUR = 0xFFFF0000;
    private static float zoom = 1; // blocks per pixel

    private final AbstractCivModernMod mod;
    private final KeyMapping key;
    private final MapCache mapCache;
    private final AutoNavigation navigation;
    private final Waypoints waypoints;
    private final PlayerWaypoints playerWaypoints;

    private final CivMapConfig config;

    private NewWaypointModal newWaypointModal;
    private EditWaypointModal editWaypointModal;
    private ImageButton openWaypointButton;

    private PositionContextMenu positionContextMenu;

    private double x;
    private double y;

    private Waypoint hoveredWaypoint;

    private int mouseBlockX;
    private int mouseBlockY;

    private boolean targeting = false;

    private boolean boating = false;

    private Waypoint newWaypoint;

    private final Set<RegionKey> yLevelInterests = new HashSet<>();

    private boolean changedConfig = false;

    public MapScreen(AbstractCivModernMod mod, KeyMapping key, CivMapConfig config, MapCache mapCache, AutoNavigation navigation, Waypoints waypoints, PlayerWaypoints playerWaypoints) {
        super(Component.translatable("civmodern.screen.map.title"));

        this.mod = mod;
        this.key = key;
        this.config = config;
        this.mapCache = mapCache;
        this.waypoints = waypoints;
        this.playerWaypoints = playerWaypoints;
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
            newWaypointModal.open("", player.getBlockX(), player.getBlockY() + 1, player.getBlockZ());
        } else {
            newWaypointModal.open(newWaypoint.name(), newWaypoint.x(), newWaypoint.y(), newWaypoint.z());
            newWaypointModal.setVisible(true);
        }
        editWaypointModal = new EditWaypointModal(waypoints);

        positionContextMenu = new PositionContextMenu(this.waypoints, newWaypointModal);
        addRenderableWidget(positionContextMenu);

        openWaypointButton = new ImageButton(this.width / 2 - 22, 10, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/new.png"), imbg -> {
            if (editWaypointModal.isTargeting()) {
                return;
            }
            newWaypointModal.setVisible(!newWaypointModal.isVisible());
            if (newWaypointModal.isVisible()) {
                editWaypointModal.setVisible(false);
                editWaypointModal.setWaypoint(null);
            }
        });
        openWaypointButton.setTooltip(Tooltip.create(Component.translatable("civmodern.map.newwaypoint.tooltip")));
        addRenderableWidget(openWaypointButton);

        ImageButton targetButton = new ImageButton(this.width / 2 + 2, 10, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.waypoints.setTarget(null);
            targeting = !targeting;
        });
        targetButton.setTooltip(Tooltip.create(Component.translatable("civmodern.map.highlight.tooltip")));
        addRenderableWidget(targetButton);

        addRenderableWidget(newWaypointModal);
        addRenderableWidget(editWaypointModal);

        ResourceLocation togglePlayersImage;
        if (config.isPlayerWaypointsEnabled()) {
            togglePlayersImage = ResourceLocation.fromNamespaceAndPath("civmodern", "gui/toggleplayersoff.png");
        } else {
            togglePlayersImage = ResourceLocation.fromNamespaceAndPath("civmodern", "gui/toggleplayers.png");
        }
        ImageButton togglePlayers = new ImageButton(this.width - 30, 10, 20, 20, togglePlayersImage, imbg -> {
            // TODO use world config
            config.setPlayerWaypointsEnabled(!config.isPlayerWaypointsEnabled());
            changedConfig = true;
            if (config.isPlayerWaypointsEnabled()) {
                imbg.setImage(ResourceLocation.fromNamespaceAndPath("civmodern", "gui/toggleplayersoff.png"));
            } else {
                imbg.setImage(ResourceLocation.fromNamespaceAndPath("civmodern", "gui/toggleplayers.png"));
            }
        });
        togglePlayers.setTooltip(Tooltip.create(Component.translatable("civmodern.map.players.tooltip")));
        addRenderableWidget(togglePlayers);
    }

    public void setNewWaypoint(Waypoint waypoint) {
        this.newWaypoint = waypoint;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Matrix3x2fStack matrices = guiGraphics.pose();

        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale() * zoom;
        Window window = Minecraft.getInstance().getWindow();

        if (!positionContextMenu.isVisible()) {
            this.mouseBlockX = (int) Math.floor(mouseX * scale + x);
            this.mouseBlockY = (int) Math.floor(mouseY * scale + y);
        }

        guiGraphics.fill(0, 0, window.getWidth(), window.getHeight(), 0xff000000);

        float renderY;
        List<BlitRenderState.Renderer> renderers = new ArrayList<>();
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
                    renderers.add(texture.draw(guiGraphics, renderX - (float) this.x, renderY - (float) this.y, scale));
                }
            }
        }
        guiGraphics.guiRenderState.submitPicturesInPictureState(new BlitRenderState(guiGraphics,
            ((source, stack) -> renderers.forEach(r -> r.render(source, stack)))));

        matrices.pushMatrix();
        matrices.translate(0, -1);

        List<Waypoint> waypointList = waypoints.getWaypoints();
        Map<String, List<Waypoint>> waypointByIcon = new HashMap<>();
        for (Waypoint waypoint : waypointList) {
            if (editWaypointModal.getWaypoint() == waypoint && editWaypointModal.hasChanged()) {
                continue;
            }
            waypointByIcon.computeIfAbsent(waypoint.icon(), k -> new ArrayList<>()).add(waypoint);
        }

        for (List<Waypoint> waypointGroup : waypointByIcon.values()) {
            for (Waypoint waypoint : waypointGroup) {
                matrices.pushMatrix();
                double x = waypoint.x() + 0.5;
                double z = waypoint.z() + 0.5;
                matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));

                waypoint.render2D(guiGraphics);
                matrices.popMatrix();
                TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                AbstractTexture abstractTexture = textureManager.getTexture(waypoint.resourceLocation());
                abstractTexture.setFilter(true, true);
            }

            for (Waypoint waypoint : waypointGroup) {
                if (waypoint.name().isBlank()) {
                    continue;
                }
                matrices.pushMatrix();
                double x = waypoint.x() + 0.5;
                double z = waypoint.z() + 0.5;
                matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));

                Font font = Minecraft.getInstance().font;

                String str = waypoint.name();

                matrices.translate(0, -16);//, -10);
                MutableComponent comp = Component.literal(str);
                guiGraphics.drawString(font, comp, -font.width(comp) / 2, 0, -1, false);//, false, last, Font.DisplayMode.NORMAL, 0, 15728880, true);
                guiGraphics.fill(-font.width(comp) / 2, -1, font.width(comp) / 2, 9, 1056964608);
                matrices.popMatrix();
            }
        }

        if (config.isPlayerWaypointsEnabled()) {
            for (PlayerWaypoint waypoint : this.playerWaypoints.getWaypoints()) {
                boolean old = waypoint.timestamp().until(Instant.now(), ChronoUnit.MINUTES) >= 10;
                int colour = (old ? 0x77 : 0xFF) << 24 | 0xFFFFFF;
                int bgcolour = (old ? 0x66 : 0xCC) << 24 | 0xCCCCCC;

                // TODO cycle between players on the same snitch
                matrices.pushMatrix();
                double x = waypoint.x() + 0.5;
                double z = waypoint.z() + 0.5;
                matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));
                waypoint.render(guiGraphics, colour);
                matrices.scale(0.8f, 0.8f);

                Font font = Minecraft.getInstance().font;

                String str = waypoint.playerName();

                matrices.translate(0, -16);
                if (zoom <= 2) {
                    MutableComponent comp = Component.literal(str);
                    guiGraphics.drawString(font, comp, (int) (-font.width(comp) / 2f), 0, colour, true);
                    MutableComponent comp2 = Component.literal("(" + getAgo(waypoint.timestamp()) + ")");
                    guiGraphics.drawString(font, comp2, (int) (-font.width(comp2) / 2f), 24, colour, true);
                }
                matrices.popMatrix();
            }
        }

//        RenderSystem.depthFunc(GL_LEQUAL);

        if (targeting || newWaypointModal.isTargeting()) {
            matrices.pushMatrix();
            matrices.translate(mouseX, mouseY);

            Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, targeting ? "target" : "waypoint", 0xFF0000);
            int transparency = newWaypointModal.isTargeting() ? 0x7F : 0xFF;
            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(targetWaypoint.resourceLocation());
            abstractTexture.setFilter(true, true);
            targetWaypoint.render2D(guiGraphics, transparency);

            matrices.popMatrix();
        }

        if (newWaypointModal.isVisible()) {
            try {
                double x = newWaypointModal.getX() + 0.5;
                double z = newWaypointModal.getZ() + 0.5;

                matrices.pushMatrix();
                matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));

                Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, "waypoint", newWaypointModal.getPreviewColour());
                targetWaypoint.render2D(guiGraphics);

                matrices.popMatrix();
                AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(targetWaypoint.resourceLocation());
                abstractTexture.setFilter(true, true);
            } catch (NumberFormatException ignored) {
            }
        }

        if (hoveredWaypoint != null) {
            matrices.pushMatrix();
            double x = hoveredWaypoint.x() + 0.5;
            double z = hoveredWaypoint.z() + 0.5;
            matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));

            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(hoveredWaypoint.resourceLocation());
            abstractTexture.setFilter(true, true);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("civmodern", "map/focus.png"), -8, -8, 0, 0, 16, 16, 16, 16, -1);

            matrices.popMatrix();
        } else if (!targeting && !editWaypointModal.isTargeting() && !newWaypointModal.isTargeting()) {
            matrices.pushMatrix();

            matrices.translate((float) ((mouseBlockX - this.x) / scale), (float) ((mouseBlockY - this.y) / scale + 1));

            matrices.scale(1 / scale, 1 / scale);
            int size = 1;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ResourceLocation.fromNamespaceAndPath("civmodern", "map/focus.png"), 0, 0, 0, 0, size, size, size, size, -1);
            matrices.popMatrix();
        }

        if (editWaypointModal.isTargeting() || (editWaypointModal.getWaypoint() != null && editWaypointModal.hasChanged())) {
            matrices.pushMatrix();
            if (editWaypointModal.isTargeting()) {
                matrices.translate(mouseX, mouseY);
            } else {
                double x = editWaypointModal.getX() + 0.5;
                double z = editWaypointModal.getZ() + 0.5;
                matrices.translate((float) ((x - this.x) / scale), (float) ((z - this.y) / scale));
            }

            Waypoint targetWaypoint = new Waypoint("", 0, 0, 0, editWaypointModal.getWaypoint().icon(), editWaypointModal.getPreviewColour());
            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(targetWaypoint.resourceLocation());
            abstractTexture.setFilter(true, true);
            if (editWaypointModal.getPreviewColour() != editWaypointModal.getColour()) {
                targetWaypoint.render2D(guiGraphics);
            } else {
                targetWaypoint.render2D(guiGraphics, 0x7f);
            }

            Font font = Minecraft.getInstance().font;

            String str = editWaypointModal.getName();
            if (!str.isBlank()) {
                matrices.translate(0, -16);
                Component comp = Component.literal(str);
                guiGraphics.drawString(font, comp, -font.width(comp) / 2, 0, -1);
                guiGraphics.fill(-font.width(comp) / 2, -1, font.width(comp) / 2, 9, 1056964608);
            }
            matrices.popMatrix();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        float prx = (float) (player.getX() - this.x) / scale;
        float pry = (float) (player.getZ() - this.y) / scale;
        matrices.pushMatrix();
        int chevron = 0xFF000000 | mod.getColourProvider().getChevronColour();
        matrices.translate(prx, pry);
        matrices.scale(4, 4);
        matrices.rotate((float) Math.toRadians(player.getViewYRot(delta) % 360f));
        guiGraphics.guiRenderState.submitGuiElement(new ChevronRenderState(
            CivModernPipelines.GUI_TRIANGLE_STRIP_BLEND,
            new Matrix3x2f(guiGraphics.pose()),
            guiGraphics.scissorStack.peek(),
            chevron));
        matrices.popMatrix();


        Queue<Vec2> dests = navigation.getDestinations();
        if (boating || !dests.isEmpty()) {

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

                matrices.pushMatrix();
                matrices.translate((float) ((to.x - x) / scale), (float) ((to.y - y) / scale));
                guiGraphics.pose().rotate(((float) Mth.atan2(dx, -dy)));
                guiGraphics.guiRenderState.submitGuiElement(new LineElementRenderState(
                    CivModernPipelines.GUI_QUADS,
                    new Matrix3x2f(guiGraphics.pose()),
                    guiGraphics.scissorStack.peek(),
                    0, 0, 0, (int) dist, 1, Color.ofArgb(BOAT_PREVIEW_LINE_COLOUR)
                ));
                matrices.popMatrix();
            }
        }

        matrices.popMatrix();

        matrices.pushMatrix();
        float textScale = 1f;
        matrices.scale(textScale, textScale);
        int px = (int) Math.floor(mouseX * scale + (float) x);
        int pz = (int) Math.floor(mouseY * scale + (float) y);
        RegionKey key = mapCache.getRegionKey(px, pz);
        if (this.yLevelInterests.add(key)) {
            mapCache.addReference(key);
        }
        Short y = mapCache.getYLevel(px, pz);
        guiGraphics.drawCenteredString(font, "(%d, %s, %d)".formatted(px, y == null ? "?" : Short.toString(y), pz), (int) (this.width / 2 / textScale), (int) ((this.height - 16) / textScale), -1);

        matrices.popMatrix();

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

        if (hoveredWaypoint != null && button == 0) {
            if (editWaypointModal.getWaypoint() != hoveredWaypoint) {
                editWaypointModal.setWaypoint(hoveredWaypoint);
                editWaypointModal.setVisible(true);
                newWaypointModal.setVisible(false);
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (super.mouseReleased(x, y, button)) {
            return true;
        }

        Window window = Minecraft.getInstance().getWindow();
        float scale = (float) window.getGuiScale() * zoom;

        if (!boating) {
            if (hoveredWaypoint == null && button == 1 && !positionContextMenu.isVisible()) {
                Short yLevel = mapCache.getYLevel(this.mouseBlockX, this.mouseBlockY);
                positionContextMenu.open(this.mouseBlockX, yLevel, this.mouseBlockY, (int) ((this.mouseBlockX - this.x) / scale), (int) ((this.mouseBlockY - this.y + 1) / scale + 1));
                positionContextMenu.setVisible(true);
                return true;
            } else if (positionContextMenu.isVisible() && !positionContextMenu.isMouseOver(x, y)) {
                positionContextMenu.setVisible(false);
                return true;
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
        hoveredWaypoint = null;
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
                hoveredWaypoint = closest;
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

        if (positionContextMenu.isVisible() && !positionContextMenu.isMouseOver(x, y)) {
            positionContextMenu.setVisible(false);
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
        for (RegionKey key : this.yLevelInterests) {
            mapCache.removeReference(key);
        }
        this.yLevelInterests.clear();
        if (changedConfig) {
            config.save();
        }
    }

    public static String getAgo(Instant timestamp) {
        Instant now = Instant.now();
        long minutesDiff = timestamp.until(now, ChronoUnit.MINUTES);
        if (minutesDiff > 0) {
            return minutesDiff + "m ago";
        }
        long secondsDiff = timestamp.until(now, ChronoUnit.SECONDS);
        long lastDigit = secondsDiff % 10;
        secondsDiff -= lastDigit;
        if (secondsDiff < 10) {
            return "now";
        }
        return secondsDiff + "s ago";
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (this.key.matches(i, j) && !newWaypointModal.isVisible() && !editWaypointModal.isVisible()) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(i, j, k);
    }
}
