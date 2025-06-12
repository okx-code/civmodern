package sh.okx.civmodern.common.map.converters;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Mth;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.*;
import sh.okx.civmodern.common.map.data.RegionLoader;
import sh.okx.civmodern.common.map.data.RegionMapUpdater;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VoxelMapConverter {

    private final MapFolder mapFile;
    private final String name;
    private final String dimension;
    private final RegistryAccess registryAccess;

    private final int v2DataLength = 256 * 256 * 18;
    private final int v4DataLength = 256 * 256 * 22;

    public VoxelMapConverter(MapFolder mapFile, String name, String dimension, RegistryAccess registryAccess) {
        this.mapFile = mapFile;
        this.name = name;
        this.dimension = dimension;
        this.registryAccess = registryAccess;
    }

    public boolean hasAlreadyConverted() {
        return this.mapFile.getFolder().toPath().resolve("voxelmap").toFile().exists();
    }

    public boolean voxelmapFilesAvailable() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("voxelmap").resolve("cache").resolve(name).resolve(dimension).toFile().exists();
    }

    public void convert() {
        File overworld = Minecraft.getInstance().gameDirectory.toPath().resolve("voxelmap").resolve("cache").resolve(name).resolve(dimension).toFile();
        File[] listed = overworld.listFiles();
        if (listed == null) {
            return;
        }
        File[] files = reorderFiles(listed);
        AbstractCivModernMod.LOGGER.info("Converting " + files.length + " VoxelMap regions to CivModern regions, this may take a few minutes...");
        int regionIndex = 0;
        boolean terminated = false;

        AtomicInteger saved = new AtomicInteger();

        Map<RegionKey, RegionLoader> regionMap = new ConcurrentHashMap<>();

        Set<String> converted = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicBoolean modified = new AtomicBoolean(false);
        File voxelmap = mapFile.getFolder().toPath().resolve("voxelmap").toFile();
        try (FileInputStream fis = new FileInputStream(voxelmap)) {
            converted.addAll(Arrays.asList(new String(fis.readAllBytes()).split("\n")));
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        IdLookup blockLookup = new IdLookup(mapFile.blockIds(), "minecraft:air");
        IdLookup biomeLookup = new IdLookup(mapFile.biomeIds(), "minecraft:void");

        // 2859 regions
        // multithreaded processing - 1 minutes 16 seconds
        // sorting - 26 seconds
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        while (regionIndex < files.length && !terminated) {
            int parallelRegions = Math.min(128, files.length - regionIndex);
            CountDownLatch latch = new CountDownLatch(parallelRegions);
            for (int regionStart = regionIndex; regionIndex < regionStart + parallelRegions; regionIndex++) {
                File subRegionFile = files[regionIndex];
                if (!subRegionFile.isFile()) {
                    continue;
                }
                if (Thread.interrupted()) {
                    terminated = true;
                    AbstractCivModernMod.LOGGER.info("Terminated VoxelMap conversion at region " + regionIndex + "/" + files.length);
                    break;
                }

                service.submit(() -> {
                    ZipFile zFile = null; // close in finally block
                    try {
                        // load subRegion file
                        zFile = new ZipFile(subRegionFile);

                        // perform basic version check
                        int version = 1;
                        ZipEntry ze = zFile.getEntry("control");
                        InputStream is;
                        if (ze != null) {
                            is = zFile.getInputStream(ze);
                            if (is != null) {
                                Properties properties = new Properties();
                                properties.load(is);
                                String versionString = properties.getProperty("version", "1");
                                try {
                                    version = Integer.parseInt(versionString);
                                } catch (NumberFormatException var16) {
                                }
                                is.close();
                            }
                        }
                        if (version != 4 && version != 2) {
                            return;
                        }

                        // load in block mappings
                        ze = zFile.getEntry("key");
                        is = zFile.getInputStream(ze);
                        Scanner sc = new Scanner(is);
                        Int2ObjectMap<String> blockMap = new Int2ObjectOpenHashMap<>();
                        while (sc.hasNextLine())
                            parseLine(sc.nextLine(), blockMap);
                        sc.close();
                        is.close();

                        // parse subRegion info
                        String subRegion = subRegionFile.getName().split("\\.")[0];
                        if (converted.contains(subRegion)) {
                            return;
                        }
                        String[] name = subRegion.split(",");

                        // version specific processing
                        if (version == 2) {

                            // load in data
                            ze = zFile.getEntry("data");
                            is = zFile.getInputStream(ze);
                            byte[] data = is.readAllBytes(); // 256 * 256 * 18
                            is.close();
                            if (data.length != v2DataLength) {
                                return;
                            }

                            // will be empty because biome data is not stored in v2 format
                            Int2ObjectMap<String> biomeMap = new Int2ObjectOpenHashMap<>();

                            loadData(2, Integer.parseInt(name[0]), Integer.parseInt(name[1]), data, blockMap, biomeMap, regionMap, blockLookup, biomeLookup);
                            modified.set(true);
                        } else if (version == 4) {
                            // load in data
                            ze = zFile.getEntry("data");
                            is = zFile.getInputStream(ze);
                            byte[] data = is.readAllBytes(); // 256 * 256 * 22
                            is.close();
                            if (data.length != v4DataLength) {
                                return;
                            }

                            // load biome map
                            ze = zFile.getEntry("biomes");
                            is = zFile.getInputStream(ze);
                            sc = new Scanner(is);
                            Int2ObjectMap<String> biomeMap = new Int2ObjectOpenHashMap<>();
                            while (sc.hasNextLine())
                                parseLine(sc.nextLine(), biomeMap);
                            sc.close();
                            is.close();

                            loadData(4, Integer.parseInt(name[0]), Integer.parseInt(name[1]), data, blockMap, biomeMap, regionMap, blockLookup, biomeLookup);
                            modified.set(true);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        if (zFile != null) {
                            try {
                                zFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
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
                converted.add(entry.getKey().x() + "," + entry.getKey().z() + ".zip");

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
            try (FileOutputStream fos = new FileOutputStream(voxelmap)) {
                fos.write(toWrite.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AbstractCivModernMod.LOGGER.info("Conversion complete for " + name + "/" + dimension);
    }

    private File[] reorderFiles(File[] files) {
        // Regions are processed in batches of 128 regions at a time (~128 MB memory usage)
        // But the order that the regions are processed in is important, as VoxelMap regions are 256x256
        // and CivModern regions are 512x512. If two VoxelMap regions that would map to the same CivModern region
        // are processed in two different batches, then this causes the CivModern region to be written and read twice.
        // This function attempts to order the VoxelMap regions so that they will be processed in the same batch.
        // This improves performance by about two thirds, even though this algorithm is O(n^2) (lame)
        File[] newFiles = new File[files.length];
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i] == null) {
                continue;
            }
            newFiles[count++] = files[i];
            RegionKey iRegion = getRegionKey(files[i].getName());
            if (iRegion == null) {
                continue;
            }
            for (int j = i + 1; j < files.length; j++) {
                if (files[j] == null) {
                    continue;
                }
                RegionKey jRegion = getRegionKey(files[j].getName());
                if (jRegion == null) {
                    continue;
                }

                if ((iRegion.x() & ~0x1) == (jRegion.x() & ~0x1) && ((iRegion.z() & ~0x1) == (jRegion.z() & ~0x1))) {
                    newFiles[count++] = files[j];
                    files[j] = null;
                }
            }
        }
        return newFiles;
    }

    private RegionKey getRegionKey(String fileName) {
        String name = fileName.split("\\.")[0];
        String[] parts = name.split(",");
        if (parts.length != 2) {
            return null;
        }
        return new RegionKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private void loadData(int version, int subRegionX, int subRegionZ, byte[] data, Int2ObjectMap<String> blockIdToName, Int2ObjectMap<String> biomeIdToName, Map<RegionKey, RegionLoader> regionMap, IdLookup blockLookup, IdLookup biomeLookup) {
        VersionedDataHelper helper;
        if (version == 4) {
            helper = new v4DataHelper(blockIdToName, biomeIdToName, regionMap, blockLookup, biomeLookup);
        } else if (version == 2) {
            helper = new v2DataHelper(blockIdToName, biomeIdToName, regionMap, blockLookup, biomeLookup);
        } else {
            throw new IllegalArgumentException("Unknown VoxelMap data version " + version);
        }

        int[] region = new int[256 * 256];
        short[] ylevels = new short[256 * 256];

        int[] westY = new int[256];
        Arrays.fill(westY, Integer.MIN_VALUE);
        int northY = Integer.MIN_VALUE;
        for (int x = 0; x < 256; x++) {
            for (int z = 0; z < 256; z++) {
                int y = helper.getHeight(data, x, z);
                int realBlockId = helper.getBlockstate(data, x, z);

                int fy = helper.getFoliageHeight(data, x, z);
                if (fy > y) {
                    y = fy - 1;
                    int foliageBlockstate = helper.getFoliageBlockstate(data, x, z);
                    if (!ColoursConfig.BLOCKS_GRASS.contains(blockIdToName.get(foliageBlockstate))) {
                        realBlockId = foliageBlockstate;
                    }
                }

                int dataValue = 0;

                int floorBlockstate = helper.getOceanFloorBlockstate(data, x, z);
                int height = helper.getOceanFloorheight(data, x, z);
                if (!"minecraft:air".equals(blockIdToName.get(floorBlockstate))) {
                    realBlockId = floorBlockstate;
                    int tmp = y;
                    y = height;
                    height = tmp;
                }
//                if (!blockIdToName.containsKey(floorBlockstate)) {
//                    floorBlockstate = getTransparentBlockstate(data, x, z);
//                } else {
//                    // wtf voxelmap?
//                    int tmp = y;
//                    y = height;
//                    height = tmp;
//
//                    tmp = realBlockId;
//                    realBlockId = floorBlockstate;
//                    floorBlockstate = tmp;
//                }

                String blockName = blockIdToName.get(realBlockId);
                if (blockName == null) {
                    blockName = "minecraft:air";
                }
                int blockId = blockLookup.getOrCreateId(blockName) + 1;

                if (blockId > 0xFFFF) {
                    AbstractCivModernMod.LOGGER.warn("convert block " + blockId + " at pos (" + x + ", " + z + ") in (" + subRegionX + ", " + subRegionZ + ")");
                    blockId = 0;
                }

                dataValue |= blockId << 16;

                int actualY;

                if (!"minecraft:air".equals(blockIdToName.get(floorBlockstate))) {
                    int depth = height - y;
                    dataValue |= Mth.clamp(depth, 0, 0xF) << 12;
                    actualY = height;
                } else {
                    actualY = y;
                }

                String biomeName = helper.getBiomeID(data, x, z);
                int biomeId = biomeLookup.getOrCreateId(biomeName);
                if (biomeId > 0xFF) {
                    AbstractCivModernMod.LOGGER.warn("biome " + biomeId + " at pos (" + x + ", " + z + ") in (" + subRegionX + ", " + subRegionZ + ")");
                    biomeId = 0;
                }
                dataValue |= biomeId;

                if (westY[z] != Integer.MIN_VALUE) {
                    if (westY[z] > y) {
                        dataValue |= 0b11 << 10;
                    } else if (westY[z] == y) {
                        dataValue |= 0b01 << 10;
                    }
                } else {
                    dataValue |= 0b10 << 10;
                }
                westY[z] = y;

                if (northY != Integer.MIN_VALUE) {
                    if (northY > y) {
                        dataValue |= 0b11 << 8;
                    } else if (northY == y) {
                        dataValue |= 0b01 << 8;
                    }
                } else {
                    dataValue |= 0b10 << 8;
                }
                northY = y;

                region[z + x * 256] = dataValue;
                ylevels[z + x * 256] = (short) actualY;

            }
            northY = Integer.MIN_VALUE;
        }

        int regionX = Math.floorDiv(subRegionX, 2);
        int regionZ = Math.floorDiv(subRegionZ, 2);

        int offsetX = Math.floorMod(subRegionX, 2);
        int offsetZ = Math.floorMod(subRegionZ, 2);

        RegionKey key = new RegionKey(regionX, regionZ);
        RegionLoader regionData = regionMap.computeIfAbsent(key, k -> new RegionLoader(k, mapFile));
        int[] saved = regionData.getOrLoadMapData();
        short[] savedY = regionData.getOrLoadYLevels();
        for (int x = offsetX * 256; x < offsetX * 256 + 256; x++) {
            for (int z = offsetZ * 256; z < offsetZ * 256 + 256; z++) {
                saved[z + x * 512] = region[(z - offsetZ * 256) + (x - offsetX * 256) * 256];
                savedY[z + x * 512] = ylevels[(z - offsetZ * 256) + (x - offsetX * 256) * 256];
            }
        }
    }

    private byte getData(byte[] data, int x, int z, int bit) {
        int index = x + z * 256 + 256 * 256 * bit;
        return data[index];
    }

    abstract class VersionedDataHelper {
        protected final Int2ObjectMap<String> blockIdToName;
        protected final Int2ObjectMap<String> biomeIdToName;
        protected final Map<RegionKey, RegionLoader> regionMap;
        protected final IdLookup blockLookup;
        protected final IdLookup biomeLookup;

        VersionedDataHelper(Int2ObjectMap<String> blockIdToName, Int2ObjectMap<String> biomeIdToName, Map<RegionKey, RegionLoader> regionMap, IdLookup blockLookup, IdLookup biomeLookup) {
            this.blockIdToName = blockIdToName;
            this.biomeIdToName = biomeIdToName;
            this.regionMap = regionMap;
            this.blockLookup = blockLookup;
            this.biomeLookup = biomeLookup;
        }

        abstract int getHeight(byte[] data, int x, int z);

        abstract int getFoliageHeight(byte[] data, int x, int z);

        abstract int getFoliageBlockstate(byte[] data, int x, int z);

        abstract int getTransparentHeight(byte[] data, int x, int z);

        abstract int getTransparentBlockstate(byte[] data, int x, int z);

        abstract int getBlockstate(byte[] data, int x, int z);

        abstract int getOceanFloorBlockstate(byte[] data, int x, int z);

        abstract int getOceanFloorheight(byte[] data, int x, int z);

        abstract String getBiomeID(byte[] data, int x, int z);
    }

    class v4DataHelper extends VersionedDataHelper {
        v4DataHelper(Int2ObjectMap<String> blockIdToName, Int2ObjectMap<String> biomeIdToName, Map<RegionKey, RegionLoader> regionMap, IdLookup blockLookup, IdLookup biomeLookup) {
            super(blockIdToName, biomeIdToName, regionMap, blockLookup, biomeLookup);
        }

        public int getHeight(byte[] data, int x, int z) {
            return (getData(data, x, z, 0) << 8) | getData(data, x, z, 1) & 0xFF;
        }

        public int getFoliageHeight(byte[] data, int x, int z) {
            return (getData(data, x, z, 15) << 8) | getData(data, x, z, 16) & 0xFF;
        }

        public int getFoliageBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 17) & 0xFF) << 8 | getData(data, x, z, 18) & 0xFF;
        }

        public int getTransparentHeight(byte[] data, int x, int z) {
            return (getData(data, x, z, 10) << 8) | getData(data, x, z, 11) & 0xFF;
        }

        public int getTransparentBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 12) & 0xFF) << 8 | getData(data, x, z, 13) & 0xFF;
        }

        public int getBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 2) & 0xFF) << 8 | getData(data, x, z, 3) & 0xFF;
        }

        public int getOceanFloorBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 7) & 0xFF) << 8 | getData(data, x, z, 8) & 0xFF;
        }

        public int getOceanFloorheight(byte[] data, int x, int z) {
            return getData(data, x, z, 5) << 8 | getData(data, x, z, 6) & 0xFF;
        }

        public String getBiomeID(byte[] data, int x, int z) {
            var rawId = (getData(data, x, z, 20) & 0xFF) << 8 | getData(data, x, z, 21) & 0xFF;
            String biomeName = biomeIdToName.get(rawId);
            if (biomeName == null) {
                return "minecraft:void";
            }
            return biomeName;
        }
    }

    class v2DataHelper extends VersionedDataHelper {
        v2DataHelper(Int2ObjectMap<String> blockIdToName, Int2ObjectMap<String> biomeIdToName, Map<RegionKey, RegionLoader> regionMap, IdLookup blockLookup, IdLookup biomeLookup) {
            super(blockIdToName, biomeIdToName, regionMap, blockLookup, biomeLookup);
        }

        public int getHeight(byte[] data, int x, int z) {
            return getData(data, x, z, 0) & 0xFF;
        }

        public int getFoliageHeight(byte[] data, int x, int z) {
            return getTransparentHeight(data, x, z);
        }

        public int getFoliageBlockstate(byte[] data, int x, int z) {
            return getTransparentBlockstate(data, x, z);
        }

        public int getTransparentHeight(byte[] data, int x, int z) {
            return getData(data, x, z, 8) & 0xFF;
        }

        public int getTransparentBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 9) & 0xFF) << 8 | getData(data, x, z, 10) & 0xFF;
        }

        public int getBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 1) & 0xFF) << 8 | getData(data, x, z, 2) & 0xFF;
        }

        public int getOceanFloorBlockstate(byte[] data, int x, int z) {
            return (getData(data, x, z, 5) & 0xFF) << 8 | getData(data, x, z, 6) & 0xFF;
        }

        public int getOceanFloorheight(byte[] data, int x, int z) {
            return getData(data, x, z, 4) & 0xFF;
        }

        public String getBiomeID(byte[] data, int x, int z) {
            var rawId = (getData(data, x, z, 16) & 0xFF) << 8 | getData(data, x, z, 17) & 0xFF;
            String biomeName = mapLegacyBiomeIds(rawId);
            if (biomeName == null) {
                return "minecraft:void";
            }
            return biomeName;
        }

        private String mapLegacyBiomeIds(int id) {
            return switch (id) {
                case 0 -> "minecraft:badlands";
                case 1 -> "minecraft:bamboo_jungle";
                case 2 -> "minecraft:basalt_deltas";
                case 3 -> "minecraft:beach";
                case 4 -> "minecraft:birch_forest";
                case 5 -> "minecraft:cherry_grove";
                case 6 -> "minecraft:cold_ocean";
                case 7 -> "minecraft:crimson_forest";
                case 8 -> "minecraft:dark_forest";
                case 9 -> "minecraft:deep_cold_ocean";
                case 10 -> "minecraft:deep_dark";
                case 11 -> "minecraft:deep_frozen_ocean";
                case 12 -> "minecraft:deep_lukewarm_ocean";
                case 13 -> "minecraft:deep_ocean";
                case 14 -> "minecraft:desert";
                case 15 -> "minecraft:dripstone_caves";
                case 16 -> "minecraft:end_barrens";
                case 17 -> "minecraft:end_highlands";
                case 18 -> "minecraft:end_midlands";
                case 19 -> "minecraft:eroded_badlands";
                case 20 -> "minecraft:flower_forest";
                case 21 -> "minecraft:forest";
                case 22 -> "minecraft:frozen_ocean";
                case 23 -> "minecraft:frozen_peaks";
                case 24 -> "minecraft:frozen_river";
                case 25 -> "minecraft:grove";
                case 26 -> "minecraft:ice_spikes";
                case 27 -> "minecraft:jagged_peaks";
                case 28 -> "minecraft:jungle";
                case 29 -> "minecraft:lukewarm_ocean";
                case 30 -> "minecraft:lush_caves";
                case 31 -> "minecraft:mangrove_swamp";
                case 32 -> "minecraft:meadow";
                case 33 -> "minecraft:mushroom_fields";
                case 34 -> "minecraft:nether_wastes";
                case 35 -> "minecraft:ocean";
                case 36 -> "minecraft:old_growth_birch_forest";
                case 37 -> "minecraft:old_growth_pine_taiga";
                case 38 -> "minecraft:old_growth_spruce_taiga";
                case 39 -> "minecraft:plains";
                case 40 -> "minecraft:river";
                case 41 -> "minecraft:savanna";
                case 42 -> "minecraft:savanna_plateau";
                case 43 -> "minecraft:small_end_islands";
                case 44 -> "minecraft:snowy_beach";
                case 45 -> "minecraft:snowy_plains";
                case 46 -> "minecraft:snowy_slopes";
                case 47 -> "minecraft:snowy_taiga";
                case 48 -> "minecraft:soul_sand_valley";
                case 49 -> "minecraft:sparse_jungle";
                case 50 -> "minecraft:stony_peaks";
                case 51 -> "minecraft:stony_shore";
                case 52 -> "minecraft:sunflower_plains";
                case 53 -> "minecraft:swamp";
                case 54 -> "minecraft:taiga";
                case 55 -> "minecraft:the_end";
                case 56 -> "minecraft:the_void";
                case 57 -> "minecraft:warm_ocean";
                case 58 -> "minecraft:warped_forest";
                case 59 -> "minecraft:windswept_forest";
                case 60 -> "minecraft:windswept_gravelly_hills";
                case 61 -> "minecraft:windswept_hills";
                case 62 -> "minecraft:windswept_savanna";
                case 63 -> "minecraft:wooded_badlands";
                default -> null;
            };
        }
    }

    private void parseLine(String line, Int2ObjectMap<String> map) {
        String[] lineParts = line.split(" ");
        int id = Integer.parseInt(lineParts[0]);
        String blockName = parseStateString(lineParts[1]);
        map.put(id, blockName);
    }

    private String parseStateString(String stateString) {
        int bracketIndex = stateString.indexOf("[");
        String resourceString = stateString.substring(0, (bracketIndex == -1) ? stateString.length() : bracketIndex);
        int curlyBracketOpenIndex = resourceString.indexOf("{");
        int curlyBracketCloseIndex = resourceString.indexOf("}");
        resourceString = resourceString.substring((curlyBracketOpenIndex == -1) ? 0 : (curlyBracketOpenIndex + 1), (curlyBracketCloseIndex == -1) ? resourceString.length() : curlyBracketCloseIndex);
        return resourceString;
    }
}
