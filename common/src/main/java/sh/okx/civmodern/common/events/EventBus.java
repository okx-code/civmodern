package sh.okx.civmodern.common.events;

import com.google.common.eventbus.DeadEvent;
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
}
