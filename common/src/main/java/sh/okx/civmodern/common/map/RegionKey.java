package sh.okx.civmodern.common.map;

import org.jetbrains.annotations.NotNull;

public record RegionKey(int x, int z) {

    public int getRegionLocalX() {
        return this.x & 31;
    }
    public int getRegionLocalZ() {
        return this.z & 31;
    }

    public @NotNull String toString() {
        return x + "," + z;
    }
}
