package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.HeadCategory;
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
import java.util.List;
import java.util.Objects;

public final class CategoriesCommand extends AbstractPaperCommand {

    private static final int PAGE_SIZE = 12;
    private static final Argument<String> PAGE = Argument.optional("page", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public CategoriesCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        int page = page(context);
        List<ListFormatter.Entry> entries = plugin.headRegistry().categories().stream()
                .filter(category -> Permissions.canViewCategory(context.sender(), category.id()))
                .sorted(Comparator.comparing(HeadCategory::id))
                .map(category -> new ListFormatter.Entry(category.id(), category.name()))
                .toList();

        for (var line : ListFormatter.format("Head Categories", entries, page, PAGE_SIZE)) {
            plugin.messages().send(context.sender(), line);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("categories")
                .alias("cat")
                .description("Lists head categories.")
                .requirement(CommandRequirements.permission(Permissions.SEARCH))
                .signature(PAGE)
                .suggest(PAGE, Suggestions.pages())
                .noArgs()
                .build();
    }

    private int page(@NotNull PaperCommandContext context) {
        if (!context.has(PAGE)) {
            return 1;
        }

        try {
            return Math.max(1, Integer.parseInt(context.get(PAGE).trim()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}