package uk.protonull.civianmod.features;

import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.protonull.civianmod.CivianModHelpers;

public enum CompactedItem {
    CRATE(0xFF_41_41),
    COMPACTED(0xFF_FF_58)
    ;

    public final int defaultColour;
    public volatile int colour;

    CompactedItem(
        final int defaultColour
    ) {
        this.defaultColour = defaultColour;
        this.colour = defaultColour;
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
            && CivianModHelpers.hasPlainLoreLine(item, CRATE_LORE, false);
    }

    private static boolean isCompacted(
        final @NotNull ItemStack item
    ) {
        return CivianModHelpers.hasPlainLoreLine(item, COMPACTED_LORE, false);
    }

    public static @NotNull ItemStack createExampleCrate() {
        return CivianModHelpers.createMaxStackItem(
            Items.CHEST,
            null, // use material max size
            DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.CRATE_LORE)
                )))
                .build()
        );
    }

    public static @NotNull ItemStack createExampleCompacted() {
        return CivianModHelpers.createMaxStackItem(
            Items.STONE,
            null, // use material max size
            DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.COMPACTED_LORE)
                )))
                .build()
        );
    }
}
