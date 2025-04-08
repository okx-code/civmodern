package uk.protonull.civianmod.mixing;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.protonull.civianmod.features.CompactedItem;

public interface CivianItemStack {
    @Nullable CompactedItem civianmod$getCompactedItemType();
    static @Nullable CompactedItem getCompactedItemType(
        final @NotNull ItemStack item
    ) {
        return ((CivianItemStack) (Object) item).civianmod$getCompactedItemType();
    }
}
