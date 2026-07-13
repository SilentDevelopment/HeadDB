package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.format.VersionFormatter;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class VersionCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public VersionCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        for (Component line : VersionFormatter.command(plugin)) {
            plugin.messages().send(context.sender(), line);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("version")
                .alias("v")
                .description("Shows plugin version and build information.")
                .requirement(CommandRequirements.permission(Permissions.VERSION))
                .noArgs()
                .build();
    }
}
