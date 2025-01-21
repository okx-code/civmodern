package sh.okx.civmodern.mod.features;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public final class SafeMining {
    public static void emitPreventedParticle(
        final @NotNull LocalPlayer player,
        final @NotNull BlockHitResult hitResult,
        final @NotNull ItemStack tool
    ) {
        player.swing(InteractionHand.MAIN_HAND);

        final Vec3 direction = hitResult.getDirection().getUnitVec3(); // This will face outwards
        final Vec3 position = hitResult.getLocation().add(direction.multiply(0.1d, 0.1d, 0.1d));

        final Random random = ThreadLocalRandom.current();

        player.clientLevel.addAlwaysVisibleParticle(
            new ItemParticleOption(ParticleTypes.ITEM, tool),
            position.x,
            position.y,
            position.z,
            random.nextDouble(-0.03d, 0.03d),
            random.nextDouble(0.07d, 0.12d),
            random.nextDouble(-0.03d, 0.03d)
        );
    }
}
