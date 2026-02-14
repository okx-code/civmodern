package sh.okx.civmodern.common.mixins;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.features.CompactedItem;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements CompactedItem.IMixin {
    @Unique
    private CompactedItem compactedItemType;

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",
        at = @At("TAIL")
    )
    protected void civmodern$determineItemType(
        final @NotNull ItemLike item,
        final int count,
        final @NotNull PatchedDataComponentMap components,
        final @NotNull CallbackInfo ci
    ) {
        this.compactedItemType = CompactedItem.determineCompactedItemType((ItemStack) (Object) this);
    }

    @Override
    public @NotNull CompactedItem civmodern$getCompactedType() {
        return this.compactedItemType;
    }

    @ModifyVariable(
        method = "copy",
        at = @At("STORE")
    )
    protected @NotNull ItemStack civmodern$alsoCopyCompactedItemType(
        final @NotNull ItemStack other
    ) {
        ((ItemStackMixin) (Object) other).compactedItemType = this.compactedItemType;
        return other;
    }
}
