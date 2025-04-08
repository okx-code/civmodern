package uk.protonull.civianmod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import uk.protonull.civianmod.features.CompactedItem;
import uk.protonull.civianmod.mixing.CivianItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(
        method = "renderItemCount",
        at = @At("HEAD"),
        argsOnly = true
    )
    protected @Nullable String civianmod$alwaysShowItemAmountIfCompacted(
        final String value,
        final @Local(argsOnly = true) ItemStack item
    ) {
        if (value != null) {
            return value;
        }
        return switch (CivianItemStack.getCompactedItemType(item)) {
            case CompactedItem type -> String.valueOf(item.getCount());
            case null -> null;
        };
    }

    @ModifyConstant(
        method = "renderItemCount",
        constant = @Constant(intValue = -1)
    )
    protected int civianmod$colourItemDecorationIfCompacted(
        final int decorationColour,
        final @Local(argsOnly = true) ItemStack item
    ) {
        return switch (CivianItemStack.getCompactedItemType(item)) {
            case CompactedItem type -> type.colour;
            case null -> decorationColour;
        };
    }
}
