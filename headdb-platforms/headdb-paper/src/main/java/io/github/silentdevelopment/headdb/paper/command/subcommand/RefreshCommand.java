package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RefreshCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public RefreshCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        boolean accepted = plugin.runtime().refreshAsync();

        if (!accepted) {
            context.reply(plugin.messages().refreshAlreadyRunning(context.sender()));
            return;
        }

        context.reply(plugin.messages().refreshStarted(context.sender()));
        if (context.isPlayer()) {
            plugin.sounds().play(context.player(), SoundKey.REFRESH);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("refresh")
                .alias("ref")
                .description("Refreshes the remote database.")
                .requirement(CommandRequirements.permission(Permissions.REFRESH))
                .noArgs()
                .build();
    }
}