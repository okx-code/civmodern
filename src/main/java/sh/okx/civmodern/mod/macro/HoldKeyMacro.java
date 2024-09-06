package sh.okx.civmodern.mod.macro;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.mod.CivModernMod;
import sh.okx.civmodern.mod.events.ClientTickEvent;
import sh.okx.civmodern.mod.events.ScrollEvent;

public class HoldKeyMacro {

    private final KeyMapping holdBinding;
    private final KeyMapping defaultBinding;
    private boolean down = false;

    public HoldKeyMacro(CivModernMod mod, KeyMapping holdBinding, KeyMapping defaultBinding) {
        mod.eventBus.register(this);
        this.holdBinding = holdBinding;
        this.defaultBinding = defaultBinding;
    }

    @Subscribe
    private void tick(
        final @NotNull ClientTickEvent event
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (down && (mc.screen != null || !mc.mouseHandler.isMouseGrabbed())) {
            set(false);
            return;
        }

        for (KeyMapping hotbar : mc.options.keyHotbarSlots) {
            if (hotbar.isDown()) {
                set(false);
                return;
            }
        }

        while (holdBinding.consumeClick()) {
            if (this.down) {
                set(false);
            } else if (!mc.player.isUsingItem()) {
                set(true);
            }
        }
    }

    @Subscribe
    private void onScroll(
        final @NotNull ScrollEvent event
    ) {
        set(false);
    }

    private void set(boolean down) {
        if (this.down == down) {
            return;
        }
        this.down = down;
        this.defaultBinding.setDown(down);
    }
}
