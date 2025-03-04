package uk.protonull.civianmod.features.macros;

import com.google.common.eventbus.Subscribe;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.events.ClientTickEvent;

public final class HoldForwardMacro {
    private final KeyMapping macroBinding;
    private final KeyMapping forwardBinding;
    private final KeyMapping backwardBinding;

    @ApiStatus.Internal
    public static boolean enabled = false;

    public HoldForwardMacro(
        final @NotNull Minecraft minecraft,
        final @NotNull KeyMapping macroBinding
    ) {
        this.macroBinding = Objects.requireNonNull(macroBinding);
        this.forwardBinding = minecraft.options.keyUp;
        this.backwardBinding = minecraft.options.keyDown;
    }

    @Subscribe
    private void onTick(
        final @NotNull ClientTickEvent event
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || this.macroBinding.isUnbound()) {
            enabled = false;
            return;
        }

        if (this.forwardBinding.isDown() || this.backwardBinding.isDown()) {
            enabled = false;
            return;
        }

        while (this.macroBinding.consumeClick()) {
            enabled = !enabled;
        }
    }
}
