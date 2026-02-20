package sh.okx.civmodern.common.map.data;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import sh.okx.civmodern.common.map.ColoursConfig;
import sh.okx.civmodern.common.map.IdLookup;
import sh.okx.civmodern.common.map.RegionAtlasTexture;
import sh.okx.civmodern.common.map.RenderQueue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RegionRenderer {
    public static final boolean perf = false;
    public static final AtomicLong totalns = new AtomicLong();
    public static final AtomicInteger count = new AtomicInteger();

    private final RegionLoader loader;

    private final IdLookup blockLookup;
    private final IdLookup biomeLookup;

    public RegionRenderer(RegionLoader loader, IdLookup blockLookup, IdLookup biomeLookup) {
        this.loader = loader;
        this.blockLookup = blockLookup;
        this.biomeLookup = biomeLookup;
    }


    public void renderChunk(RegionAtlasTexture texture, int rx, int rz, int chunkX, int chunkZ) {
        render(texture, rx, rz, chunkX * 16, chunkX * 16 + 16, chunkZ * 16, chunkZ * 16 + 16);
    }

    public void render(RegionAtlasTexture texture, int rx, int rz) {
        long start;
        if (perf) {
            start = System.nanoTime();
        }

        render(texture, rx, rz, 0, 512, 0, 512);

        if (perf) {
            count.incrementAndGet();
            totalns.addAndGet(System.nanoTime() - start);
        }
    }

    public void render(RegionAtlasTexture texture, int rx, int rz, int minX, int maxX, int minZ, int maxZ) {
        try {
            this.loader.getRenderLock().lock();

            int[] data = this.loader.getOrLoadMapData();

            short[] colours = new short[(maxX - minX) * (maxZ - minZ)];
            int ptr = 0;

            Int2IntMap blockCache = new Int2IntOpenHashMap();
            Int2IntMap biomeCache = new Int2IntOpenHashMap();

            // todo fix
            RegistryAccess registryAccess = Minecraft.getInstance().player.level().registryAccess();
            Registry<Biome> registry = registryAccess.lookupOrThrow(Registries.BIOME);
            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    int packedData = data[z + x * 512];
                    int blockId = (packedData >>> 16) & 0xFFFF;
                    int waterDepth = (packedData >>> 12) & 0xF;
                    int biomeId = packedData & 0xFF;

                    int color;
                    int blockBiomeId = blockId << 8 | biomeId;
                    int cachedColor = blockCache.getOrDefault(blockBiomeId, -1);
                    if (cachedColor == -1) {
                        if (blockId == 0) {
                            color = 0;
                        } else {
                            Optional<Holder.Reference<Block>> holder = registryAccess.lookupOrThrow(Registries.BLOCK).get(Identifier.parse(blockLookup.getName(blockId - 1)));
                            if (holder.isEmpty()) {
                                color = 0;
                            } else {
                                Holder.Reference<Block> blockHolder = holder.get();
                                String key = blockHolder.key().identifier().toString();
                                Integer getColour = ColoursConfig.BLOCK_COLOURS.get(key);
                                if (getColour == null) {
                                    color = blockHolder.value().defaultMapColor().col;
                                } else {
                                    color = getColour;
                                }

                                if (ColoursConfig.BLOCKS_GRASS.contains(key)) {
                                    Biome biome = registry.getValue(Identifier.parse(biomeLookup.getName(biomeId)));
                                    color = mix(biome.getGrassColor(0, 0), color);
                                } else if (ColoursConfig.BLOCKS_FOLIAGE.contains(key)) {
                                    Biome biome = registry.getValue(Identifier.parse(biomeLookup.getName(biomeId)));
                                    color = mix(biome.getFoliageColor(), color);
                                }
                                blockCache.put(blockBiomeId, color);
                            }
                        }
                    } else {
                        color = cachedColor;
                    }

                    if (waterDepth > 0) {
                        int fluidColor = biomeCache.getOrDefault(biomeId, -1);
                        if (fluidColor == -1) {
                            Biome biome = registry.getValue(Identifier.parse(biomeLookup.getName(biomeId)));
                            fluidColor = fancyFluids(biome.getWaterColor(), 0.05F);
                            biomeCache.put(biomeId, fluidColor);
                        }
                        color = blend(fluidColor, color | 0xFF000000) & 0xFFFFFF;
                    }

                    int bt = Integer.bitCount((packedData >>> 8) & 0xF);
                    int alpha;
                    if (bt == 0 || bt == 1) {
                        alpha = 0;
                    } else if (bt == 2) {
                        alpha = waterDepth > 0 ? 0x11 : 0x22;
                    } else {
                        alpha = waterDepth > 0 ? 0x22 : 0x44;
                    }

                    if (alpha != 0) {
                        color = blend(color, (double) alpha / 0xFF);
                    }

                    int red = color >>> 16 & 0xFF;
                    int green = color >>> 8 & 0xFF;
                    int blue = color & 0xFF;

                    short rgb565 = 0;
                    rgb565 |= (red >>> 3) << 11;
                    rgb565 |= (green >>> 2) << 5;
                    rgb565 |= (blue >>> 3);

                    colours[ptr++] = rgb565;
                }
            }

            if (RenderSystem.isOnRenderThread()) {
                texture.update(colours, rx, rz, minX, maxX, minZ, maxZ);
            } else {
                RenderQueue.queue(() -> texture.update(colours, rx, rz, minX, maxX, minZ, maxZ));
            }
        } finally {
            this.loader.getRenderLock().unlock();
        }
    }

    public static int blend(int color1, double a0) {
        double r = (red(color1) * (1 - a0));
        double g = (green(color1) * (1 - a0));
        double b = (blue(color1) * (1 - a0));
        return (int) r << 16 | (int) g << 8 | (int) b;
    }

    public static int fancyFluids(int biomeWaterColour, float depth) {
        // let's do some maths to get pretty fluid colors based on depth
        int color = biomeWaterColour;
        color = lerpARGB(color, 0xFF000000, Mth.clamp(cubicOut(depth / 1.5F), 0, 0.45f));
        color = setAlpha((int) (quinticOut(Mth.clamp(depth * 5F, 0, 1)) * 0xFF), color);
        return color;
    }

    public static int setAlpha(int alpha, int argb) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    public static int lerpARGB(int color0, int color1, float delta) {
        if (color0 == color1) return color0;
        if (delta >= 1F) return color1;
        if (delta <= 0F) return color0;
        return (int) Mth.lerp(delta, alpha(color0), alpha(color1)) << 24 |
            (int) Mth.lerp(delta, red(color0), red(color1)) << 16 |
            (int) Mth.lerp(delta, green(color0), green(color1)) << 8 |
            (int) Mth.lerp(delta, blue(color0), blue(color1));
    }

    public static int blend(int color0, int color1) {
        double a0 = (double) alpha(color0) / 0xFF;
        double a1 = (double) alpha(color1) / 0xFF;
        double a = a0 + a1 * (1 - a0);
        double r = (red(color0) * a0 + red(color1) * a1 * (1 - a0)) / a;
        double g = (green(color0) * a0 + green(color1) * a1 * (1 - a0)) / a;
        double b = (blue(color0) * a0 + blue(color1) * a1 * (1 - a0)) / a;
        return ((int) a * 0xFF) << 24 | (int) r << 16 | (int) g << 8 | (int) b;
    }

    public static float quinticOut(float t) {
        return 1F + ((t -= 1F) * t * t * t * t);
    }

    public static float cubicOut(float t) {
        return 1F + ((t -= 1F) * t * t);
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

    public static int mix(int color0, int color1) {
        float r = red(color0) / 255f * red(color1) / 255f;
        float g = green(color0) / 255f * green(color1) / 255f;
        float b = blue(color0) / 255f * blue(color1) / 255f;
        return (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }
}
