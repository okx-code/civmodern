package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class EditWaypointModal extends Modal<FlowLayout> {

    private final Waypoints waypoints;

    private TextBoxComponent xBox;
    private TextBoxComponent yBox;
    private TextBoxComponent zBox;

    private TextBoxComponent nameBox;

    private Button doneButton;

    private Waypoint waypoint;
    private boolean targeting = false;

    public EditWaypointModal(Waypoints waypoints) {
        super(OwoUIAdapter.createWithoutScreen(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - 104, 64, 196, 116, Containers::verticalFlow));
        this.waypoints = waypoints;
    }

    public void setWaypoint(Waypoint waypoint) {
        this.waypoint = waypoint;
        this.layout.rootComponent.clearChildren();

        if (this.waypoint == null) {
            return;
        }

        Pattern inputFilter = Pattern.compile("^-?[0-9]*$");
        Predicate<String> numFilter = s -> inputFilter.matcher(s).matches();

        doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            this.done();
        }).build();
        Button cancelButton = Button.builder(CommonComponents.GUI_CANCEL, button -> {
            setVisible(false);
            this.waypoint = null;
        }).build();
        ImageButton deleteButton = new ImageButton(0, 0, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/delete.png"), imbg -> {
            if (this.waypoint != null) {
                this.waypoints.removeWaypoint(this.waypoint);
            }
            this.waypoint = null;
            setVisible(false);
        });
        ImageButton coordsButton = new ImageButton(0, 0, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.visible = false;
            this.targeting = true;
        });
        xBox = Components.textBox(Sizing.fixed(44), Integer.toString(waypoint.x()));
        xBox.setFilter(numFilter);
        xBox.onChanged().subscribe(value -> this.updateDone());
        yBox = Components.textBox(Sizing.fixed(44), Integer.toString(waypoint.y()));
        yBox.setFilter(numFilter);
        yBox.onChanged().subscribe(value -> this.updateDone());
        zBox = Components.textBox(Sizing.fixed(44), Integer.toString(waypoint.z()));
        zBox.setFilter(numFilter);
        zBox.onChanged().subscribe(value -> this.updateDone());
        this.layout.rootComponent
            .child(
                Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(24))
                    .child(Components.label(Component.literal("Name")).margins(Insets.right(8)))
                    .child(nameBox = Components.textBox(Sizing.expand(), waypoint.name()))
                    .verticalAlignment(VerticalAlignment.CENTER)
                    .margins(Insets.horizontal(4).withTop(4))
            )
            .child(
                Containers.horizontalFlow(Sizing.fill(), Sizing.content())
                    .child(Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(40))
                        .child(Containers.grid(Sizing.content(), Sizing.content(), 2, 3)
                            .child(Components.label(Component.literal("X")).margins(Insets.of(0, 4, 1, 0)), 0, 0)
                            .child(Components.label(Component.literal("Y")).margins(Insets.of(0, 4, 1, 0)), 0, 1)
                            .child(Components.label(Component.literal("Z")).margins(Insets.of(0, 4, 1, 0)), 0, 2)
                            .child(xBox.margins(Insets.right(4)), 1, 0)
                            .child(yBox.margins(Insets.right(4)), 1, 1)
                            .child(zBox.margins(Insets.right(4)), 1, 2)
                            .positioning(Positioning.relative(0, 0))
                        )
                        .child(
                            coordsButton.margins(Insets.bottom(6)).positioning(Positioning.relative(100, 100))
                        )
                    )
                    .margins(Insets.horizontal(4).withTop(4))
            )
            .child(
                Containers.horizontalFlow(Sizing.fill(), Sizing.content())
                    .child(doneButton.horizontalSizing(Sizing.fill(40)).positioning(Positioning.relative(0, 0)))
                    .child(cancelButton.horizontalSizing(Sizing.fill(30)).margins(Insets.right(8)))
                    .child(deleteButton)
                    .horizontalAlignment(HorizontalAlignment.RIGHT)
                    .margins(Insets.horizontal(4).withTop(4)))
            .surface(Surface.DARK_PANEL)
            .padding(Insets.of(8));

        this.layout.inflateAndMount();
    }

    public String getName() {
        return nameBox.getValue();
    }

    public Waypoint getWaypoint() {
        return waypoint;
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

    public void done() {
        try {
            int x = Integer.parseInt(this.xBox.getValue());
            int y = Integer.parseInt(this.yBox.getValue());
            int z = Integer.parseInt(this.zBox.getValue());
            waypoints.removeWaypoint(this.waypoint);
            waypoints.addWaypoint(new Waypoint(this.nameBox.getValue(), x, y, z, this.waypoint.icon()));
            setVisible(false);
        } catch (NumberFormatException ignored) {

        }
    }

    public boolean isTargeting() {
        return targeting;
    }

    public boolean hasChanged() {
        try {
            return getX() != this.waypoint.x() || getZ() != this.waypoint.z();
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public void setTargetResult(int x, int z) {
        this.xBox.setValue(Integer.toString(x));
        this.zBox.setValue(Integer.toString(z));
        this.targeting = false;
        this.visible = true;
    }

    public int getX() {
        return Integer.parseInt(this.xBox.getValue());
    }

    public int getZ() {
        return Integer.parseInt(this.zBox.getValue());
    }
}
