package sh.okx.civmodern.common.mixins.serverdata;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sh.okx.civmodern.common.CivServer;

@Mixin(ServerData.class)
public abstract class ServerDataMixin implements CivServer.ServerData {
    @Unique
    private boolean _cm$isCivServer;

    @Unique
    @Override
    public boolean isCivServer() {
        return this._cm$isCivServer;
    }

    @Unique
    @Override
    public void isCivServer(
        final boolean isCivServer
    ) {
        this._cm$isCivServer = isCivServer;
    }

    @Inject(
        method = "copyFrom",
        at = @At("TAIL")
    )
    private void cm_inject$copyFrom(
        final @NotNull ServerData serverData,
        final @NotNull CallbackInfo ci
    ) {
        this._cm$isCivServer = ((CivServer.ServerData) serverData).isCivServer();
    }

    @Inject(
        method = "write",
        at = @At(
            value = "RETURN",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void cm_inject$write(
        final @NotNull CallbackInfoReturnable<CompoundTag> cir,
        final @NotNull CompoundTag tag
    ) {
        tag.putBoolean(CivServer.IS_CIV_SERVER_KEY, isCivServer());
    }

    @Inject(
        method = "read",
        at = @At(
            value = "RETURN",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void cm_inject$read(
        final @NotNull CompoundTag compoundTag,
        final @NotNull CallbackInfoReturnable<ServerData> cir,
        final @NotNull ServerData serverData
    ) {
        ((ServerDataMixin) (Object) serverData).isCivServer(compoundTag.getBoolean(CivServer.IS_CIV_SERVER_KEY));
    }
}
