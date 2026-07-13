package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.format.SupportReport;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ReportCommand extends AbstractPaperCommand {

    private final HeadDBPlugin plugin;

    public ReportCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        String report = SupportReport.create(plugin, context.sender());

        if (context.sender() instanceof Player) {
            sendPlayerReport(context, report);
            return;
        }

        sendConsoleReport(context, report);
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("report").alias("rpt").description("Creates a full support report.").requirement(CommandRequirements.permission(Permissions.REPORT)).noArgs().build();
    }

    private void sendPlayerReport(@NotNull PaperCommandContext context, @NotNull String report) {
        Component copy = Component.text("HERE", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.copyToClipboard(report)).hoverEvent(HoverEvent.showText(Component.text("Copy the full support report.", NamedTextColor.GRAY)));

        plugin.messages().send(context.sender(), Component.empty());
        plugin.messages().send(context.sender(), Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text("Report", NamedTextColor.RED)));
        plugin.messages().send(context.sender(), Component.text("Click ", NamedTextColor.GRAY).append(copy).append(Component.text(" to copy the full support report.", NamedTextColor.GRAY)));
        plugin.messages().send(context.sender(), Component.text("Paste this report when asking for support.", NamedTextColor.GRAY));
        plugin.messages().send(context.sender(), Component.empty());
    }

    private void sendConsoleReport(@NotNull PaperCommandContext context, @NotNull String report) {
        plugin.messages().send(context.sender(), Component.empty());

        for (String line : report.split("\\R", -1)) {
            if (line.isBlank()) {
                plugin.messages().send(context.sender(), Component.empty());
                continue;
            }

            plugin.messages().send(context.sender(), Component.text(line, NamedTextColor.GRAY));
        }

        plugin.messages().send(context.sender(), Component.empty());
    }

}
