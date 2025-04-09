package uk.protonull.civianmod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.features.ExpIngredients;
import uk.protonull.civianmod.features.ItemDurability;
import uk.protonull.civianmod.mixing.CivianItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements CivianItemStack {
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
    // Add optional tooltip lines
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
    private void civianmod$addOptionalTooltipLines(
        final @NotNull Item.TooltipContext tooltipContext,
        final Player player,
        final @NotNull TooltipFlag tooltipFlag,
        final @NotNull CallbackInfoReturnable<List<Component>> cir,
        final @Local @NotNull List<Component> tooltipLines
    ) {
        final var self = (ItemStack) (Object) this;
        ItemDurability.addRepairLevelLine(self, tooltipLines, tooltipFlag);
        ItemDurability.addDamageLevelLine(self, tooltipLines, tooltipFlag);
        ExpIngredients.addExpTooltip(self, tooltipLines);
    }

    /**
     * Prevents the default durability line from being added, giving control of that to
     * {@link uk.protonull.civianmod.features.ItemDurability#addDamageLevelLine(net.minecraft.world.item.ItemStack, java.util.List, net.minecraft.world.item.TooltipFlag)}
     */
    @Redirect(
        method = "getTooltipLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isDamaged()Z"
        )
    )
    private boolean civianmod$preventVanillaDamageLine(
        final @NotNull ItemStack item
    ) {
        return false;
    }
}
