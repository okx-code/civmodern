package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockLookup {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Int2ObjectMap<String> idToBlock;
    private final Map<String, Integer> blockToId;

    private int highest = 0;

    public BlockLookup(Int2ObjectMap<String> blockNames) {
        this.idToBlock = new Int2ObjectOpenHashMap<>(blockNames);

        this.blockToId = new Object2IntOpenHashMap<>();
        for (Int2ObjectMap.Entry<String> entry : blockNames.int2ObjectEntrySet()) {
            this.blockToId.put(entry.getValue(), entry.getIntKey());
            if (entry.getIntKey() > highest) {
                highest = entry.getIntKey();
            }
        }
    }

    public int getOrCreateBlockId(String blockName) {
        try {
            lock.writeLock().lock();
            return blockToId.computeIfAbsent(blockName, k -> {
                highest++;
                idToBlock.put(highest, k);
                return highest;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getBlockName(int id) {
        try {
            lock.readLock().lock();
            return idToBlock.getOrDefault(id, "minecraft:stone");
        } finally {
            lock.readLock().unlock();
        }
    }

    public Int2ObjectMap<String> getBlockNames() {
        return idToBlock;
    }
}
