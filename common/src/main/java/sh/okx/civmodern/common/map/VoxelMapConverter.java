package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.events.ScrollEvent;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VoxelMapConverter {

  private final MapFile mapFile;
  private final String name;
  private final String dimension;

  public VoxelMapConverter(MapFile mapFile, String name, String dimension) {
    this.mapFile = mapFile;
    this.name = name;
    this.dimension = dimension;
  }

  public boolean voxelMapFileExists() {
    return this.mapFile.getFolder().toPath().resolve("voxelmap").toFile().exists();
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

    Map<RegionKey, RegionData> regionMap = new ConcurrentHashMap<>();

    Set<String> converted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    boolean modified = false;
    File voxelmap = mapFile.getFolder().toPath().resolve("voxelmap").toFile();
    try(FileInputStream fis = new FileInputStream(voxelmap)) {
      converted.addAll(Arrays.asList(new String(fis.readAllBytes()).split("\n")));
    } catch (FileNotFoundException ignored) {
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    // 2859 regions
    // multithreaded processing - 1 minutes 16 seconds
    // sorting - 26 seconds
    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    while (regionIndex < files.length && !terminated) {
      for (int regionStart = regionIndex; regionIndex < Math.min(files.length, regionStart + 128); regionIndex++) {
        File subRegionFile = files[regionIndex];
        if (Thread.interrupted()) {
          terminated = true;
          AbstractCivModernMod.LOGGER.info("Terminated VoxelMap conversion at region " + regionIndex + "/" + files.length);
          break;
        }

        try {
          String subRegion = subRegionFile.getName().split("\\.")[0];
          if (converted.contains(subRegion)) {
            continue;
          }
          ZipFile zFile = new ZipFile(subRegionFile);
          int total = 0;
          byte[] data = new byte[256 * 256 * 18];
          ZipEntry ze = zFile.getEntry("data");
          InputStream is = zFile.getInputStream(ze);
          int count;
          for (byte[] byteData = new byte[2048]; (count = is.read(byteData, 0, 2048)) != -1 && count + total <= 256 * 256 * 18; total += count)
            System.arraycopy(byteData, 0, data, total, count);
          is.close();
          ze = zFile.getEntry("key");
          is = zFile.getInputStream(ze);
          Scanner sc = new Scanner(is);
          Int2IntMap map = new Int2IntOpenHashMap();
          while (sc.hasNextLine())
            parseLine(sc.nextLine(), map);
          sc.close();
          is.close();
          int version = 1;
          ze = zFile.getEntry("control");
          if (ze != null) {
            is = zFile.getInputStream(ze);
            if (is != null) {
              Properties properties = new Properties();
              properties.load(is);
              String versionString = properties.getProperty("version", "1");
              try {
                version = Integer.parseInt(versionString);
              } catch (NumberFormatException var16) {
                version = 1;
              }
              is.close();
            }
          }
          if (version != 2) {
            continue;
          }
          zFile.close();
          if (total == 256 * 256 * 18) {
            String[] name = subRegion.split(",");
            loadData(Integer.parseInt(name[0]), Integer.parseInt(name[1]), data, map, regionMap);
            modified = true;
          }
        } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
          ex.printStackTrace();
        }
      }

      List<Future<?>> futures = new ArrayList<>(regionMap.size());
      for (Map.Entry<RegionKey, RegionData> entry : regionMap.entrySet()) {
        futures.add(service.submit(() -> {
          mapFile.save(entry.getKey(), entry.getValue());
          converted.add(entry.getKey().x() + "," + entry.getKey().z() + ".zip");

          int savedCount = saved.incrementAndGet();
          if (savedCount == files.length || savedCount % 128 == 0) {
            AbstractCivModernMod.LOGGER.info("Saved " + savedCount + " regions...");
          }
        }));
      }

      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (InterruptedException e) {
          terminated = true;
          AbstractCivModernMod.LOGGER.info("Terminated saving VoxelMap conversion at region " + regionIndex + "/" + files.length);
          break;
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
      }

      AbstractCivModernMod.LOGGER.info("Processed " + regionIndex + "/" + files.length + " regions...");
      regionMap.clear();
    }

    try {
      service.shutdown();
      service.awaitTermination(100, TimeUnit.DAYS);
    } catch (InterruptedException ignored) {
    }

    if (modified) {
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

  private void loadData(int subRegionX, int subRegionZ, byte[] data, Int2IntMap map, Map<RegionKey, RegionData> regionMap) {
    int[] region = new int[256 * 256];

    int[] westY = new int[256];
    Arrays.fill(westY, Integer.MIN_VALUE);
    int northY = Integer.MIN_VALUE;
    int water = Registry.BLOCK.getId(Blocks.WATER);
    for (int x = 0; x < 256; x++) {
      for (int z = 0; z < 256; z++) {
        int y = getHeight(data, x, z);


        int dataValue = 0;

        int realBlockId = getBlockstate(data, x, z);

        int floorBlockstate = getOceanFloorBlockstate(data, x, z);
        int height = getOceanFloorheight(data, x, z);
        if (!map.containsKey(floorBlockstate)) {
          floorBlockstate = getTransparentBlockstate(data, x, z);
          height = getTransparentHeight(data, x, z);
        } else {
          // wtf voxelmap?
          int tmp = y;
          y = height;
          height = tmp;

          tmp = realBlockId;
          realBlockId = floorBlockstate;
          floorBlockstate = tmp;
        }

        int blockId = map.get(realBlockId);

        if (blockId > 0xFFFF) {
          AbstractCivModernMod.LOGGER.warn("convert block " + blockId + " at pos (" + x + ", " + z + ") in (" + subRegionX + ", " + subRegionZ + ")");
          blockId = 0;
        }

        dataValue |= blockId << 16;
        if (map.get(floorBlockstate) == water) {
          int depth = height - y;
          dataValue |= Mth.clamp(depth, 0, 0xF) << 12;
        }

        int biomeId = getBiomeID(data, x, z);
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
      }
      northY = Integer.MIN_VALUE;
    }

    int regionX = Math.floorDiv(subRegionX, 2);
    int regionZ = Math.floorDiv(subRegionZ, 2);

    int offsetX = Math.floorMod(subRegionX, 2);
    int offsetZ = Math.floorMod(subRegionZ, 2);

    RegionKey key = new RegionKey(regionX, regionZ);
    RegionData regionData = regionMap.computeIfAbsent(key, k -> {
      RegionData fileData = mapFile.getRegion(k);
      if (fileData == null) {
        return new RegionData();
      }
      return fileData;
    });
    int[] saved = regionData.getData();
    for (int x = offsetX * 256; x < offsetX * 256 + 256; x++) {
      for (int z = offsetZ * 256; z < offsetZ * 256 + 256; z++) {
        saved[z + x * 512] = region[(z - offsetZ * 256) + (x - offsetX * 256) * 256];
      }
    }
  }
  private byte getData(byte[] data, int x, int z, int bit) {
    int index = x + z * 256 + 256 * 256 * bit;
    return data[index];
  }

  public int getHeight(byte[] data, int x, int z) {
    return getData(data, x, z, 0) & 0xFF;
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

  public int getBiomeID(byte[] data, int x, int z) {
    return (getData(data, x, z, 16) & 0xFF) << 8 | getData(data, x, z, 17) & 0xFF;
  }

  private static void parseLine(String line, Int2IntMap map) {
    String[] lineParts = line.split(" ");
    int id = Integer.parseInt(lineParts[0]);
    int blockState = parseStateString(lineParts[1]);
    if (blockState != 0) {
      map.put(id, blockState);
    }
  }

  private static int parseStateString(String stateString) {
    int bracketIndex = stateString.indexOf("[");
    String resourceString = stateString.substring(0, (bracketIndex == -1) ? stateString.length() : bracketIndex);
    int curlyBracketOpenIndex = resourceString.indexOf("{");
    int curlyBracketCloseIndex = resourceString.indexOf("}");
    resourceString = resourceString.substring((curlyBracketOpenIndex == -1) ? 0 : (curlyBracketOpenIndex + 1), (curlyBracketCloseIndex == -1) ? resourceString.length() : curlyBracketCloseIndex);
    String[] resourceStringParts = resourceString.split(":");
    ResourceLocation resourceLocation = null;
    if (resourceStringParts.length == 1) {
      resourceLocation = new ResourceLocation(resourceStringParts[0]);
    } else if (resourceStringParts.length == 2) {
      resourceLocation = new ResourceLocation(resourceStringParts[0], resourceStringParts[1]);
    }
    Block block = Registry.BLOCK.get(resourceLocation);
    if (block != Blocks.AIR || resourceString.equals("minecraft:air")) {
      return Registry.BLOCK.getId(block);
    }
    return 0;
  }
}
