package uk.protonull.civianmod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.protonull.civianmod.config.CivianModConfig;
import uk.protonull.civianmod.config.ItemSettings;
import uk.protonull.civianmod.config.TooltipLineOption;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.mixing.CivianItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements CivianItemStack {
    @Shadow
    public abstract DataComponentMap getComponents();

    @Shadow
    public abstract boolean isDamageableItem();

    @Shadow
    public abstract int getDamageValue();

    @Shadow
    public abstract int getMaxDamage();

    // ============================================================
    // Compacted item detection
    // ============================================================

    @Unique
    private CompactedItem compactedItemType;

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",
        at = @At("TAIL")
    )
    protected void civianmod$determineItemType(
        final @NotNull ItemLike item,
        final int count,
        final @NotNull PatchedDataComponentMap components,
        final @NotNull CallbackInfo ci
    ) {
        this.compactedItemType = CompactedItem.determineCompactedItemType((ItemStack) (Object) this);
    }

    @Override
    public @Nullable CompactedItem civianmod$getCompactedItemType() {
        return this.compactedItemType;
    }

    // ============================================================
    // Showing an item's repair level
    // ============================================================

    @Inject(
        method = "getTooltipLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z",
            ordinal = 1,
            shift = At.Shift.BEFORE
        )
    )
    private void civianmod$inject$getTooltipLines$showRepairLevel(
        final @NotNull Item.TooltipContext tooltipContext,
        final Player player,
        final @NotNull TooltipFlag tooltipFlag,
        final @NotNull CallbackInfoReturnable<List<Component>> cir,
        final @Local @NotNull List<Component> tooltipLines
    ) {
        final ItemSettings itemSettings = CivianModConfig.HANDLER.instance().itemSettings;
        addRepairLevelLine(tooltipLines, tooltipFlag, itemSettings.showRepairLevel);
        addDamageLevelLine(tooltipLines, tooltipFlag, itemSettings.showDamageLevel);
        addExpIngredientLine(tooltipLines, tooltipFlag, itemSettings.showIsExpIngredient);
    }

    @Unique
    private void addRepairLevelLine(
        final @NotNull List<Component> tooltipLines,
        final @NotNull TooltipFlag tooltipFlag,
        final @NotNull TooltipLineOption show
    ) {
        if (show == TooltipLineOption.NEVER) {
            return;
        }
        if (show == TooltipLineOption.ADVANCED && !tooltipFlag.isAdvanced()) {
            return;
        }
        final int repairCost = getComponents().getOrDefault(DataComponents.REPAIR_COST, 0);
        if (repairCost < 1) {
            return;
        }
        tooltipLines.add(Component.translatable(
            "civianmod.repair.level",
            Integer.toString(repairCost)
        ));
    }

    @Unique
    private void addDamageLevelLine(
        final @NotNull List<Component> tooltipLines,
        final @NotNull TooltipFlag tooltipFlag,
        final @NotNull TooltipLineOption show
    ) {
        if (show == TooltipLineOption.NEVER) {
            return;
        }
        if (show == TooltipLineOption.ADVANCED && !tooltipFlag.isAdvanced()) {
            return;
        }
        if (!isDamageableItem()) {
            return;
        }
        final int damage = getDamageValue();
        if (damage <= 0) {
            return;
        }
        final int maxDamage = getMaxDamage();
        tooltipLines.add(Component.translatable(
            "item.durability",
            maxDamage - damage,
            maxDamage
        ));
    }

    @Unique
    private void addExpIngredientLine(
        final @NotNull List<Component> tooltipLines,
        final @NotNull TooltipFlag tooltipFlag,
        final boolean show
    ) {
        if (!show) {
            return;
        }
        if (!ExpIngredients.isExpIngredient((ItemStack) (Object) this)) {
            return;
        }
        tooltipLines.add(
            Component.empty()
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("civianmod.xp.ingredient"))
        );
    }

    /**
     * Prevents the default durability line from being added, giving control of that to
     * {@link #addDamageLevelLine(java.util.List, net.minecraft.world.item.TooltipFlag, uk.protonull.civianmod.config.TooltipLineOption)}
     */
    @Redirect(
        method = "getTooltipLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isDamaged()Z"
        )
    )
    private boolean civianmod$redirect$getTooltipLines$preventVanillaDamageLine(
        final @NotNull ItemStack item
    ) {
        return false;
    }
}
