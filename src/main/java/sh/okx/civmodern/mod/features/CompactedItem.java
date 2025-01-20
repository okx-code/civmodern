package sh.okx.civmodern.mod.features;

import java.util.List;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.CivModernHelpers;

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
            && CivModernHelpers.hasPlainLoreLine(item, CRATE_LORE);
    }

    public static boolean hasCompactedItemLore(
        final @NotNull ItemStack item
    ) {
        return CivModernHelpers.hasPlainLoreLine(item, COMPACTED_LORE);
    }

    public static @NotNull CompactedItemType getCompactedItemType(
        final @NotNull ItemStack item
    ) {
        return ((CompactedItem.PotentiallyCompactedItem) (Object) item).civmodern$getCompactedItemType();
    }

    public interface PotentiallyCompactedItem {
        @NotNull CompactedItemType civmodern$getCompactedItemType();
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
