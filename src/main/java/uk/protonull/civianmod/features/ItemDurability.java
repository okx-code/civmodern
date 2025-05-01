package uk.protonull.civianmod.features;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.protonull.civianmod.config.TooltipLineOption;

public record ItemDurability(
    int damage,
    int maxDamage
) {
    /**
     * Damage is stored starting at 0 and increasing until reaching MAX, whereas
     * items display the durability starting at MAX and decreasing until it
     * breaks. This returns the latter.
     */
    public int getCurrentDurability() {
        return maxDamage() - damage();
    }

    public static @Nullable ItemDurability from(
        final ItemStack item
    ) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        if (item.has(DataComponents.UNBREAKABLE)) {
            return null;
        }
        final Integer max = item.get(DataComponents.MAX_DAMAGE);
        if (max == null) {
            return null;
        }
        final Integer damage = item.get(DataComponents.DAMAGE);
        if (damage == null) {
            return null;
        }
        return new ItemDurability(
            damage,
            max
        );
    }

    public static final TooltipLineOption DEFAULT_SHOW_REPAIR_LEVEL = TooltipLineOption.ALWAYS;
    public static volatile TooltipLineOption showRepairLevel = DEFAULT_SHOW_REPAIR_LEVEL;

    public static final TooltipLineOption DEFAULT_SHOW_DAMAGE_LEVEL = TooltipLineOption.ALWAYS;
    public static volatile TooltipLineOption showDamageLevel = DEFAULT_SHOW_DAMAGE_LEVEL;

    public static void addRepairLevelLine(
        final @NotNull ItemStack item,
        final @NotNull List<Component> tooltipLines,
        final @NotNull TooltipFlag tooltipFlag
    ) {
        final TooltipLineOption show = showRepairLevel;
        if (show == TooltipLineOption.NEVER) {
            return;
        }
        if (show == TooltipLineOption.ADVANCED && !tooltipFlag.isAdvanced()) {
            return;
        }
        final int repairCost = item.getComponents().getOrDefault(DataComponents.REPAIR_COST, 0);
        if (repairCost < 1) {
            return;
        }
        tooltipLines.add(Component.translatable(
            "civianmod.repair.level",
            Integer.toString(repairCost)
        ));
    }

    public static void addDamageLevelLine(
        final @NotNull ItemStack item,
        final @NotNull List<Component> tooltipLines,
        final @NotNull TooltipFlag tooltipFlag
    ) {
        final TooltipLineOption show = showDamageLevel;
        if (show == TooltipLineOption.NEVER) {
            return;
        }
        if (show == TooltipLineOption.ADVANCED && !tooltipFlag.isAdvanced()) {
            return;
        }
        final ItemDurability durability = from(item);
        if (durability == null) {
            return;
        }
        if (durability.damage() <= 0) {
            return;
        }
        tooltipLines.add(Component.translatable(
            "item.durability",
            durability.getCurrentDurability(),
            durability.maxDamage()
        ));
    }
}
