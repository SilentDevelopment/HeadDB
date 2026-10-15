package io.github.silentdevelopment.headdb.paper.gui.category;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class CategoryFacetRegistry {

    private static final Map<String, CategoryFacet> FACETS = Map.of(
            "alphabet", new CategoryFacet("alphabet", "Alphabet Styles", "font-", "Font (", ")")
    );

    private CategoryFacetRegistry() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Optional<CategoryFacet> find(@NotNull String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");
        return Optional.ofNullable(FACETS.get(categoryId.trim().toLowerCase(Locale.ROOT)));
    }
}
