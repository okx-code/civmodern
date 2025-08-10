package uk.protonull.civianmod.mixins;

import java.util.function.Consumer;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
        method = "addDetailsToTooltip",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z",
            shift = At.Shift.BEFORE
        )
    )
    private void civianmod$addOptionalTooltipLines(
        final @NotNull Item.TooltipContext tooltipContext,
        final @NotNull TooltipDisplay tooltipDisplay,
        Player player,
        final @NotNull TooltipFlag tooltipFlag,
        final @NotNull Consumer<Component> tooltipAdder,
        final @NotNull CallbackInfo ci
    ) {
        final var self = (ItemStack) (Object) this;
        ItemDurability.addRepairLevelLine(self, tooltipDisplay, tooltipAdder, tooltipFlag);
        ItemDurability.addDamageLevelLine(self, tooltipDisplay, tooltipAdder, tooltipFlag);
        ExpIngredients.addExpTooltip(self, tooltipAdder);
    }

    /**
     * Prevents the default durability line from being added, giving control of that to
     * {@link uk.protonull.civianmod.features.ItemDurability#addDamageLevelLine(ItemStack, TooltipDisplay, Consumer, TooltipFlag)}
     */
    @Redirect(
        method = "addDetailsToTooltip",
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
