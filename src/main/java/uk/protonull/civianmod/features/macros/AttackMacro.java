package uk.protonull.civianmod.features.macros;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.events.StartOfClientTickEvent;
import uk.protonull.civianmod.events.BeforeHotbarSlotChangedEvent;
import uk.protonull.civianmod.mixins.KeyMappingAccessor;

public class AttackMacro {

    private final KeyMapping holdBinding;
    private final KeyMapping defaultBinding;

    private boolean attacking = false;

    private boolean down = false;

    private long lastAttack;

    public AttackMacro(KeyMapping holdBinding, KeyMapping defaultBinding) {
        this.holdBinding = holdBinding;
        this.defaultBinding = defaultBinding;
    }

    @Subscribe
    private void tick(
        final @NotNull StartOfClientTickEvent event
    ) {
        final Minecraft mc = event.minecraft();
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
            } else if (lastAttack + 1000 > System.currentTimeMillis()) {
                return;
            }

            KeyMappingAccessor accessor = (KeyMappingAccessor) defaultBinding;

            accessor.civianmod$setClickCount(accessor.civianmod$getClickCount() + 1);

            down = true;
            lastAttack = System.currentTimeMillis();
        }
    }

    @Subscribe
    private void onScroll(
        final @NotNull BeforeHotbarSlotChangedEvent event
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