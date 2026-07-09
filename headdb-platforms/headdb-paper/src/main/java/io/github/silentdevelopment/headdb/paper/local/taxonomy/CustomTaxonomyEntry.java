package io.github.silentdevelopment.headdb.paper.local.taxonomy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record CustomTaxonomyEntry(@NotNull String id, @NotNull String name, @NotNull String description, @NotNull Instant createdAt, @NotNull Instant updatedAt, @Nullable UUID createdBy) {

    public CustomTaxonomyEntry(@NotNull String id, @NotNull String name, @NotNull String description, @Nullable UUID createdBy) {
        this(id, name, description, Instant.now(), Instant.now(), createdBy);
    }

    public CustomTaxonomyEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");

        id = normalizeId(id);
        name = requireText(name, "Name");
        description = description.trim().isBlank() ? "Local custom entry." : description.trim();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public @NotNull CustomTaxonomyEntry withName(@NotNull String name) {
        return new CustomTaxonomyEntry(id, name, description, createdAt, Instant.now(), createdBy);
    }

    public @NotNull CustomTaxonomyEntry withDescription(@NotNull String description) {
        return new CustomTaxonomyEntry(id, name, description, createdAt, Instant.now(), createdBy);
    }

    public static @NotNull String normalizeId(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
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
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank.");
        }
        return normalized;
    }
}
