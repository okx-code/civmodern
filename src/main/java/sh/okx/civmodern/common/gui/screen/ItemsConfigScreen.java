package sh.okx.civmodern.common.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.features.CompactedItem;
import sh.okx.civmodern.common.gui.widget.ColourTextEditBox;
import sh.okx.civmodern.common.gui.widget.OwoButton;
import sh.okx.civmodern.common.gui.widget.OwoColourPicker;

final class ItemsConfigScreen extends BaseOwoScreen<FlowLayout> {
    private final CivMapConfig config;
    private final Screen previousScreen;

    ItemsConfigScreen(
        final @NotNull CivMapConfig config,
        final @NotNull Screen previousScreen
    ) {
        this.config = Objects.requireNonNull(config);
        this.previousScreen = previousScreen;
    }

    /// Prevent Minecraft from pausing various parts of Minecraft while this screen is open
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(
        final @NotNull FlowLayout rootComponent
    ) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
        rootComponent.verticalAlignment(VerticalAlignment.TOP);

        final var picker = new OwoColourPicker(rootComponent);

        final var body = Containers.verticalFlow(Sizing.content(), Sizing.content());
        body.horizontalAlignment(HorizontalAlignment.CENTER);
        body.gap(10);
        body.padding(Insets.of(10, 10, 0, 0));
        rootComponent.child(body);

        // Title
        body.child(
            Components.label(Component.translatable("civmodern.screen.items.title"))
                .color(Color.ofRgb(0xFF_FF_FF))
                .shadow(true)
        );

        // Compacted item colours
        body.child(colourPickerRow(
            picker,
            0xFF_FF_58,
            CompactedItem.COMPACTED::getRBG,
            CompactedItem.COMPACTED::setRBG,
            CompactedItem.createExampleCompacted()
        ));

        // Crate colours
        body.child(colourPickerRow(
            picker,
            0xFF_41_41,
            CompactedItem.CRATE::getRBG,
            CompactedItem.CRATE::setRBG,
            CompactedItem.createExampleCrate()
        ));

        // Show repair cost
        body.child(OwoButton.toggleButton(
            Component.translatable("civmodern.screen.items.repair"),
            this.config::isShowRepairCost,
            this.config::setShowRepairCost,
            Tooltip.create(Component.translatable("civmodern.screen.items.repair.tooltip"))
        ));

        body.child(Components.spacer());
        body.child(new OwoButton(
            CommonComponents.GUI_DONE,
            (button) -> {
                this.config.save();
                this.minecraft.setScreen(null); // .onClose() will redirect to the .previousScreen
            }
        ));
    }

    private @NotNull FlowLayout colourPickerRow(
        final @NotNull OwoColourPicker picker,
        final int defaultColour,
        final @NotNull IntSupplier colourGetter,
        final @NotNull IntConsumer colourSetter,
        final @NotNull ItemStack exampleItem
    ) {
        final var container = Containers.horizontalFlow(
            Sizing.content(),
            Sizing.content()
        );
        container.allowOverflow(true);
        container.gap(2);
        final var colourField = new ColourTextEditBox(
            Sizing.fixed(60), // width
            colourGetter,
            colourSetter
        );
        colourField.margins(Insets.top(-1)); // For some reason, fields are offset vertically by 1px
        return container
            .child(OwoButton.imageButton(
                ResourceLocation.fromNamespaceAndPath("civmodern", "gui/colour.png"),
                (button) -> picker.showPopup(button, colourGetter, colourSetter)
            ))
            .child(colourField)
            .child(OwoButton.imageButton(
                ResourceLocation.fromNamespaceAndPath("civmodern", "gui/rollback.png"),
                (button) -> {
                    colourSetter.accept(defaultColour);
                    colourField.setColourText(defaultColour);
                }
            ))
            .child(Components.item(exampleItem).configure((ItemComponent component) -> {
                component.sizing(Sizing.fixed(16), Sizing.fixed(16));
                component.margins(Insets.both(1, 1));
                component.showOverlay(true); // show decorations (count, damage, etc)
                component.setTooltipFromStack(true);
                component.mouseDown().subscribe((mouseX, mouseY, button) -> {
                    if (button != InputConstants.MOUSE_BUTTON_MIDDLE) {
                        return false;
                    }
                    final Minecraft minecraft = this.minecraft;
                    if (minecraft == null) {
                        return true; // Do nothing, avoid NPE
                    }
                    final LocalPlayer player = minecraft.player;
                    if (player == null) {
                        return true; // Do nothing, avoid NPE
                    }
                    if (!player.isCreative()) {
                        return true; // Do nothing, not allowed
                    }
                    if (player.addItem(exampleItem.copy())) {
                        player.inventoryMenu.broadcastChanges();
                    }
                    return true;
                });
            }));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
        this.config.save();
    }
}
