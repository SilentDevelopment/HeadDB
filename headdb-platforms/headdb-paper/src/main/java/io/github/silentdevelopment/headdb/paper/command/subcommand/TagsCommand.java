package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.format.ListFormatter;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public final class TagsCommand extends AbstractPaperCommand {

    private static final int PAGE_SIZE = 12;
    private static final Argument<String> QUERY = Argument.optional("query", PaperArgumentTypes.STRING);
    private static final Argument<String> PAGE = Argument.optional("page", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public TagsCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        ParsedListRequest request = request(context);

        var entries = plugin.headRegistry().tags().stream()
                .filter(tag -> matches(tag.id(), tag.name(), request.query()))
                .sorted(Comparator.comparing(HeadTag::id))
                .map(tag -> new ListFormatter.Entry(tag.id(), tag.name()))
                .toList();

        for (var line : ListFormatter.format("Head Tags", entries, request.page(), PAGE_SIZE)) {
            context.reply(line);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("tags")
                .alias("t")
                .description("Lists tags.")
                .requirement(CommandRequirements.permission(Permissions.SEARCH))
                .signature(QUERY, PAGE)
                .suggest(QUERY, Suggestions.tags(plugin))
                .suggest(PAGE, Suggestions.pages())
                .noArgs()
                .build();
    }

    private static boolean matches(@NotNull String id, @NotNull String name, @NotNull String query) {
        if (query.isBlank()) {
            return true;
        }

        String normalized = query.toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(normalized) || name.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private static @NotNull ParsedListRequest request(@NotNull PaperCommandContext context) {
        String query = "";
        int page = 1;

        if (!context.has(QUERY)) {
            return new ParsedListRequest(query, page);
        }

        String first = context.get(QUERY).trim();
        if (isInteger(first)) {
            return new ParsedListRequest("", parsePage(first));
        }

        query = first;

        if (context.has(PAGE)) {
            page = parsePage(context.get(PAGE));
        }

        return new ParsedListRequest(query, page);
    }

    private static int parsePage(@NotNull String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static boolean isInteger(@NotNull String raw) {
        try {
            Integer.parseInt(raw.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private record ParsedListRequest(@NotNull String query, int page) {
    }
}