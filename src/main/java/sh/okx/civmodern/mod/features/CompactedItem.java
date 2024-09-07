package sh.okx.civmodern.mod.features;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

public final class CompactedItem {
    public static final String LORE = "Compacted Item";

    public static final int DEFAULT_COLOR = 0xFF_FF_58;
    public static volatile int COLOUR = DEFAULT_COLOR;

    public static boolean hasCompactedItemLore(
        final @NotNull ItemStack item
    ) {
        final ItemLore lore = item.getComponents().get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (final Component line : lore.lines()) {
            if (line == null) {
                continue;
            }
            final var content = new StringBuilder();
            for (final Component child : line.toFlatList()) {
                if (!Style.EMPTY.equals(child.getStyle())) {
                    return false;
                }
                content.append(child.getString());
            }
            if (CompactedItem.LORE.contentEquals(content)) {
                return true;
            }
        }
        return false;
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
