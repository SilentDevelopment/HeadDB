package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.command.format.StatusFormatter;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class StatusCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public StatusCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        for (Component line : StatusFormatter.format(plugin, context.sender())) {
            plugin.messages().send(context.sender(), line);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("status")
                .alias("st")
                .description("Shows database status.")
                .requirement(CommandRequirements.permission(Permissions.STATUS))
                .noArgs()
                .build();
    }
}