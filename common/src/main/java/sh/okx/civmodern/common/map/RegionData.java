package sh.okx.civmodern.common.map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MaterialColor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

public class RegionData {

    private static final Set<Block> INVISIBLE_BLOCKS = Set.of(Blocks.TALL_GRASS, Blocks.FERN, Blocks.GRASS, Blocks.LARGE_FERN);


    // 16 bits - block EXCEPT water
    // 4 bits - water depth, > 0 if water, unsigned
    // 4 bits - y offset from previous block, signed
    // 8 bits - biome
    // TODO optimize for cache lines, currently we are using 64 cache lines per chunk when we can use 16
    private final int[] data = new int[512 * 512];

    public void updateChunk(LevelChunk chunk) {
        int rx = chunk.getPos().getRegionLocalX() * 16;
        int rz = chunk.getPos().getRegionLocalZ() * 16;

        int[] yValues = new int[16];
        for (int x = rx; x < rx + 16; x++) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getMaxBuildHeight(), chunk.getPos().getRegionZ() * 512);
            iterateDown(chunk, pos);
            yValues[x - rx] = pos.getY();
        }
        for (int x = rx; x < rx + 16; x++) {
            for (int z = rz; z < rz + 16; z++) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getMaxBuildHeight(), z + chunk.getPos().getRegionZ() * 512);
                BlockState state = iterateDown(chunk, pos);

                int dataValue = 0;

                Block block;
                int depth;

                int yOffset = 0;

                DepthResult depthResult = findDepthIfFluid(pos, state, chunk);
                if (depthResult != null) {
                    block = depthResult.state.getBlock();
                    depth = depthResult.depth;
                } else {
                    block = state.getBlock();
                    depth = 0;
                    yOffset = Mth.clamp(pos.getY() - yValues[x - rx], -8, 7);
                    if (yOffset < 0) {
                        yOffset = -yOffset - 1 ^ 0xF;
                    }
                    yValues[x - rx] = pos.getY();
                }

                if (yOffset > 15) {
                    throw new IllegalArgumentException("y offset invalid " + yOffset);
                }

                int blockId = Registry.BLOCK.getId(block);
                if (blockId > 0xFFFF) {
                    throw new IllegalArgumentException("block " + blockId + " at pos " + pos);
                }
                dataValue |= blockId << 16;
                dataValue |= Math.min(depth, 0xF) << 12;
                dataValue |= yOffset << 8; // y offset

                int biomeId = chunk.getLevel().registryAccess().registry(Registry.BIOME_REGISTRY).get().getId(chunk.getLevel().getBiome(pos).value());
                if (biomeId > 0xFF) {
                    throw new IllegalArgumentException("biome " + biomeId + " at pos " + pos);
                }
                dataValue |= biomeId;

                data[z + x * 512] = dataValue;
            }
        }
    }

    private record DepthResult(int depth, BlockState state) {
    }

    private static @Nullable DepthResult findDepthIfFluid(final BlockPos blockPos, final BlockState state, final LevelChunk chunk) {
        if (blockPos.getY() > chunk.getMinBuildHeight() && state.getBlock() == Blocks.WATER) {
            BlockState fluidState;
            int fluidDepth = 0;

            int yBelowSurface = blockPos.getY() - 1;
            final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            mutablePos.set(blockPos);
            do {
                mutablePos.setY(yBelowSurface--);
                fluidState = chunk.getBlockState(mutablePos);
                ++fluidDepth;
            } while (yBelowSurface > chunk.getMinBuildHeight() && fluidDepth <= 10 && !fluidState.getFluidState().isEmpty());

            return new DepthResult(fluidDepth, fluidState);
        }
        return null;
    }

    private BlockState iterateDown(LevelChunk chunk, BlockPos.MutableBlockPos mutablePos) {
        BlockState state;
        if (chunk.getLevel().dimensionType().hasCeiling()) {
            do {
                mutablePos.move(Direction.DOWN);
                state = chunk.getBlockState(mutablePos);
            } while (!state.isAir() && mutablePos.getY() > chunk.getMinBuildHeight());
        }
        do {
            mutablePos.move(Direction.DOWN);
            state = chunk.getBlockState(mutablePos);
        } while ((state.getBlock().defaultMaterialColor() == MaterialColor.NONE || INVISIBLE_BLOCKS.contains(state.getBlock())) && mutablePos.getY() > chunk.getMinBuildHeight());
        return state;
    }

    public void render(RegionTexture texture) {
        int[] colours = texture.getColours();

        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                int packedData = data[z + x * 512];
                int blockId = (packedData >>> 16) & 0xFFFF;
                int waterDepth = (packedData >>> 12) & 0xF;
                int yOffset = (packedData >>> 8) & 0xF;
                int biome = packedData & 0xFF;

                Holder<Block> blockHolder = Registry.BLOCK.getHolder(blockId).get();
                int color = ColoursConfig.BLOCK_COLOURS.getOrDefault(blockHolder.unwrapKey().get().location().toString(), blockHolder.value().defaultMaterialColor().col);

                if (waterDepth > 0) {
                    int blockColor = color;
                    color = shade(ColoursConfig.BLOCK_COLOURS.get("minecraft:water"), 0.85F - (waterDepth * 0.01F));
                    color = mix(color, blockColor, 0.2F / (waterDepth / 2.0F));
                } else {
                    int odd = (x + z & 1);
                    double diffY = ((double) (yOffset > 7 ? -(yOffset ^ 0xF) - 1 : yOffset)) * 4.0D / (double) 4 + ((double) odd - 0.5D) * 0.4D;
                    byte colorOffset = (byte) (diffY > 0.6D ? 2 : (diffY < -0.6D ? 0 : 1));
                    if (yOffset != 0) {
                        System.out.println(yOffset);
                    }
                    color = shade(color, colorOffset);
                }

                // rightmost 8 bits are alpha, representing water depth or y offset
                colours[z + x * 512] = color << 8;
            }
        }

        texture.update();
    }

    public static int shade(int color, float ratio) {
        int r = (int) ((color >> 16 & 0xFF) * ratio);
        int g = (int) ((color >> 8 & 0xFF) * ratio);
        int b = (int) ((color & 0xFF) * ratio);
        return (r << 16) | (g << 8) | b;
    }

    public static int shade(int color, int shade) {
        final float ratio = switch (shade) {
            case 0 -> 180F / 255F;
            case 1 -> 220F / 255F;
            case 2 -> 1.0F;
            default -> throw new IllegalStateException("Unexpected shade: " + shade);
        };
        return shade(color, ratio);
    }

    public static int mix(int c1, int c2, float ratio) {
        if (ratio >= 1F) {
            return c2;
        } else if (ratio <= 0F) {
            return c1;
        }
        float iRatio = 1.0F - ratio;

        int r1 = c1 >> 16 & 0xFF;
        int g1 = c1 >> 8 & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = c2 >> 16 & 0xFF;
        int g2 = c2 >> 8 & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return r << 16 | g << 8 | b;
    }
}
