package sh.okx.civmodern.mod.mixins;

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
import sh.okx.civmodern.mod.features.CompactedItem;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private boolean isCompactedItem = false;

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
        if (this.isCompactedItem = CompactedItem.isMarkedAsCompacted(item)) {
            return String.valueOf(item.getCount());
        }
        return null;
    }

    @ModifyConstant(
        method = "renderItemCount",
        constant = @Constant(intValue = -1)
    )
    protected int civmodern$renderItemDecorations$colourItemDecorationIfCompacted(
        final int decorationColour
    ) {
        if (this.isCompactedItem) {
            return CompactedItem.COLOUR;
        }
        return decorationColour;
    }
}
