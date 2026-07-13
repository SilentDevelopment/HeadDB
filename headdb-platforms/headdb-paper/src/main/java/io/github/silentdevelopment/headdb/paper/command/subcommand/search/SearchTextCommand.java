package io.github.silentdevelopment.headdb.paper.command.subcommand.search;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.format.SearchFormatter;
import io.github.silentdevelopment.headdb.paper.command.search.SearchOptions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.query.HeadQuery;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SearchTextCommand extends AbstractPaperCommand {

    private static final Argument<String> QUERY = Argument.greedyRequired("query", PaperArgumentTypes.GREEDY_STRING);

    private final HeadDBPlugin plugin;

    public SearchTextCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        try {
            SearchRequest request = SearchOptions.advancedRequest(context, context.get(QUERY).trim());
            execute(context, request);
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("text")
                .description("Searches heads by text.")
                .requirement(CommandRequirements.permission(Permissions.SEARCH))
                .signature(QUERY)

                .option(SearchOptions.CATEGORY)
                .option(SearchOptions.IDS)
                .option(SearchOptions.TAGS)
                .option(SearchOptions.COLLECTIONS)
                .option(SearchOptions.SORT)
                .option(SearchOptions.DIRECTION)
                .option(SearchOptions.PAGE)
                .option(SearchOptions.LIMIT)

                .suggest(SearchOptions.CATEGORY_VALUE, Suggestions.categories(plugin))
                .suggest(SearchOptions.IDS_VALUE, Suggestions.headIdsCsv(plugin))
                .suggest(SearchOptions.TAGS_VALUE, Suggestions.tagsCsv(plugin))
                .suggest(SearchOptions.COLLECTIONS_VALUE, Suggestions.collectionsCsv(plugin))
                .suggest(SearchOptions.SORT_VALUE, Suggestions.sorts())
                .suggest(SearchOptions.DIRECTION_VALUE, Suggestions.directions())
                .suggest(SearchOptions.PAGE_VALUE, Suggestions.pages())
                .suggest(SearchOptions.LIMIT_VALUE, Suggestions.limits())

                .build();
    }

    private void execute(@NotNull PaperCommandContext context, @NotNull SearchRequest request) {
        if (context.isPlayer()) {
            plugin.messages().send(context.sender(), plugin.messages().searchGuiNotReady(context.sender()));
            return;
        }

        HeadQueryResult result = search(request);

        if (result.total() > 0 && request.page() > result.totalPages()) {
            request = request.withPage(result.totalPages());
            result = search(request);
        }

        for (Component line : SearchFormatter.format(context.sender(), request, result)) {
            plugin.messages().send(context.sender(), line);
        }
    }

    private @NotNull HeadQueryResult search(@NotNull SearchRequest request) {
        HeadQuery.Builder builder = HeadQuery.builder()
                .text(request.query())
                .ids(request.ids())
                .tags(request.tags())
                .collections(request.collections())
                .page(request.page(), request.limit())
                .sort(request.sort())
                .direction(request.direction());

        if (request.category() != null) {
            builder.category(request.category());
        }

        return plugin.headRegistry().search(builder.build());
    }
}