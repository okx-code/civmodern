package sh.okx.civmodern.common.features;

import java.awt.Color;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CompactedItem {
    CRATE(0xFF_41_41),
    COMPACTED(0xFF_FF_58),
    ;

    public final int defaultColour;
    public final Color defaultAwtColor;
    private volatile int colour;

    CompactedItem(
        final int defaultColour
    ) {
        this.defaultColour = defaultColour;
        this.defaultAwtColor = new Color(defaultColour);
        this.colour = defaultColour;
    }

    public int getRBG() {
        return 0xFF_00_00_00 | this.colour;
    }

    public void setRBG(
        final int colour
    ) {
        this.colour = colour;
    }

    private static final String CRATE_LORE = "Crate";
    private static final String COMPACTED_LORE = "Compacted Item";

    public static @Nullable CompactedItem determineCompactedItemType(
        final @NotNull ItemStack item
    ) {
        if (CompactedItem.isCrate(item)) {
            return CompactedItem.CRATE;
        }
        else if (CompactedItem.isCompacted(item)) {
            return CompactedItem.COMPACTED;
        }
        else {
            return null;
        }
    }

    private static boolean isCrate(
        final @NotNull ItemStack item
    ) {
        return item.getItem() == Items.CHEST
            && hasLastPlainLoreLine(item, CRATE_LORE, true);
    }

    private static boolean isCompacted(
        final @NotNull ItemStack item
    ) {
        return hasLastPlainLoreLine(item, COMPACTED_LORE, true);
    }

    private static boolean hasLastPlainLoreLine(
        final @NotNull ItemStack item,
        final @NotNull String expected,
        final boolean caseSensitive
    ) {
        final ItemLore lore = item.getComponents().get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        final Component line;
        try {
            line = lore.lines().getLast();
        }
        catch (final NoSuchElementException ignored) {
            return false;
        }
        final var content = new StringBuilder();
        for (final Component child : line.toFlatList()) {
            if (!Style.EMPTY.equals(child.getStyle())) {
                return false;
            }
            content.append(child.getString());
        }
        return caseSensitive
            ? expected.contentEquals(content)
            : expected.equalsIgnoreCase(content.toString());
    }

    public static @NotNull ItemStack createExampleCrate() {
        return new ItemStack(
            Items.CHEST.builtInRegistryHolder(),
            64,
            DataComponentPatch.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.CRATE_LORE)
                )))
                .build()
        );
    }

    public static @NotNull ItemStack createExampleCompacted() {
        return new ItemStack(
            Items.STONE.builtInRegistryHolder(),
            64,
            DataComponentPatch.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.COMPACTED_LORE)
                )))
                .build()
        );
    }
}
