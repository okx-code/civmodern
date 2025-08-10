package sh.okx.civmodern.common.mixins;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;

// Mixin into ClientPacketListener to capture the hashed seed from the login packet
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onHandleLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        AbstractCivModernMod.getInstance().getWorldListener().setSeed(packet.commonPlayerSpawnInfo().seed());
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void onHandleLogin(ClientboundRespawnPacket packet, CallbackInfo ci) {
        AbstractCivModernMod.getInstance().getWorldListener().setSeed(packet.commonPlayerSpawnInfo().seed());
    }
}
