package io.github.silentdevelopment.headdb.paper.search;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record SearchRequest(
        @NotNull String query,
        @NotNull Set<HeadId> ids,
        @NotNull Set<String> categories,
        @NotNull Set<String> tags,
        @NotNull Set<String> collections,
        @NotNull HeadSort sort,
        @NotNull SortDirection direction,
        int page,
        int limit,
        boolean categoryLocked
) {

    public static @NotNull SearchRequest singleCategory(@NotNull String query, @NotNull Set<HeadId> ids, @Nullable String category, @NotNull Set<String> tags, @NotNull Set<String> collections, @NotNull HeadSort sort, @NotNull SortDirection direction, int page, int limit) {
        return category(query, ids, category, tags, collections, sort, direction, page, limit, false);
    }

    public static @NotNull SearchRequest lockedCategory(@NotNull String query, @NotNull Set<HeadId> ids, @NotNull String category, @NotNull Set<String> tags, @NotNull Set<String> collections, @NotNull HeadSort sort, @NotNull SortDirection direction, int page, int limit) {
        Objects.requireNonNull(category, "category");
        return category(query, ids, category, tags, collections, sort, direction, page, limit, true);
    }

    private static @NotNull SearchRequest category(@NotNull String query, @NotNull Set<HeadId> ids, @Nullable String category, @NotNull Set<String> tags, @NotNull Set<String> collections, @NotNull HeadSort sort, @NotNull SortDirection direction, int page, int limit, boolean categoryLocked) {
        return new SearchRequest(query, ids, categorySet(category), tags, collections, sort, direction, page, limit, categoryLocked);
    }

    public SearchRequest {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(collections, "collections");
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(direction, "direction");

        query = query.trim();
        ids = Set.copyOf(ids);
        categories = normalizeSet(categories);
        tags = normalizeSet(tags);
        collections = normalizeSet(collections);

        if (categoryLocked && categories.size() != 1) {
            throw new IllegalArgumentException("Locked category search request must have exactly one category.");
        }

        if (page < 1) {
            throw new IllegalArgumentException("Search page must be at least 1.");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Search limit must be at least 1.");
        }
    }

    public boolean isEmpty() {
        return query.isBlank() && ids.isEmpty() && categories.isEmpty() && tags.isEmpty() && collections.isEmpty();
    }

    public boolean hasFilters() {
        if (!ids.isEmpty()) {
            return true;
        }

        if (!tags.isEmpty()) {
            return true;
        }

        if (!collections.isEmpty()) {
            return true;
        }

        return !categories.isEmpty() && !categoryLocked;
    }

    public boolean canChangeCategory() {
        return !categoryLocked;
    }

    public @Nullable String category() {
        return categories.stream().findFirst().orElse(null);
    }

    public @NotNull SearchRequest withPage(int page) {
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, page, limit, categoryLocked);
    }

    public @NotNull SearchRequest withQuery(@NotNull String query) {
        Objects.requireNonNull(query, "query");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest withIds(@NotNull Set<HeadId> ids) {
        Objects.requireNonNull(ids, "ids");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest withSort(@NotNull HeadSort sort) {
        Objects.requireNonNull(sort, "sort");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest withDirection(@NotNull SortDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest withCategoryFilter(@Nullable String category) {
        if (categoryLocked) {
            return this;
        }

        return withCategoryFilters(categorySet(category));
    }

    public @NotNull SearchRequest withCategoryFilters(@NotNull Set<String> categories) {
        Objects.requireNonNull(categories, "categories");

        if (categoryLocked) {
            return this;
        }

        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, false);
    }

    public @NotNull SearchRequest withLockedCategory(@NotNull String category) {
        Objects.requireNonNull(category, "category");
        return new SearchRequest(query, ids, categorySet(category), tags, collections, sort, direction, 1, limit, true);
    }

    public @NotNull SearchRequest withTags(@NotNull Set<String> tags) {
        Objects.requireNonNull(tags, "tags");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest withCollections(@NotNull Set<String> collections) {
        Objects.requireNonNull(collections, "collections");
        return new SearchRequest(query, ids, categories, tags, collections, sort, direction, 1, limit, categoryLocked);
    }

    public @NotNull SearchRequest toggleCategoryFilter(@NotNull String category) {
        Objects.requireNonNull(category, "category");

        if (categoryLocked) {
            return this;
        }

        Set<String> updated = new LinkedHashSet<>(categories);
        String normalized = normalize(category);

        if (normalized.isBlank()) {
            return this;
        }

        if (updated.contains(normalized)) {
            updated.remove(normalized);
        } else {
            updated.add(normalized);
        }

        return withCategoryFilters(updated);
    }

    public @NotNull SearchRequest toggleTag(@NotNull String tag) {
        Objects.requireNonNull(tag, "tag");

        Set<String> updated = new LinkedHashSet<>(tags);
        String normalized = normalize(tag);

        if (normalized.isBlank()) {
            return this;
        }

        if (updated.contains(normalized)) {
            updated.remove(normalized);
        } else {
            updated.add(normalized);
        }

        return withTags(updated);
    }

    public @NotNull SearchRequest toggleCollection(@NotNull String collection) {
        Objects.requireNonNull(collection, "collection");

        Set<String> updated = new LinkedHashSet<>(collections);
        String normalized = normalize(collection);

        if (normalized.isBlank()) {
            return this;
        }

        if (updated.contains(normalized)) {
            updated.remove(normalized);
        } else {
            updated.add(normalized);
        }

        return withCollections(updated);
    }

    public @NotNull SearchRequest withoutFilters() {
        Set<String> keptCategories = categoryLocked ? categories : Set.of();
        return new SearchRequest(query, Set.of(), keptCategories, Set.of(), Set.of(), sort, direction, 1, limit, categoryLocked);
    }

    private static @NotNull Set<String> categorySet(@Nullable String category) {
        if (category == null) {
            return Set.of();
        }

        String normalized = normalize(category);

        if (normalized.isBlank()) {
            return Set.of();
        }

        return Set.of(normalized);
    }

    private static @NotNull Set<String> normalizeSet(@NotNull Set<String> values) {
        Set<String> result = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String normalized = normalize(value);

            if (normalized.isBlank()) {
                continue;
            }

            result.add(normalized);
        }

        return Set.copyOf(result);
    }

    private static @NotNull String normalize(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}