package io.github.silentdevelopment.headdb.paper.local;

import io.github.silentdevelopment.headdb.core.database.DefaultHeadDatabase;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.paper.local.custom.CustomHeadStore;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.override.HeadOverrideMerger;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverrideStore;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadService;
import io.github.silentdevelopment.headdb.paper.local.taxonomy.CustomTaxonomyService;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class HeadRegistry implements io.github.silentdevelopment.headdb.registry.HeadRegistry {

    private final DefaultHeadDatabase remoteDatabase;
    private final RemoteHeadOverrideStore overrideStore;
    private final CustomHeadStore customHeadStore;
    private final PlayerHeadService playerHeadService;
    private final CustomTaxonomyService customTagService;
    private final CustomTaxonomyService customCollectionService;
    private final HeadOverrideMerger merger;

    public HeadRegistry(@NotNull DefaultHeadDatabase remoteDatabase, @NotNull RemoteHeadOverrideStore overrideStore, @NotNull CustomHeadStore customHeadStore, @NotNull PlayerHeadService playerHeadService, @NotNull CustomTaxonomyService customTagService, @NotNull CustomTaxonomyService customCollectionService) {
        this.remoteDatabase = Objects.requireNonNull(remoteDatabase, "remoteDatabase");
        this.overrideStore = Objects.requireNonNull(overrideStore, "overrideStore");
        this.customHeadStore = Objects.requireNonNull(customHeadStore, "customHeadStore");
        this.playerHeadService = Objects.requireNonNull(playerHeadService, "playerHeadService");
        this.customTagService = Objects.requireNonNull(customTagService, "customTagService");
        this.customCollectionService = Objects.requireNonNull(customCollectionService, "customCollectionService");
        this.merger = new HeadOverrideMerger();
    }

    public @NotNull DatabaseStatus status() {
        return remoteDatabase.status();
    }

    public @NotNull DatabaseStats stats() {
        HeadQueryResult all = search(HeadQuery.builder().limit(1).build());
        DatabaseStats remote = remoteDatabase.stats();
        return new DatabaseStats(all.total(), categories().size(), tags().size(), collections().size(), remote.revocations());
    }

    public @NotNull Optional<Head> find(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");

        if (id.isRemote()) {
            return remoteDatabase.findById(id).map(this::effectiveRemoteHead);
        }

        if (id.isCustom()) {
            return customHeadStore.find(id);
        }

        return playerHeadService.resolveCached(id.key());
    }

    public @NotNull CompletableFuture<Optional<Head>> resolve(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");

        if (id.isPlayer()) {
            return playerHeadService.resolve(id.key()).thenApply(Optional::of);
        }

        Optional<Head> head = find(id);
        if (head.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.completedFuture(head);
    }

    public @NotNull HeadQueryResult search(@NotNull HeadQuery query) {
        Objects.requireNonNull(query, "query");

        List<Head> matches = filter(searchableHeads(false), query).sorted(comparator(query)).toList();
        int from = Math.min(query.offset(), matches.size());
        int to = Math.min(from + query.limit(), matches.size());
        return new HeadQueryResult(matches.subList(from, to), matches.size(), query.offset(), query.limit());
    }

    public @NotNull HeadQueryResult searchIncludingHidden(@NotNull HeadQuery query) {
        Objects.requireNonNull(query, "query");

        List<Head> matches = filter(searchableHeads(true), query).sorted(comparator(query)).toList();
        int from = Math.min(query.offset(), matches.size());
        int to = Math.min(from + query.limit(), matches.size());
        return new HeadQueryResult(matches.subList(from, to), matches.size(), query.offset(), query.limit());
    }


    public @NotNull List<Head> heads(boolean includeHidden) {
        return searchableHeads(includeHidden);
    }

    public @NotNull List<Head> hiddenHeads() {
        Map<HeadId, RemoteHeadOverride> overrides = remoteOverridesById();
        List<Head> hidden = new ArrayList<>();
        for (Head remote : remoteDatabase.heads()) {
            RemoteHeadOverride override = overrides.get(remote.id());
            if (override == null || !Boolean.TRUE.equals(override.hidden())) {
                continue;
            }

            hidden.add(merger.merge(remote, override));
        }

        return hidden.stream().sorted(Comparator.comparing(Head::category).thenComparing(Head::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public @NotNull Map<String, Integer> categoryCounts(boolean includeHidden) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Head head : searchableHeads(includeHidden)) {
            counts.merge(head.category(), 1, Integer::sum);
        }

        return Map.copyOf(counts);
    }

    public boolean hidden(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isRemote()) {
            return false;
        }

        return overrideStore.find(id).map(RemoteHeadOverride::hidden).map(Boolean.TRUE::equals).orElse(false);
    }

    public @NotNull Optional<HeadCategory> category(@NotNull String id) {
        String normalized = normalizeId(id);
        return categories().stream().filter(category -> category.id().equals(normalized)).findFirst();
    }

    public @NotNull Optional<HeadTag> tag(@NotNull String id) {
        String normalized = normalizeId(id);
        return tags().stream().filter(tag -> tag.id().equals(normalized)).findFirst();
    }

    public @NotNull Optional<HeadCollection> collection(@NotNull String id) {
        String normalized = normalizeId(id);
        return collections().stream().filter(collection -> collection.id().equals(normalized)).findFirst();
    }

    public @NotNull List<HeadCategory> categories() {
        Map<String, HeadCategory> remoteCategories = new LinkedHashMap<>();
        for (HeadCategory category : remoteDatabase.categories()) {
            remoteCategories.put(category.id(), category);
        }

        Map<String, HeadCategory> localCategories = new LinkedHashMap<>();
        for (Head head : customHeadStore.list()) {
            localCategories.putIfAbsent(head.category(), new HeadCategory(head.category(), displayName(head.category()), "Local custom category."));
        }
        for (RemoteHeadOverride override : overrideStore.list()) {
            if (override.category() == null) {
                continue;
            }

            localCategories.putIfAbsent(override.category(), new HeadCategory(override.category(), displayName(override.category()), "Local override category."));
        }

        List<HeadCategory> categories = new ArrayList<>();
        categories.addAll(remoteCategories.values().stream().sorted(Comparator.comparing(HeadCategory::id)).toList());
        categories.addAll(localCategories.values().stream().filter(category -> !remoteCategories.containsKey(category.id())).sorted(Comparator.comparing(HeadCategory::id)).toList());
        return List.copyOf(categories);
    }

    public @NotNull List<HeadTag> tags() {
        Map<String, HeadTag> tags = new LinkedHashMap<>();
        for (HeadTag tag : remoteDatabase.tags()) {
            tags.put(tag.id(), tag);
        }
        for (Head head : customHeadStore.list()) {
            for (String tag : head.tags()) {
                tags.putIfAbsent(tag, new HeadTag(tag, displayName(tag), "Local tag."));
            }
        }
        for (RemoteHeadOverride override : overrideStore.list()) {
            for (String tag : override.addTags()) {
                tags.putIfAbsent(tag, new HeadTag(tag, displayName(tag), "Local override tag."));
            }
            if (override.replaceTags() != null) {
                for (String tag : override.replaceTags()) {
                    tags.putIfAbsent(tag, new HeadTag(tag, displayName(tag), "Local override tag."));
                }
            }
        }
        for (var tag : customTagService.list()) {
            tags.put(tag.id(), new HeadTag(tag.id(), tag.name(), tag.description()));
        }
        return tags.values().stream().sorted(Comparator.comparing(HeadTag::id)).toList();
    }

    public @NotNull List<HeadCollection> collections() {
        Map<String, HeadCollection> collections = new LinkedHashMap<>();
        for (HeadCollection collection : remoteDatabase.collections()) {
            collections.put(collection.id(), collection);
        }
        for (Head head : customHeadStore.list()) {
            for (String collection : head.collections()) {
                collections.putIfAbsent(collection, new HeadCollection(collection, displayName(collection), "Local collection."));
            }
        }
        for (var collection : customCollectionService.list()) {
            collections.put(collection.id(), new HeadCollection(collection.id(), collection.name(), collection.description()));
        }
        return collections.values().stream().sorted(Comparator.comparing(HeadCollection::id)).toList();
    }

    public @NotNull Optional<List<String>> lore(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");

        if (id.isRemote()) {
            return overrideStore.find(id).map(RemoteHeadOverride::lore).filter(Objects::nonNull);
        }

        if (id.isCustom()) {
            return customHeadStore.findStored(id).map(StoredCustomHead::lore);
        }

        return Optional.empty();
    }

    public @NotNull RemoteHeadOverrideStore overrides() {
        return overrideStore;
    }

    public @NotNull CustomHeadStore customHeads() {
        return customHeadStore;
    }

    public @NotNull PlayerHeadService playerHeads() {
        return playerHeadService;
    }

    public void onLocalMutation() {
        // Hook kept for future in-memory index invalidation.
    }

    private @NotNull List<Head> searchableHeads(boolean includeHidden) {
        Map<HeadId, RemoteHeadOverride> overrides = remoteOverridesById();
        List<Head> heads = new ArrayList<>();

        for (Head remote : remoteDatabase.heads()) {
            RemoteHeadOverride override = overrides.get(remote.id());
            if (!includeHidden && override != null && Boolean.TRUE.equals(override.hidden())) {
                continue;
            }

            heads.add(override == null ? remote : merger.merge(remote, override));
        }

        heads.addAll(customHeadStore.list());
        return heads;
    }

    private @NotNull Map<HeadId, RemoteHeadOverride> remoteOverridesById() {
        Map<HeadId, RemoteHeadOverride> overrides = new LinkedHashMap<>();
        for (RemoteHeadOverride override : overrideStore.list()) {
            overrides.put(override.headId(), override);
        }
        return overrides;
    }

    private @NotNull Head effectiveRemoteHead(@NotNull Head head) {
        return overrideStore.find(head.id()).map(override -> merger.merge(head, override)).orElse(head);
    }

    private @NotNull Stream<Head> filter(@NotNull List<Head> heads, @NotNull HeadQuery query) {
        Stream<Head> stream = heads.stream();

        if (query.source() != null) {
            HeadSource source = query.source();
            stream = stream.filter(head -> head.id().source() == source);
        } else {
            stream = stream.filter(head -> head.id().source() != HeadSource.PLAYER);
        }

        if (!query.ids().isEmpty()) {
            Set<HeadId> ids = query.ids();
            stream = stream.filter(head -> ids.contains(head.id()));
        }

        if (!query.categories().isEmpty()) {
            Set<String> categories = query.categories();
            stream = stream.filter(head -> categories.contains(head.category()));
        }

        if (!query.tags().isEmpty()) {
            stream = stream.filter(head -> head.tags().containsAll(query.tags()));
        }

        if (!query.collections().isEmpty()) {
            stream = stream.filter(head -> head.collections().containsAll(query.collections()));
        }

        if (!query.text().isBlank()) {
            String text = query.text().toLowerCase(Locale.ROOT);
            stream = stream.filter(head -> matchesText(head, text));
        }

        return stream;
    }

    private boolean matchesText(@NotNull Head head, @NotNull String text) {
        if (head.name().toLowerCase(Locale.ROOT).contains(text)) {
            return true;
        }
        if (head.id().value().contains(text)) {
            return true;
        }
        if (head.texture().hash().contains(text)) {
            return true;
        }
        if (head.category().contains(text)) {
            return true;
        }
        for (String tag : head.tags()) {
            if (tag.contains(text)) {
                return true;
            }
        }
        for (String collection : head.collections()) {
            if (collection.contains(text)) {
                return true;
            }
        }
        return false;
    }

    private @NotNull Comparator<Head> comparator(@NotNull HeadQuery query) {
        if (query.sort() == HeadSort.RELEVANCE) {
            return relevanceComparator(query);
        }

        Comparator<Head> comparator = switch (query.sort()) {
            case ID -> idComparator();
            case NAME -> Comparator.comparing(head -> head.name().toLowerCase(Locale.ROOT));
            case CATEGORY -> Comparator.comparing(Head::category).thenComparing(idComparator());
            default -> throw new IllegalStateException("Unsupported head sort: " + query.sort());
        };

        if (query.direction() == SortDirection.DESCENDING) {
            return comparator.reversed();
        }

        return comparator;
    }

    private @NotNull Comparator<Head> relevanceComparator(@NotNull HeadQuery query) {
        if (query.text().isBlank()) {
            return idComparator();
        }
        String text = query.text().toLowerCase(Locale.ROOT);
        return Comparator.comparingInt((Head head) -> relevanceScore(head, text)).reversed().thenComparing(idComparator());
    }

    private int relevanceScore(@NotNull Head head, @NotNull String text) {
        String name = head.name().toLowerCase(Locale.ROOT);
        if (name.equals(text)) return 100;
        if (name.startsWith(text)) return 75;
        if (name.contains(text)) return 50;
        if (head.category().equals(text)) return 40;
        if (head.tags().contains(text)) return 35;
        if (head.collections().contains(text)) return 30;
        if (head.id().value().contains(text)) return 20;
        if (head.texture().hash().contains(text)) return 10;
        return 0;
    }

    private @NotNull Comparator<Head> idComparator() {
        return Comparator.comparing((Head head) -> head.id().source().ordinal()).thenComparing(head -> comparableIdKey(head.id()));
    }

    private @NotNull ComparableIdKey comparableIdKey(@NotNull HeadId id) {
        if (id.source() == HeadSource.REMOTE && isNumeric(id.key())) {
            return new ComparableIdKey(Long.parseLong(id.key()), "");
        }
        return new ComparableIdKey(Long.MAX_VALUE, id.key());
    }

    private boolean isNumeric(@NotNull String value) {
        if (value.isEmpty()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) return false;
        }
        return true;
    }

    private static @NotNull String normalizeId(@NotNull String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be empty.");
        }
        return normalized;
    }

    private static @NotNull String displayName(@NotNull String id) {
        String[] parts = id.replace('_', '-').split("-");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return words.isEmpty() ? id : String.join(" ", words);
    }

    private record ComparableIdKey(long number, @NotNull String text) implements Comparable<ComparableIdKey> {
        private ComparableIdKey {
            Objects.requireNonNull(text, "text");
        }

        @Override
        public int compareTo(@NotNull ComparableIdKey other) {
            int numberCompare = Long.compare(number, other.number);
            if (numberCompare != 0) return numberCompare;
            return text.compareTo(other.text);
        }
    }
}