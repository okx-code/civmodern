package sh.okx.civmodern.common.mixins;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sh.okx.civmodern.common.map.FabricShaderProgram;

@Mixin(targets = "net.minecraft.client.renderer.ShaderInstance$1")
abstract class ShaderProgramImportProcessorMixin {
    @Unique
    private String capturedImport;

    @Inject(method = "applyImport", at = @At("HEAD"))
    private void captureImport(boolean inline, String name, CallbackInfoReturnable<String> info) {
        capturedImport = name;
    }

    @ModifyVariable(method = "applyImport", at = @At("STORE"), ordinal = 0, argsOnly = true)
    private String modifyImportId(String id, boolean inline) {
        if (!inline && capturedImport.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            return FabricShaderProgram.rewriteAsId(id, capturedImport);
        }

        return id;
    }

    @Inject(method = "applyImport", at = @At("RETURN"))
    private void uncaptureImport(boolean inline, String name, CallbackInfoReturnable<String> info) {
        capturedImport = null;
    }
}