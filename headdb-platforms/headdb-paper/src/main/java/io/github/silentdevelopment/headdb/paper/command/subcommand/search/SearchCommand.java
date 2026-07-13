package io.github.silentdevelopment.headdb.paper.command.subcommand.search;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.format.SearchFormatter;
import io.github.silentdevelopment.headdb.paper.command.search.SearchOptions;
import io.github.silentdevelopment.headdb.paper.search.SearchQueries;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.CommandDefinition;
import io.github.silentdevelopment.relay.core.command.builder.CommandBuilder;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommandGroup;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class SearchCommand extends AbstractPaperCommandGroup {

    private static final Argument<String> QUERY = Argument.greedyOptional("query", PaperArgumentTypes.GREEDY_STRING);

    private final HeadDBPlugin plugin;
    private final List<CommandDefinition<CommandSender>> children;

    public SearchCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.children = List.of(new SearchTextCommand(plugin), new SearchTagCommand(plugin), new SearchCategoryCommand(plugin), new SearchCollectionCommand(plugin), new SearchHeadCommand(plugin));
    }

    @Override
    public @NotNull List<CommandDefinition<CommandSender>> children() {
        return children;
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!Permissions.has(context.sender(), Permissions.SEARCH)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        SearchRequest request;

        try {
            String query = context.has(QUERY) ? context.get(QUERY).trim() : "";
            request = SearchOptions.advancedRequest(context, query);
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception.getMessage()));
            return;
        }

        if (!canSearch(context, request)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        if (request.isEmpty()) {
            plugin.messages().send(context.sender(), plugin.messages().searchUsage(context.sender()));
            return;
        }

        execute(context, request);
    }

    @Override
    protected @NotNull CommandBuilder<CommandSender> buildCommand() {
        return CommandBuilder.<CommandSender>literal("search")
                .alias("s")
                .description("Searches the database.")
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
                .suggest(QUERY, Suggestions.searchQuery(plugin))
                .suggest(SearchOptions.CATEGORY_VALUE, Suggestions.categories(plugin))
                .suggest(SearchOptions.IDS_VALUE, Suggestions.headIdsCsv(plugin))
                .suggest(SearchOptions.TAGS_VALUE, Suggestions.tagsCsv(plugin))
                .suggest(SearchOptions.COLLECTIONS_VALUE, Suggestions.collectionsCsv(plugin))
                .suggest(SearchOptions.SORT_VALUE, Suggestions.sorts())
                .suggest(SearchOptions.DIRECTION_VALUE, Suggestions.directions())
                .suggest(SearchOptions.PAGE_VALUE, Suggestions.pages())
                .suggest(SearchOptions.LIMIT_VALUE, Suggestions.limits());
    }

    private void execute(@NotNull PaperCommandContext context, @NotNull SearchRequest request) {
        if (context.isPlayer()) {
            plugin.guis().openSearch(context.player(), request);
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

    private boolean canSearch(@NotNull PaperCommandContext context, @NotNull SearchRequest request) {
        if (request.category() != null) {
            return Permissions.canViewCategory(context.sender(), request.category());
        }

        return Permissions.canViewAllCategories(context.sender());
    }

    private @NotNull HeadQueryResult search(@NotNull SearchRequest request) {
        return plugin.headRegistry().search(SearchQueries.query(request));
    }
}