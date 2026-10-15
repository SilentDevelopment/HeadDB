package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.updater.UpdateCheckResult;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VersionFormatter {

    private static final String HEADDB_URL = "https://github.com/SilentDevelopment/HeadDB";
    private static final String SILENT_DEVELOPMENT_URL = "https://github.com/SilentDevelopment";
    private static final String THE_SILENT_PRO_URL = "https://github.com/TheSilentPro";

    private VersionFormatter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Component startup(
            @NotNull HeadDBPlugin plugin,
            @Nullable UpdateCheckResult updateResult
    ) {
        Objects.requireNonNull(plugin, "plugin");

        BuildInfo buildInfo = BuildInfo.read(plugin);
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(Component.empty());

        lines.add(Component.empty());
        lines.add(startupRunningLine(plugin));
        lines.add(Component.empty());
        lines.add(versionLine(buildInfo.version(), updateResult));

        if (plugin.config().isDebug()) {
            lines.add(field("Build", buildInfo.displayBuild()));
            lines.add(field("Branch", value(buildInfo.branch())));
            lines.add(field("Commit", value(buildInfo.commit())));
            lines.add(field("Timestamp", value(buildInfo.buildTime())));
        }

        lines.add(Component.empty());
        lines.add(Component.empty());

        return joinLines(lines);
    }

    public static @NotNull List<Component> command(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        PluginMeta meta = plugin.getPluginMeta();
        BuildInfo buildInfo = BuildInfo.read(plugin);
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(commandRunningLine(meta, buildInfo));
        lines.add(Component.empty());
        lines.add(helpLine());

        if (sender.hasPermission(Permissions.ADMIN)) {
            lines.add(Component.empty());
            lines.add(section("Build"));
            lines.add(indentedField("Version", buildInfo.version()));
            lines.add(indentedField("Build", buildInfo.displayBuild()));
            lines.add(indentedField("Channel", buildInfo.channel()));
            lines.add(indentedField("Run ID", valueOrUnavailable(buildInfo.runId())));
            lines.add(indentedField("Commit", valueOrUnavailable(buildInfo.commit())));
            lines.add(indentedField("Full commit", valueOrUnavailable(buildInfo.fullCommit())));
            lines.add(indentedField("Branch", valueOrUnavailable(buildInfo.branch())));
            lines.add(indentedField("Timestamp", valueOrUnavailable(buildInfo.buildTime())));
        }

        Component buttons = actionButtons(sender);
        if (!buttons.equals(Component.empty())) {
            lines.add(Component.empty());
            lines.add(buttons);
        }

        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    private static @NotNull Component startupRunningLine(@NotNull HeadDBPlugin plugin) {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        String authorText = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

        return Component.text("Running ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getPluginMeta().getName(), NamedTextColor.RED))
                .append(Component.text(" by ", NamedTextColor.GRAY))
                .append(Component.text(authorText, NamedTextColor.GOLD));
    }

    private static @NotNull Component commandRunningLine(@NotNull PluginMeta meta, @NotNull BuildInfo buildInfo) {
        return Component.text("Running ", NamedTextColor.GRAY)
                .append(link(
                        meta.getName() + " " + buildInfo.version(),
                        HEADDB_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" by ", NamedTextColor.GRAY))
                .append(link(
                        "SilentDevelopment",
                        SILENT_DEVELOPMENT_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                .append(link(
                        "TheSilentPro",
                        THE_SILENT_PRO_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ));
    }

    private static @NotNull Component versionLine(
            @NotNull String version,
            @Nullable UpdateCheckResult updateResult
    ) {
        return Component.text("Version: ", NamedTextColor.GRAY)
                .append(Component.text(version, NamedTextColor.GOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(status(updateResult))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    private static @NotNull Component status(@Nullable UpdateCheckResult updateResult) {
        if (updateResult == null) {
            return Component.text("Not Checked", NamedTextColor.DARK_GRAY);
        }

        if (updateResult.failed()) {
            return Component.text("Check Failed", NamedTextColor.RED);
        }

        if (updateResult.updateAvailable()) {
            return Component.text("Update Available", NamedTextColor.YELLOW);
        }

        return Component.text("Latest", NamedTextColor.GOLD);
    }

    private static @NotNull Component section(@NotNull String title) {
        return Component.text(" > ", NamedTextColor.DARK_GRAY)
                .append(Component.text(title, NamedTextColor.GOLD));
    }

    private static @NotNull Component field(@NotNull String key, @NotNull String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.GOLD));
    }

    private static @NotNull Component indentedField(@NotNull String key, @NotNull String value) {
        return Component.text("      ")
                .append(Component.text(key + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.GOLD));
    }

    private static @NotNull Component helpLine() {
        return Component.text(" > ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Run ", NamedTextColor.GRAY))
                .append(Component.text("/hdb help", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.suggestCommand("/hdb help"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to suggest /hdb help", NamedTextColor.GRAY))))
                .append(Component.text(" for command information.", NamedTextColor.GRAY));
    }

    private static @NotNull Component actionButtons(@NotNull CommandSender sender) {
        List<Component> buttons = new ArrayList<>();

        if (Permissions.has(sender, Permissions.RELOAD)) {
            buttons.add(button("RELOAD", "/hdb reload", "Reload config, messages, and runtime."));
        }

        if (Permissions.has(sender, Permissions.VERIFY)) {
            buttons.add(button("VERIFY", "/hdb verify", "Verify the remote database without replacing the active database."));
        }

        if (Permissions.has(sender, Permissions.REFRESH)) {
            buttons.add(button("REFRESH", "/hdb refresh", "Fetch the latest remote head database."));
        }

        if (Permissions.has(sender, Permissions.STATUS)) {
            buttons.add(button("STATUS", "/hdb status", "Show database and refresh status."));
        }

        if (Permissions.has(sender, Permissions.DEBUG)) {
            buttons.add(button("DEBUG", "/hdb debug", "Show detailed runtime diagnostics."));
        }

        if (buttons.isEmpty()) {
            return Component.empty();
        }

        Component result = buttons.getFirst();

        for (int index = 1; index < buttons.size(); index++) {
            result = result.append(divider()).append(buttons.get(index));
        }

        return result;
    }

    private static @NotNull Component button(
            @NotNull String label,
            @NotNull String command,
            @NotNull String hover
    ) {
        return Component.text("[ ", NamedTextColor.DARK_GRAY)
                .append(Component.text(label, NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY))))
                .append(Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component divider() {
        return Component.text(" · ", NamedTextColor.DARK_GRAY);
    }

    private static @NotNull Component link(
            @NotNull String text,
            @NotNull String url,
            @NotNull String hover,
            @NotNull NamedTextColor color
    ) {
        return Component.text(text, color)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    private static @NotNull Component joinLines(@NotNull List<Component> lines) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        Component result = lines.getFirst();

        for (int index = 1; index < lines.size(); index++) {
            result = result.appendNewline().append(lines.get(index));
        }

        return result;
    }

    private static @NotNull String value(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "Unavailable";
        }

        return value;
    }

    private static @NotNull String valueOrUnavailable(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }

        return value;
    }
}
