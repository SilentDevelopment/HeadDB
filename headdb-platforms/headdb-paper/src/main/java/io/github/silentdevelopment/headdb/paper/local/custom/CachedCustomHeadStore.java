package io.github.silentdevelopment.headdb.paper.local.custom;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CachedCustomHeadStore implements CustomHeadStore {

    private final CustomHeadStore delegate;
    private Map<HeadId, StoredCustomHead> cache;

    public CachedCustomHeadStore(@NotNull CustomHeadStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public synchronized @NotNull Optional<StoredCustomHead> findStored(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isCustom()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache().get(id));
    }

    @Override
    public synchronized @NotNull Collection<StoredCustomHead> listStored() {
        return List.copyOf(cache().values());
    }

    @Override
    public synchronized void save(@NotNull StoredCustomHead head) {
        Objects.requireNonNull(head, "head");
        delegate.save(head);
        Map<HeadId, StoredCustomHead> updated = new LinkedHashMap<>(cache());
        updated.put(head.headId(), head);
        cache = Collections.unmodifiableMap(updated);
    }

    @Override
    public synchronized boolean delete(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isCustom()) {
            return false;
        }
        boolean deleted = delegate.delete(id);
        if (!deleted) {
            return false;
        }
        Map<HeadId, StoredCustomHead> updated = new LinkedHashMap<>(cache());
        updated.remove(id);
        cache = Collections.unmodifiableMap(updated);
        return true;
    }

    public synchronized void invalidate() {
        cache = null;
    }

    private @NotNull Map<HeadId, StoredCustomHead> cache() {
        Map<HeadId, StoredCustomHead> current = cache;
        if (current != null) {
            return current;
        }
        Map<HeadId, StoredCustomHead> loaded = new LinkedHashMap<>();
        for (StoredCustomHead head : delegate.listStored()) {
            loaded.put(head.headId(), head);
        }
        current = Collections.unmodifiableMap(loaded);
        cache = current;
        return current;
    }
}
