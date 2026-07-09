package io.github.silentdevelopment.headdb.paper.local.custom;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record StoredCustomHead(
        @NotNull String id,
        @NotNull String name,
        @NotNull String textureHash,
        @Nullable String textureSignature,
        @NotNull List<String> lore,
        @NotNull Set<String> tags,
        @NotNull Set<String> collections,
        @NotNull String category,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt,
        @Nullable UUID createdBy,
        boolean draft
) {

    public StoredCustomHead(@NotNull String id, @NotNull String name, @NotNull String textureHash, @Nullable String textureSignature, @NotNull List<String> lore, @NotNull Set<String> tags, @NotNull Set<String> collections, @NotNull String category, @NotNull Instant createdAt, @NotNull Instant updatedAt, @Nullable UUID createdBy) {
        this(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, updatedAt, createdBy, false);
    }

    public StoredCustomHead {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(textureHash, "textureHash");
        if (lore == null) {
            lore = List.of();
        }
        if (tags == null) {
            tags = Set.of();
        }
        if (collections == null) {
            collections = Set.of();
        }
        if (category == null || category.isBlank()) {
            category = "custom";
        }
        if (createdAt == null) {
            createdAt = Instant.EPOCH;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }

        id = normalizeSlug(id);
        name = requireText(name, "Custom head name");
        textureHash = requireTextureHash(textureHash);
        textureSignature = textureSignature == null || textureSignature.isBlank() ? null : textureSignature.trim();
        lore = List.copyOf(lore);
        tags = normalizeSet(tags);
        collections = normalizeSet(collections);
        category = normalizeSlug(category);
    }

    public @NotNull HeadId headId() {
        return HeadId.custom(id);
    }

    public @NotNull Head toHead() {
        return new Head(headId(), draft ? "[DRAFT] " + name : name, new HeadTexture(textureHash), category, tags, collections);
    }

    public @NotNull StoredCustomHead withName(@NotNull String name) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }


    public @NotNull StoredCustomHead withDraft(boolean draft) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }

    public @NotNull StoredCustomHead withCategory(@NotNull String category) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }

    public @NotNull StoredCustomHead withTags(@NotNull Set<String> tags) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }

    public @NotNull StoredCustomHead withCollections(@NotNull Set<String> collections) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }

    public @NotNull StoredCustomHead withLore(@NotNull List<String> lore) {
        return new StoredCustomHead(id, name, textureHash, textureSignature, lore, tags, collections, category, createdAt, Instant.now(), createdBy, draft);
    }

    private static @NotNull Set<String> normalizeSet(@NotNull Set<String> values) {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeSlug(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return Set.copyOf(result);
    }

    public static @NotNull String normalizeSlug(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("ID cannot be blank.");
        }

        for (int index = 0; index < normalized.length(); index++) {
            char c = normalized.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.') {
                continue;
            }
            throw new IllegalArgumentException("ID contains an invalid character: " + c);
        }

        return normalized;
    }

    private static @NotNull String requireText(@NotNull String value, @NotNull String name) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty.");
        }
        return normalized;
    }

    private static @NotNull String requireTextureHash(@NotNull String value) {
        String hash = requireText(value, "Texture hash").toLowerCase(java.util.Locale.ROOT);
        for (int index = 0; index < hash.length(); index++) {
            char c = hash.charAt(index);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                continue;
            }
            throw new IllegalArgumentException("Texture hash must be hexadecimal.");
        }
        return hash;
    }
}
