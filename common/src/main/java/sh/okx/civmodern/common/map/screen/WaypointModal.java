package sh.okx.civmodern.common.map.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

public class WaypointModal implements Renderable, GuiEventListener, NarratableEntry {

    private boolean visible = true;

    private final Waypoints waypoints;
    private final Font font;
    private final EditBox editBox;
    private final EditBox xBox;
    private final EditBox yBox;
    private final EditBox zBox;
    private final ImageButton coordsButton;
    private final Button doneButton;

    public WaypointModal(Waypoints waypoints, Font font, EditBox editBox, EditBox xBox, EditBox yBox, EditBox zBox, ImageButton coordsButton, Button doneButton) {
        this.waypoints = waypoints;
        this.font = font;
        this.editBox = editBox;
        this.xBox = xBox;
        this.yBox = yBox;
        this.zBox = zBox;
        this.coordsButton = coordsButton;
        this.doneButton = doneButton;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        if (!visible) {
            return;
        }

        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        int x0 = width / 2 - 104;
        int x1 = width / 2 + 104;
        int y0 = 56;
        int y1 = 56 + 104;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ResourceLocation.withDefaultNamespace("hud/effect_background"));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        bufferBuilder.addVertex(x0, y1, 0.0f).setUv((float) x0 / 32.0F, (float) (y1) / 32.0F).setColor(32, 32, 32, 255);
        bufferBuilder.addVertex(x1, y1, 0.0f).setUv((float) x1 / 32.0F, (float) (y1) / 32.0F).setColor(32, 32, 32, 255);
        bufferBuilder.addVertex(x1, y0, 0.0f).setUv((float) x1 / 32.0F, (float) (y0) / 32.0F).setColor(32, 32, 32, 255);
        bufferBuilder.addVertex(x0, y0, 0.0f).setUv((float) x0 / 32.0F, (float) (y0) / 32.0F).setColor(32, 32, 32, 255);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        Component nameText = Component.literal("Name");
        int left = width / 2 - 96;
        guiGraphics.drawString(this.font, nameText, left, 64 + 5, 0xffffff, true);
        guiGraphics.drawString(this.font, Component.literal("X"), left, 64 + 28, 0xffffff, true);
        guiGraphics.drawString(this.font, Component.literal("Y"), left + 56, 64 + 28, 0xffffff, true);
        guiGraphics.drawString(this.font, Component.literal("Z"), left + 56 + 56, 64 + 28, 0xffffff, true);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.editBox.setVisible(visible);
        this.xBox.setVisible(visible);
        this.yBox.setVisible(visible);
        this.zBox.setVisible(visible);
        this.coordsButton.visible = visible;
        this.doneButton.visible = visible;
    }

    public void updateDone() {
        try {
            Integer.parseInt(this.xBox.getValue());
            Integer.parseInt(this.yBox.getValue());
            Integer.parseInt(this.zBox.getValue());

            this.doneButton.active = true;
        } catch (NumberFormatException ex) {
            this.doneButton.active = false;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }

    public boolean overlaps(double x, double y) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int x0 = width / 2 - 104;
        int x1 = width / 2 + 104;
        int y0 = 56;
        int y1 = 56 + 104;

        return visible && x >= x0 && x < x1 && y >= y0 && y <= y1;
    }

    public void done() {
        try {
            int x = Integer.parseInt(this.xBox.getValue());
            int y = Integer.parseInt(this.yBox.getValue());
            int z = Integer.parseInt(this.zBox.getValue());
            waypoints.addWaypoint(new Waypoint(this.editBox.getValue(), x, y, z, "waypoint"));
            setVisible(false);
        } catch (NumberFormatException ignored) {

        }
    }

    @Override
    public void setFocused(boolean bl) {

    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
