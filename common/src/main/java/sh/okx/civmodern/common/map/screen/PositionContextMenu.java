package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

import java.util.ArrayList;
import java.util.List;

public class PositionContextMenu extends Modal<FlowLayout> {

    private final Waypoints waypoints;
    private final NewWaypointModal newWaypointModal;
    private final List<ScalableLabelComponent> options = new ArrayList<>();

    public PositionContextMenu(Waypoints waypoints, NewWaypointModal newWaypointModal) {
        super(OwoUIAdapter.createWithoutScreen(0, 0, 80, 46, Containers::verticalFlow));
        this.waypoints = waypoints;
        this.newWaypointModal = newWaypointModal;
    }

    public void open(int targetX, Short targetY, int targetZ, int x, int z) {
        this.layout.rootComponent.clearChildren();
        ScalableLabelComponent createWaypoint = new ScalableLabelComponent(Component.literal("Create waypoint"), c -> {
            this.setVisible(false);
            newWaypointModal.open("", targetX, targetY != null ? targetY + 2 : Minecraft.getInstance().player.getBlockY() + 1, targetZ);
            newWaypointModal.setVisible(true);
        });
        options.add(createWaypoint);
        ScalableLabelComponent teleportHere = new ScalableLabelComponent(Component.literal("Teleport here").withColor(targetY == null ? 0xff777777 : 0xffffffff), c -> {
            if (targetY != null) {
                Minecraft.getInstance().setScreen(null);
                Minecraft.getInstance().player.connection.sendUnsignedCommand("teleport " + targetX + " " + (targetY + 1) + " " + targetZ);
            }
        });
        options.add(teleportHere);
        ScalableLabelComponent highlightPosition = new ScalableLabelComponent(Component.literal("Highlight position"), c -> {
            this.setVisible(false);
            this.waypoints.setTarget(new Waypoint("", targetX, targetY == null ? 64 : targetY, targetZ, "target", 0xFF0000));
        });
        options.add(highlightPosition);
        ScalableLabelComponent copyToClipboard = new ScalableLabelComponent(Component.literal("Copy to clipboard").withColor(targetY == null ? 0xff777777 : 0xffffffff), c -> {
            if (targetY != null) {
                Minecraft.getInstance().setScreen(null);
                String copied = "[x:%s,y:%s,z:%s]".formatted(targetX, targetY, targetZ);
                Minecraft.getInstance().player.displayClientMessage(Component.translatable("civmodern.map.copy", Component.literal(copied)), false);
                Minecraft.getInstance().keyboardHandler.setClipboard(copied);
            }
        });
        options.add(copyToClipboard);
        this.layout.rootComponent
            .child(createWaypoint.textHeight(7).margins(Insets.horizontal(2)))
            .child(Components.box(Sizing.expand(), Sizing.fixed(1)).color(Color.ofRgb(0x60605f)).margins(Insets.of(1, 2, 0, 0)))
            .child(teleportHere.hoverEffect(targetY != null).textHeight(7).margins(Insets.horizontal(2)))
            .child(Components.box(Sizing.expand(), Sizing.fixed(1)).color(Color.ofRgb(0x60605f)).margins(Insets.of(1, 2, 0, 0)))
            .child(highlightPosition.textHeight(7).margins(Insets.horizontal(2)))
            .child(Components.box(Sizing.expand(), Sizing.fixed(1)).color(Color.ofRgb(0x60605f)).margins(Insets.of(1, 2, 0, 0)))
            .child(copyToClipboard.hoverEffect(targetY != null).textHeight(7).margins(Insets.horizontal(2)))
            .padding(Insets.both(2, 3))
            .surface(Surface.TOOLTIP);

        this.layout.moveAndResize(x, z, this.layout.width(), this.layout.height());
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (!visible) {
            return false;
        }
        for (ScalableLabelComponent component : options) {
            if (component.onMouseClick(d, e, i)) {
                return true;
            }
        }
        return super.mouseClicked(d, e, i);
    }
}
