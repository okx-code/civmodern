package sh.okx.civmodern.mod.features.macros;

import com.google.common.eventbus.Subscribe;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.events.AllKeysReleasedEvent;
import sh.okx.civmodern.mod.events.ClientTickEvent;
import sh.okx.civmodern.mod.mixins.KeyMappingAccessor;

public final class HoldForwardMacro {
    private final KeyMapping macroBinding;
    private final KeyMapping forwardBinding;
    private final KeyMappingAccessor forwardBindingAccessor;
    private final KeyMapping backwardBinding;
    private boolean enabled = false;

    public HoldForwardMacro(
        final @NotNull Minecraft minecraft,
        final @NotNull KeyMapping macroBinding
    ) {
        this.macroBinding = Objects.requireNonNull(macroBinding);
        this.forwardBinding = minecraft.options.keyUp;
        this.forwardBindingAccessor = (KeyMappingAccessor) minecraft.options.keyUp;
        this.backwardBinding = minecraft.options.keyDown;
    }

    @Subscribe
    private void onTick(
        final @NotNull ClientTickEvent event
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.enabled = false;
            return;
        }

        if (this.forwardBindingAccessor.civmodern$accessor$getClickCount() > 0) {
            this.enabled = false;
        }

        if (this.backwardBinding.consumeClick()) {
            this.enabled = false;
            this.forwardBindingAccessor.civmodern$invoker$release();
        }

        while (this.macroBinding.consumeClick()) {
            setEnabled(!this.enabled);
        }
    }

    private void setEnabled(
        final boolean enabled
    ) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            this.forwardBinding.setDown(true);
        }
        else {
            this.forwardBindingAccessor.civmodern$invoker$release();
        }
    }

    @Subscribe
    private void onAllKeysReleased(
        final @NotNull AllKeysReleasedEvent event
    ) {
        if (this.enabled) {
            this.forwardBinding.setDown(true);
        }
    }
}
