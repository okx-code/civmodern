package sh.okx.civmodern.mod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sh.okx.civmodern.mod.config.CivModernConfig;
import sh.okx.civmodern.mod.features.SafeMining;

@Mixin(Minecraft.class)
public abstract class SafeMiningMixin {
    @Shadow
    public @Nullable LocalPlayer player;

    @Shadow
    public @Nullable MultiPlayerGameMode gameMode;

    @Inject(
        method = "startAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    protected void civmodern$preventToolBreakage(
        final @NotNull CallbackInfoReturnable<Boolean> cir,
        final @Local @NotNull ItemStack tool,
        final @Local @NotNull BlockHitResult hitResult
    ) {
        if (!CivModernConfig.HANDLER.instance().itemSettings.safeMining) {
            return;
        }
        assert this.player != null;
        if (tool.nextDamageWillBreak()) {
            SafeMining.emitPreventedParticle(this.player, hitResult, tool);
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "continueAttack",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    protected void civmodern$preventToolBreakage(
        final boolean leftClick,
        final @NotNull CallbackInfo ci,
        final @Local @NotNull BlockHitResult hitResult
    ) {
        if (!CivModernConfig.HANDLER.instance().itemSettings.safeMining) {
            return;
        }
        assert this.player != null;
        final ItemStack tool = this.player.getMainHandItem();
        if (tool.nextDamageWillBreak()) {
            SafeMining.emitPreventedParticle(this.player, hitResult, tool);
            this.gameMode.stopDestroyBlock();
            ci.cancel();
        }
    }
}
