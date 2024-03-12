package sh.okx.civmodern.common.mixins.serverdata;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.CivServer;
import sh.okx.civmodern.common.mixins.accessors.ScreenAccessor;

@Mixin(DirectJoinServerScreen.class)
public abstract class DirectJoinServerScreenMixin implements ScreenAccessor {
    @Final
    @Shadow
    private ServerData serverData;

    @Shadow
    private EditBox ipEdit;

    @Inject(
        method = "init",
        at = @At("TAIL")
    )
    private void cm_inject$init(
        final @NotNull CallbackInfo ci
    ) {
        cm_invoker$addRenderableWidget(CivServer.createCheckbox(
            this.ipEdit.getX() + this.ipEdit.getWidth() + 4,
            this.ipEdit.getY() + 1,
            cm_accessor$getFont(),
            (CivServer.ServerData) this.serverData
        ));
    }
}
