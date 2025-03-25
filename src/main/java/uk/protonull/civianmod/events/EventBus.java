package uk.protonull.civianmod.events;

import com.google.common.eventbus.DeadEvent;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public final class EventBus extends com.google.common.eventbus.EventBus {
    public EventBus(
        final @NotNull String identifier
    ) {
        super(identifier);
    }

    @Override
    public void post(
        final @NotNull Object event
    ) {
        if (!(event instanceof DeadEvent)) {
            super.post(event);
        }
    }

    public void emitStartOfClientTickEvent(
        final @NotNull Minecraft minecraft
    ) {
        post(new StartOfClientTickEvent(minecraft));
    }
}
