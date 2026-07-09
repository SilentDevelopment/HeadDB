package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.runtime.PlatformRequirements;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.headdb.paper.updater.UpdateCheckResult;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class DebugCommand extends AbstractPaperCommand {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final HeadDBPlugin plugin;

    public DebugCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (context.isPlayer()) {
            plugin.sounds().play(context.player(), SoundKey.DEBUG);
        }

        BuildInfo buildInfo = BuildInfo.read(plugin);
        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats remoteStats = plugin.runtime().database().stats();
        RefreshState refresh = plugin.runtime().refreshState();
        PlatformRequirements.Compatibility compatibility = PlatformRequirements.inspect(plugin);
        UpdateCheckResult updateResult = plugin.updater().lastResult();

        context.reply(Component.empty());
        context.reply(Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text("Debug", NamedTextColor.RED)));
        context.reply(line("Version", buildInfo.version() + " (" + value(buildInfo.commit()) + ")"));
        context.reply(line("Runtime", plugin.getServer().getName() + " " + plugin.getServer().getMinecraftVersion() + " | Java " + compatibility.javaFeature()));
        context.reply(line("Supported", yesNo(compatibility.supported())));
        context.reply(databaseLine(status));
        context.reply(line("Heads", remoteStats.heads()));
        context.reply(line("Categories", remoteStats.categories()));
        context.reply(line("Tags", remoteStats.tags()));
        context.reply(line("Collections", remoteStats.collections()));
        context.reply(line("Revocations", remoteStats.revocations()));
        context.reply(line("Hidden Heads", plugin.headRegistry().hiddenHeads().size()));
        context.reply(line("Overrides", plugin.headRegistry().overrides().list().size()));
        context.reply(line("More Heads", plugin.headRegistry().customHeads().list().size()));
        context.reply(line("Player Heads", plugin.headRegistry().playerHeads().knownPlayers().size()));
        context.reply(line("More Categories", plugin.customCategories().list().size()));
        context.reply(line("Refresh", refreshText(refresh)));
        context.reply(line("Last Refresh", lastRefreshText(refresh)));
        context.reply(line("Updater", updateText(updateResult)));

        if (Permissions.has(context.sender(), Permissions.REPORT)) {
            context.reply(Component.text("Report: ", NamedTextColor.GRAY).append(Component.text("/hdb report", NamedTextColor.GOLD)));
        }

        context.reply(Component.empty());
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("debug").alias("d").description("Shows concise runtime diagnostics.").requirement(CommandRequirements.permission(Permissions.DEBUG)).noArgs().build();
    }

    private static @NotNull Component databaseLine(@NotNull DatabaseStatus status) {
        Component line = Component.text("Database: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(status.state()), statusColor(status)));
        String source = value(status.source());

        if (!source.equals("none")) {
            line = line.append(Component.text(" from ", NamedTextColor.GRAY)).append(Component.text(source, NamedTextColor.GOLD));
        }

        return line;
    }

    private static @NotNull String refreshText(@NotNull RefreshState refresh) {
        if (refresh.running()) {
            return "running " + refresh.currentOperation();
        }

        return "idle";
    }

    private static @NotNull String lastRefreshText(@NotNull RefreshState refresh) {
        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.SUCCESS) {
            return refresh.lastOperation() + " completed at " + formatInstant(refresh.lastSuccessfulRefresh());
        }

        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.FAILURE) {
            return refresh.lastOperation() + " failed at " + formatInstant(refresh.lastFailedRefresh());
        }

        return "never";
    }

    private static @NotNull String updateText(@Nullable UpdateCheckResult result) {
        if (result == null) {
            return "not checked";
        }

        if (result.failed()) {
            return "failed: " + value(result.failureMessage());
        }

        if (result.updateAvailable() && result.release() != null) {
            return "available " + result.release().version().raw();
        }

        if (result.updateAvailable()) {
            return "available";
        }

        return "current";
    }

    private static @NotNull Component line(@NotNull String key, @Nullable Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull NamedTextColor statusColor(@NotNull DatabaseStatus status) {
        String state = String.valueOf(status.state());

        if ("LOADED".equalsIgnoreCase(state)) {
            return NamedTextColor.GOLD;
        }

        if ("LOADING".equalsIgnoreCase(state)) {
            return NamedTextColor.YELLOW;
        }

        return NamedTextColor.RED;
    }

    private static @NotNull String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static @NotNull String formatInstant(@Nullable Instant instant) {
        if (instant == null) {
            return "never";
        }

        return TIME_FORMAT.format(instant);
    }

    private static @NotNull String value(@Nullable Object value) {
        if (value == null) {
            return "none";
        }

        String string = String.valueOf(value);
        if (string.isBlank()) {
            return "none";
        }

        return string;
    }

}
