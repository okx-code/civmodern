package sh.okx.civmodern.common.map;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MapFile {

  private final File folder;

  public MapFile(File folder) {
    this.folder = folder;
  }

  public File getFolder() {
    return folder;
  }

  public void save(RegionKey key, RegionData data) {
    File file = this.folder.toPath().resolve(key.x() + "," + key.z()).toFile();
    try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
      for (int d : data.getData()) {
        out.writeInt(d);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public Set<RegionKey> listRegions() {
    String[] list = this.folder.list();
    if (list == null) {
      return Collections.emptySet();
    }
    Set<RegionKey> regions = new HashSet<>();
    for (String file : list) {
      String[] parts = file.split(",");
      if (parts.length == 2) {
        regions.add(new RegionKey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
      }
    }
    return regions;
  }

  public RegionData getRegion(RegionKey key) {
    File file = this.folder.toPath().resolve(key.x() + "," + key.z()).toFile();
    if (!file.exists()) {
      return null;
    }
    long k = System.nanoTime();
    try (InputStream in = new GZIPInputStream(new FileInputStream(file))) {
      RegionData region = new RegionData();
      byte[] buf = new byte[512 * 512 * 4];
      int totalRead = 0;
      while (totalRead < buf.length) {
        int read = in.read(buf, totalRead, buf.length - totalRead);
        if (read < 0) {
          return null;
        }
        totalRead += read;
      }
      DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(buf));
      int[] data = region.getData();
      for (int i = 0; i < data.length; i++) {
        data[i] = dataIn.readInt();
      }
      long s=  System.nanoTime();
      System.out.println("load " + key + " " + (s-k)/1000 + "us");
      return region;
    } catch (FileNotFoundException ignored) {
      return null;
    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
