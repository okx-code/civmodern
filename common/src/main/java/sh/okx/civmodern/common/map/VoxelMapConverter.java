package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import sh.okx.civmodern.common.AbstractCivModernMod;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VoxelMapConverter {

  private final MapFile mapFile;

  public VoxelMapConverter(MapFile mapFile) {
    this.mapFile = mapFile;
  }

  public void convert() {

    File voxelmap = mapFile.getFolder().toPath().resolve("voxelmap").toFile();
    Set<String> converted;
    boolean modified = false;
    try(FileInputStream fis = new FileInputStream(voxelmap)) {
      converted = new HashSet<>(Arrays.asList(new String(fis.readAllBytes()).split("\n")));
    } catch (FileNotFoundException ignored) {
      converted = new HashSet<>();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    Map<RegionKey, RegionData> regionMap = new HashMap<>();

    File overworld = Minecraft.getInstance().gameDirectory.toPath().resolve("voxelmap").resolve("cache").resolve("play.civmc.net").resolve("overworld").toFile();
    File[] files = overworld.listFiles();
    AbstractCivModernMod.LOGGER.info("Converting " + files.length + " VoxelMap regions to CivModern regions, this may take a few minutes...");
    for (File subRegionFile : files) {
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
          converted.add(subRegion);
          modified = true;
        }
      } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException ex) {
        ex.printStackTrace();
      }
    }

    AbstractCivModernMod.LOGGER.info("Saving " + regionMap.size() + " regions...");

    mapFile.save(regionMap);
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

        int blockId = map.get(getBlockstate(data, x, z));
        if (blockId > 0xFFFF) {
          AbstractCivModernMod.LOGGER.warn("convert block " + blockId + " at pos (" + x + ", " + z + ") in (" + subRegionX + ", " + subRegionZ + ")");
          blockId = 0;
        }
        dataValue |= blockId << 16;

        if (map.get(getTransparentBlockstate(data, x, z)) == water) {
          int depth = getTransparentHeight(data, x, z) - y;
          dataValue |= Math.min(depth, 0xF) << 12;
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
    RegionData regionData = regionMap.get(key);
    if (regionData == null) {
      regionData = mapFile.getRegion(key);
      if (regionData == null) {
        regionData = new RegionData();
      }
      regionMap.put(key, regionData);
    }
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
