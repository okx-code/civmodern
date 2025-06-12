package sh.okx.civmodern.common.map;

public record RegionKey(int x, int z) {

    public int getRegionLocalX() {
        return this.x & 31;
    }
    public int getRegionLocalZ() {
        return this.z & 31;
    }
}
