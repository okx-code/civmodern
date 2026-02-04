package sh.okx.civmodern.common.features;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.ColourProvider;

public enum CompactedItem {
    COMPACTED, CRATE;

    public static final String COMPACTED_LORE = "Compacted Item";
    public static final String CRATE_LORE = "Crate";

    /// Convenience shortcut
    public static @Nullable CompactedItem getType(
        final @NotNull ItemStack item
    ) {
        return ((IMixin) (Object) item).civmodern$getCompactedType();
    }

    /// @see sh.okx.civmodern.common.mixins.ItemStackMixin
    public interface IMixin {
        @Nullable CompactedItem civmodern$getCompactedType();
    }

    private static final Pattern LEGACY_FORMATTER_REGEX = Pattern.compile("§.");
    public static @Nullable CompactedItem detectType(
        final @NotNull ItemStack item
    ) {
        final ItemLore lore = item.getComponents().get(DataComponents.LORE);
        if (lore == null) {
            return null;
        }
        final Component lastLine;
        try {
            lastLine = lore.lines().getLast(); // Why this throws instead of returning an optional is baffling
        }
        catch (final NoSuchElementException ignored) {
            return null;
        }
        final var combined = new StringBuilder();
        for (final Component child : lastLine.toFlatList()) {
            if (!Style.EMPTY.equals(child.getStyle())) {
                return null;
            }
            final String content = child.getString();
            if (LEGACY_FORMATTER_REGEX.matcher(content).matches()) {
                return null;
            }
            combined.append(content);
        }
        return switch (combined.toString()) {
            case COMPACTED_LORE -> COMPACTED;
            case CRATE_LORE -> CRATE;
            default -> null;
        };
    }

    public static @Nullable Integer getColourFor(
        final @NotNull CompactedItem type
    ) {
        final AbstractCivModernMod mod = AbstractCivModernMod.getInstance();
        final ColourProvider provider = mod.getColourProvider();
        return switch (type) {
            case COMPACTED -> provider.getCompactedColour();
            case CRATE -> mod.getConfig().isCratesAreCompacted() ? provider.getCompactedColour() : null;
        };
    }

    public static @NotNull ItemStack createExampleCompactedItem() {
        final var item = new ItemStack(Items.STONE, 64);
        item.applyComponents(
            DataComponentMap.builder()
                .set(DataComponents.LORE, new ItemLore(
                    List.of(Component.literal(CompactedItem.COMPACTED_LORE))
                ))
                .build()
        );
        getType(item); // Calculate and store type on item
        return item;
    }
}
