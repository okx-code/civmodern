package sh.okx.civmodern.common.events;

import java.util.function.Consumer;

public interface EventBus {
  <T extends Event> void listen(Class<T> event, Consumer<T> listener);
}
