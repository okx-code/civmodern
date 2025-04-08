package uk.protonull.civianmod;

import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
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
        final @NotNull String expected,
        final boolean ignoreCase
    ) {
        return matchesPlainText(item.getComponents().get(DataComponents.CUSTOM_NAME), expected, ignoreCase);
    }

    public static boolean hasPlainLoreLine(
        final @NotNull ItemStack item,
        final @NotNull String expected,
        final boolean ignoreCase
    ) {
        if (item.getComponents().get(DataComponents.LORE) instanceof final ItemLore lore) {
            for (final Component line : lore.lines()) {
                if (matchesPlainText(line, expected, ignoreCase)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean matchesPlainText(
        final Component component,
        final @NotNull String expected,
        final boolean ignoreCase
    ) {
        if (isNullOrEmpty(component)) {
            return false;
        }
        final var combined = new StringBuilder();
        for (final Component child : component.toFlatList()) {
            if (!Style.EMPTY.equals(child.getStyle())) {
                return false;
            }
            final String content = child.getString();
            if (LEGACY_FORMATTER_REGEX.matcher(content).matches()) {
                return false;
            }
            combined.append(content);
        }
        return ignoreCase
            ? expected.equalsIgnoreCase(combined.toString())
            : expected.contentEquals(combined);
    }

    public static @NotNull String getPlainString(
        final @NotNull Component component
    ) {
        return LEGACY_FORMATTER_REGEX
            .matcher(component.getString())
            .replaceAll("");
    }

    public static @NotNull ItemStack createMaxStackItem(
        final @NotNull Item material,
        final Integer amount,
        final @NotNull DataComponentMap components
    ) {
        final var defaultComponents = new PatchedDataComponentMap(material.components());
        defaultComponents.setAll(components);
        return new ItemStack(
            material,
            Objects.requireNonNullElseGet(amount, material::getDefaultMaxStackSize),
            defaultComponents
        );
    }
}
