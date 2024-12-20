package sh.okx.civmodern.common.map;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockLookup {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<String> idToBlock;
    private final Map<String, Integer> blockToId;

    public BlockLookup(List<String> blockNames) {
        this.idToBlock = new ArrayList<>(blockNames);

        this.blockToId = new Object2IntOpenHashMap<>();
        for (int i = 0; i < blockNames.size(); i++) {
            this.blockToId.put(blockNames.get(i), i);
        }
    }

    public int getOrCreateBlockId(String blockName) {
        try {
            lock.writeLock().lock();
            return blockToId.computeIfAbsent(blockName, k -> {
                idToBlock.add(k);
                return blockToId.size();
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getBlockName(int id) {
        try {
            lock.readLock().lock();
            return id >= idToBlock.size() ? "minecraft:stone" : idToBlock.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> getBlockNames() {
        return idToBlock;
    }
}
