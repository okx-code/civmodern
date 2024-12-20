package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
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

public class NewWaypointModal extends Modal<FlowLayout> {

    private final Waypoints waypoints;

    private final TextBoxComponent xBox;
    private final TextBoxComponent yBox;
    private final TextBoxComponent zBox;

    private final TextBoxComponent nameBox;

    private final Button doneButton;

    private boolean targeting = false;

    public NewWaypointModal(Waypoints waypoints) {
        super(OwoUIAdapter.createWithoutScreen(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - 104, 48, 196, 116, Containers::verticalFlow));
        this.waypoints = waypoints;

        Pattern inputFilter = Pattern.compile("^-?[0-9]*$");
        Predicate<String> numFilter = s -> inputFilter.matcher(s).matches();

        doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            this.done();
        }).build();
        ImageButton coordsButton = new ImageButton(0, 0, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.visible = false;
            this.targeting = true;
        });
        LocalPlayer player = Minecraft.getInstance().player;
        xBox = Components.textBox(Sizing.fixed(44), Integer.toString(player.getBlockX()));
        xBox.setFilter(numFilter);
        xBox.onChanged().subscribe(value -> this.updateDone());
        yBox = Components.textBox(Sizing.fixed(44), Integer.toString(player.getBlockY()));
        yBox.setFilter(numFilter);
        yBox.onChanged().subscribe(value -> this.updateDone());
        zBox = Components.textBox(Sizing.fixed(44), Integer.toString(player.getBlockZ()));
        zBox.setFilter(numFilter);
        zBox.onChanged().subscribe(value -> this.updateDone());
        this.layout.rootComponent
            .child(
                Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(24))
                    .child(Components.label(Component.literal("Name")).margins(Insets.right(8)))
                    .child(nameBox = Components.textBox(Sizing.expand()))
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
            .child(doneButton.margins(Insets.horizontal(4).withTop(4)))
            .surface(Surface.DARK_PANEL)
            .padding(Insets.of(8));

        this.layout.inflateAndMount();
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
            waypoints.addWaypoint(new Waypoint(this.nameBox.getValue(), x, y, z, "waypoint"));
            setVisible(false);
        } catch (NumberFormatException ignored) {

        }
    }

    public boolean isTargeting() {
        return targeting;
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
