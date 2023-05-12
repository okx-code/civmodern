package sh.okx.civmodern.common.map;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import org.checkerframework.checker.nullness.qual.NonNull;
import sh.okx.civmodern.common.AbstractCivModernMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class RegionData {

    private static final int[] mapGrass;
    private static final int[] mapFoliage;
    private static Int2IntMap colourMap = new Int2IntOpenHashMap();

    static {
        BufferedImage imgGrass, imgFoliage;

        try {
            imgGrass = ImageIO.read(RegionData.class.getResourceAsStream("/grass.png"));
            imgFoliage = ImageIO.read(RegionData.class.getResourceAsStream("/foliage.png"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read color images", e);
        }

        mapGrass = getColorsFromImage(imgGrass);
        mapFoliage = getColorsFromImage(imgFoliage);

        for (Block block : Registry.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }

            ResourceLocation idd;
            try {
                List<BakedQuad> quads = Minecraft.getInstance().getBlockRenderer().getBlockModel(state)
                    .getQuads(state, Direction.UP, new Random());
                if (quads.isEmpty()) {
                    continue;
                }
                BakedQuad quad = quads.get(0);
                Field spriteField = BakedQuad.class.getDeclaredFields()[3];
                spriteField.setAccessible(true);
                TextureAtlasSprite sprite = (TextureAtlasSprite) spriteField.get(quad);
                idd = new ResourceLocation(sprite.getName().getNamespace(), "textures/" + sprite.getName().getPath() + ".png");
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Minecraft.getInstance().getTextureManager().bindForSetup(idd);
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);
            IntBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, 0x8035, buffer);
            int r = 0, g = 0, b = 0;
            int size = width * height;
            for (int i = 0; i < size; i++) {
                int colour = buffer.get(i);
                int cr = (colour >> 24) & 0xFF;
                int cg = (colour >> 16) & 0xFF;
                int cb = (colour >> 8) & 0xFF;
                r += cr * cr;
                g += cg * cg;
                b += cb * cb;
            }
            int avg = 0;
            avg |= ((int) Math.sqrt(r / size) << 24);
            avg |= ((int) Math.sqrt(g / size) << 16);
            avg |= ((int) Math.sqrt(b / size) << 8);
            avg |= 0xFF;
            colourMap.put(Registry.BLOCK.getId(block), avg >> 8);
        }
    }

    private static final Set<Block> INVISIBLE_BLOCKS = Set.of(Blocks.TALL_GRASS, Blocks.FERN, Blocks.GRASS, Blocks.LARGE_FERN);


    // 16 bits - block EXCEPT water
    // 4 bits - water depth, > 0 if water, values map to: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 14, 18, 23, 29, 37
    // 2 bits - west Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 2 bits - north Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 8 bits - biome
    // TODO optimize for cache lines?
    private final int[] data = new int[512 * 512];

    public void updateChunk(LevelChunk chunk) {
        Registry<Biome> registry = chunk.getLevel().registryAccess().registry(Registry.BIOME_REGISTRY).get();

        int rx = chunk.getPos().getRegionLocalX() * 16;
        int rz = chunk.getPos().getRegionLocalZ() * 16;

        int[] westY = new int[16];
        Arrays.fill(westY, Integer.MIN_VALUE);
        int northY = Integer.MIN_VALUE;
        for (int x = rx; x < rx + 16; x++) {
            BlockPos.MutableBlockPos pos;
            for (int z = rz; z < rz + 16; z++) {
                pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x + chunk.getPos().getRegionX() * 512, z + chunk.getPos().getRegionZ() * 512), z + chunk.getPos().getRegionZ() * 512);
                BlockState state = chunk.getBlockState(pos);

                int dataValue = 0;

                Block block;
                int depth;

                if (state.getFluidState().is(Fluids.WATER)) {
                    BlockPos bottomPos = new BlockPos.MutableBlockPos(pos.getX(), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()), pos.getZ());
                    depth = pos.getY() - bottomPos.getY();
                    block = chunk.getBlockState(bottomPos).getBlock();
                } else {
                    block = state.getBlock();
                    depth = 0;
                }

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

                data[z + x * 512] = dataValue;
            }
            northY = Integer.MIN_VALUE;
        }
    }

    public int[] getData() {
        return data;
    }

    public void render(RegionTexture texture) {
        long f = System.nanoTime();
        int[] colours = texture.getColours();

        // TODO rewrite in zig

        Int2IntMap blockCache = new Int2IntOpenHashMap();
        int waterColour = ColoursConfig.BLOCK_COLOURS.get("minecraft:water");
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
//                    color = ColoursConfig.BLOCK_COLOURS.getOrDefault(key, blockHolder.value().defaultMaterialColor().col);
                    color = colourMap.getOrDefault(blockId, blockHolder.value().defaultMaterialColor().col);

                    if (ColoursConfig.BLOCKS_GRASS.contains(key)) {
                        Biome biome = registry.byId(biomeId);
                        color = mix(biome.getGrassColor(0, 0), color);
//                        float temperature = Mth.clamp(biome.getBaseTemperature(), 0f, 1f);
//                        float humidity = Mth.clamp(biome.getDownfall(), 0f, 1f);
//                        Optional<Integer> override = biome.getSpecialEffects().getGrassColorOverride();
//                        if (override.isPresent()) {
//                            color = mix(override.get(), color);
//                        } else {
//                            color = mix(getDefaultGrassColor(temperature, humidity), color);
//                        }
                    } else if (ColoursConfig.BLOCKS_FOLIAGE.contains(key)) {
                        Biome biome = registry.byId(biomeId);
                        color = mix(biome.getFoliageColor(), color);
//                        float temperature = Mth.clamp(0.0F, 1.0F, biome.getBaseTemperature());
//                        float humidity = Mth.clamp(0.0F, 1.0F, biome.getDownfall());
//                        Optional<Integer> override = biome.getSpecialEffects().getFoliageColorOverride();
//                        if (override.isPresent()) {
//                            color = mix(override.get(), color);
//                        } else {
//                            color = mix(getDefaultFoliageColor(temperature, humidity), color);
//                        }
                    }

                    blockCache.put(blockBiomeId, color);
                } else {
                    color = blockCache.get(blockBiomeId);
                }

                if (waterDepth > 0) {
                    // TODO change water algorithm
                    int blockColor = color;
                    color = shade(waterColour, 0.85F - (waterDepth * 0.01F));
                    color = mix(color, blockColor, 0.2F / (waterDepth / 2.0F));

                    Biome biome = registry.byId(biomeId);
                    int fluidColor = fancyFluids(biome, waterDepth * 0.025F);
                    color = blend(fluidColor, color) & 0xFFFFFF;
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

                // rightmost 8 bits are alpha, representing water depth or z offset
                colours[x + z * 512] = color << 8;
            }
        }

        long l = System.nanoTime();
        System.out.println("data - " + (l - f) / 1000 + "us");

        if (RenderSystem.isOnRenderThread()) {
            texture.update();
        } else {
            RenderSystem.recordRenderCall(texture::update);
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

    public static int getDefaultGrassColor(double temperature, double humidity) {
        return getDefaultColor(temperature, humidity, mapGrass);
    }

    public static int getDefaultFoliageColor(double temperature, double humidity) {
        return getDefaultColor(temperature, humidity, mapFoliage);
    }

    private static int getDefaultColor(double temperature, double humidity, int[] map) {
        int i = (int) ((1.0 - temperature) * 255.0);
        int j = (int) ((1.0 - (humidity * temperature)) * 255.0);
        int k = j << 8 | i;
        return k > map.length ? 0 : map[k];
    }
}