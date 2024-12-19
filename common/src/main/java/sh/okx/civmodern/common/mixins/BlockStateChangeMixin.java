package sh.okx.civmodern.common.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.BlockStateChangeEvent;

@Mixin(Level.class)
public class BlockStateChangeMixin {
  @Inject(at = @At("HEAD"), method = "onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V")
  private void handleRespawn(BlockPos pos, BlockState before, BlockState after, CallbackInfo info) {
    AbstractCivModernMod.getInstance().eventBus.post(new BlockStateChangeEvent((Level)(Object)this, pos));
  }
}
