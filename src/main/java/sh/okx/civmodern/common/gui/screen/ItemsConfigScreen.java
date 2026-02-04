package sh.okx.civmodern.common.gui.screen;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.common.CivMapConfig;
import sh.okx.civmodern.common.ColourProvider;
import sh.okx.civmodern.common.features.CompactedItem;
import sh.okx.civmodern.common.gui.widget.ColourTextEditBox;
import sh.okx.civmodern.common.gui.widget.HsbColourPicker;
import sh.okx.civmodern.common.gui.widget.ImageButton;
import sh.okx.civmodern.common.gui.widget.TextRenderable;
import sh.okx.civmodern.common.gui.widget.ToggleButton;

final class ItemsConfigScreen extends AbstractConfigScreen {
    private static final ItemStack ITEM = CompactedItem.createExampleCompactedItem();

    private final ColourProvider colourProvider;

    private HsbColourPicker colourPicker;
    private int itemX;
    private int itemY;

    ItemsConfigScreen(
        final @NotNull CivMapConfig config,
        final @NotNull ColourProvider colourProvider,
        final @NotNull MainConfigScreen parent
    ) {
        super(
            config,
            Objects.requireNonNull(parent),
            Component.translatable("civmodern.screen.items.title")
        );
        this.colourProvider = Objects.requireNonNull(colourProvider);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableOnly(new TextRenderable.CentreAligned(
            this.font,
            this.centreX,
            getHeaderY(),
            this.title
        ));

        int offsetY = getBodyY();

        this.itemX = centreX - 8; // Items have a render size of 16x16
        this.itemY = offsetY;
        offsetY += 16 + 10;

        final var compactedColourEditBox = addRenderableWidget(new ColourTextEditBox(
            this.font,
            this.centreX - 30,
            offsetY,
            60, // width
            20, // height
            this.config::getColour,
            this.config::setColour
        ));
        addRenderableWidget(this.colourPicker = new HsbColourPicker(
            this.centreX - (compactedColourEditBox.getWidth() / 2) - 5 - 20,
            offsetY,
            20,
            20,
            this.config.getColour(),
            (colour) -> {
                compactedColourEditBox.setValue("#" + "%06X".formatted(colour));
                this.config.setColour(colour);
            },
            this.colourProvider::setTemporaryCompactedColour,
            () -> {}
        ));
        addRenderableWidget(new ImageButton(
            this.centreX + (compactedColourEditBox.getWidth() / 2) + 5,
            offsetY,
            20,
            20,
            ResourceLocation.tryBuild("civmodern", "gui/rollback.png"),
            (button) -> {
                final int colour = 0xffff58;
                compactedColourEditBox.setValue("#FFFF58");
                this.config.setColour(colour);
                this.colourPicker.close();
            }
        ));
        offsetY += Button.DEFAULT_HEIGHT + 10;

        addRenderableWidget(new ToggleButton(
            this.centreX - (Button.DEFAULT_WIDTH / 2),
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.items.crates"),
            this.config::isCratesAreCompacted,
            this.config::setCratesAreCompacted,
            Tooltip.create(Component.translatable("civmodern.screen.items.crates.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));
        offsetY += Button.DEFAULT_HEIGHT + 4;

        addRenderableWidget(new ToggleButton(
            this.centreX - (Button.DEFAULT_WIDTH / 2),
            offsetY,
            ToggleButton.DEFAULT_BUTTON_WIDTH,
            Component.translatable("civmodern.screen.items.repair"),
            this.config::isShowRepairCost,
            this.config::setShowRepairCost,
            Tooltip.create(Component.translatable("civmodern.screen.items.repair.tooltip")),
            ToggleButton.DEFAULT_NARRATION
        ));

        addRenderableWidget(
            Button
                .builder(
                    CommonComponents.GUI_DONE,
                    (button) -> {
                        this.config.save();
                        this.minecraft.setScreen(this.parent);
                    }
                )
                .width(150)
                .pos(
                    this.centreX - 75,
                    getFooterY(offsetY)
                )
                .build()
        );
    }

    @Override
    public void render(
        final @NotNull GuiGraphics guiGraphics,
        final int mouseX,
        final int mouseY,
        final float delta
    ) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.renderItem(ITEM, this.itemX, this.itemY);
        guiGraphics.renderItemDecorations(this.font, ITEM, this.itemX, this.itemY);

        if (isCursorOverItem(mouseX, mouseY)) {
//            guiGraphics.renderTooltip(this.font, ITEM, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(
        final double mouseX,
        final double mouseY,
        final int button
    ) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            if (this.minecraft != null) {
                final LocalPlayer player = this.minecraft.player;
                if (player != null && isCursorOverItem((int) mouseX, (int) mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1 && player.isCreative()) {
                    player.addItem(ITEM.copy());
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private boolean isCursorOverItem(
        final int mouseX,
        final int mouseY
    ) {
        return mouseX >= this.itemX - 1
            && mouseX < this.itemX + 17
            && mouseY > this.itemY - 1
            && mouseY < this.itemY + 17;
    }

    @Override
    public void mouseMoved(
        final double mouseX,
        final double mouseY
    ) {
        super.mouseMoved(mouseX, mouseY);
        if (this.colourPicker != null) {
            this.colourPicker.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        this.colourProvider.setTemporaryCompactedColour(null);
        this.config.save();
    }
}
