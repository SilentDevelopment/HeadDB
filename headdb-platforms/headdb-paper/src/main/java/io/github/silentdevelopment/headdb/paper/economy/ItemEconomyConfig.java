package io.github.silentdevelopment.headdb.paper.economy;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public record ItemEconomyConfig(
        @NotNull String match,
        @NotNull String material,
        @NotNull String singular,
        @NotNull String plural
) {

    public ItemEconomyConfig {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(singular, "singular");
        Objects.requireNonNull(plural, "plural");

        match = match.trim().toLowerCase(Locale.ROOT);
        material = material.trim();
        singular = singular.trim();
        plural = plural.trim();
    }

    public @NotNull String singularOr(@NotNull String fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return singular.isBlank() ? fallback : singular;
    }

    public @NotNull String pluralOr(@NotNull String fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return plural.isBlank() ? fallback : plural;
    }
}
