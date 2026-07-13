package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class VerifyCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public VerifyCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!Permissions.has(context.sender(), Permissions.VERIFY)) {
            plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), MessageKey.COMMAND_ERROR_NO_PERMISSION));
            if (context.isPlayer()) {
                plugin.sounds().play(context.player(), SoundKey.NO_PERMISSION);
            }
            return;
        }

        CommandSender sender = context.sender();

        boolean accepted = plugin.runtime().verifyRemoteAsync(
                snapshot -> reply(sender, success(sender, snapshot)),
                throwable -> reply(sender, plugin.messages().verifyFailed(sender, failureMessage(throwable)))
        );

        if (!accepted) {
            plugin.messages().send(context.sender(), plugin.messages().verifyAlreadyRunning(context.sender()));
            return;
        }

        plugin.messages().send(context.sender(), plugin.messages().verifyStarted(context.sender()));
        if (context.isPlayer()) {
            plugin.sounds().play(context.player(), SoundKey.VERIFY);
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("verify")
                .description("Verifies the remote database without replacing the active database.")
                .requirement(CommandRequirements.permission(Permissions.VERIFY))
                .noArgs()
                .build();
    }

    private void reply(@NotNull CommandSender sender, @NotNull Component message) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> plugin.messages().send(sender, message));
    }

    private @NotNull Component success(@NotNull CommandSender sender, @NotNull DatabaseSnapshot snapshot) {
        DatabaseStats stats = snapshot.stats();

        return Component.empty()
                .append(plugin.messages().verifySuccess(sender))
                .append(Component.newline())
                .append(line("Manifest ID", snapshot.manifestId()))
                .append(Component.newline())
                .append(line("Catalog ID", snapshot.artifactId()))
                .append(Component.newline())
                .append(line("Heads", stats.heads()))
                .append(Component.newline())
                .append(line("Categories", stats.categories()))
                .append(Component.newline())
                .append(line("Tags", stats.tags()))
                .append(Component.newline())
                .append(line("Collections", stats.collections()))
                .append(Component.newline())
                .append(line("Revocations", stats.revocations()));
    }

    private static @NotNull String failureMessage(@NotNull Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return message;
    }

    private static @NotNull Component line(@NotNull String key, @NotNull Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }
}
