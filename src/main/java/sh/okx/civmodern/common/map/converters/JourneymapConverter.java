package sh.okx.civmodern.common.map.converters;

import com.google.common.net.HostAndPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.IdLookup;
import sh.okx.civmodern.common.map.MapFolder;
import sh.okx.civmodern.common.map.RegionKey;
import sh.okx.civmodern.common.map.data.RegionLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class JourneymapConverter implements Converter {

    private final MapFolder mapFile;
    private final String name;
    private final String dimension;
    private final RegistryAccess registryAccess;

    public JourneymapConverter(MapFolder mapFile, String name, String dimension, RegistryAccess registryAccess) {
        this.mapFile = mapFile;
        this.name = name; // is world name / server ip depending on sp vs mp respectively
        this.dimension = dimension;
        this.registryAccess = registryAccess;
    }

    public boolean hasAlreadyConverted() {
        return this.mapFile.getHistory().mods.containsKey("journeymap");
    }

    /**
     * Gets correct directory for journeymap based on current context
     *
     * @return directory
     */
    private Path getJourmeymapDimensionDir() {
        var base = Minecraft.getInstance().gameDirectory.toPath().resolve("journeymap").resolve("data");

        // if singleplayer
        if (Minecraft.getInstance().isLocalServer()) {
            return base.resolve("sp").resolve(name).resolve(dimension);
        }

        // else multiplayer
        var currServer = Minecraft.getInstance().getCurrentServer();
        assert currServer != null;
        var mpDirName = String.format("%s_id_%d_ip_%s~%s",
                currServer.name, AbstractCivModernMod.getInstance().getWorldListener().getSeed(),
                currServer.ip.replace(".", "~"),
                getIpFromAddress(currServer.ip)
        );

        // try multiworld support first
        var mutliWorldPath = base.resolve("mp").resolve(mpDirName).resolve(dimension);
        if (mutliWorldPath.toFile().exists()) {
            return mutliWorldPath;
        }
        // fallback to "normal" schema
        return base.resolve("mp").resolve(currServer.name).resolve(dimension);
    }

    public boolean filesAvailable() {
        return getJourmeymapDimensionDir().toFile().exists();
    }

    private String getIpFromAddress(String address) {
        HostAndPort hostAndPort = HostAndPort.fromString(address);
        ServerAddress serverAddress = new ServerAddress(hostAndPort.getHost(), hostAndPort.getPortOrDefault(25565));
        var resolved = ServerNameResolver.DEFAULT.resolveAddress(serverAddress);
        if (resolved.isEmpty()) return null;
        return resolved.get().getHostIp().replace(".", "~") + "~" + serverAddress.getPort();
    }

    public void convert() {
        File[] files = getJourmeymapDimensionDir().resolve("cache").toFile().listFiles();
        if (files == null) {
            return;
        }
        AbstractCivModernMod.LOGGER.info("Converting {} Journeymap regions to CivModern regions, this may take a few minutes...", files.length);
        int regionIndex = 0;
        boolean terminated = false;

        AtomicInteger saved = new AtomicInteger();

        Map<RegionKey, RegionLoader> regionMap = new ConcurrentHashMap<>();

        Set<String> converted = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean modified = new AtomicBoolean(false);
        File journeymap = mapFile.getFolder().toPath().resolve("journeymap").toFile();
        try (FileInputStream fis = new FileInputStream(journeymap)) {
            converted.addAll(Arrays.asList(new String(fis.readAllBytes()).split("\n")));
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        IdLookup blockLookup = new IdLookup(mapFile.blockIds(), "minecraft:air");
        IdLookup biomeLookup = new IdLookup(mapFile.biomeIds(), "minecraft:void");

        RegionStorageInfo regionInfo = new RegionStorageInfo("JourneyMap World", Level.OVERWORLD, "thing");

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while (regionIndex < files.length && !terminated) {
            int parallelRegions = Math.min(128, files.length - regionIndex);
            CountDownLatch latch = new CountDownLatch(parallelRegions);
            for (int regionStart = regionIndex; regionIndex < regionStart + parallelRegions; regionIndex++) {
                File subRegionFile = files[regionIndex];
                if (!subRegionFile.isFile()) {
                    continue;
                }
                RegionKey regionKey = getRegionKey(subRegionFile.getName());

                if (Thread.interrupted()) {
                    terminated = true;
                    AbstractCivModernMod.LOGGER.info("Terminated Journeymap conversion at region {}/{}", regionIndex, files.length);
                    break;
                }

                service.submit(() -> {
                    try (var file = new RegionFile(regionInfo, subRegionFile.toPath(), subRegionFile.toPath().getParent(), false)) {
                        var regionModified = false;
                        for (int i = 0; i < 1024; i++) {
                            int x = i / 32;
                            int z = i % 32;

                            // RegionFile
                            var in = file.getChunkDataInputStream(new ChunkPos(x, z));
                            if (in == null) {
                                // AbstractCivModernMod.LOGGER.warn("InputStream for Journeymap chunk at {},{} from region {} is null", x, z, regionKey);
                                continue;
                            }

                            var chunk = NbtIo.read(in);

                            loadData(regionKey, new RegionKey(x, z), chunk, regionMap, blockLookup, biomeLookup);
                            // AbstractCivModernMod.LOGGER.info("Imported Journeymap chunk at {},{} from region {}", x, z, regionKey);
                            regionModified = true;
                        }
                        if (regionModified) modified.set(true);
                        // AbstractCivModernMod.LOGGER.info("Imported Journeymap region at {}", regionKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mapFile.saveBulk(regionMap, service);
            for (Map.Entry<RegionKey, RegionLoader> entry : regionMap.entrySet()) {
                converted.add("r." + entry.getKey().x() + "." + entry.getKey().z() + ".mca");

                int savedCount = saved.incrementAndGet();
                if (savedCount == files.length || savedCount % 128 == 0) {
                    AbstractCivModernMod.LOGGER.info("Saved " + savedCount + " regions...");
                }
            }

            AbstractCivModernMod.LOGGER.info("Processed " + regionIndex + "/" + files.length + " regions...");
            regionMap.clear();
        }

        mapFile.saveBlockIds(blockLookup.getNames());
        mapFile.saveBiomeIds(biomeLookup.getNames());

        if (modified.get()) {
            MapFolder.ModData modData = new MapFolder.ModData();
            modData.regions = new ArrayList<>(converted);
            mapFile.getHistory().mods.put("journeymap", modData);
            mapFile.saveHistory();
        }

        AbstractCivModernMod.LOGGER.info("Conversion complete for " + name + "/" + dimension);
    }

    private void loadData(RegionKey regionKey, RegionKey chunkCords, CompoundTag chunkData, Map<RegionKey, RegionLoader> regionMap, IdLookup blockLookup, IdLookup biomeLookup) {
        int[] chunk = new int[16 * 16];
        short[] ylevels = new short[16 * 16];

        int[] westY = new int[16];
        int[] northY = new int[16];
        Arrays.fill(westY, Integer.MIN_VALUE);
        Arrays.fill(northY, Integer.MIN_VALUE);

        for (String xzCords : chunkData.keySet()) {
            CompoundTag cordData = chunkData.getCompound(xzCords).get();

            // ensure good data
            if (xzCords.equals("LastChange") || xzCords.equals("pos")) {
                // TODO: figure out what pos represents
                continue;
            } else if (!cordData.contains("biome_name")) {
                // means column is probably empty
                continue;
            }

            var blockXZ = parseXZFromKey(xzCords);
            if (blockXZ.length != 2) {
                AbstractCivModernMod.LOGGER.warn("In Region {},{} unknown cord in format: {}", regionKey.x(), regionKey.z(), xzCords);
                continue;
            }
            int rawX = blockXZ[0];
            int rawZ = blockXZ[1];

            int x = rawX & 15; // or blockX % 16
            int z = rawZ & 15; // or blockX % 16

            int dataValue = 0;

            var blockstates = cordData.getCompound("blockstates").get();
            var y = cordData.getInt("top_y").get();

            // int y = topY;
            // String realBlockId = blockstates.getCompound("" + topY).getString("Name");

            // // attempt to find the lowest block
            // int bottemY = Integer.MAX_VALUE;
            // for (var block : blockstates.getAllKeys()) {
            //     int blockY;
            //     try {
            //         blockY = Integer.parseInt(block);
            //     } catch (NumberFormatException e) {
            //         blockY = Integer.MAX_VALUE;
            //     }
            //
            //     if (blockY < bottemY) {
            //         bottemY = blockY;
            //     }
            // }
            // String floorBlockState = blockstates.getCompound("" + bottemY).getString("Name");
            // if (!"minecraft:water".equals(floorBlockState)) {
            //     realBlockId = floorBlockState;
            //     int tmp = y;
            //     y = bottemY;
            //
            // }

            String blockName = blockstates.getCompound("" + y).get().getString("Name").get();
            if (blockName == null) {
                blockName = "minecraft:air";
            }
            int blockId = blockLookup.getOrCreateId(blockName) + 1;

            if (blockId > 0xFFFF) {
                AbstractCivModernMod.LOGGER.warn("convert block " + blockId + " at pos (" + x + ", " + z + ") in (" + regionKey.x() + ", " + regionKey.z() + ")");
                blockId = 0;
            }

            dataValue |= blockId << 16;

            var biomeName = cordData.getString("biome_name").get();
            int biomeId = biomeLookup.getOrCreateId(biomeName);
            if (biomeId > 0xFF) {
                AbstractCivModernMod.LOGGER.warn("biome " + biomeId + " at pos (" + x + ", " + z + ") in (" + regionKey.x() + ", " + regionKey.z() + ")");
                biomeId = 0;
            }
            dataValue |= biomeId;

            // attempt to compute block west of cords, and get Y value for shadowing
            var westYCord = (rawX - 1) + "," + rawZ;
            if (chunkData.contains(westYCord)) {
                var westYTag = chunkData.getCompound(westYCord).get();
                if (westYTag.contains("top_y")) {
                    var westYValue = westYTag.getInt("top_y").get();

                    if (westYValue > y) {
                        dataValue |= 0b11 << 10;
                    } else if (westYValue == y) {
                        dataValue |= 0b01 << 10;
                    }
                }
            } else {
                dataValue |= 0b10 << 10;
            }

            // attempt to compute block north of cords, and get Y value for shadowing
            var northYCord = rawX + "," + (rawZ - 1);
            if (chunkData.contains(northYCord)) {
                var northYTag = chunkData.getCompound(northYCord).get();
                if (northYTag.contains("top_y")) {
                    var northYValue = northYTag.getInt("top_y").get();

                    if (northYValue > y) {
                        dataValue |= 0b11 << 8;
                    } else if (northYValue == y) {
                        dataValue |= 0b01 << 8;
                    }
                }
            } else {
                dataValue |= 0b10 << 8;
            }

            // if (westY[x] != Integer.MIN_VALUE) {
            //     if (westY[x] > y) {
            //         dataValue |= 0b11 << 10;
            //     } else if (westY[x] == y) {
            //         dataValue |= 0b01 << 10;
            //     }
            // } else {
            //     dataValue |= 0b10 << 10;
            // }
            // westY[x] = y;
            //
            // if (northY[z] != Integer.MIN_VALUE) {
            //     if (northY[z] > y) {
            //         dataValue |= 0b11 << 8;
            //     } else if (northY[z] == y) {
            //         dataValue |= 0b01 << 8;
            //     }
            // } else {
            //     dataValue |= 0b10 << 8;
            // }
            // northY[z] = y;

            int index = x * 16 + z;
            chunk[index] = dataValue;
            ylevels[index] = y.shortValue();
        }

        RegionLoader regionData = regionMap.computeIfAbsent(regionKey, k -> new RegionLoader(k, mapFile));
        int[] saved = regionData.getOrLoadMapData();
        short[] savedY = regionData.getOrLoadYLevels();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int globalBlockX = chunkCords.x() * 16 + x;
                int globalBlockZ = chunkCords.z() * 16 + z;

                int index = globalBlockZ + globalBlockX * 512;
                saved[index] = chunk[x * 16 + z];
                savedY[index] = ylevels[x * 16 + z];
            }
        }
    }

    private int[] parseXZFromKey(String cord) {

        var cordsStr = cord.split("\\,");
        if (cordsStr.length != 2) {
            return new int[]{};
        }

        try {
            int x = Integer.parseInt(cordsStr[0]);
            int z = Integer.parseInt(cordsStr[1]);

            return new int[]{x, z};
        } catch (NumberFormatException e) {
            return new int[]{};
        }
    }

    public RegionKey getRegionKey(String fileName) {
        // r.0.-2.mca
        var name = fileName.substring(2); // remove r.
        String[] parts = name.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        // get cords
        return new RegionKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
}
