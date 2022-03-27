package sh.okx.civmodern.forge;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import net.minecraft.client.renderer.debug.WorldGenAttemptRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sh.okx.civmodern.common.events.ClientTickEvent;
import sh.okx.civmodern.common.events.Event;
import sh.okx.civmodern.common.events.EventBus;
import sh.okx.civmodern.common.events.PostRenderGameOverlayEvent;
import sh.okx.civmodern.common.events.ScrollEvent;
import sh.okx.civmodern.common.events.WorldRenderEvent;

public class ForgeEventBus implements EventBus {

  private final Map<Class<? extends Event>, Set<Consumer<Event>>> map = new ConcurrentHashMap<>();

  public ForgeEventBus() {
    map.put(ClientTickEvent.class, new CopyOnWriteArraySet<>());
    map.put(PostRenderGameOverlayEvent.class, new CopyOnWriteArraySet<>());
    map.put(WorldRenderEvent.class, new CopyOnWriteArraySet<>());
    map.put(ScrollEvent.class, new CopyOnWriteArraySet<>());

    MinecraftForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (event.phase == Phase.START) {
      push(new ClientTickEvent());
    }
  }

  @SubscribeEvent
  public void onRender(RenderGameOverlayEvent.Post event) {
    if (event.getType() == ElementType.ALL) {
     push(new PostRenderGameOverlayEvent(event.getMatrixStack(), event.getPartialTicks()));
    }
  }

  @SubscribeEvent
  public void onWorldRender(RenderLevelLastEvent event) {
    push(new WorldRenderEvent(event.getPoseStack(), event.getPartialTick()));
  }

  @Override
  public void push(Event event) {
    for (Consumer<Event> consumer : map.getOrDefault(event.getClass(), Collections.emptySet())) {
      consumer.accept(event);
    }
  }

  @Override
  public <T extends Event> void listen(Class<T> event, Consumer<T> listener) {
    Set<Consumer<Event>> set = map.get(event);
    if (set == null) {
      throw new IllegalArgumentException("Class not supported: " + event);
    }
    set.add((Consumer<Event>) listener);
  }
}
