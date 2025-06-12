package sh.okx.civmodern.common.map.converters;

import sh.okx.civmodern.common.map.RegionKey;

import java.io.File;

public abstract class Converter {
    public abstract boolean hasAlreadyConverted();
    public abstract boolean filesAvailable();
    public abstract void convert();

    protected abstract RegionKey getRegionKey(String fileName);

    File[] reorderFiles(File[] files) {
        // Regions are processed in batches of 128 regions at a time (~128 MB memory usage)
        // But the order that the regions are processed in is important, as VoxelMap regions are 256x256
        // and CivModern regions are 512x512. If two VoxelMap regions that would map to the same CivModern region
        // are processed in two different batches, then this causes the CivModern region to be written and read twice.
        // This function attempts to order the VoxelMap regions so that they will be processed in the same batch.
        // This improves performance by about two thirds, even though this algorithm is O(n^2) (lame)
        File[] newFiles = new File[files.length];
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i] == null) {
                continue;
            }
            newFiles[count++] = files[i];
            RegionKey iRegion = getRegionKey(files[i].getName());
            if (iRegion == null) {
                continue;
            }
            for (int j = i + 1; j < files.length; j++) {
                if (files[j] == null) {
                    continue;
                }
                RegionKey jRegion = getRegionKey(files[j].getName());
                if (jRegion == null) {
                    continue;
                }

                if ((iRegion.x() & ~0x1) == (jRegion.x() & ~0x1) && ((iRegion.z() & ~0x1) == (jRegion.z() & ~0x1))) {
                    newFiles[count++] = files[j];
                    files[j] = null;
                }
            }
        }
        return newFiles;
    }
}
