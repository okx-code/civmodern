package sh.okx.civmodern.common.map.converters;

import com.google.common.net.HostAndPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.IdLookup;
import sh.okx.civmodern.common.map.MapFolder;
import sh.okx.civmodern.common.map.RegionKey;
import sh.okx.civmodern.common.map.data.RegionLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class JourneymapConverter extends Converter {

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
        // TODO: switch this to json file
        return this.mapFile.getFolder().toPath().resolve("journeymap").toFile().exists();
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
        return base.resolve("mp").resolve(mpDirName).resolve(dimension);
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
        var listed = getJourmeymapDimensionDir().resolve("cache").toFile().listFiles();
        if (listed == null) {
            return;
        }
        var files = reorderFiles(listed);
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

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while (regionIndex < files.length && !terminated) {
            int parallelRegions = Math.min(128, files.length - regionIndex);
            CountDownLatch latch = new CountDownLatch(parallelRegions);
            for (int regionStart = regionIndex; regionIndex < regionStart + parallelRegions; regionIndex++) {
                File subRegionFile = files[regionIndex];
                if (!subRegionFile.isFile()) {
                    continue;
                }
                var regionKey = getRegionKey(subRegionFile.getName());

                if (Thread.interrupted()) {
                    terminated = true;
                    AbstractCivModernMod.LOGGER.info("Terminated Journeymap conversion at region {}/{}", regionIndex, files.length);
                    break;
                }

                service.submit(() -> {
                    try (var file = new JourneymapNbtFileWrapper(subRegionFile.toPath(), false)) {
                        for (int i = 0; i < 1024; i++) {
                            int x = i / 32;
                            int z = i % 32;

                            var in = file.getChunkDataInputStream(new ChunkPos(x, z));
                            if (in == null) {
                                continue;
                            }

                            var chunk = NbtIo.read(in);
                            loadData(regionKey, chunk);
                        }
                        modified.set(true);
                    } catch (Exception e) {
                        e.printStackTrace();
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
            StringBuilder toWrite = new StringBuilder();
            for (String r : converted) {
                toWrite.append(r).append("\n");
            }
            try (FileOutputStream fos = new FileOutputStream(journeymap)) {
                fos.write(toWrite.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AbstractCivModernMod.LOGGER.info("Conversion complete for " + name + "/" + dimension);
    }

    private void loadData(RegionKey regionKey, CompoundTag chunk) {
        int[] region = new int[256 * 256];
        short[] ylevels = new short[256 * 256];

        int[] westY = new int[256];
        Arrays.fill(westY, Integer.MIN_VALUE);
        int northY = Integer.MIN_VALUE;

        for (var xzCords : chunk.getAllKeys()) {
            var cordData = chunk.getCompound(xzCords);

            if (xzCords.equals("LastChange") || xzCords.equals("pos")) {
                // TODO: figure out what pos represents
                continue;
            } else if (!cordData.contains("biome_name")) {
                // means column is probably empty
                continue;
            }

            var biome = cordData.getString("biome_name");
            var topY = cordData.getInt("top_y");
            var blockstates = cordData.getCompound("blockstates");

            // attempt to find the lowest block
            int bottemY = Integer.MAX_VALUE;
            for (var block : blockstates.getAllKeys()) {
                int blockY;
                try {
                    blockY = Integer.parseInt(block);
                } catch (NumberFormatException e) {
                    blockY = Integer.MAX_VALUE;
                }

                if (blockY < bottemY) {
                    bottemY = blockY;
                }
            }
        }

        // for (var xzCords : data.getAllKeys()) {
        //     if (xzCords.equals("LastChange") || xzCords.equals("pos")) {
        //         // TODO: figure out what pos represents
        //         return;
        //     }
        //
        //     var cords = xzCords.split(",");
        //     if (cords.length != 2) {
        //         AbstractCivModernMod.LOGGER.warn("Unknown block cord format: {}", xzCords);
        //         return;
        //     }
        //     var cordData = data.getCompound(xzCords);
        //
        //     var biome = cordData.getString("biome_name");
        //     var topY = cordData.getInt("top_y");
        //
        //     var blockstates = cordData.getCompound("blockstates");
        //     if (blockstates.size() != 2 && printedCount < 1) {
        //         AbstractCivModernMod.LOGGER.info("Blockstate info for {}", xzCords);
        //         for (var blockstateKey : blockstates.getAllKeys()) {
        //             var blockstate = blockstates.getCompound(blockstateKey);
        //             AbstractCivModernMod.LOGGER.info(blockstateKey + " " +
        //                     blockstate.getString("Name") + " level = "
        //                     + blockstates.getCompound("Properties").getInt("level"));
        //         }
        //         printedCount++;
        //     }
        // }
    }

    protected RegionKey getRegionKey(String fileName) {
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
