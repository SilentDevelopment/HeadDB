package io.github.silentdevelopment.headdb.paper.item;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class CachingHeadItemFactory implements HeadItemFactory {

    private static final int DEFAULT_MAX_SIZE = 4096;

    private final HeadItemFactory delegate;
    private final LongSupplier revisionSupplier;
    private final Map<CacheKey, ItemStack> cache;

    public CachingHeadItemFactory(@NotNull HeadItemFactory delegate) {
        this(delegate, DEFAULT_MAX_SIZE, () -> 0L);
    }

    public CachingHeadItemFactory(@NotNull HeadItemFactory delegate, int maxSize) {
        this(delegate, maxSize, () -> 0L);
    }

    public CachingHeadItemFactory(@NotNull HeadItemFactory delegate, int maxSize, @NotNull LongSupplier revisionSupplier) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.revisionSupplier = Objects.requireNonNull(revisionSupplier, "revisionSupplier");
        this.cache = new LruItemCache(maxSize);
    }

    @Override
    public @NotNull ItemStack create(@NotNull Head head) {
        Objects.requireNonNull(head, "head");
        CacheKey key = new CacheKey(head.id(), revisionSupplier.getAsLong());

        synchronized (cache) {
            ItemStack cached = cache.get(key);

            if (cached != null) {
                return cached.clone();
            }
        }

        ItemStack created = delegate.create(head);
        ItemStack prototype = created.clone();

        synchronized (cache) {
            cache.put(key, prototype);
        }

        return prototype.clone();
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }

    private record CacheKey(@NotNull HeadId id, long revision) {
        private CacheKey {
            Objects.requireNonNull(id, "id");
        }
    }

    private static final class LruItemCache extends LinkedHashMap<CacheKey, ItemStack> {

        private final int maxSize;

        private LruItemCache(int maxSize) {
            super(128, 0.75F, true);

            if (maxSize < 0) {
                throw new IllegalArgumentException("maxSize cannot be negative.");
            }

            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, ItemStack> eldest) {
            if (maxSize == 0) {
                return false;
            }

            return size() > maxSize;
        }
    }
}
