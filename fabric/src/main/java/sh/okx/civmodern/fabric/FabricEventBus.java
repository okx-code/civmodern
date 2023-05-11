package sh.okx.civmodern.fabric;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import sh.okx.civmodern.common.events.*;

public class FabricEventBus implements EventBus {

  private final Map<Class<? extends Event>, Set<Consumer<Event>>> map = new ConcurrentHashMap<>();

  public FabricEventBus() {
    map.put(ClientTickEvent.class, new CopyOnWriteArraySet<>());
    map.put(PostRenderGameOverlayEvent.class, new CopyOnWriteArraySet<>());
    map.put(WorldRenderEvent.class, new CopyOnWriteArraySet<>());
    map.put(ScrollEvent.class, new CopyOnWriteArraySet<>());
    map.put(ChunkLoadEvent.class, new CopyOnWriteArraySet<>());
    map.put(JoinEvent.class, new CopyOnWriteArraySet<>());
    map.put(LeaveEvent.class, new CopyOnWriteArraySet<>());
    map.put(RespawnEvent.class, new CopyOnWriteArraySet<>());
    map.put(BlockStateChangeEvent.class, new CopyOnWriteArraySet<>());

    ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> push(new JoinEvent()));
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> push(new LeaveEvent()));
    ClientTickEvents.START_CLIENT_TICK.register(client -> push(new ClientTickEvent()));
    HudRenderCallback.EVENT.register(((matrixStack, tickDelta) -> push(new PostRenderGameOverlayEvent(matrixStack, tickDelta))));
    ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> push(new ChunkLoadEvent(level, chunk)));
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
