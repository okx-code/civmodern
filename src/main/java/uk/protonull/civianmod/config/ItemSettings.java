package uk.protonull.civianmod.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ColorController;
import java.awt.Color;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.features.CompactedItem;

public final class ItemSettings {
    private static final Color DEFAULT_CRATE_ITEM_COLOUR = new Color(CompactedItem.DEFAULT_CRATE_COLOR);
    private static final Color DEFAULT_COMPACTED_ITEM_COLOUR = new Color(CompactedItem.DEFAULT_COMPACTED_COLOR);
    private static final TooltipLineOption DEFAULT_SHOW_REPAIR_LEVEL = TooltipLineOption.ALWAYS;
    private static final TooltipLineOption DEFAULT_SHOW_DAMAGE_LEVEL = TooltipLineOption.ALWAYS;
    private static final boolean DEFAULT_SHOW_IS_EXP_INGREDIENT = true;
    private static final boolean DEFAULT_SAFE_MINING = true;

    @SerialEntry
    public @NotNull Color crateItemColour = DEFAULT_CRATE_ITEM_COLOUR;
    @SerialEntry
    public @NotNull Color compactedItemColour = DEFAULT_COMPACTED_ITEM_COLOUR;
    @SerialEntry
    public @NotNull TooltipLineOption showRepairLevel = DEFAULT_SHOW_REPAIR_LEVEL;
    @SerialEntry
    public @NotNull TooltipLineOption showDamageLevel = DEFAULT_SHOW_DAMAGE_LEVEL;
    @SerialEntry
    public boolean showIsExpIngredient = DEFAULT_SHOW_IS_EXP_INGREDIENT;
    @SerialEntry
    public boolean safeMining = DEFAULT_SAFE_MINING;

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull OptionGroup generateGroup(
        final @NotNull ItemSettings itemSettings
    ) {
        return OptionGroup.createBuilder()
            .name(Component.translatable("civianmod.config.group.items"))
            .collapsed(true)
            .option(generateCrateItemColour(itemSettings))
            .option(generateCompactedItemColour(itemSettings))
            .option(generateShowRepairLevel(itemSettings))
            .option(generateShowDamageLevel(itemSettings))
            .option(generateShowIsExpIngredient(itemSettings))
            .option(generateSafeMining(itemSettings))
            .build();
    }

    private static @NotNull Option<?> generateCrateItemColour(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.crateColour"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.crateColour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.CompactedItemType.CRATE))
            .binding(
                DEFAULT_CRATE_ITEM_COLOUR,
                () -> itemSettings.crateItemColour,
                (colour) -> itemSettings.crateItemColour = colour
            )
            .addListener((opt, event) -> CompactedItem.CRATE_COLOUR = opt.pendingValue().getRGB())
            .build();
    }

    private static @NotNull Option<?> generateCompactedItemColour(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.compactedColour"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.compactedColour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.CompactedItemType.COMPACTED))
            .binding(
                DEFAULT_COMPACTED_ITEM_COLOUR,
                () -> itemSettings.compactedItemColour,
                (colour) -> itemSettings.compactedItemColour = colour
            )
            .addListener((opt, event) -> CompactedItem.COMPACTED_COLOUR = opt.pendingValue().getRGB())
            .build();
    }

    private static @NotNull Option<?> generateShowRepairLevel(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<TooltipLineOption>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.repairLevel"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.repairLevel.desc")))
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
            .name(Component.translatable("civianmod.config.group.items.damageLevel"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.damageLevel.desc")))
            .controller(TooltipLineOption::controller)
            .binding(
                DEFAULT_SHOW_DAMAGE_LEVEL,
                () -> itemSettings.showDamageLevel,
                (show) -> itemSettings.showDamageLevel = show
            )
            .build();
    }

    private static @NotNull Option<?> generateShowIsExpIngredient(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.isExpIngredient"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.isExpIngredient.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_SHOW_IS_EXP_INGREDIENT,
                () -> itemSettings.showIsExpIngredient,
                (show) -> itemSettings.showIsExpIngredient = show
            )
            .build();
    }

    private static @NotNull Option<?> generateSafeMining(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.safeMining"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.safeMining.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                DEFAULT_SAFE_MINING,
                () -> itemSettings.safeMining,
                (enabled) -> itemSettings.safeMining = enabled
            )
            .build();
    }

    // ============================================================
    // Custom controllers
    // ============================================================

    private static final class CompactedItemColourController extends ColorController {
        private final CompactedItem.CompactedItemType type;

        public CompactedItemColourController(
            final @NotNull Option<Color> option,
            final @NotNull CompactedItem.CompactedItemType type
        ) {
            super(option, false);
            this.type = switch (type) {
                case CRATE, COMPACTED -> type;
                default -> throw new IllegalArgumentException("Does not support compacted item type: " + type);
            };
        }

        @Override
        public @NotNull AbstractWidget provideWidget(
            final @NotNull YACLScreen screen,
            final @NotNull Dimension<Integer> widgetDimension
        ) {
            return new CompactedItemColourWidget(
                switch (this.type) {
                    case CRATE -> CompactedItem.createExampleCrate();
                    case COMPACTED -> CompactedItem.createExampleCompacted();
                    default -> throw new IllegalStateException("How did [" + this.type + "] get here?!");
                },
                this,
                screen,
                widgetDimension
            );
        }

        private static final class CompactedItemColourWidget extends ColorControllerElement {
            private final ItemStack item;
            private final MutableDimension<Integer> itemDimension = Dimension.ofInt(0, 0, 16, 16);

            public CompactedItemColourWidget(
                final @NotNull ItemStack item,
                final @NotNull ColorController control,
                final @NotNull YACLScreen screen,
                final @NotNull Dimension<Integer> dim
            ) {
                super(control, screen, dim.withWidth(dim.width() - 20));
                this.item = Objects.requireNonNull(item);
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

                graphics.renderItem(this.item, this.itemDimension.x(), this.itemDimension.y());
                graphics.renderItemDecorations(this.client.font, this.item, this.itemDimension.x(), this.itemDimension.y());
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
                    if (player.addItem(this.item.copy())) {
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
