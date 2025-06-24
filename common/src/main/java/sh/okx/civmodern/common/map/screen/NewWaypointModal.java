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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class NewWaypointModal extends Modal<FlowLayout> {

    private final Waypoints waypoints;

    private TextBoxComponent xBox;
    private TextBoxComponent yBox;
    private TextBoxComponent zBox;

    private TextBoxComponent nameBox;

    private Button doneButton;

    private HsbColourPicker colourPicker;
    private int colour = 0xFF0000;
    private int previewColour = colour;

    private boolean targeting = false;

    public NewWaypointModal(Waypoints waypoints) {
        super(OwoUIAdapter.createWithoutScreen(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - 104, 48, 196, 116, Containers::verticalFlow));
        super.layout.rootComponent.allowOverflow(true);
        this.waypoints = waypoints;
    }

    public void open(String name, int x, int y, int z) {
        Pattern inputFilter = Pattern.compile("^-?[0-9]*$");
        Predicate<String> numFilter = s -> inputFilter.matcher(s).matches();

        doneButton = Button.builder(CommonComponents.GUI_DONE, button -> {
            this.done();
        }).build();
        ImageButton coordsButton = new ImageButton(0, 0, 20, 20, ResourceLocation.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.visible = false;
            this.targeting = true;
        });
        xBox = Components.textBox(Sizing.fixed(44), Integer.toString(x));
        xBox.setFilter(numFilter);
        xBox.onChanged().subscribe(value -> this.updateDone());
        yBox = Components.textBox(Sizing.fixed(26), Integer.toString(y));
        yBox.setFilter(numFilter);
        yBox.onChanged().subscribe(value -> this.updateDone());
        zBox = Components.textBox(Sizing.fixed(44), Integer.toString(z));
        zBox.setFilter(numFilter);
        zBox.onChanged().subscribe(value -> this.updateDone());

        colourPicker = new HsbColourPicker(
            0,
            0,
            20,
            20,
            this.colour,
            (colour) -> {
                this.colour = colour;
                this.previewColour = colour;
            },
            preview -> {
                this.previewColour = Objects.requireNonNullElse(preview, colour);
            },
            () -> {
            }
        );
        colourPicker.setRVisible(false);

        nameBox = Components.textBox(Sizing.expand(), name);
        this.layout.rootComponent.clearChildren();
        this.layout.rootComponent
            .child(
                Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(24))
                    .child(Components.label(Component.literal("Name")).margins(Insets.right(8)))
                    .child(nameBox)
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
                            .child(xBox.margins(Insets.right(3)), 1, 0)
                            .child(yBox.margins(Insets.right(3)), 1, 1)
                            .child(zBox.margins(Insets.right(3)), 1, 2)
                            .positioning(Positioning.relative(0, 0))
                        )
                        .child(
                            Containers.horizontalFlow(Sizing.content(), Sizing.fixed(40))
                                .child(coordsButton.margins(Insets.right(4)))
                                .child(colourPicker)
                                .margins(Insets.bottom(6))
                                .alignment(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)
                                .positioning(Positioning.relative(100, 100))
                        )
                    )
                    .margins(Insets.horizontal(4).withTop(4))
            )
            .child(doneButton.margins(Insets.horizontal(4).withTop(4)))
            .surface(Surface.DARK_PANEL)
            .padding(Insets.of(6));

        this.layout.inflateAndMount();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (visible) {
            this.colourPicker.setRVisible(true);
            this.colourPicker.renderWidget(guiGraphics, mouseX, mouseY, delta);
            this.colourPicker.setRVisible(false);
        }
    }

    @Override
    public boolean isMouseOver(double d, double e) {
        if (!visible) {
            return false;
        }
        if (super.isMouseOver(d, e)) {
            return true;
        }
        return this.colourPicker.isMouseOver(d, e);
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (!visible) {
            return false;
        }
        return this.colourPicker.mouseClicked(d, e, i) || super.mouseClicked(d, e, i);
    }

    @Override
    public void mouseMoved(double d, double e) {
        if (!visible) {
            return;
        }
        this.colourPicker.mouseMoved(d, e);
    }

    @Override
    public boolean mouseDragged(double d, double e, int i, double f, double g) {
        if (!visible) {
            return false;
        }
        if (this.colourPicker.mouseDragged(d, e, i, f, g)) {
            return true;
        }
        return super.mouseDragged(d, e, i, f, g);
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
            waypoints.addWaypoint(new Waypoint(this.nameBox.getValue(), x, y, z, "waypoint", this.colour));
            setVisible(false);
        } catch (NumberFormatException ignored) {

        }
    }

    public boolean isTargeting() {
        return targeting;
    }

    public void setTargetResult(int x, int y, int z) {
        this.xBox.setValue(Integer.toString(x));
        this.yBox.setValue(Integer.toString(y));
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

    public int getY() {
        return Integer.parseInt(this.yBox.getValue());
    }

    public int getPreviewColour() {
        return previewColour;
    }
}
