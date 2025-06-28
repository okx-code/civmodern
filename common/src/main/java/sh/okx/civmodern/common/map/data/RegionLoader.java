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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RegionLoader {

    private final Set<RegionDataType> loaded = EnumSet.noneOf(RegionDataType.class);

    private final Lock lock = new ReentrantLock();

    private final Lock renderLock = new ReentrantLock();

    private int[] mapData;
    private short[] yLevels;
    private int[] chunkTimestamps;

    private final AtomicBoolean hasBeenRendered = new AtomicBoolean(false);

    private final Map<RegionDataType, AtomicInteger> interests = new ConcurrentHashMap<>();

    private final RegionKey key;
    private final MapFolder mapFolder;

    public RegionLoader(RegionKey key, MapFolder mapFolder) {
        this.key = key;
        this.mapFolder = mapFolder;
    }

    public void addInterest(RegionDataType type) {
        this.interests.compute(type, (k, v) -> {
            if (v == null) {
                return new AtomicInteger(1);
            } else {
                v.incrementAndGet();
                return v;
            }
        });
    }

    public void removeInterest(RegionDataType type) {
        this.interests.compute(type, (k, v) -> {
            if (v == null) {
                return null;
            } else if (v.decrementAndGet() == 0) {
                return null;
            } else {
                return v;
            }
        });
    }

    public boolean hasInterests() {
        return !this.interests.isEmpty();
    }

    public int[] getOrLoadMapData() {
        if (this.mapData == null) {
            this.lock.lock();
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
                this.lock.unlock();
            }
        }
        return this.mapData;
    }

    public short[] getOrLoadYLevels() {
        if (this.yLevels == null) {
            this.lock.lock();
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
                this.lock.unlock();
            }
        }
        return this.yLevels;
    }

    public int[] getOrLoadChunkTimestamps() {
        if (this.chunkTimestamps == null) {
            this.lock.lock();
            try {
                if (this.chunkTimestamps != null) {
                    return this.chunkTimestamps;
                }
                byte[] regionData = this.mapFolder.getRegionData(key, RegionDataType.CHUNK_TIMESTAMPS);
                this.chunkTimestamps = new int[RegionMapUpdater.SIZE / 16 * RegionMapUpdater.SIZE / 16];
                if (regionData != null) {
                    ByteBuffer.wrap(regionData).asIntBuffer().get(this.chunkTimestamps);
                }
                this.loaded.add(RegionDataType.CHUNK_TIMESTAMPS);
            } finally {
                this.lock.unlock();
            }
        }
        return this.chunkTimestamps;
    }

    public boolean render() {
        return this.hasBeenRendered.compareAndSet(false, true);
    }

    public Set<RegionDataType> getLoaded() {
        this.lock.lock();
        try {
            return new HashSet<>(this.loaded);
        } finally {
            this.lock.unlock();
        }
    }

    public Lock getRenderLock() {
        return renderLock;
    }
}
