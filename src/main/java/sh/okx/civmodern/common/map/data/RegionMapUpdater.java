package sh.okx.civmodern.common.map.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.ColoursConfig;
import sh.okx.civmodern.common.map.IdLookup;
import sh.okx.civmodern.common.map.RegionDataType;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

public class RegionMapUpdater {

    public static int SIZE = 512;

    // 16 bits - block EXCEPT water
    // 4 bits - water depth, > 0 if water
    // 2 bits - west Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 2 bits - north Y, 01 if equal, 11 if above, 00 if below, 10 if unknown (border)
    // 8 bits - biome
    // big endian
    private final RegionLoader loader;
    private final IdLookup blockLookup;
    private final IdLookup biomeLookup;

    public RegionMapUpdater(RegionLoader loader, IdLookup blockLookup, IdLookup biomeLookup) {
        this.loader = loader;
        this.blockLookup = blockLookup;
        this.biomeLookup = biomeLookup;
    }

    private int getHeight(RegistryAccess registryAccess, ChunkAccess chunk, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(chunk.getPos().getBlockX(x), chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1, chunk.getPos().getBlockZ(z));
        int depth;
        do {
            pos.setY(pos.getY() - 1);
            Block block;
            BlockState state = chunk.getBlockState(pos);
            if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) {
                BlockPos bottomPos = new BlockPos.MutableBlockPos(pos.getX(), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()), pos.getZ());
                depth = pos.getY() - bottomPos.getY();
                block = chunk.getBlockState(bottomPos).getBlock();
            } else {
                block = state.getBlock();
                depth = 0;
            }
            if (ColoursConfig.BLOCK_COLOURS.getOrDefault(registryAccess.lookupOrThrow(Registries.BLOCK).getKey(block).toString(), block.defaultMapColor().col) > 0) {
                break;
            }
        } while (pos.getY() > chunk.getMinY());
        return pos.getY() - depth;
    }

    public boolean updateChunk(RegistryAccess registryAccess, ChunkAccess chunk) {
        Registry<Biome> registry = registryAccess.lookupOrThrow(Registries.BIOME);
        boolean updated = false;

        int rx = chunk.getPos().getRegionLocalX() * 16;
        int rz = chunk.getPos().getRegionLocalZ() * 16;
        loader.getLock().writeLock().lock();
        try {
            loader.loadAllData();
            int[] data = loader.getOrLoadMapData();
            short[] ylevels = loader.getOrLoadYLevels();
            short[] waterylevels = loader.getOrLoadWaterYLevels();
            long[] chunkTimestamps = loader.getOrLoadChunkTimestamps();

            short[] northY = new short[16];
            for (int x = 0; x < 16; x++) {
                int pos = (rz - 1) + ((rx + x) * 512);
                if (pos < 0 || pos >= ylevels.length) {
                    northY[x] = Short.MIN_VALUE;
                    continue;
                }
                short waterY = waterylevels[pos];
                if (waterY != 0) {
                    northY[x] = (short) (waterY < 0 ? waterY : waterY - 1);
                } else {
                    short y = ylevels[pos];
                    if (y > 0) {
                        y -= 1;
                    } else if (y == 0) {
                        y = Short.MIN_VALUE;
                    }
                    northY[x] = y;
                }
            }
            short[] westY = new short[16];
            for (int z = 0; z < 16; z++) {
                int pos = (rz + z) + ((rx - 1) * 512);
                if (pos < 0 || pos >= ylevels.length) {
                    westY[z] = Short.MIN_VALUE;
                    continue;
                }
                short waterY = waterylevels[pos];
                if (waterY != 0) {
                    westY[z] = (short) (waterY < 0 ? waterY : waterY - 1);
                } else {
                    short y = ylevels[pos];
                    if (y > 0) {
                        y -= 1;
                    } else if (y == 0) {
                        y = Short.MIN_VALUE;
                    }
                    westY[z] = y;
                }
            }

            int chunkIndex = rz / 16 + rx / 16 * 512 / 16;
            chunkTimestamps[chunkIndex] = System.currentTimeMillis();

            for (int x = rx; x < rx + 16; x++) {
                BlockPos.MutableBlockPos pos;
                for (int z = rz; z < rz + 16; z++) {
                    pos = new BlockPos.MutableBlockPos(x + chunk.getPos().getRegionX() * 512, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1, z + chunk.getPos().getRegionZ() * 512);

                    int dataValue = 0;
                    int current = data[z + x * 512];
                    int yCurrent = ylevels[z + x * 512];

                    Block block;
                    int depth;

                    do {
                        pos.setY(pos.getY() - 1);
                        BlockState state = chunk.getBlockState(pos);
                        if (state.getFluidState().is(Fluids.WATER) || state.getFluidState().is(Fluids.FLOWING_WATER)) {
                            BlockPos bottomPos = new BlockPos.MutableBlockPos(pos.getX(), chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, pos.getX(), pos.getZ()), pos.getZ());
                            depth = pos.getY() - bottomPos.getY();
                            block = chunk.getBlockState(bottomPos).getBlock();
                        } else {
                            block = state.getBlock();
                            depth = 0;
                        }

                        if (ColoursConfig.BLOCK_COLOURS.getOrDefault(registryAccess.lookupOrThrow(Registries.BLOCK).getKey(block).toString(), block.defaultMapColor().col) > 0) {
                            break;
                        }
                    } while (pos.getY() > chunk.getMinY());

                    int blockId = blockLookup.getOrCreateId(registryAccess.lookupOrThrow(Registries.BLOCK).getKey(block).toString()) + 1;
                    if (blockId > 0xFFFE) {
                        AbstractCivModernMod.LOGGER.warn("block " + blockId + " at pos " + pos);
                        blockId = 0;
                    }
                    dataValue |= blockId << 16;
                    dataValue |= Math.min(depth, 0xF) << 12;

                    if (westY[z - rz] != Short.MIN_VALUE) {
                        if (westY[z - rz] > pos.getY() - depth) {
                            dataValue |= 0b11 << 10;
                        } else if (westY[z - rz] == pos.getY() - depth) {
                            dataValue |= 0b01 << 10;
                        }
                    } else if ((current >> 10 & 0x3) == 0) {
                        dataValue |= 0b10 << 10;
                    } else {
                        dataValue |= current & 0xC00;
                    }
                    westY[z - rz] = (short) (pos.getY() - depth);

                    if (northY[x - rx] != Short.MIN_VALUE) {
                        if (northY[x - rx] > pos.getY() - depth) {
                            dataValue |= 0b11 << 8;
                        } else if (northY[x - rx] == pos.getY() - depth) {
                            dataValue |= 0b01 << 8;
                        }
                    } else if ((current >> 8 & 0x3) == 0) {
                        dataValue |= 0b10 << 8;
                    } else {
                        dataValue |= current & 0x300;
                    }
                    northY[x - rx] = (short) (pos.getY() - depth);

                    int biomeId = biomeLookup.getOrCreateId(registry.getKey(chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2).value()).toString());
                    if (biomeId > 0xFF) {
                        AbstractCivModernMod.LOGGER.warn("biome " + biomeId + " at pos " + pos);
                        biomeId = 0;
                    }
                    dataValue |= biomeId;

                    data[z + x * 512] = dataValue;
                    short ylevel = (short) pos.getY();
                    if (ylevel >= 0) {
                        ylevel++; // zero has a special significance as the null value
                    }
                    ylevels[z + x * 512] = ylevel;
                    if (depth != 0) {
                        short waterylevel = (short) (pos.getY() - depth);
                        if (waterylevel >= 0) {
                            waterylevel++;
                        }
                        waterylevels[z + x * 512] = waterylevel;
                    }

                    if (current != dataValue || ylevel != yCurrent) {
                        updated = true;
                    }
                }
            }
        } finally {
            loader.getLock().writeLock().unlock();
        }

        return updated;
    }
}
