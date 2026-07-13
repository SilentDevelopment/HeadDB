package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class OpenCommand extends AbstractPaperCommand {

    private static final int GUI_PAGE_SIZE = 28;
    private static final Argument<String> CATEGORY = Argument.optional("category", PaperArgumentTypes.STRING);
    private static final Argument<String> PLAYER = Argument.optional("player", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public OpenCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!context.has(CATEGORY)) {
            openMain(context);
            return;
        }

        String category = SearchParser.singleId(context.get(CATEGORY), "category");
        Optional<HeadCategory> optionalCategory = plugin.headRegistry().category(category);

        if (optionalCategory.isEmpty()) {
            plugin.messages().send(context.sender(), plugin.messages().unknownCategory(context.sender(), category));
            return;
        }

        Player target = resolveTarget(context);
        if (target == null) {
            return;
        }

        if (!Permissions.canOpenFor(context.sender(), target)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        if (!Permissions.has(target, Permissions.GUI_CATEGORY_OPEN) || !Permissions.canViewCategory(target, optionalCategory.get().id())) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        SearchRequest request = SearchRequest.lockedCategory("", Set.of(), optionalCategory.get().id(), Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, GUI_PAGE_SIZE);
        plugin.guis().openSearch(target, request);
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("open")
                .alias("o")
                .description("Opens the GUI or a category GUI.")
                .requirement(CommandRequirements.permission(Permissions.OPEN))
                .signature(CATEGORY, PLAYER)
                .suggest(CATEGORY, Suggestions.categories(plugin))
                .suggest(PLAYER, Suggestions.players())
                .noArgs()
                .build();
    }

    private void openMain(@NotNull PaperCommandContext context) {
        if (!context.isPlayer()) {
            plugin.messages().send(context.sender(), plugin.messages().openConsoleSelfUsage(context.sender()));
            return;
        }

        Player player = context.player();

        if (!Permissions.has(player, Permissions.OPEN) || !Permissions.has(player, Permissions.GUI_MAIN)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        plugin.guis().openMain(player);
    }

    private @Nullable Player resolveTarget(@NotNull PaperCommandContext context) {
        if (!context.has(PLAYER)) {
            if (context.isPlayer()) {
                return context.player();
            }

            plugin.messages().send(context.sender(), plugin.messages().openConsoleTargetUsage(context.sender()));
            return null;
        }

        String name = context.get(PLAYER).trim();
        if (name.isEmpty()) {
            plugin.messages().send(context.sender(), plugin.messages().targetEmpty(context.sender()));
            return null;
        }

        Player player = Bukkit.getPlayerExact(name);
        if (player != null) {
            return player;
        }

        plugin.messages().send(context.sender(), plugin.messages().playerNotOnline(context.sender(), name));
        return null;
    }
}