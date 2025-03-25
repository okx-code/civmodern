package uk.protonull.civianmod.features;

import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.CivianModHelpers;

public final class CompactedItem {
    public static final String CRATE_LORE = "Crate";
    public static final String COMPACTED_LORE = "Compacted Item";

    public static final int DEFAULT_CRATE_COLOR = 0xFF_41_41;
    public static volatile int CRATE_COLOUR = DEFAULT_CRATE_COLOR;

    public static final int DEFAULT_COMPACTED_COLOR = 0xFF_FF_58;
    public static volatile int COMPACTED_COLOUR = DEFAULT_COMPACTED_COLOR;

    public static boolean isCrate(
        final @NotNull ItemStack item
    ) {
        return item.getItem() == Items.CHEST
            && CivianModHelpers.hasPlainLoreLine(item, CRATE_LORE, false);
    }

    public static boolean isCompacted(
        final @NotNull ItemStack item
    ) {
        return CivianModHelpers.hasPlainLoreLine(item, COMPACTED_LORE, false);
    }

    public static @NotNull CompactedItemType getCompactedItemType(
        final @NotNull ItemStack item
    ) {
        return ((CompactedItem.PotentiallyCompactedItem) (Object) item).civianmod$getCompactedItemType();
    }

    public interface PotentiallyCompactedItem {
        @NotNull CompactedItemType civianmod$getCompactedItemType();
    }

    public enum CompactedItemType {
        NORMAL, CRATE, COMPACTED
    }

    public static @NotNull ItemStack createExampleCrate() {
        final var item = new ItemStack(Items.CHEST);
        item.setCount(item.getMaxStackSize());
        item.applyComponents(
            DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.CRATE_LORE)
                )))
                .build()
        );
        return item;
    }

    public static @NotNull ItemStack createExampleCompacted() {
        final var item = new ItemStack(Items.STONE);
        item.setCount(item.getMaxStackSize());
        item.applyComponents(
            DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(CompactedItem.COMPACTED_LORE)
                )))
                .build()
        );
        return item;
    }
}
