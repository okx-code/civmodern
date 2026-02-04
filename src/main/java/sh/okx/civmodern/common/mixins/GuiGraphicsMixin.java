package sh.okx.civmodern.common.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.features.CompactedItem;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @ModifyVariable(
        method = "renderItemCount",
        at = @At("HEAD"),
        argsOnly = true
    )
    protected @Nullable String civmodern$showCompactedItemCount(
        final String value,
        final @Local(argsOnly = true) ItemStack item
    ) {
        if (CompactedItem.getType(item) == CompactedItem.NEITHER || value != null) {
            return value;
        }
        return String.valueOf(item.getCount());
    }

    @ModifyArg(
        method = "renderItemCount",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"
        ),
        index = 4, // "k"
        remap = false
    )
    protected int civmodern$colourCompactedItemCount(
        final int originalColour,
        final @Local(argsOnly = true) ItemStack item
    ) {
        return 0xFF_00_00_00 | CompactedItem.getColourFor(CompactedItem.getType(item)).orElse(originalColour);
    }
}
