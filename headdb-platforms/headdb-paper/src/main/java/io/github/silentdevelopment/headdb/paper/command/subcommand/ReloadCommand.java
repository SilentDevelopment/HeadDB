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

public final class ReloadCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public ReloadCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        plugin.messages().send(context.sender(), plugin.messages().reloadStarted(context.sender()));

        try {
            plugin.reload();
            plugin.startUpdater();
        } catch (Exception exception) {
            plugin.getSLF4JLogger().error("Failed to reload HeadDB.", exception);
            plugin.messages().send(context.sender(), plugin.messages().reloadFailed(context.sender()));
            return;
        }

        plugin.messages().send(context.sender(), plugin.messages().reloadSuccess(context.sender()));
        if (context.isPlayer()) {
            plugin.sounds().play(context.player(), SoundKey.RELOAD);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("reload")
                .aliases("rl", "rel")
                .description("Reloads configuration and runtime.")
                .requirement(CommandRequirements.permission(Permissions.RELOAD))
                .noArgs()
                .build();
    }
}