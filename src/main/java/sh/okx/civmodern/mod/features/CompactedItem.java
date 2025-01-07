package sh.okx.civmodern.mod.features;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.CivModernHelpers;

public final class CompactedItem {
    public static final String LORE = "Compacted Item";

    public static final int DEFAULT_COLOR = 0xFF_FF_58;
    public static volatile int COLOUR = DEFAULT_COLOR;

    public static boolean hasCompactedItemLore(
        final @NotNull ItemStack item
    ) {
        return CivModernHelpers.hasPlainLoreLine(item, LORE);
    }

    public static boolean isMarkedAsCompacted(
        final @NotNull ItemStack item
    ) {
        return ((CompactedItem.PotentiallyCompactedItem) (Object) item).civmodern$isMarkedAsCompacted();
    }

    public interface PotentiallyCompactedItem {
        boolean civmodern$isMarkedAsCompacted();
    }
}
