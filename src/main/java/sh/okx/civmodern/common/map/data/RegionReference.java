package sh.okx.civmodern.common.map.data;

import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RegionReference {
    private volatile RegionLoader hardReference;
    private volatile SoftReference<RegionLoader> softReference;

    private final AtomicInteger refCount;

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public RegionReference(RegionLoader loader, int initialRefCount) {
        this.hardReference = loader;
        this.refCount = new AtomicInteger(initialRefCount);
    }

    public RegionLoader getLoader() {
        if (this.hardReference == null) {
            return this.softReference.get();
        } else {
            return this.hardReference;
        }
    }

    public void addReference(RegionLoader loader) {
        this.refCount.incrementAndGet();
        if (this.hardReference == null) {
            this.hardReference = Objects.requireNonNull(this.softReference.get());
            if (loader != this.hardReference) {
                throw new IllegalStateException();
            }
        }
    }

    public void removeReference() {
        int i = this.refCount.decrementAndGet();
        if (i < 0) {
            throw new IllegalStateException("ref count is negative");
        } else if (i == 0) {
            this.softReference = new SoftReference<>(this.hardReference);
            this.hardReference = null;
        }
    }

    public void markDirty() {
        Objects.requireNonNull(this.hardReference);
        if (!this.isReferenced()) {
            throw new IllegalStateException("cannot mark dirty region with no references");
        }
        this.dirty.set(true);
    }

    public boolean clearDirty() {
        return this.dirty.getAndSet(false);
    }

    public boolean isReferenced() {
        return this.refCount.get() > 0;
    }
}
