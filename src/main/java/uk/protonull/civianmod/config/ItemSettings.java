package uk.protonull.civianmod.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
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
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.features.ItemDurability;
import uk.protonull.civianmod.features.SafeMining;

public final class ItemSettings {
    @SerialEntry
    public @NotNull Color crateItemColour = CompactedItem.CRATE.defaultAwtColor;
    @SerialEntry
    public @NotNull Color compactedItemColour = CompactedItem.COMPACTED.defaultAwtColor;
    @SerialEntry
    public @NotNull TooltipLineOption showRepairLevel = ItemDurability.DEFAULT_SHOW_REPAIR_LEVEL;
    @SerialEntry
    public @NotNull TooltipLineOption showDamageLevel = ItemDurability.DEFAULT_SHOW_DAMAGE_LEVEL;
    @SerialEntry
    public boolean showIsExpIngredient = ExpIngredients.DEFAULT_ENABLED;
    @SerialEntry
    public boolean safeMiningEnabled = SafeMining.DEFAULT_ENABLED;
    @SerialEntry
    public int safeMiningThreshold = SafeMining.DEFAULT_THRESHOLD;

    void apply() {
        CompactedItem.CRATE.colour = this.crateItemColour.getRGB();
        CompactedItem.COMPACTED.colour = this.compactedItemColour.getRGB();
        ItemDurability.showDamageLevel = this.showDamageLevel;
        ItemDurability.showRepairLevel = this.showRepairLevel;
        ExpIngredients.enabled = this.showIsExpIngredient;
        SafeMining.enabled = this.safeMiningEnabled;
        SafeMining.threshold = this.safeMiningThreshold;
    }

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
            .option(generateSafeMiningEnabled(itemSettings))
            .option(generateSafeMiningThreshold(itemSettings))
            .build();
    }

    private static @NotNull Option<?> generateCrateItemColour(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.crateColour"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.crateColour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.CRATE))
            .binding(
                CompactedItem.CRATE.defaultAwtColor,
                () -> itemSettings.crateItemColour,
                (colour) -> itemSettings.crateItemColour = colour
            )
            .addListener((opt, event) -> CompactedItem.CRATE.colour = opt.pendingValue().getRGB())
            .build();
    }

    private static @NotNull Option<?> generateCompactedItemColour(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.compactedColour"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.compactedColour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.COMPACTED))
            .binding(
                CompactedItem.COMPACTED.defaultAwtColor,
                () -> itemSettings.compactedItemColour,
                (colour) -> itemSettings.compactedItemColour = colour
            )
            .addListener((opt, event) -> CompactedItem.COMPACTED.colour = opt.pendingValue().getRGB())
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
                ItemDurability.DEFAULT_SHOW_REPAIR_LEVEL,
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
                ItemDurability.DEFAULT_SHOW_DAMAGE_LEVEL,
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
                ExpIngredients.DEFAULT_ENABLED,
                () -> itemSettings.showIsExpIngredient,
                (show) -> itemSettings.showIsExpIngredient = show
            )
            .build();
    }

    private static @NotNull Option<?> generateSafeMiningEnabled(
        final @NotNull ItemSettings itemSettings
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.safeMining"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.safeMining.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                SafeMining.DEFAULT_ENABLED,
                () -> itemSettings.safeMiningEnabled,
                (enabled) -> itemSettings.safeMiningEnabled = enabled
            )
            .build();
    }

    private static @NotNull Option<?> generateSafeMiningThreshold(
        final @NotNull ItemSettings settings
    ) {
        return Option.<Integer>createBuilder()
            .name(Component.translatable("civianmod.config.group.items.safe-mining-threshold"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.group.items.safe-mining-threshold.desc")))
            .controller((opt) -> IntegerSliderControllerBuilder.create(opt)
                .range(1, 20)
                .step(1)
            )
            .binding(
                SafeMining.DEFAULT_THRESHOLD,
                () -> settings.safeMiningThreshold,
                (threshold) -> settings.safeMiningThreshold = threshold
            )
            .build();
    }

    // ============================================================
    // Custom controllers
    // ============================================================

    private static final class CompactedItemColourController extends ColorController {
        private final CompactedItem type;

        public CompactedItemColourController(
            final @NotNull Option<Color> option,
            final @NotNull CompactedItem type
        ) {
            super(option, false);
            this.type = Objects.requireNonNull(type);
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
