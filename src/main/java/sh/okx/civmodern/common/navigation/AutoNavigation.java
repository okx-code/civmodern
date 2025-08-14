package sh.okx.civmodern.common.navigation;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.HappyGhast;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class AutoNavigation {

    private Deque<Vec2> destinations = new ArrayDeque<>();

    public AutoNavigation(AbstractCivModernMod mod) {
        mod.eventBus.register(this);
    }

    public void addDestination(Vec2 destination) {
        this.destinations.add(destination);
    }

    public Deque<Vec2> getDestinations() {
        return destinations;
    }

    public void reset() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.keyUp.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        this.destinations.clear();
        rotation = 0;
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

        boolean isValidVehicle = (
                player.getVehicle() instanceof HappyGhast || player.getVehicle() instanceof AbstractHorse || player.getVehicle() instanceof Boat
        );
        if (!isValidVehicle) {
            reset();
            return;
        }

        // TODO when user hits a key then stop macro (maybe add button to resume previous route)
        // TODO shadow? when showing preview route
        // TODO shift click - show this on the gui instead of queuing

        if (player.getVehicle() instanceof Boat boat) {
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
        } else {
            Vec2 destination = destinations.peek();
            if (Mth.lengthSquared(player.getVehicle().getX() - destination.x, player.getVehicle().getZ() - destination.y) < 2 * 2) {
                // Look in the direction of the destination
                destinations.poll();
                if (destinations.peek() == null) {
                    mc.options.keyLeft.setDown(false);
                    mc.options.keyRight.setDown(false);
                    mc.options.keyUp.setDown(false);
                    return;
                }
            } else {
                 // Janky turning. Toggleable by config
                 Vec3 destinationVec3 = new Vec3(destination.x, player.getEyeY(), destination.y);

                 float turnLerp = 0.01f;
                 double destinationDistance = destinationVec3.subtract(player.getEyePosition()).length();
                 Vec3 turnStart = player.getEyePosition().add(player.getLookAngle().multiply(destinationDistance, 0, destinationDistance));
                 Vec3 turnCurrent = turnStart.lerp(destinationVec3, turnLerp);

                 player.lookAt(EntityAnchorArgument.Anchor.EYES, turnCurrent);
                 player.getVehicle().lookAt(EntityAnchorArgument.Anchor.EYES, turnCurrent);
            }
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyUp.setDown(true);
        }
    }
}
