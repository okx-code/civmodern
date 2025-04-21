package uk.protonull.civianmod.features.macros;

import com.google.common.eventbus.Subscribe;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.events.StartOfClientTickEvent;

public final class ToggleSneakMacro {
    private final KeyMapping macroBinding;
    private final KeyMapping sneakBinding;

    @ApiStatus.Internal
    public static boolean enabled = false;

    public ToggleSneakMacro(
        final @NotNull Minecraft minecraft,
        final @NotNull KeyMapping macroBinding
    ) {
        this.macroBinding = Objects.requireNonNull(macroBinding);
        this.sneakBinding = minecraft.options.keyShift;
    }

    @Subscribe
    private void onTick(
        final @NotNull StartOfClientTickEvent event
    ) {
        final LocalPlayer player = event.minecraft().player;
        if (player == null || this.macroBinding.isUnbound() || event.minecraft().options.toggleCrouch().get()) {
            enabled = false;
            return;
        }

        while (this.macroBinding.consumeClick()) {
            enabled = !enabled;
        }

        while (this.sneakBinding.consumeClick()) {
            enabled = false;
        }

        if (
            player.isInWater()
                || player.isSwimming()
                || player.isSprinting()
                || player.isFallFlying() // Elytra gliding
                || player.getAbilities().flying // Creative flying
        ) {
            enabled = false;
        }
    }
}
