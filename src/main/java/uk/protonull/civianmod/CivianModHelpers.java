package uk.protonull.civianmod;

import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

public final class CivianModHelpers {
    private static final Pattern LEGACY_FORMATTER_REGEX = Pattern.compile("§.");

    public static boolean isNullOrEmpty(
        final Component component
    ) {
        return component == null || Component.empty().equals(component);
    }

    public static boolean hasPlainDisplayName(
        final @NotNull ItemStack item,
        final @NotNull String expected
    ) {
        final Component displayName = item.getComponents().get(DataComponents.CUSTOM_NAME);
        if (isNullOrEmpty(displayName)) {
            return false;
        }
        final var content = new StringBuilder();
        for (final Component child : displayName.toFlatList()) {
            if (!Style.EMPTY.equals(child.getStyle())) {
                return false;
            }
            content.append(child.getString());
        }
        return expected.contentEquals(content);
    }

    public static boolean hasPlainLoreLine(
        final @NotNull ItemStack item,
        final @NotNull String expected
    ) {
        final ItemLore lore = item.getComponents().get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (final Component line : lore.lines()) {
            if (isNullOrEmpty(line)) {
                continue;
            }
            final var content = new StringBuilder();
            for (final Component child : line.toFlatList()) {
                if (!Style.EMPTY.equals(child.getStyle())) {
                    return false;
                }
                content.append(child.getString());
            }
            if (expected.contentEquals(content)) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull String getPlainString(
        final @NotNull Component component
    ) {
        return LEGACY_FORMATTER_REGEX
            .matcher(component.getString())
            .replaceAll("");
    }
}
