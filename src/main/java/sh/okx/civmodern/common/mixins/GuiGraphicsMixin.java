package sh.okx.civmodern.common.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.features.CompactedItem;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private CompactedItem compactedItemType;

    @ModifyVariable(
        method = "renderItemCount",
        at = @At("HEAD"),
        argsOnly = true
    )
    protected @Nullable String civmodern$alwaysShowItemAmountIfCompacted(
        final String value,
        final @Local(argsOnly = true) ItemStack item
    ) {
        if (value != null) {
            return value;
        }
        return switch (this.compactedItemType = CompactedItem.determineCompactedItemType(item)) {
            case COMPACTED, CRATE -> String.valueOf(item.getCount());
            case null -> null;
        };
    }

    @ModifyConstant(
        method = "renderItemCount",
        constant = @Constant(intValue = -1)
    )
    protected int civmodern$colourItemDecorationIfCompacted(
        final int colour
    ) {
        return switch (this.compactedItemType) {
            case final CompactedItem type -> type.getRBG();
            case null -> colour;
        };
    }
}
