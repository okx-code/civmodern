package sh.okx.civmodern.common.macro;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.ScrollEvent;
import sh.okx.civmodern.common.mixins.KeyMappingAccessor;

public class AttackMacro {

    private final KeyMapping holdBinding;
    private final KeyMapping defaultBinding;

    private boolean attacking = false;

    private boolean down = false;

    private long lastAttack;

    public AttackMacro(AbstractCivModernMod mod, KeyMapping holdBinding, KeyMapping defaultBinding) {
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

        if (attacking && (mc.screen != null || !mc.mouseHandler.isMouseGrabbed())) {
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
            if (attacking) {
                set(false);
            } else {
                set(true);
            }
        }

        if (attacking) {
            if (down) {
                down = false;
                return;
            } else if (lastAttack + 200 > System.currentTimeMillis()) {
                return;
            }

            KeyMappingAccessor accessor = (KeyMappingAccessor) defaultBinding;

            accessor.setClickCount(accessor.getClickCount() + 1);

            down = true;
            lastAttack = System.currentTimeMillis();
        }
    }

    @Subscribe
    private void onScroll(
        final @NotNull ScrollEvent event
    ) {
        set(false);
    }

    private void set(boolean attacking) {
        if (this.attacking == attacking) {
            return;
        }
        this.attacking = attacking;
        if (!attacking) {
            this.defaultBinding.setDown(false);
        }
    }
}
