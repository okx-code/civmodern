package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import org.checkerframework.checker.nullness.qual.NonNull;
import sh.okx.civmodern.common.AbstractCivModernMod;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class RegionData {

    // 16 bits - block EXCEPT water
    // 4 bits - water depth, > 0 if water, values map to: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 14, 18, 23, 29, 37
    // 2 bits - west Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 2 bits - north Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 8 bits - biome
    // TODO optimize for cache lines?
    private final int[] data = new int[512 * 512];

    public boolean updateChunk(LevelChunk chunk) {
        boolean updated = false;
        Registry<Biome> registry = chunk.getLevel().registryAccess().registry(Registry.BIOME_REGISTRY).get();

        int rx = chunk.getPos().getRegionLocalX() * 16;
        int rz = chunk.getPos().getRegionLocalZ() * 16;

        int[] westY = new int[16];
        Arrays.fill(westY, Integer.MIN_VALUE);
        int northY = Integer.MIN_VALUE;
        for (int x = rx; x < rx + 16; x++) {
            BlockPos.MutableBlockPos pos;
            for (int z = rz; z < rz + 16; z++) {
                pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x + chunk.getPos().getRegionX() * 512, z + chunk.getPos().getRegionZ() * 512) + 1, z + chunk.getPos().getRegionZ() * 512);

                int dataValue = 0;

                Block block;
                int depth;

                do {
                    pos.setY(pos.getY() - 1);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.getFluidState().is(Fluids.WATER)) {
                        BlockPos bottomPos = new BlockPos.MutableBlockPos(pos.getX(), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()), pos.getZ());
                        depth = pos.getY() - bottomPos.getY();
                        block = chunk.getBlockState(bottomPos).getBlock();
                    } else {
                        block = state.getBlock();
                        depth = 0;
                    }

                    if (ColoursConfig.BLOCK_COLOURS.getOrDefault(Registry.BLOCK.getKey(block).toString(), block.defaultMaterialColor().col) > 0) {
                        break;
                    }
                } while(pos.getY() > chunk.getMinBuildHeight());

                int blockId = Registry.BLOCK.getId(block);
                if (blockId > 0xFFFF) {
                    AbstractCivModernMod.LOGGER.warn("block " + blockId + " at pos " + pos);
                    blockId = 0;
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


                int biomeId = registry.getId(chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value());
                if (biomeId > 0xFF) {
                    AbstractCivModernMod.LOGGER.warn("biome " + biomeId + " at pos " + pos);
                    biomeId = 0;
                }
                dataValue |= biomeId;

                int current = data[z + x * 512];
                if (current != dataValue) {
                    updated = true;
                }
                data[z + x * 512] = dataValue;
            }
            northY = Integer.MIN_VALUE;
        }

        return updated;
    }

    public int[] getData() {
        return data;
    }

    public void render(RegionAtlasTexture texture, int rx, int rz) {
        long f = System.nanoTime();
        short[] colours = new short[512 * 512];

        // TODO rewrite in zig

        Int2IntMap blockCache = new Int2IntOpenHashMap();
        Registry<Biome> registry = Minecraft.getInstance().player.getLevel().registryAccess().registry(Registry.BIOME_REGISTRY).get();
        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                int packedData = data[z + x * 512];
                int blockId = (packedData >>> 16) & 0xFFFF;
                int waterDepth = (packedData >>> 12) & 0xF;
                int biomeId = packedData & 0xFF;

                int color;
                int blockBiomeId = blockId << 8 | biomeId;
                if (!blockCache.containsKey(blockBiomeId)) {
                    Holder.Reference<Block> blockHolder = (Holder.Reference<Block>) Registry.BLOCK.getHolder(blockId).get();
                    String key = blockHolder.key().location().toString();
                    color = ColoursConfig.BLOCK_COLOURS.getOrDefault(key, blockHolder.value().defaultMaterialColor().col);

                    if (ColoursConfig.BLOCKS_GRASS.contains(key)) {
                        Biome biome = registry.byId(biomeId);
                        color = mix(biome.getGrassColor(0, 0), color);
                    } else if (ColoursConfig.BLOCKS_FOLIAGE.contains(key)) {
                        Biome biome = registry.byId(biomeId);
                        color = mix(biome.getFoliageColor(), color);
                    }

                    blockCache.put(blockBiomeId, color);
                } else {
                    color = blockCache.get(blockBiomeId);
                }

                if (waterDepth > 0) {
                    Biome biome = registry.byId(biomeId);
                    int fluidColor = fancyFluids(biome, waterDepth * 0.025F);
                    color = blend(fluidColor, color | 0xFF000000) & 0xFFFFFF;
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

                int red = color >> 16 & 0xFF;
                int green = color >> 8 & 0xFF;
                int blue = color & 0xFF;

                short rgb565 = 0;
                rgb565 |= (red / 8) << 11;
                rgb565 |= (green / 4) << 5;
                rgb565 |= (blue / 8);

                colours[x + z * 512] = rgb565;
            }
        }

        long l = System.nanoTime();
        System.out.println("data - " + (l - f) / 1000 + "us");

        if (RenderSystem.isOnRenderThread()) {
            texture.update(colours, rx, rz);
        } else {
            RenderSystem.recordRenderCall(() -> texture.update(colours, rx, rz));
        }
    }

    public static int fancyFluids(Biome biome, float depth) {
        // let's do some maths to get pretty fluid colors based on depth
        int color = biome.getWaterColor();
        color = lerpARGB(color, 0xFF000000, Mth.clamp(cubicOut(depth / 1.5F), 0, 0.45f));
        color = setAlpha((int) (quinticOut(Mth.clamp(depth * 5F, 0, 1)) * 0xFF), color);
        return color;
    }

    public static int blend(int color1, double a0) {
        double r = (red(color1) * (1 - a0));
        double g = (green(color1) * (1 - a0));
        double b = (blue(color1) * (1 - a0));
        return rgb((int) r, (int) g, (int) b);
    }

    public static int setAlpha(int alpha, int argb) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpARGB(int color0, int color1, float delta) {
        if (color0 == color1) return color0;
        if (delta >= 1F) return color1;
        if (delta <= 0F) return color0;
        return argb(
            (int) Mth.lerp(delta, alpha(color0), alpha(color1)),
            (int) Mth.lerp(delta, red(color0), red(color1)),
            (int) Mth.lerp(delta, green(color0), green(color1)),
            (int) Mth.lerp(delta, blue(color0), blue(color1))
        );
    }

    public static int blend(int color0, int color1) {
        double a0 = (double) alpha(color0) / 0xFF;
        double a1 = (double) alpha(color1) / 0xFF;
        double a = a0 + a1 * (1 - a0);
        double r = (red(color0) * a0 + red(color1) * a1 * (1 - a0)) / a;
        double g = (green(color0) * a0 + green(color1) * a1 * (1 - a0)) / a;
        double b = (blue(color0) * a0 + blue(color1) * a1 * (1 - a0)) / a;
        return argb((int) a * 0xFF, (int) r, (int) g, (int) b);
    }

    public static float quinticOut(float t) {
        return 1F + ((t -= 1F) * t * t * t * t);
    }

    public static float cubicOut(float t) {
        return 1F + ((t -= 1F) * t * t);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static int alpha(int argb) {
        return argb >> 24 & 0xFF;
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


    public static int mix(int color0, int color1) {
        int r = red(color0) + red(color1);
        int g = green(color0) + green(color1);
        int b = blue(color0) + blue(color1);
        return rgb(r >> 1, g >> 1, b >> 2);
    }

    private static int[] getColorsFromImage(@NonNull BufferedImage image) {
        int[] map = new int[256 * 256];
        for (int x = 0; x < 256; ++x) {
            for (int y = 0; y < 256; ++y) {
                int rgb = image.getRGB(x, y);
                map[x + y * 256] = (red(rgb) << 16) | (green(rgb) << 8) | blue(rgb);
            }
        }
        return map;
    }
}