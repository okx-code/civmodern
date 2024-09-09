package sh.okx.civmodern.mod.features.macros;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.events.ClientTickEvent;
import sh.okx.civmodern.mod.mixins.KeyMappingAccessor;

public final class ToggleSneakMacro {
    private final KeyMapping macroBinding;
    private final KeyMapping sneakBinding;
    private final KeyMappingAccessor sneakBindingAccessor;
    private final OptionInstance<Boolean> toggleSneak;

    public ToggleSneakMacro(
        final @NotNull Minecraft minecraft,
        final @NotNull KeyMapping macroBinding
    ) {
        this.macroBinding = Objects.requireNonNull(macroBinding);
        this.sneakBinding = minecraft.options.keyShift;
        this.sneakBindingAccessor = (KeyMappingAccessor) this.sneakBinding;
        this.toggleSneak = minecraft.options.toggleCrouch();
    }

    @Subscribe
    private void onTick(
        final @NotNull ClientTickEvent event
    ) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        final boolean isSneaking = this.sneakBinding.isDown();

        while (this.macroBinding.consumeClick()) {
            this.toggleSneak.set(false);
            if (isSneaking) {
                this.sneakBinding.setDown(false);
                continue;
            }
            this.sneakBinding.setDown(true);
            this.toggleSneak.set(true);
        }

        while (this.sneakBinding.consumeClick()) {
            this.toggleSneak.set(false);
        }

        if (
            player.isInWater()
                || player.isSwimming()
                || player.isSprinting()
                || player.isFallFlying() // Elytra gliding
                || player.getAbilities().flying // Creative flying
        ) {
            this.toggleSneak.set(false);
            this.sneakBinding.setDown(InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow().getWindow(),
                this.sneakBindingAccessor.civmodern$accessor$getKey().getValue()
            ));
        }
    }
}
