package sh.okx.civmodern.common.mixins;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.features.CompactedItem;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements CompactedItem.IMixin {
    @Unique
    private CompactedItem compactedItemType = null;

    @Override
    public @NotNull CompactedItem civmodern$getCompactedType() {
        if (this.compactedItemType == null) {
            return this.compactedItemType = CompactedItem.detectType((ItemStack) (Object) this);
        }
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
