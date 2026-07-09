package io.github.silentdevelopment.headdb.paper.command;

import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadEntry;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import io.github.silentdevelopment.relay.suggestion.SuggestionContext;
import io.github.silentdevelopment.relay.suggestion.SuggestionProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Suggestions {

    private static final int MAX_SUGGESTIONS = 50;
    private static final List<String> HEAD_ID_PREFIXES = List.of("remote:", "custom:", "player:");

    private Suggestions() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull SuggestionProvider<CommandSender> headIds(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> headIdSuggestions(context.currentToken());
    }

    public static @NotNull SuggestionProvider<CommandSender> headIdsCsv(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> csv(context.currentToken(), Suggestions::headIdSuggestions);
    }

    public static @NotNull SuggestionProvider<CommandSender> players() {
        return context -> playerSuggestions(context.currentToken());
    }


    public static @NotNull SuggestionProvider<CommandSender> customHeads(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> values(context.currentToken(), plugin.headRegistry().customHeads().listStored(), StoredCustomHead::id);
    }

    public static @NotNull SuggestionProvider<CommandSender> knownPlayers(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> values(context.currentToken(), plugin.headRegistry().playerHeads().knownPlayers(), PlayerHeadEntry::name);
    }

    public static @NotNull SuggestionProvider<CommandSender> playerOrHeadIds(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        return context -> {
            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(playerSuggestions(context.currentToken()));
            suggestions.addAll(headIdSuggestions(context.currentToken()));
            return distinctLimited(suggestions);
        };
    }

    public static @NotNull SuggestionProvider<CommandSender> categories(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> values(context, () -> plugin.headRegistry().categories(), HeadCategory::id);
    }

    public static @NotNull SuggestionProvider<CommandSender> tags(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> values(context, () -> plugin.headRegistry().tags(), HeadTag::id);
    }

    public static @NotNull SuggestionProvider<CommandSender> tagsCsv(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> csv(context.currentToken(), token -> values(token, plugin.headRegistry().tags(), HeadTag::id));
    }

    public static @NotNull SuggestionProvider<CommandSender> collections(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> values(context, () -> plugin.headRegistry().collections(), HeadCollection::id);
    }

    public static @NotNull SuggestionProvider<CommandSender> collectionsCsv(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> csv(context.currentToken(), token -> values(token, plugin.headRegistry().collections(), HeadCollection::id));
    }

    public static @NotNull SuggestionProvider<CommandSender> sorts() {
        return context -> enumValues(context.currentToken(), HeadSort.values());
    }

    public static @NotNull SuggestionProvider<CommandSender> directions() {
        return context -> enumValues(context.currentToken(), SortDirection.values());
    }

    public static @NotNull SuggestionProvider<CommandSender> taxonomyActions() {
        return context -> matching(context.currentToken(), List.of("create", "delete"));
    }

    public static @NotNull SuggestionProvider<CommandSender> pages() {
        return context -> matching(context.currentToken(), List.of("1", "2", "3", "4", "5", "10"));
    }

    public static @NotNull SuggestionProvider<CommandSender> limits() {
        return context -> matching(context.currentToken(), List.of("10", "25", "50"));
    }

    public static @NotNull SuggestionProvider<CommandSender> amounts() {
        return context -> matching(context.currentToken(), List.of("1", "2", "3", "4", "8", "16", "32", "64"));
    }

    public static @NotNull SuggestionProvider<CommandSender> searchQuery(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return context -> List.of();
    }

    private static @NotNull List<String> headIdSuggestions(@NotNull String rawToken) {
        String token = rawToken.trim();

        if (token.isBlank()) {
            return HEAD_ID_PREFIXES;
        }

        if (startsWithIgnoreCase("remote:", token) || startsWithIgnoreCase("custom:", token) || startsWithIgnoreCase("player:", token)) {
            return matching(token, HEAD_ID_PREFIXES);
        }

        return List.of();
    }

    private static @NotNull List<String> playerSuggestions(@NotNull String token) {
        List<String> suggestions = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            suggestions.add(player.getName());
        }

        return matching(token, suggestions);
    }

    private static <T> @NotNull List<String> values(
            @NotNull SuggestionContext<CommandSender> context,
            @NotNull Supplier<Collection<T>> values,
            @NotNull Function<T, String> mapper
    ) {
        return values(context.currentToken(), values.get(), mapper);
    }

    private static <T> @NotNull List<String> values(
            @NotNull String token,
            @NotNull Collection<T> values,
            @NotNull Function<T, String> mapper
    ) {
        List<String> suggestions = values.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList();

        return matching(token, suggestions);
    }

    private static @NotNull List<String> enumValues(@NotNull String token, @NotNull Enum<?>[] values) {
        List<String> suggestions = new ArrayList<>();

        for (Enum<?> value : values) {
            suggestions.add(value.name());
        }

        return matching(token, suggestions);
    }

    private static @NotNull List<String> csv(@NotNull String rawToken, @NotNull Function<String, List<String>> delegate) {
        int separator = rawToken.lastIndexOf(',');

        if (separator < 0) {
            return delegate.apply(rawToken);
        }

        String prefix = rawToken.substring(0, separator + 1);
        String current = rawToken.substring(separator + 1).trim();

        List<String> suggestions = new ArrayList<>();

        for (String suggestion : delegate.apply(current)) {
            suggestions.add(prefix + suggestion);
        }

        return suggestions;
    }

    private static @NotNull List<String> matching(@NotNull String rawToken, @NotNull Collection<String> values) {
        String token = rawToken.trim();
        List<String> suggestions = new ArrayList<>();

        for (String value : values) {
            if (!startsWithIgnoreCase(value, token)) {
                continue;
            }

            suggestions.add(value);

            if (suggestions.size() >= MAX_SUGGESTIONS) {
                break;
            }
        }

        return suggestions;
    }

    private static @NotNull List<String> distinctLimited(@NotNull Collection<String> values) {
        Set<String> unique = new LinkedHashSet<>(values);
        List<String> suggestions = new ArrayList<>();

        for (String value : unique) {
            suggestions.add(value);

            if (suggestions.size() >= MAX_SUGGESTIONS) {
                break;
            }
        }

        return suggestions;
    }

    private static boolean startsWithIgnoreCase(@NotNull String value, @NotNull String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}