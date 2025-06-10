package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IdLookup {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Int2ObjectMap<String> fromId;
    private final Map<String, Integer> toId;
    private final String defaultLookup;

    private int highest = 0;

    public IdLookup(Int2ObjectMap<String> names, String defaultLookup) {
        this.fromId = new Int2ObjectOpenHashMap<>(names);
        this.defaultLookup = defaultLookup;

        this.toId = new Object2IntOpenHashMap<>();
        for (Int2ObjectMap.Entry<String> entry : names.int2ObjectEntrySet()) {
            this.toId.put(entry.getValue(), entry.getIntKey());
            if (entry.getIntKey() > highest) {
                highest = entry.getIntKey();
            }
        }
    }

    public int getOrCreateId(String name) {
        try {
            lock.writeLock().lock();
            return toId.computeIfAbsent(name, k -> {
                highest++;
                fromId.put(highest, k);
                return highest;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getName(int id) {
        try {
            lock.readLock().lock();
            return fromId.getOrDefault(id, defaultLookup);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Int2ObjectMap<String> getNames() {
        return fromId;
    }
}
