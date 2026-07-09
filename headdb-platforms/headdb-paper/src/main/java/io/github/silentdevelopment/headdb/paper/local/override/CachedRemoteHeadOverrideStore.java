package io.github.silentdevelopment.headdb.paper.local.override;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CachedRemoteHeadOverrideStore implements RemoteHeadOverrideStore {

    private final RemoteHeadOverrideStore delegate;
    private Map<HeadId, RemoteHeadOverride> cache;

    public CachedRemoteHeadOverrideStore(@NotNull RemoteHeadOverrideStore delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public synchronized @NotNull Optional<RemoteHeadOverride> find(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isRemote()) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache().get(id));
    }

    @Override
    public synchronized @NotNull Collection<RemoteHeadOverride> list() {
        return List.copyOf(cache().values());
    }

    @Override
    public synchronized void save(@NotNull RemoteHeadOverride override) {
        Objects.requireNonNull(override, "override");
        delegate.save(override);
        Map<HeadId, RemoteHeadOverride> updated = new LinkedHashMap<>(cache());
        if (override.empty()) {
            updated.remove(override.headId());
        } else {
            updated.put(override.headId(), override);
        }
        cache = Collections.unmodifiableMap(updated);
    }

    @Override
    public synchronized boolean delete(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isRemote()) {
            return false;
        }
        boolean deleted = delegate.delete(id);
        if (!deleted) {
            return false;
        }
        Map<HeadId, RemoteHeadOverride> updated = new LinkedHashMap<>(cache());
        updated.remove(id);
        cache = Collections.unmodifiableMap(updated);
        return true;
    }

    @Override
    public synchronized int deleteOrphans(@NotNull Set<HeadId> validRemoteIds) {
        Objects.requireNonNull(validRemoteIds, "validRemoteIds");
        int deleted = delegate.deleteOrphans(validRemoteIds);
        if (deleted <= 0) {
            return 0;
        }
        Map<HeadId, RemoteHeadOverride> updated = new LinkedHashMap<>(cache());
        updated.keySet().removeIf(id -> !validRemoteIds.contains(id));
        cache = Collections.unmodifiableMap(updated);
        return deleted;
    }

    public synchronized void invalidate() {
        cache = null;
    }

    private @NotNull Map<HeadId, RemoteHeadOverride> cache() {
        Map<HeadId, RemoteHeadOverride> current = cache;
        if (current != null) {
            return current;
        }
        Map<HeadId, RemoteHeadOverride> loaded = new LinkedHashMap<>();
        for (RemoteHeadOverride override : delegate.list()) {
            loaded.put(override.headId(), override);
        }
        current = Collections.unmodifiableMap(loaded);
        cache = current;
        return current;
    }
}
