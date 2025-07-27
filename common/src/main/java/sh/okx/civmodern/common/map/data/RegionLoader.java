package sh.okx.civmodern.common.map.data;

import sh.okx.civmodern.common.map.MapFolder;
import sh.okx.civmodern.common.map.RegionDataType;
import sh.okx.civmodern.common.map.RegionKey;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RegionLoader {

    private final Set<RegionDataType> loaded = EnumSet.noneOf(RegionDataType.class);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock renderLock = new ReentrantLock();

    private volatile int[] mapData;
    private volatile short[] yLevels;
    private volatile long[] chunkTimestamps;

    private final AtomicBoolean hasBeenRendered = new AtomicBoolean(false);

    private final RegionKey key;
    private final MapFolder mapFolder;

    public RegionLoader(RegionKey key, MapFolder mapFolder) {
        this.key = key;
        this.mapFolder = mapFolder;
    }

    public int[] getOrLoadMapData() {
        if (this.mapData == null) {
            this.lock.writeLock().lock();
            try {
                if (this.mapData != null) {
                    return this.mapData;
                }
                byte[] regionData = this.mapFolder.getRegionData(key, RegionDataType.MAP);
                this.mapData = new int[RegionMapUpdater.SIZE * RegionMapUpdater.SIZE];
                if (regionData != null) {
                    ByteBuffer.wrap(regionData).asIntBuffer().get(this.mapData);
                }
                this.loaded.add(RegionDataType.MAP);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return this.mapData;
    }

    public short[] getOrLoadYLevels() {
        if (this.yLevels == null) {
            this.lock.writeLock().lock();
            try {
                if (this.yLevels != null) {
                    return this.yLevels;
                }
                byte[] regionData = this.mapFolder.getRegionData(key, RegionDataType.Y_LEVELS);
                this.yLevels = new short[RegionMapUpdater.SIZE * RegionMapUpdater.SIZE];
                if (regionData != null) {
                    ByteBuffer.wrap(regionData).asShortBuffer().get(this.yLevels);
                }
                this.loaded.add(RegionDataType.Y_LEVELS);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return this.yLevels;
    }

    public long[] getOrLoadChunkTimestamps() {
        if (this.chunkTimestamps == null) {
            this.lock.writeLock().lock();
            try {
                if (this.chunkTimestamps != null) {
                    return this.chunkTimestamps;
                }
                byte[] regionData = this.mapFolder.getRegionData(key, RegionDataType.CHUNK_TIMESTAMPS);
                this.chunkTimestamps = new long[RegionMapUpdater.SIZE / 16 * RegionMapUpdater.SIZE / 16];
                if (regionData != null) {
                    ByteBuffer.wrap(regionData).asLongBuffer().get(this.chunkTimestamps);
                }
                this.loaded.add(RegionDataType.CHUNK_TIMESTAMPS);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
        return this.chunkTimestamps;
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
