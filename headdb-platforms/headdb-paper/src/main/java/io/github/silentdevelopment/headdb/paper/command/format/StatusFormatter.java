package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StatusFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private StatusFormatter() {
    }

    public static @NotNull List<Component> format(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats remoteStats = plugin.runtime().database().stats();
        RefreshState refresh = plugin.runtime().refreshState();

        int hiddenHeads = plugin.headRegistry().hiddenHeads().size();
        int moreHeads = plugin.headRegistry().customHeads().list().size();
        int overrides = plugin.headRegistry().overrides().list().size();
        int playerHeads = plugin.headRegistry().playerHeads().knownPlayers().size();
        int moreCategories = plugin.customCategories().list().size();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text("Status", NamedTextColor.RED)));
        lines.add(databaseLine(status));
        lines.add(line("Heads", remoteStats.heads()));
        lines.add(line("Hidden Heads", hiddenHeads));
        lines.add(line("More Heads", moreHeads));
        lines.add(line("Player Heads", playerHeads));
        lines.add(line("Categories", remoteStats.categories()));
        lines.add(line("More Categories", moreCategories));
        lines.add(line("Tags", remoteStats.tags()));
        lines.add(line("Collections", remoteStats.collections()));
        lines.add(line("Revocations", remoteStats.revocations()));
        lines.add(line("Overrides", overrides));
        lines.add(refreshLine(refresh, sender));
        lines.add(lastRefreshLine(refresh));

        String failure = firstPresent(status.lastError(), refresh.lastFailureMessage());
        if (failure != null) {
            lines.add(line("Last error", failure));
        }

        addSupportLine(lines, sender);
        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    private static @NotNull Component databaseLine(@NotNull DatabaseStatus status) {
        Component line = Component.text("Database: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(status.state()), statusColor(status)));
        String source = value(status.source());

        if (!source.equals("none")) {
            line = line.append(Component.text(" from ", NamedTextColor.GRAY)).append(Component.text(source, NamedTextColor.GOLD));
        }

        return line;
    }

    private static @NotNull Component refreshLine(@NotNull RefreshState refresh, @NotNull CommandSender sender) {
        String text = refresh.running() ? "running " + refresh.currentOperation() : "idle";
        Component line = line("Refresh", text);

        if (!refresh.running() && Permissions.has(sender, Permissions.REFRESH)) {
            line = line.append(Component.text("  ")).append(refreshButton());
        }

        return line;
    }

    private static @NotNull Component lastRefreshLine(@NotNull RefreshState refresh) {
        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.SUCCESS) {
            return line("Last Refresh", refresh.lastOperation() + " completed at " + formatInstant(refresh.lastSuccessfulRefresh()));
        }

        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.FAILURE) {
            return line("Last Refresh", refresh.lastOperation() + " failed at " + formatInstant(refresh.lastFailedRefresh()));
        }

        return line("Last Refresh", "never");
    }

    private static @NotNull Component refreshButton() {
        return Component.text("[ ", NamedTextColor.DARK_GRAY).append(Component.text("REFRESH", NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/hdb refresh")).hoverEvent(HoverEvent.showText(Component.text("Click to refresh the database.", NamedTextColor.GRAY)))).append(Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private static void addSupportLine(@NotNull List<Component> lines, @NotNull CommandSender sender) {
        boolean canDebug = Permissions.has(sender, Permissions.DEBUG);
        boolean canReport = Permissions.has(sender, Permissions.REPORT);

        if (!canDebug && !canReport) {
            return;
        }

        Component line = Component.text("Support: ", NamedTextColor.GRAY);

        if (canDebug) {
            line = line.append(Component.text("/hdb debug", NamedTextColor.GOLD));
        }

        if (canDebug && canReport) {
            line = line.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        }

        if (canReport) {
            line = line.append(Component.text("/hdb report", NamedTextColor.GOLD));
        }

        lines.add(line);
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

    private static @NotNull Component line(@NotNull String key, @Nullable Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
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

    private static @Nullable String firstPresent(@Nullable String first, @Nullable String second) {
        String normalizedFirst = normalize(first);

        if (normalizedFirst != null) {
            return normalizedFirst;
        }

        return normalize(second);
    }

    private static @Nullable String normalize(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

}
