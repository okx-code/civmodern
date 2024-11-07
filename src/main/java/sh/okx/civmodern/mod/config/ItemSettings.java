package sh.okx.civmodern.mod.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ColorController;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.features.CompactedItem;

public final class ItemSettings {
    private static final Color DEFAULT_COMPACTED_ITEM_COLOUR = new Color(CompactedItem.DEFAULT_COLOR);
    private static final TooltipLineOption DEFAULT_SHOW_REPAIR_LEVEL = TooltipLineOption.ALWAYS;
    private static final TooltipLineOption DEFAULT_SHOW_DAMAGE_LEVEL = TooltipLineOption.ALWAYS;

    @SerialEntry
    public @NotNull Color compactedItemColour = DEFAULT_COMPACTED_ITEM_COLOUR;
    @SerialEntry
    public @NotNull TooltipLineOption showRepairLevel = DEFAULT_SHOW_REPAIR_LEVEL;
    @SerialEntry
    public @NotNull TooltipLineOption showDamageLevel = DEFAULT_SHOW_DAMAGE_LEVEL;

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull OptionGroup generateGroup(
        final @NotNull ItemSettings itemSettings
    ) {
        return OptionGroup.createBuilder()
            .name(Component.translatable("civmodern.config.group.items"))
            .option(generateCompactedItemColour(itemSettings))
            .option(generateShowRepairLevel(itemSettings))
            .option(generateShowDamageLevel(itemSettings))
            .build();
    }

    private static @NotNull Option<?> generateCompactedItemColour(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civmodern.config.group.items.compactedColour"))
            .description(OptionDescription.of(Component.translatable("civmodern.config.group.items.compactedColour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt))
            .binding(
                DEFAULT_COMPACTED_ITEM_COLOUR,
                () -> itemSettings.compactedItemColour,
                (colour) -> itemSettings.compactedItemColour = colour
            )
            .listener((opt, colour) -> CompactedItem.COLOUR = colour.getRGB())
            .build();
    }

    private static @NotNull Option<?> generateShowRepairLevel(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<TooltipLineOption>createBuilder()
            .name(Component.translatable("civmodern.config.group.items.repairLevel"))
            .description(OptionDescription.of(Component.translatable("civmodern.config.group.items.repairLevel.desc")))
            .controller(TooltipLineOption::controller)
            .binding(
                DEFAULT_SHOW_REPAIR_LEVEL,
                () -> itemSettings.showRepairLevel,
                (show) -> itemSettings.showRepairLevel = show
            )
            .build();
    }

    private static @NotNull Option<?> generateShowDamageLevel(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<TooltipLineOption>createBuilder()
            .name(Component.translatable("civmodern.config.group.items.damageLevel"))
            .description(OptionDescription.of(Component.translatable("civmodern.config.group.items.damageLevel.desc")))
            .controller(TooltipLineOption::controller)
            .binding(
                DEFAULT_SHOW_DAMAGE_LEVEL,
                () -> itemSettings.showDamageLevel,
                (show) -> itemSettings.showDamageLevel = show
            )
            .build();
    }

    // ============================================================
    // Custom controllers
    // ============================================================

    private static final class CompactedItemColourController extends ColorController {
        public CompactedItemColourController(
            final @NotNull Option<Color> option
        ) {
            super(option, false);
        }

        @Override
        public @NotNull AbstractWidget provideWidget(
            final @NotNull YACLScreen screen,
            final @NotNull Dimension<Integer> widgetDimension
        ) {
            return new CompactedItemColourWidget(this, screen, widgetDimension);
        }

        private static final class CompactedItemColourWidget extends ColorControllerElement {
            private static final ItemStack ITEM = new ItemStack(Items.STONE, 64); static {
                ITEM.applyComponents(
                    DataComponentMap.builder()
                        .set(DataComponents.LORE, new ItemLore(List.of(
                            Component.literal(CompactedItem.LORE)
                        )))
                        .build()
                );
            }

            private final MutableDimension<Integer> itemDimension = Dimension.ofInt(0, 0, 16, 16);

            public CompactedItemColourWidget(
                final @NotNull ColorController control,
                final @NotNull YACLScreen screen,
                final @NotNull Dimension<Integer> dim
            ) {
                super(control, screen, dim.withWidth(dim.width() - 20));
            }

            @Override
            public void render(
                final @NotNull GuiGraphics graphics,
                final int mouseX,
                final int mouseY,
                final float delta
            ) {
                super.render(graphics, mouseX, mouseY, delta);

                this.itemDimension
                    .setX(getDimension().xLimit() + 20 + 2)
                    .setY(getDimension().centerY() - 8);

                graphics.renderItem(ITEM, this.itemDimension.x(), this.itemDimension.y());
                graphics.renderItemDecorations(this.client.font, ITEM, this.itemDimension.x(), this.itemDimension.y());
            }

            @Override
            public boolean mouseClicked(
                final double mouseX,
                final double mouseY,
                final int button
            ) {
                if (button == InputConstants.MOUSE_BUTTON_MIDDLE && this.itemDimension.isPointInside((int) mouseX, (int) mouseY)) {
                    final LocalPlayer player = this.client.player;
                    if (player == null) {
                        return true; // Do nothing, avoid NPE
                    }
                    if (!player.isCreative()) {
                        return true; // Do nothing, not allowed
                    }
                    if (player.addItem(ITEM.copy())) {
                        // This is directly copy-pasted from [net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen]
                        // since whenever you open your creative inventory after adding a couple of compacted items, it
                        // will send a [net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket] for
                        // each of the changed slots.
                        final var listener = new CreativeInventoryListener(this.client);
                        player.inventoryMenu.addSlotListener(listener);
                        player.inventoryMenu.broadcastChanges();
                        player.inventoryMenu.removeSlotListener(listener);
                    }
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
    }
}
