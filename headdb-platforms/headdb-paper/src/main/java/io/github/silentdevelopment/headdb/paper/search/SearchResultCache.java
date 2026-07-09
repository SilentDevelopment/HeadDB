package io.github.silentdevelopment.headdb.paper.search;

import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.local.HeadRegistry;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SearchResultCache {

    private static final int DEFAULT_MAX_ENTRIES = 32;
    private static final Duration DEFAULT_TTL = Duration.ofSeconds(10);

    private final ConcurrentHashMap<Key, Entry> cache;
    private final int maxEntries;
    private final long ttlNanos;

    public SearchResultCache() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_TTL);
    }

    public SearchResultCache(int maxEntries, @NotNull Duration ttl) {
        Objects.requireNonNull(ttl, "ttl");

        if (maxEntries < 1) {
            throw new IllegalArgumentException("Search result cache max entries must be at least 1.");
        }

        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Search result cache TTL must be positive.");
        }

        this.cache = new ConcurrentHashMap<>();
        this.maxEntries = maxEntries;
        this.ttlNanos = ttl.toNanos();
    }

    public @NotNull HeadQueryResult search(@NotNull HeadRegistry registry, @NotNull SearchRequest request, int page, int limit) {
        return search(registry, request, page, limit, false);
    }

    public @NotNull HeadQueryResult search(@NotNull HeadRegistry registry, @NotNull SearchRequest request, int page, int limit, boolean includeHidden) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(request, "request");

        long now = System.nanoTime();
        DatabaseStatus status = registry.status();
        Key key = Key.create(status, registry.revision(), request, includeHidden);

        Entry cached = cache.get(key);
        if (cached != null && !cached.expired(now, ttlNanos)) {
            return cached.result(page, limit);
        }

        HeadQuery query = SearchQueries.query(request, 1, Math.min(limit, HeadQuery.MAX_LIMIT));
        Entry created = new Entry(registry.searchAll(query, includeHidden), now);
        cache.put(key, created);
        clean(now);

        return created.result(page, limit);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private void clean(long now) {
        if (cache.size() <= maxEntries) {
            return;
        }

        cache.entrySet().removeIf(entry -> entry.getValue().expired(now, ttlNanos));

        if (cache.size() <= maxEntries) {
            return;
        }

        cache.clear();
    }

    private record Entry(@NotNull List<Head> heads, long createdAtNanos) {

        private Entry {
            heads = List.copyOf(Objects.requireNonNull(heads, "heads"));
        }

        private @NotNull HeadQueryResult result(int page, int limit) {
            int offset = Math.max(0, (Math.max(1, page) - 1) * Math.max(1, limit));
            int from = Math.min(offset, heads.size());
            int to = Math.min(from + Math.max(1, limit), heads.size());
            return new HeadQueryResult(heads.subList(from, to), heads.size(), offset, Math.max(1, limit));
        }

        private boolean expired(long now, long ttlNanos) {
            return now - createdAtNanos > ttlNanos;
        }
    }

    private record Key(
            @NotNull String state,
            @NotNull String source,
            @NotNull String manifestId,
            @NotNull String artifactId,
            @NotNull String loadedAt,
            long registryRevision,
            @NotNull String query,
            @NotNull Set<HeadId> ids,
            @NotNull Set<String> categories,
            @NotNull Set<String> tags,
            @NotNull Set<String> collections,
            @NotNull HeadSort sort,
            @NotNull SortDirection direction,
            boolean categoryLocked,
            boolean includeHidden
    ) {

        private Key {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(manifestId, "manifestId");
            Objects.requireNonNull(artifactId, "artifactId");
            Objects.requireNonNull(loadedAt, "loadedAt");
            Objects.requireNonNull(query, "query");
            ids = Set.copyOf(Objects.requireNonNull(ids, "ids"));
            categories = Set.copyOf(Objects.requireNonNull(categories, "categories"));
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
            collections = Set.copyOf(Objects.requireNonNull(collections, "collections"));
            Objects.requireNonNull(sort, "sort");
            Objects.requireNonNull(direction, "direction");
        }

        private static @NotNull Key create(@NotNull DatabaseStatus status, long registryRevision, @NotNull SearchRequest request, boolean includeHidden) {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(request, "request");

            return new Key(
                    status.state().name(),
                    status.source().name(),
                    text(status.manifestId()),
                    text(status.artifactId()),
                    status.loadedAt() == null ? "" : status.loadedAt().toString(),
                    registryRevision,
                    request.query(),
                    request.ids(),
                    request.categories(),
                    request.tags(),
                    request.collections(),
                    request.sort(),
                    request.direction(),
                    request.categoryLocked(),
                    includeHidden
            );
        }

        private static @NotNull String text(String value) {
            if (value == null) {
                return "";
            }

            return value;
        }
    }
}
