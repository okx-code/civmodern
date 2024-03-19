package sh.okx.civmodern.common.mixins.compacted;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.features.compacted.PotentiallyCompactedItem;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private boolean cm_unique$isCompactedItem = false;

    @ModifyVariable(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            shift = At.Shift.BEFORE
        ),
        argsOnly = true
    )
    public @NotNull ItemStack cm_modify_variable$renderItemDecorations(
        final @NotNull ItemStack stack,
        final @Local(argsOnly = true) LocalRef<String> text
    ) {
        if (this.cm_unique$isCompactedItem = ((PotentiallyCompactedItem) (Object) stack).isMarkedAsCompacted()) {
            text.set(Integer.toString(stack.getCount()));
        }
        return stack;
    }

    @ModifyConstant(
        method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
        constant = @Constant(intValue = 16777215)
    )
    public int cm_modify_constant$renderItemDecorations(
        final int decorationColour
    ) {
        if (this.cm_unique$isCompactedItem) {
            return AbstractCivModernMod.getInstance().getColourProvider().getCompactedColour();
        }
        return decorationColour;
    }
}
