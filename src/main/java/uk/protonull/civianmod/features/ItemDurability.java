package uk.protonull.civianmod.features;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.config.TooltipLineOption;

public final class ItemDurability {
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
        if (!item.isDamageableItem()) {
            return;
        }
        final int damage = item.getDamageValue();
        if (damage <= 0) {
            return;
        }
        final int maxDamage = item.getMaxDamage();
        tooltipLines.add(Component.translatable(
            "item.durability",
            maxDamage - damage,
            maxDamage
        ));
    }
}
