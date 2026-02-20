package sh.okx.civmodern.common.map.screen;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.UIContainers;
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
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import sh.okx.civmodern.common.gui.widget.ColourTextEditBox;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import net.minecraft.client.input.MouseButtonEvent;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.map.waypoints.Waypoint;
import sh.okx.civmodern.common.map.waypoints.Waypoints;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class EditWaypointModal extends Modal<FlowLayout> {

    private final Waypoints waypoints;

    private TextBoxComponent xBox;
    private TextBoxComponent yBox;
    private TextBoxComponent zBox;
    private ColourTextEditBox colourBox;
    private HsbColourPicker colourPicker;
    private int colour = 0xFF0000;
    private int previewColour = colour;

    private TextBoxComponent nameBox;

    private Button doneButton;

    private Waypoint waypoint;
    private boolean targeting = false;

    public EditWaypointModal(Waypoints waypoints) {
        super(OwoUIAdapter.createWithoutScreen(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - 104 - 12, 48, 220, 116, UIContainers::verticalFlow));
        super.layout.rootComponent.allowOverflow(true);
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
        ImageButton deleteButton = new ImageButton(0, 0, 20, 20, Identifier.fromNamespaceAndPath("civmodern", "gui/delete.png"), imbg -> {
            if (this.waypoint != null) {
                this.waypoints.removeWaypoint(this.waypoint);
            }
            this.waypoint = null;
            setVisible(false);
        });
        ImageButton copyButton = new ImageButton(0, 0, 20, 20, Identifier.fromNamespaceAndPath("civmodern", "gui/copy.png"), imbg -> {
            StringBuilder builder = new StringBuilder("[");
            if (!this.waypoint.name().isBlank()) {
                builder.append("name:%s,".formatted(this.waypoint.name()));
            }
            builder.append("x:%s,y:%s,z:%s]".formatted(this.waypoint.x(), this.waypoint.y(), this.waypoint.z()));
            Minecraft.getInstance().keyboardHandler.setClipboard(builder.toString());
            Minecraft.getInstance().setScreen(null);
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("civmodern.map.copy", Component.literal(builder.toString())).withColor(0x379FA3), false);
            Minecraft.getInstance().keyboardHandler.setClipboard(builder.toString());
            this.waypoint = null;
        });
        colourPicker = new HsbColourPicker(
            0,
            0,
            20,
            20,
            waypoint.colour(),
            (colour) -> {
                colourBox.setColourFromInt(colour);
                this.colour = colour;
                this.previewColour = colour;
            },
            preview -> {
                this.previewColour = Objects.requireNonNullElse(preview, colour);
            },
            () -> {
            }
        );
        this.colour = this.previewColour = waypoint.colour();
        colourPicker.setRVisible(false);
        ImageButton coordsButton = new ImageButton(0, 0, 20, 20, Identifier.fromNamespaceAndPath("civmodern", "gui/target.png"), imbg -> {
            this.visible = false;
            this.targeting = true;
        });
        xBox = UIComponents.textBox(Sizing.fixed(44), Integer.toString(waypoint.x()));
        xBox.setFilter(numFilter);
        xBox.onChanged().subscribe(value -> this.updateDone());
        yBox = UIComponents.textBox(Sizing.fixed(26), Integer.toString(waypoint.y()));
        yBox.setFilter(numFilter);
        yBox.onChanged().subscribe(value -> this.updateDone());
        zBox = UIComponents.textBox(Sizing.fixed(44), Integer.toString(waypoint.z()));
        zBox.setFilter(numFilter);
        zBox.onChanged().subscribe(value -> this.updateDone());

        colourBox = new ColourTextEditBox(Sizing.fixed(55), () -> colour, c -> {
            this.colour = c;
            this.previewColour = c;
        });

        this.layout.rootComponent.clearChildren();
        this.layout.rootComponent
            .child(
                UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(24))
                    .child(UIComponents.label(Component.literal("Name")).margins(Insets.right(8)))
                    .child(nameBox = UIComponents.textBox(Sizing.expand(), waypoint.name()))
                    .verticalAlignment(VerticalAlignment.CENTER)
                    .margins(Insets.horizontal(4).withTop(4))
            )
            .child(
                UIContainers.horizontalFlow(Sizing.fill(), Sizing.content())
                    .child(UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(40))
                        .child(UIContainers.grid(Sizing.content(), Sizing.content(), 2, 3)
                            .child(UIComponents.label(Component.literal("X")).margins(Insets.of(0, 4, 1, 0)), 0, 0)
                            .child(UIComponents.label(Component.literal("Y")).margins(Insets.of(0, 4, 1, 0)), 0, 1)
                            .child(UIComponents.label(Component.literal("Z")).margins(Insets.of(0, 4, 1, 0)), 0, 2)
                            .child(xBox.margins(Insets.right(3)), 1, 0)
                            .child(yBox.margins(Insets.right(3)), 1, 1)
                            .child(zBox.margins(Insets.right(3)), 1, 2)
                            .positioning(Positioning.relative(0, 0))
                        )
                        .child(
                            UIContainers.horizontalFlow(Sizing.content(), Sizing.fixed(40))
                                .child(coordsButton.margins(Insets.right(4)))
                                .child(copyButton.margins(Insets.right(4)))
                                .child(deleteButton)
                                .margins(Insets.bottom(6))
                                .alignment(HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM)
                                .positioning(Positioning.relative(100, 100))
                        )
                    )
                    .margins(Insets.horizontal(4).withTop(4))
            )
            .child(
                UIContainers.horizontalFlow(Sizing.fill(), Sizing.content())
                    .child(doneButton.horizontalSizing(Sizing.fill(28)).margins(Insets.right(8).withTop(1)).positioning(Positioning.relative(0, 0)))
                    .child(cancelButton.horizontalSizing(Sizing.fill(28)).margins(Insets.right(8).withTop(1)))
                    .child(colourBox)
                    .child(colourPicker.margins(Insets.top(1).withLeft(2)))
                    .horizontalAlignment(HorizontalAlignment.RIGHT)
                    .margins(Insets.horizontal(4).withTop(4)))
            .surface(Surface.DARK_PANEL)
            .padding(Insets.of(6));

        this.layout.inflateAndMount();
    }

    public String getName() {
        return nameBox.getValue();
    }

    public Waypoint getWaypoint() {
        return waypoint;
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
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!visible) {
            return false;
        }
        return this.colourPicker.mouseClicked(event, bl) || super.mouseClicked(event, bl);
    }

    @Override
    public void mouseMoved(double d, double e) {
        if (!visible) {
            return;
        }
        this.colourPicker.mouseMoved(d, e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double f, double g) {
        if (!visible) {
            return false;
        }
        if (this.colourPicker.mouseDragged(event, f, g)) {
            return true;
        }
        return super.mouseDragged(event, f, g);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!visible) {
            return false;
        }
        if (this.colourPicker.mouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
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
            waypoints.addWaypoint(new Waypoint(this.nameBox.getValue(), x, y, z, this.waypoint.icon(), this.colour));
            setVisible(false);
        } catch (NumberFormatException ignored) {

        }
    }

    public boolean isTargeting() {
        return targeting;
    }

    public boolean hasChanged() {
        try {
            return getX() != this.waypoint.x() || getZ() != this.waypoint.z() || getY() != this.waypoint.y() || previewColour != this.waypoint.colour();
        } catch (NumberFormatException ex) {
            return false;
        }
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

    public int getY() {
        return Integer.parseInt(this.yBox.getValue());
    }

    public int getZ() {
        return Integer.parseInt(this.zBox.getValue());
    }

    public int getPreviewColour() {
        return previewColour;
    }

    public int getColour() {
        return colour;
    }
}
