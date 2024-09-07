package sh.okx.civmodern.mod.mixins;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.gui.ElementListWidgetExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ElementListWidgetExt.class)
public abstract class YaclAllowMiddleClickMixin {
    /**
     * @author Protonull
     * @reason Fixes <a href="https://github.com/isXander/YetAnotherConfigLib/issues/207">#207</a>
     */
    @Overwrite
    protected boolean isValidMouseClick(
        final int button
    ) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_MIDDLE, InputConstants.MOUSE_BUTTON_RIGHT -> true;
            default -> false;
        };
    }
}
