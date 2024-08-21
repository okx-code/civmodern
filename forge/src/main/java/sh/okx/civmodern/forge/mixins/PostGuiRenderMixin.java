package sh.okx.civmodern.forge.mixins;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;

/**
 * This didn't use to be necessary thanks to the RenderGuiEvent.Post event, but that seems to have been removed and
 * there's no equivalent that I am aware of. This is a stopgap measure until an equivalent or better is found.
 */
@Mixin(Gui.class)
public abstract class PostGuiRenderMixin {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LayeredDraw;render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            shift = At.Shift.AFTER
        )
    )
    protected void civmodern$emitPostRenderEvent(
        final @NotNull GuiGraphics guiGraphics,
        final float partialTick,
        final @NotNull CallbackInfo ci
    ) {
        AbstractCivModernMod.getInstance().eventBus.post(new PostRenderGameOverlayEvent(
            guiGraphics,
            partialTick
        ));
    }
}
