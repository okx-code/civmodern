package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.*;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MapFolder {

    private static final int VERSION = 0;

    private final File folder;
    private final Connection connection;

    public MapFolder(File folder) {
        this.folder = folder;

        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + folder.toPath().resolve("map.sqlite").toAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS meta (key TEXT NOT NULL PRIMARY KEY, value BLOB)");
                statement.execute("CREATE TABLE IF NOT EXISTS waypoints (name TEXT NOT NULL, x INT NOT NULL, y INT NOT NULL, z INT NOT NULL, icon TEXT NOT NULL, colour INT NOT NULL, PRIMARY KEY (x, y, z))");
                statement.execute("CREATE TABLE IF NOT EXISTS blocks (name TEXT NOT NULL UNIQUE, id INTEGER NOT NULL UNIQUE)");
                statement.execute("CREATE TABLE IF NOT EXISTS regions (x INT NOT NULL, z INT NOT NULL, data BLOB NOT NULL, PRIMARY KEY (x, z))");

                statement.execute("INSERT INTO meta VALUES (\"version\", " + VERSION + ") ON CONFLICT DO NOTHING");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFolder() {
        return folder;
    }

    public Connection getConnection() {
        return connection;
    }

    public void save(RegionKey key, RegionData data) {
        saveBulk(Collections.singletonMap(key, data));
    }

    public void saveBulk(Map<RegionKey, RegionData> dataMap) {
        synchronized (this.connection) {
            try (PreparedStatement statement = this.connection.prepareStatement("INSERT INTO regions (x, z, data) VALUES (?, ?, ?) ON CONFLICT DO UPDATE SET data = ?")) {

                for (Map.Entry<RegionKey, RegionData> entry : dataMap.entrySet()) {
                    statement.setInt(1, entry.getKey().x());
                    statement.setInt(2, entry.getKey().z());

                    ByteBuffer buf = ByteBuffer.allocate(512 * 512 * 4);
                    buf.asIntBuffer().put(entry.getValue().getData());

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream(out);
                    gzip.write(buf.array());
                    gzip.close();

                    byte[] bytes = out.toByteArray();
                    statement.setBytes(3, bytes);
                    statement.setBytes(4, bytes);

                    statement.addBatch();
                }

                statement.executeBatch();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Set<RegionKey> listRegions() {
        synchronized (this.connection) {
            try (Statement statement = this.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT x, z FROM regions");

                Set<RegionKey> keys = new HashSet<>();
                while (resultSet.next()) {
                    keys.add(new RegionKey(resultSet.getInt("x"), resultSet.getInt("z")));
                }

                return keys;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Int2ObjectMap<String> blockIds() {
        synchronized (this.connection) {
            try (Statement statement = this.connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT name, id FROM blocks");

                Int2ObjectMap<String> blockIds = new Int2ObjectOpenHashMap<>();
                while (resultSet.next()) {
                    blockIds.put(resultSet.getInt("id"), resultSet.getString("name"));
                }

                return blockIds;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void saveBlockIds(Int2ObjectMap<String> blockIds) {
        synchronized (this.connection) {
            try (PreparedStatement statement = this.connection.prepareStatement("INSERT INTO blocks (name, id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {

                for (Int2ObjectMap.Entry<String> entry : blockIds.int2ObjectEntrySet()) {
                    statement.setString(1, entry.getValue());
                    statement.setInt(2, entry.getIntKey());
                    statement.addBatch();
                }

                statement.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public RegionData getRegion(BlockLookup blockLookup, RegionKey key) {
        synchronized (this.connection) {
            try (PreparedStatement statement = this.connection.prepareStatement("SELECT data FROM regions WHERE x = ? AND z = ?")) {
                statement.setInt(1, key.x());
                statement.setInt(2, key.z());

                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return null;
                }

                RegionData region = new RegionData(blockLookup);
                byte[] compressed = resultSet.getBytes("data");

                ByteArrayInputStream in = new ByteArrayInputStream(compressed);
                GZIPInputStream gzip = new GZIPInputStream(in);
                byte[] data = gzip.readAllBytes();
                gzip.close();
                ByteBuffer.wrap(data).asIntBuffer().get(region.getData());

                return region;
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
