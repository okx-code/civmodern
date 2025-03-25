package uk.protonull.civianmod.features;

import java.util.Arrays;
import java.util.stream.Collectors;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import uk.protonull.civianmod.CivianModHelpers;

public final class ClickRailDest {
    public static final boolean DEFAULT_ENABLED = false;
    public static volatile boolean ENABLED = DEFAULT_ENABLED;

    public static final long DEFAULT_CLICK_DELAY = 1_000; // 1000ms (1 second)
    public static volatile long CLICK_DELAY = DEFAULT_CLICK_DELAY;

    private static long lastClickTime = 0;

    public static @NotNull InteractionResult handleBlockClick(
        final @NotNull Player player,
        final @NotNull Level level,
        final @NotNull InteractionHand hand,
        final @NotNull BlockPos blockPos,
        final @NotNull Direction direction
    ) {
        if (!ENABLED) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof final LocalPlayer localPlayer)) {
            return InteractionResult.PASS;
        }

        final InteractionResult result; {
            final PlayerInfo info = localPlayer.getPlayerInfo();
            if (info == null) {
                return InteractionResult.PASS;
            }
            switch (info.getGameMode()) {
                case SURVIVAL, ADVENTURE -> result = InteractionResult.PASS;
                case CREATIVE -> result = InteractionResult.CONSUME;
                case null, default -> { return InteractionResult.PASS; }
            }
        }

        if (!(level.getBlockEntity(blockPos) instanceof final SignBlockEntity sign)) {
            return InteractionResult.PASS;
        }

        final Component[] lines = sign.getText(sign.isFacingFrontText(player)).getMessages(false);
        if (lines.length < 1) {
            return InteractionResult.PASS;
        }
        if (!CivianModHelpers.matchesPlainText(lines[0], "[set destination]", true)) {
            return InteractionResult.PASS;
        }

        if (isOnCooldown()) {
            return InteractionResult.PASS;
        }

        // FROM HERE, SETTING DESTINATION IS ASSURED

        final String dest = "dest %s".formatted(
            Arrays.stream(lines)
                .skip(1)
                .map(CivianModHelpers::getPlainString)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(" "))
        );

        localPlayer.minecraft.execute(() -> localPlayer.connection.sendCommand(dest));
        return result;
    }

    private static boolean isOnCooldown() {
        final long now = System.currentTimeMillis();
        final long clickDelay = CLICK_DELAY;
        if (clickDelay <= 0) {
            lastClickTime = 0L;
        }
        else if ((now - lastClickTime) < clickDelay) {
            return true;
        }
        else {
            lastClickTime = now;
        }
        return false;
    }
}
