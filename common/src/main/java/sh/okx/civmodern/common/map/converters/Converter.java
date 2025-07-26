package sh.okx.civmodern.common.map.converters;

import sh.okx.civmodern.common.map.RegionKey;

import java.io.File;

public interface Converter {
    boolean hasAlreadyConverted();

    boolean filesAvailable();

    void convert();

    RegionKey getRegionKey(String fileName);
}
