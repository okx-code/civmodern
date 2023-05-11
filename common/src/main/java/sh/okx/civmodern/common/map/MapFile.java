package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MapFile {

  private final File file;
  private final File headerFile;
  private final Object2IntMap<RegionKey> header = new Object2IntOpenHashMap<>();
  private FileInputStream input;

  public MapFile(File file, File headerFile) {
    this.file = file;
    this.headerFile = headerFile;
    try {
      this.input = new FileInputStream(file);
    } catch (FileNotFoundException ignored) {
    }
  }

  public void save(Map<RegionKey, RegionData> dataMap) {
    try (FileOutputStream outputHeader = new FileOutputStream(headerFile);
         FileOutputStream outputData = new FileOutputStream(file);
         DataOutputStream outHeader = new DataOutputStream(new BufferedOutputStream(outputHeader));
         DataOutputStream outData = new DataOutputStream(new BufferedOutputStream(outputData))) {
      Object2IntMap<RegionData> dataList = new Object2IntOpenHashMap<>(dataMap.size());

      Map<RegionKey, RegionData> newEntries = new HashMap<>(dataMap);
      for (Object2IntMap.Entry<RegionKey> entry : header.object2IntEntrySet()) {
        RegionData removed = newEntries.remove(entry.getKey());
        if (removed != null) {
          dataList.put(removed, entry.getIntValue());
        }
      }

      outHeader.writeInt(0);
      outHeader.writeInt(header.size() + newEntries.size());

      outHeader.flush();
      outputHeader.getChannel().position(8 + header.size() * 4L);
      int pos = header.size();
      for (RegionKey key : newEntries.keySet()) {
        outHeader.writeShort((short) key.x());
        outHeader.writeShort((short) key.z());
        dataList.put(newEntries.get(key), pos);
        pos++;
      }

      for (Object2IntMap.Entry<RegionData> data : dataList.object2IntEntrySet()) {
        outData.flush();
        outputData.getChannel().position( data.getIntValue() * 4 * 512 * 512L);
        for (int d : data.getKey().getData()) {
          outData.writeInt(d);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      if (input != null) {
        input.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void loadHeader() {
    try (FileInputStream input = new FileInputStream(headerFile);
         DataInputStream in = new DataInputStream(new BufferedInputStream(input))) {

      in.readInt();

      int size = in.readInt();

      for (int i = 0; i < size; i++) {
        short x = in.readShort();
        short z = in.readShort();
        this.header.put(new RegionKey(x, z), i);
      }
    } catch (FileNotFoundException ignored) {
    } catch (IOException e) {
      this.header.clear();
      e.printStackTrace();
    }
  }

  public RegionData getRegion(RegionKey key) {
    if (!this.header.containsKey(key) || input == null) {
      return null;
    }

    try {
      int slot = this.header.getInt(key);
      int byteOffset = slot * 4 * 512 * 512;
      input.getChannel().position(byteOffset);

      RegionData region = new RegionData();
      int[] data = region.getData();

      byte[] dataBytes = new byte[data.length * 4];
      input.read(dataBytes);

      DataInputStream dis = new DataInputStream(new ByteArrayInputStream(dataBytes));
      for (int i = 0; i < data.length; i++) {
        data[i] = dis.readInt();
      }

      return region;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
