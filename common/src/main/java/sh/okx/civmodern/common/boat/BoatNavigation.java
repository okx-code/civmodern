package sh.okx.civmodern.common.boat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec2;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

public class BoatNavigation {

  private Queue<Vec2> destinations = new ArrayDeque<>();

  public BoatNavigation(AbstractCivModernMod mod) {
      mod.eventBus.register(this);
  }

  public void addDestination(Vec2 destination) {
    this.destinations.add(destination);
  }

  public void setDestination(Vec2 destination) {
    Minecraft mc = Minecraft.getInstance();
    mc.options.keyUp.setDown(false);
    mc.options.keyLeft.setDown(false);
    mc.options.keyRight.setDown(false);
    this.destinations.clear();
    this.destinations.add(destination);
  }

  public Queue<Vec2> getDestinations() {
    return destinations;
  }

  private double rotation = 0;

  @Subscribe
  public void tick(ClientTickEvent event) {
    if (destinations.isEmpty()) {
      return;
    }

    Minecraft mc = Minecraft.getInstance();
    LocalPlayer player = mc.player;
    if (player == null) {
      this.destinations.clear();
      rotation = 0;
      return;
    }

    if (!(player.getVehicle() instanceof Boat boat)) {
      mc.options.keyUp.setDown(false);
      mc.options.keyLeft.setDown(false);
      mc.options.keyRight.setDown(false);
      this.destinations.clear();
      rotation = 0;
      return;
    }

    Vec2 destination = destinations.peek();
    if (Mth.lengthSquared(player.getVehicle().getX() - destination.x, player.getVehicle().getZ() - destination.y) < 5 * 5) {
      mc.options.keyUp.setDown(false);
      mc.options.keyLeft.setDown(false);
      mc.options.keyRight.setDown(false);
      rotation = 0;
      destinations.poll();
      if ((destination = destinations.peek()) == null) {
        return;
      }
    }

    // TODO when user hits a key then stop macro (maybe add button to resume previous route)
    // TODO shadow? when showing preview route
    // TODO shift click - show this on the gui instead of queuing


    float yRot = boat.getYRot();
    float yRotRadians = (float) Math.toRadians(yRot);
    if (yRotRadians < 0) {
      yRotRadians += 2 * Mth.PI;
    }
    double target = Mth.atan2(-destination.x + player.getX(), destination.y - player.getZ());
    double diff = yRotRadians - target;
    while (diff < 0) {
      diff += 2 * Mth.PI;
    }
    while (diff > 2 * Mth.PI) {
      diff -= 2 * Mth.PI;
    }
    double remainingMovement = Math.toRadians((Math.abs(rotation) + 1.1) * 10);
    // todo fix signs here, this is not differentiating between left and right rotation
    rotation *= 0.9;
    if (diff < remainingMovement || Math.PI * 2 - diff < remainingMovement) {
      mc.options.keyLeft.setDown(false);
      mc.options.keyRight.setDown(false);
    } else if (diff < Mth.PI) {
      rotation--;
      mc.options.keyLeft.setDown(true);
      mc.options.keyRight.setDown(false);
    } else {
      rotation++;
      mc.options.keyLeft.setDown(false);
      mc.options.keyRight.setDown(true);
    }
    mc.options.keyUp.setDown(true);
  }
}
