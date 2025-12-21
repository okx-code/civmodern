package sh.okx.civmodern.common.map.data;

import sh.okx.civmodern.common.map.MapFolder;
import sh.okx.civmodern.common.map.RegionDataType;
import sh.okx.civmodern.common.map.RegionKey;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegionLoader {

    private final Set<RegionDataType> loaded = EnumSet.noneOf(RegionDataType.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock renderLock = new ReentrantLock();

    private volatile int[] mapData;
    private volatile short[] yLevels;
    private volatile short[] waterYLevels;
    private volatile long[] chunkTimestamps;

    private final AtomicBoolean hasBeenRendered = new AtomicBoolean(false);

    private final RegionKey key;
    private final MapFolder mapFolder;

    public RegionLoader(RegionKey key, MapFolder mapFolder) {
        this.key = key;
        this.mapFolder = mapFolder;
    }

    public void loadAllData() {
        if (this.mapData != null && this.yLevels != null && this.chunkTimestamps != null) {
            return;
        }
        this.lock.writeLock().lock();
        try {
            if (this.mapData != null && this.yLevels != null && this.chunkTimestamps != null) {
                return;
            }
            for (Map.Entry<RegionDataType, byte[]> entry : this.mapFolder.getAllRegionData(key).entrySet()) {
                switch (entry.getKey()) {
                    case MAP -> {
                        if (this.mapData == null) setMapData(entry.getValue());
                    }
                    case Y_LEVELS -> {
                        if (this.yLevels == null) setYLevels(entry.getValue());
                    }
                    case WATER_Y_LEVELS -> {
                        if (this.waterYLevels == null) setWaterYLevels(entry.getValue());
                    }
                    case CHUNK_TIMESTAMPS -> {
                        if (this.chunkTimestamps == null) setChunkTimestamps(entry.getValue());
                    }
                }
                this.loaded.add(entry.getKey());
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void setMapData(byte[] regionData) {
        this.mapData = new int[RegionMapUpdater.SIZE * RegionMapUpdater.SIZE];
        if (regionData != null) {
            ByteBuffer.wrap(regionData).asIntBuffer().get(this.mapData);
        }
    }

    private void setYLevels(byte[] regionData) {
        this.yLevels = new short[RegionMapUpdater.SIZE * RegionMapUpdater.SIZE];
        if (regionData != null) {
            ByteBuffer.wrap(regionData).asShortBuffer().get(this.yLevels);
        }
    }

    private void setWaterYLevels(byte[] regionData) {
        this.waterYLevels = new short[RegionMapUpdater.SIZE * RegionMapUpdater.SIZE];
        if (regionData != null) {
            ByteBuffer.wrap(regionData).asShortBuffer().get(this.waterYLevels);
        }
    }

    private void setChunkTimestamps(byte[] regionData) {
        this.chunkTimestamps = new long[RegionMapUpdater.SIZE / 16 * RegionMapUpdater.SIZE / 16];
        if (regionData != null) {
            ByteBuffer.wrap(regionData).asLongBuffer().get(this.chunkTimestamps);
        }
    }

    private <T> T getOrLoadData(Supplier<T> getter, Consumer<byte[]> setter, RegionDataType dataType) {
        if (getter.get() == null) {
            this.lock.writeLock().lock();
            try {
                T val;
                if ((val = getter.get()) != null) {
                    return val;
                }
                byte[] regionData = this.mapFolder.getRegionData(key, dataType);
                setter.accept(regionData);
                this.loaded.add(dataType);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return getter.get();
    }

    public int[] getOrLoadMapData() {
        return getOrLoadData(() -> this.mapData, this::setMapData, RegionDataType.MAP);
    }

    public short[] getOrLoadYLevels() {
        return getOrLoadData(() -> this.yLevels, this::setYLevels, RegionDataType.Y_LEVELS);
    }

    public long[] getOrLoadChunkTimestamps() {
        return getOrLoadData(() -> this.chunkTimestamps, this::setChunkTimestamps, RegionDataType.CHUNK_TIMESTAMPS);
    }

    public short[] getOrLoadWaterYLevels() {
        return getOrLoadData(() -> this.waterYLevels, this::setWaterYLevels, RegionDataType.WATER_Y_LEVELS);
    }

    public boolean render() {
        return this.hasBeenRendered.compareAndSet(false, true);
    }

    public Set<RegionDataType> getLoaded() {
        this.lock.readLock().lock();
        try {
            return new HashSet<>(this.loaded);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public Lock getRenderLock() {
        return renderLock;
    }
}
