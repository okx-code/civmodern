package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.MaterialColor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Set;

public class RegionData {

    private static final Set<Block> INVISIBLE_BLOCKS = Set.of(Blocks.TALL_GRASS, Blocks.FERN, Blocks.GRASS, Blocks.LARGE_FERN);


    // 16 bits - block EXCEPT water
    // 4 bits - water depth, > 0 if water, unsigned
    // 2 bits - west Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 2 bits - north Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 8 bits - biome
    // TODO optimize for cache lines, currently we are using 64 cache lines per chunk when we can use 16
    private final int[] data = new int[512 * 512];

    public void updateChunk(LevelChunk chunk) {
        int rx = chunk.getPos().getRegionLocalX() * 16;
        int rz = chunk.getPos().getRegionLocalZ() * 16;

        int[] westY = new int[16];
        Arrays.fill(westY, Integer.MIN_VALUE);
        int northY = Integer.MIN_VALUE;
        for (int x = rx; x < rx + 16; x++) {
            BlockPos.MutableBlockPos pos = null;
            for (int z = rz; z < rz + 16; z++) {
                pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getMaxBuildHeight(), z + chunk.getPos().getRegionZ() * 512);
                BlockState state = iterateDown(chunk, pos);

                int dataValue = 0;

                Block block;
                int depth;

                DepthResult depthResult = findDepthIfFluid(pos, state, chunk);
                if (depthResult != null) {
                    block = depthResult.state.getBlock();
                    depth = depthResult.depth;
                } else {
                    block = state.getBlock();
                    depth = 0;
                }

                int blockId = Registry.BLOCK.getId(block);
                if (blockId > 0xFFFF) {
                    throw new IllegalArgumentException("block " + blockId + " at pos " + pos);
                }
                dataValue |= blockId << 16;
                dataValue |= Math.min(depth, 0xF) << 12;

                if (westY[z - rz] != Integer.MIN_VALUE) {
                    if (westY[z - rz] > pos.getY()) {
                        dataValue |= 0b11 << 10;
                    } else if (westY[z - rz] == pos.getY()) {
                        dataValue |= 0b01 << 10;
                    }
                } else {
                    dataValue |= 0b10 << 10;
                }
                westY[z - rz] = pos.getY();

                if (northY != Integer.MIN_VALUE) {
                    if (northY > pos.getY()) {
                        dataValue |= 0b11 << 8;
                    } else if (northY == pos.getY()) {
                        dataValue |= 0b01 << 8;
                    }
                } else {
                    dataValue |= 0b10 << 8;
                }
                northY = pos.getY();

                int biomeId = chunk.getLevel().registryAccess().registry(Registry.BIOME_REGISTRY).get().getId(chunk.getLevel().getBiome(pos).value());
                if (biomeId > 0xFF) {
                    throw new IllegalArgumentException("biome " + biomeId + " at pos " + pos);
                }
                dataValue |= biomeId;

                data[z + x * 512] = dataValue;
            }
            northY = Integer.MIN_VALUE;
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
        long f = System.nanoTime();
        int[] colours = texture.getColours();


        // TODO rewrite in zig

        for (int j = 0; j < 10; j++) {
            long a = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                Int2IntMap blockCache = new Int2IntOpenHashMap();
                int waterColour = ColoursConfig.BLOCK_COLOURS.get("minecraft:water");
                for (int x = 0; x < 512; x++) {
                    for (int z = 0; z < 512; z++) {
                        int packedData = data[z + x * 512];
                        int blockId = (packedData >>> 16) & 0xFFFF;
                        int waterDepth = (packedData >>> 12) & 0xF;
                        int biome = packedData & 0xFF;

                        int color;
                        if (!blockCache.containsKey(blockId)) {
                            Holder.Reference<Block> blockHolder = (Holder.Reference<Block>) Registry.BLOCK.getHolder(blockId).get();
                            color = ColoursConfig.BLOCK_COLOURS.getOrDefault(blockHolder.key().toString(), blockHolder.value().defaultMaterialColor().col);
                            blockCache.put(blockId, color);
                        } else {
                            color = blockCache.get(blockId);
                        }

                        if (waterDepth > 0) {
                            int blockColor = color;
                            color = shade(waterColour, 0.85F - (waterDepth * 0.01F));
                            color = mix(color, blockColor, 0.2F / (waterDepth / 2.0F));
                        } else {
                            int bt = Integer.bitCount((packedData >>> 8) & 0xF);
                            int alpha;
                            if (bt == 0 || bt == 1) {
                                alpha = 0;
                            } else if (bt == 2) {
                                alpha = 0x22;
                            } else {
                                alpha = 0x44;
                            }

                            color = blend(color, (double) alpha / 0xFF);
                        }

                        // rightmost 8 bits are alpha, representing water depth or y offset
                        colours[z + x * 512] = color << 8;
                    }
                }
            }
            long b = System.nanoTime();
            System.out.println((b-a)/100000 + "us ." + j);
        }

        long s = System.nanoTime();
        texture.update();
        long n=  System.nanoTime();
        System.out.println((s-f)/1000 + "ns process");
        System.out.println((n-s)/1000 + "ns update");
    }

    public static int blend(int color1, double a0) {
        double r = (red(color1) * (1 - a0));
        double g = (green(color1) * (1 - a0));
        double b = (blue(color1) * (1 - a0));
        return rgb((int) r, (int) g, (int) b);
    }

    public static int red(int argb) {
        return argb >> 16 & 0xFF;
    }

    public static int green(int argb) {
        return argb >> 8 & 0xFF;
    }

    public static int blue(int argb) {
        return argb & 0xFF;
    }

    public static int rgb(int red, int green, int blue) {
        return red << 16 | green << 8 | blue;
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
