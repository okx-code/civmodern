package sh.okx.civmodern.common.map;

import org.jetbrains.annotations.NotNull;

public record RegionKey(int x, int z) {
    public @NotNull String toString() {
        return x + "," + z;
    }
}
