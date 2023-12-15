package sh.okx.civmodern.fabric;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.Event;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.ScrollEvent;
import sh.okx.civmodern.common.events.WorldRenderEvent;

public class FabricEventBus implements EventBus {

  private final Map<Class<? extends Event>, Set<Consumer<Event>>> map = new ConcurrentHashMap<>();

  public FabricEventBus() {
    map.put(ClientTickEvent.class, new CopyOnWriteArraySet<>());
    map.put(PostRenderGameOverlayEvent.class, new CopyOnWriteArraySet<>());
    map.put(WorldRenderEvent.class, new CopyOnWriteArraySet<>());
    map.put(ScrollEvent.class, new CopyOnWriteArraySet<>());

    ClientTickEvents.START_CLIENT_TICK.register(client -> push(new ClientTickEvent()));
    HudRenderCallback.EVENT.register(((guiGraphics, tickDelta) -> push(new PostRenderGameOverlayEvent(guiGraphics, tickDelta))));
  }

  @Override
  public void push(Event event) {
    for (Consumer<Event> consumer : map.getOrDefault(event.getClass(), Collections.emptySet())) {
      consumer.accept(event);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Event> void listen(Class<T> event, Consumer<T> listener) {
    Set<Consumer<Event>> set = map.get(event);
    if (set == null) {
      throw new IllegalArgumentException("Class not supported: " + event);
    }
    set.add((Consumer<Event>) listener);
  }
}
