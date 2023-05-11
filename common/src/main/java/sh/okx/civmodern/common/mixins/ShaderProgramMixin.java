package sh.okx.civmodern.common.mixins;

import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sh.okx.civmodern.common.map.FabricShaderProgram;

@Mixin(ShaderInstance.class)
abstract class ShaderProgramMixin {
    @Shadow
    @Final
    private String name;

    // Allow loading FabricShaderPrograms from arbitrary namespaces.
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;)V"), allow = 1)
    private String modifyProgramId(String id) {
        if ((Object) this instanceof FabricShaderProgram) {
            return FabricShaderProgram.rewriteAsId(id, name);
        }

        return id;
    }

    // Allow loading shader stages from arbitrary namespaces.
    @ModifyVariable(method = "getOrCreate", at = @At("STORE"), ordinal = 1)
    private static String modifyStageId(String id, ResourceProvider factory, Program.Type type, String name) {
        if (name.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            return FabricShaderProgram.rewriteAsId(id, name);
        }

        return id;
    }
}