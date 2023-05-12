package sh.okx.civmodern.common.map;

import java.io.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MapFile {

  private final File folder;

  public MapFile(File folder) {
    this.folder = folder;
  }

  public void save(Map<RegionKey, RegionData> dataMap) {
    this.folder.mkdir();

    for (Map.Entry<RegionKey, RegionData> entry : dataMap.entrySet()) {
      File file = this.folder.toPath().resolve(entry.getKey().x() + "," + entry.getKey().z()).toFile();
      try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
        for (int d : entry.getValue().getData()) {
          out.writeInt(d);
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }
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
      int totalRead = 0;//in.readNBytes(512 * 512 * 4);
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
