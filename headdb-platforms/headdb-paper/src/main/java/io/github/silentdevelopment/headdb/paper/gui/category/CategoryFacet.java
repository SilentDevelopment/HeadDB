package io.github.silentdevelopment.headdb.paper.gui.category;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public record CategoryFacet(
        @NotNull String categoryId,
        @NotNull String title,
        @NotNull String tagPrefix,
        @NotNull String tagNamePrefix,
        @NotNull String tagNameSuffix
) {

    public CategoryFacet {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(tagPrefix, "tagPrefix");
        Objects.requireNonNull(tagNamePrefix, "tagNamePrefix");
        Objects.requireNonNull(tagNameSuffix, "tagNameSuffix");

        categoryId = normalize(categoryId);
        title = title.trim();
        tagPrefix = normalize(tagPrefix);
        tagNamePrefix = tagNamePrefix.trim();
        tagNameSuffix = tagNameSuffix.trim();

        if (categoryId.isBlank()) {
            throw new IllegalArgumentException("Facet category ID cannot be blank.");
        }

        if (title.isBlank()) {
            throw new IllegalArgumentException("Facet title cannot be blank.");
        }

        if (tagPrefix.isBlank()) {
            throw new IllegalArgumentException("Facet tag prefix cannot be blank.");
        }
    }

    public boolean matchesTag(@NotNull String tagId) {
        Objects.requireNonNull(tagId, "tagId");
        return normalize(tagId).startsWith(tagPrefix);
    }

    public @NotNull String displayName(@NotNull String tagId, @NotNull String tagName) {
        Objects.requireNonNull(tagId, "tagId");
        Objects.requireNonNull(tagName, "tagName");

        String normalizedId = normalize(tagId);
        String name = tagName.trim();

        if (!matchesTag(normalizedId)) {
            return name.isBlank() ? normalizedId : name;
        }

        if (!tagNamePrefix.isBlank() && !tagNameSuffix.isBlank()
                && name.startsWith(tagNamePrefix)
                && name.endsWith(tagNameSuffix)
                && name.length() > tagNamePrefix.length() + tagNameSuffix.length()) {
            return name.substring(tagNamePrefix.length(), name.length() - tagNameSuffix.length()).trim();
        }

        String suffix = normalizedId.substring(tagPrefix.length());
        return humanize(suffix);
    }

    private static @NotNull String normalize(@NotNull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String humanize(@NotNull String value) {
        String[] parts = value.split("-");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }

        return result.isEmpty() ? value : result.toString();
    }
}
