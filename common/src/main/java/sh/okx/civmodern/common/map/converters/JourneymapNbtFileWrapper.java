package sh.okx.civmodern.common.map.converters;

import net.minecraft.world.level.chunk.storage.RegionBitmap;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import sh.okx.civmodern.common.AbstractCivModernMod;
import sh.okx.civmodern.common.map.RegionKey;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Used to wrap journeymap's weird nbt shenanigans
 */
public class JourneymapNbtFileWrapper {
    public RegionKey regionKey;
    public Path filePath;

    private RegionFileVersion fileVersion;
    private final FileChannel file;

    private final ByteBuffer header;
    private final IntBuffer offsets;
    private final IntBuffer timestamps;
    private final RegionBitmap usedSectors;

    public JourneymapNbtFileWrapper(RegionKey regionKey, Path filePath) throws IOException {
        this.regionKey = regionKey;
        this.filePath = filePath;

        header = ByteBuffer.allocateDirect(8192);
        usedSectors = new RegionBitmap();
        // todo: use correct version later ig
        fileVersion = RegionFileVersion.VERSION_DEFLATE;
        offsets = header.asIntBuffer();
        offsets.limit(1024);

        header.position(4096);
        timestamps = header.asIntBuffer();

        file = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);

        usedSectors.force(0, 2);
        header.position(0);

        final int readBytes = this.file.read(this.header, 0L);
        if (readBytes != -1) {
            final long size = Files.size(filePath);
            for (int k = 0; k < 1024; ++k) {
                final int offsetIndex = this.offsets.get(k);
                if (offsetIndex != 0) {
                    final int sectorNumber = getSectorNumber(offsetIndex);
                    final int numSectors = getNumSectors(offsetIndex);
                    if (sectorNumber < 2 || numSectors == 0 || sectorNumber * 4096L > size) {
                        this.offsets.put(k, 0);
                    } else {
                        this.usedSectors.force(sectorNumber, numSectors);
                    }
                }
            }
            // AbstractCivModernMod.LOGGER.warn("Offset for Subregion {},{} is {}", regionKey.x(), regionKey.z(), offsets);
        } else {
            // AbstractCivModernMod.LOGGER.warn("Failed to read bytes for Subregion {},{} ", regionKey.x(), regionKey.z());
        }

        // AbstractCivModernMod.LOGGER.warn("Done for Subregion {},{}", regionKey.x(), regionKey.z());
    }

    public DataInputStream getInputStream() throws IOException {
        var offset = getOffset();
        // todo: fix all offsets are zero
        if (offset != 0) {
            var sectorNum = offset >> 8 & 0xFFFFFF;
            var numOfSectors = offset & 0xFF;
            var bytes = numOfSectors * 4096;
            var bytebuffer = ByteBuffer.allocate(bytes);
            file.read(bytebuffer, numOfSectors * 4096L);
            bytebuffer.flip();
            if (bytebuffer.remaining() >= 5) {
                final int size = bytebuffer.getInt();
                final byte buf = bytebuffer.get();
                if (size != 0) {
                    final int position = size - 1;
                    if (position <= bytebuffer.remaining() && position >= 0) {
                        return this.createChunkInputStream(buf, createStream(bytebuffer, position));
                    }
                }
            } else {
                AbstractCivModernMod.LOGGER.warn("Subregion {},{} ! >= 5", regionKey.x(), regionKey.z());
            }
        } else {
            AbstractCivModernMod.LOGGER.warn("Subregion {},{} offset is 0", regionKey.x(), regionKey.z());
        }

        return null;
    }

    private int getOffsetIndex() {
        return regionKey.getRegionLocalX() + regionKey.getRegionLocalZ() * 32;
    }

    private int getOffset() {
        // 0
        return this.offsets.get(getOffsetIndex());
    }

    private static int getSectorNumber(final int offset) {
        return offset >> 8 & 0xFFFFFF;
    }

    private static int getNumSectors(final int offset) {
        return offset & 0xFF;
    }

    private DataInputStream createChunkInputStream(final byte version, final InputStream inputStream) throws IOException {
        var fileVersion = RegionFileVersion.fromId((int) version);
        if (fileVersion != null) {
            return new DataInputStream(new BufferedInputStream(fileVersion.wrap(inputStream)));
        }
        return null;
    }

    private ByteArrayInputStream createStream(final ByteBuffer buf, final int length) {
        return new ByteArrayInputStream(buf.array(), buf.position(), length);
    }
}
