package sh.okx.civmodern.common.map;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    ByteBuffer buf = ByteBuffer.allocate(512 * 512 * 4);
    try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(file))) {
      buf.asIntBuffer().put(data.getData());
      out.write(buf.array());
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
    try (InputStream in = new GZIPInputStream(new FileInputStream(file), 4096)) {
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
      ByteBuffer.wrap(buf).asIntBuffer().get(region.getData());
      return region;
    } catch (FileNotFoundException ignored) {
      return null;
    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
