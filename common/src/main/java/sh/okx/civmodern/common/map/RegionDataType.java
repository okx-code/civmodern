package sh.okx.civmodern.common.map;

public enum RegionDataType {
    MAP("map"),
    Y_LEVELS("y_levels");

    private final String databaseKey;

    RegionDataType(String databaseKey) {
        this.databaseKey = databaseKey;
    }

    public String getDatabaseKey() {
        return databaseKey;
    }
}
