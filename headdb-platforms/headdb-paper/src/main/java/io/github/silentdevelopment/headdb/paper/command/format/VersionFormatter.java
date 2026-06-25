package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.updater.GitHubRelease;
import io.github.silentdevelopment.headdb.paper.updater.UpdateCheckResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VersionFormatter {

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
        lines.add(runningLine(plugin));
        lines.add(versionLine(buildInfo.version(), updateResult));

        if (plugin.config().isDebug()) {
            lines.add(field("Build", value(buildInfo.buildNumber())));
            lines.add(field("Branch", value(buildInfo.branch())));
            lines.add(field("Commit", value(buildInfo.commit())));
            lines.add(field("Timestamp", value(buildInfo.buildTime())));
        }

        return joinLines(lines);
    }

    public static @NotNull List<Component> command(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        BuildInfo buildInfo = BuildInfo.read(plugin);
        UpdateCheckResult updateResult = plugin.updater().lastResult();
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(runningLine(plugin));
        lines.add(versionLine(buildInfo.version(), updateResult));
        lines.add(field("Build", value(buildInfo.buildNumber())));
        lines.add(field("Branch", value(buildInfo.branch())));
        lines.add(field("Commit", value(buildInfo.commit())));
        lines.add(field("Timestamp", value(buildInfo.buildTime())));

        Component actions = updateActions(updateResult);

        if (actions != null) {
            lines.add(Component.empty());
            lines.add(actions);
        }

        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    private static @NotNull Component runningLine(@NotNull HeadDBPlugin plugin) {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        String authorText = authors.isEmpty() ? "Unknown" : String.join(", ", authors);

        return Component.text("Running ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getPluginMeta().getName(), NamedTextColor.RED))
                .append(Component.text(" by ", NamedTextColor.GRAY))
                .append(Component.text(authorText, NamedTextColor.GOLD));
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

    private static @NotNull Component field(@NotNull String key, @NotNull String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.GOLD));
    }

    private static @Nullable Component updateActions(@Nullable UpdateCheckResult updateResult) {
        if (updateResult == null || !updateResult.updateAvailable()) {
            return null;
        }

        GitHubRelease release = updateResult.release();

        if (release == null) {
            return null;
        }

        Component open = Component.text("[OPEN]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.openUrl(release.htmlUrl()))
                .hoverEvent(HoverEvent.showText(Component.text("Open the release page.", NamedTextColor.GRAY)));

        Component update = Component.text("[UPDATE]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/hdb update"))
                .hoverEvent(HoverEvent.showText(Component.text("Download and install this update.", NamedTextColor.GRAY)));

        return open
                .append(Component.text(" -=- ", NamedTextColor.GRAY))
                .append(update);
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
}
