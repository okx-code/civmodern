package sh.okx.civmodern.common.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.RespawnEvent;

@Mixin(ClientPacketListener.class)
public class RespawnMixin {
  @Inject(at = @At("TAIL"), method = "handleRespawn(Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;)V")
  private void handleRespawn(ClientboundRespawnPacket packet, CallbackInfo info) {
    AbstractCivModernMod.getInstance().eventBus.post(new RespawnEvent());
  }
}
