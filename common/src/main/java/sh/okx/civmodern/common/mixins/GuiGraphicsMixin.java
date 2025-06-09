package sh.okx.civmodern.common.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.features.ExtendedItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(method = "renderItemCount", at = @At("HEAD"), argsOnly = true)
    protected @Nullable String civmodern$showCompactedItem(String value, @Local(argsOnly = true) ItemStack item) {
        if (value != null) {
            return value;
        } else if (((ExtendedItemStack) (Object) item).isMarkedAsCompacted()) {
            return String.valueOf(item.getCount());
        } else {
            return null;
        }
    }

    @ModifyConstant(method = "renderItemCount", constant = @Constant(intValue = -1))
    protected int civmodern$colourCompactedItem(int itemColour, @Local(argsOnly = true) ItemStack item) {
        if (((ExtendedItemStack) (Object) item).isMarkedAsCompacted()) {
            return AbstractCivModernMod.getInstance().getColourProvider().getCompactedColour();
        } else {
            return itemColour;
        }
    }
}
