package uk.protonull.civianmod.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.api.utils.MutableDimension;
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
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.config.TooltipLineOption;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.features.ItemDurability;
import uk.protonull.civianmod.features.SafeMining;

public final class ItemConfigGui {
    public static @NotNull ConfigCategory generateCategory(
        final @NotNull CivianModConfig config
    ) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.title"))
            .tooltip(Component.translatable("civianmod.config.tab.item.desc"))
            .option(LabelOption.create(Component.translatable("civianmod.config.tab.item.label.compacted")))
            .option(generateCrateItemColour(config))
            .option(generateCompactedItemColour(config))
            .option(LabelOption.create(Component.translatable("civianmod.config.tab.item.label.tooltips")))
            .option(generateShowRepairLevel(config))
            .option(generateShowDamageLevel(config))
            .option(generateShowExpIngredient(config))
            .option(LabelOption.create(Component.translatable("civianmod.config.tab.item.label.tool-management")))
            .option(generateSafeMiningEnabled(config))
            .option(generateSafeMiningThreshold(config))
            .build();
    }

    private static @NotNull Option<?> generateCrateItemColour(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.crate-colour.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.crate-colour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.CRATE))
            .binding(
                CompactedItem.CRATE.defaultAwtColor,
                () -> config.itemColourCrate,
                (colour) -> config.itemColourCrate = colour
            )
            .addListener((opt, event) -> CompactedItem.CRATE.colour = opt.pendingValue().getRGB())
            .build();
    }

    private static @NotNull Option<?> generateCompactedItemColour(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Color>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.compacted-colour.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.compacted-colour.desc")))
            .controller((opt) -> () -> new CompactedItemColourController(opt, CompactedItem.COMPACTED))
            .binding(
                CompactedItem.COMPACTED.defaultAwtColor,
                () -> config.itemColourCompacted,
                (colour) -> config.itemColourCompacted = colour
            )
            .addListener((opt, event) -> CompactedItem.COMPACTED.colour = opt.pendingValue().getRGB())
            .build();
    }

    private static @NotNull Option<?> generateShowRepairLevel(
        final @NotNull CivianModConfig config
    ) {
        return Option.<TooltipLineOption>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.repair-level.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.repair-level.desc")))
            .controller(TooltipLineOption::controller)
            .binding(
                ItemDurability.DEFAULT_SHOW_REPAIR_LEVEL,
                () -> config.showRepairLevel,
                (show) -> config.showRepairLevel = show
            )
            .build();
    }

    private static @NotNull Option<?> generateShowDamageLevel(
        final @NotNull CivianModConfig config
    ) {
        return Option.<TooltipLineOption>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.damage-level.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.damage-level.desc")))
            .controller(TooltipLineOption::controller)
            .binding(
                ItemDurability.DEFAULT_SHOW_DAMAGE_LEVEL,
                () -> config.showDamageLevel,
                (show) -> config.showDamageLevel = show
            )
            .build();
    }

    private static @NotNull Option<?> generateShowExpIngredient(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.exp-ingredient.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.exp-ingredient.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                ExpIngredients.DEFAULT_ENABLED,
                () -> config.showExpTooltip,
                (show) -> config.showExpTooltip = show
            )
            .build();
    }

    private static @NotNull Option<?> generateSafeMiningEnabled(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.safe-mining-enabled.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.safe-mining-enabled.desc")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                SafeMining.DEFAULT_ENABLED,
                () -> config.safeMiningEnabled,
                (enabled) -> config.safeMiningEnabled = enabled
            )
            .build();
    }

    private static @NotNull Option<?> generateSafeMiningThreshold(
        final @NotNull CivianModConfig config
    ) {
        return Option.<Integer>createBuilder()
            .name(Component.translatable("civianmod.config.tab.item.option.safe-mining-threshold.label"))
            .description(OptionDescription.of(Component.translatable("civianmod.config.tab.item.option.safe-mining-threshold.desc")))
            .controller((opt) -> IntegerSliderControllerBuilder.create(opt)
                .range(1, 20)
                .step(1)
            )
            .binding(
                SafeMining.DEFAULT_THRESHOLD,
                () -> config.safeMiningThreshold,
                (threshold) -> config.safeMiningThreshold = threshold
            )
            .build();
    }
}

final class CompactedItemColourController extends ColorController {
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
}

final class CompactedItemColourWidget extends ColorController.ColorControllerElement {
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
