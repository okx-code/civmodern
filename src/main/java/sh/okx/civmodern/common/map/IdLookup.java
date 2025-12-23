package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IdLookup {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Int2ObjectMap<String> fromId;
    private volatile Int2ObjectMap<String> fromIdReplica;
    private final Map<String, Integer> toId;
    private final String defaultLookup;

    private int highest = 0;

    public IdLookup(Int2ObjectMap<String> names, String defaultLookup) {
        this.fromId = new Int2ObjectOpenHashMap<>(names);
        this.fromIdReplica = new Int2ObjectOpenHashMap<>(fromId);
        this.defaultLookup = defaultLookup;

        this.toId = new ConcurrentHashMap<>();
        for (Int2ObjectMap.Entry<String> entry : names.int2ObjectEntrySet()) {
            this.toId.put(entry.getValue(), entry.getIntKey());
            if (entry.getIntKey() > highest) {
                highest = entry.getIntKey();
            }
        }
    }

    public int getOrCreateId(String name) {
        int id = toId.getOrDefault(name, -1);
        if (id != -1) {
            return id;
        }
        try {
            lock.writeLock().lock();
            Integer computed = toId.computeIfAbsent(name, k -> {
                highest++;
                return highest;
            });
            fromId.put(highest, name);
            fromIdReplica = new Int2ObjectOpenHashMap<>(fromId);
            return computed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getName(int id) {
        return fromIdReplica.getOrDefault(id, defaultLookup);
    }

    public Int2ObjectMap<String> getNames() {
        return new Int2ObjectOpenHashMap<>(fromIdReplica);
    }
}
