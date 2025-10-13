package sh.okx.civmodern.common.map;

public enum RegionDataType {
    MAP("map"),
    Y_LEVELS("y_levels"),
    CHUNK_TIMESTAMPS("chunk_timestamps"),
    WATER_Y_LEVELS("water_y_levels");

    private final String databaseKey;

    RegionDataType(String databaseKey) {
        this.databaseKey = databaseKey;
    }

    public String getDatabaseKey() {
        return databaseKey;
    }
}
