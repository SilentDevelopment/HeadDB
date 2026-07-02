package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ItemCacheCommand extends AbstractPaperCommand {

    private static final Argument<String> ACTION = Argument.optional("action", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;

    public ItemCacheCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!context.has(ACTION)) {
            context.reply(plugin.messages().itemCacheUsage(context.sender()));
            return;
        }

        String action = context.get(ACTION).trim();

        if (!action.equalsIgnoreCase("clear")) {
            context.reply(plugin.messages().itemCacheUsage(context.sender()));
            return;
        }

        int before = plugin.itemCacheSize();
        plugin.clearItemCache();
        context.reply(plugin.messages().itemCacheCleared(context.sender(), before));
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("itemcache")
                .alias("ic")
                .description("Manages the generated item cache.")
                .requirement(CommandRequirements.permission(Permissions.ITEM_CACHE))
                .signature(ACTION)
                .noArgs()
                .build();
    }
}