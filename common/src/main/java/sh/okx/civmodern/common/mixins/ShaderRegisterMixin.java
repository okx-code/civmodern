package sh.okx.civmodern.common.mixins;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sh.okx.civmodern.common.map.ShaderManager;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public class ShaderRegisterMixin {
    @Inject(at = @At(value = "NEW", target = "net/minecraft/client/renderer/ShaderInstance", ordinal = 0), method = "reloadShaders(Lnet/minecraft/server/packs/resources/ResourceManager;)V", locals = LocalCapture.CAPTURE_FAILHARD)
    private void reloadShaders(ResourceManager arg0, CallbackInfo ci, List list, List<Pair<ShaderInstance, Consumer<ShaderInstance>>> list1) throws IOException {
        ShaderManager.registerShaders(arg0, list1);
    }
}
