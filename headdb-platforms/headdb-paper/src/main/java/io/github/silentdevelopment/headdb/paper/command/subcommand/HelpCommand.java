package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.format.HelpFormatter;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class HelpCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public HelpCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!Permissions.has(context.sender(), Permissions.HELP)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        for (Component line : HelpFormatter.format(context.sender())) {
            plugin.messages().send(context.sender(), line);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("help")
                .alias("h")
                .description("Shows command help.")
                .requirement(CommandRequirements.permission(Permissions.HELP))
                .noArgs()
                .build();
    }
}