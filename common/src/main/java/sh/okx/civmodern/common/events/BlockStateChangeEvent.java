package sh.okx.civmodern.common.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public record BlockStateChangeEvent(Level level, BlockPos pos) implements Event {}
