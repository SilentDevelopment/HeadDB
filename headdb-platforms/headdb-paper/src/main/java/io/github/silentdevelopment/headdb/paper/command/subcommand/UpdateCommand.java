package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UpdateCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public UpdateCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        boolean accepted = plugin.updater().checkAndInstallAsync(context.sender());

        if (!accepted) {
            return;
        }

        context.reply(Component.text("Checking for updates...", NamedTextColor.GRAY));
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("update")
                .description("Checks for updates and installs the latest available version.")
                .requirement(CommandRequirements.permission(Permissions.UPDATE))
                .noArgs()
                .build();
    }

}
