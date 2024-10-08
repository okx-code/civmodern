package sh.okx.civmodern.common.mixins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.features.ExtendedItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private boolean civmodern$isCompactedItem = false;

    @Redirect(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getCount()I",
            ordinal = 0
        )
    )
    protected int civmodern$alwaysShowItemAmountIfCompacted(
        final @NotNull ItemStack stack
    ) {
        if (this.civmodern$isCompactedItem = ((ExtendedItemStack) (Object) stack).isMarkedAsCompacted()) {
            return 0; // Will force the real count to be displayed since it's a !=1 check
        }
        return stack.getCount();
    }

    @ModifyConstant(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        constant = @Constant(intValue = 16777215)
    )
    protected int civmodern$colourItemDecorationIfCompacted(
        final int decorationColour
    ) {
        if (this.civmodern$isCompactedItem) {
            return AbstractCivModernMod.getInstance().getColourProvider().getCompactedColour();
        }
        return decorationColour;
    }
}
