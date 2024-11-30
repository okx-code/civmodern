package sh.okx.civmodern.mod.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.gui.ElementListWidgetExt;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElementListWidgetExt.class)
public abstract class YaclAllowMiddleClickMixin {
    /**
     * Fixes <a href="https://github.com/isXander/YetAnotherConfigLib/issues/207">#207</a>
     */
    @Inject(
        method = "isValidMouseClick",
        at = @At("HEAD"),
        cancellable = true
    )
    protected void civmodern$allowMiddleMouseClicks(
        final int button,
        final @NotNull CallbackInfoReturnable<Boolean> cir
    ) {
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) {
            cir.setReturnValue(true);
        }
    }
}
