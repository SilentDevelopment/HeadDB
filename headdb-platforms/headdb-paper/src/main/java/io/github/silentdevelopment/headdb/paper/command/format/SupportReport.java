package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.runtime.PlatformRequirements;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import io.github.silentdevelopment.headdb.paper.updater.GitHubRelease;
import io.github.silentdevelopment.headdb.paper.updater.UpdateCheckResult;
import io.github.silentdevelopment.headdb.paper.updater.UpdateService;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class SupportReport {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private SupportReport() {
    }

    public static @NotNull String create(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        StringBuilder report = new StringBuilder(4096);
        BuildInfo buildInfo = BuildInfo.read(plugin);
        PluginConfig config = plugin.config();
        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats databaseStats = plugin.runtime().database().stats();
        DatabaseStats registryStats = plugin.headRegistry().stats();
        RefreshState refresh = plugin.runtime().refreshState();
        Server server = plugin.getServer();
        PlatformRequirements.Compatibility compatibility = PlatformRequirements.inspect(plugin);
        Path dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();

        section(report, "Plugin");
        line(report, "Version", buildInfo.version());
        line(report, "Base version", buildInfo.baseVersion());
        line(report, "Build", value(buildInfo.buildNumber()));
        line(report, "Attempt", value(buildInfo.buildAttempt()));
        line(report, "Commit", value(buildInfo.commit()));
        line(report, "Full commit", value(buildInfo.fullCommit()));
        line(report, "Branch", value(buildInfo.branch()));
        line(report, "Build time", value(buildInfo.buildTime()));

        section(report, "Server");
        line(report, "Software", server.getName());
        line(report, "Server version", server.getVersion());
        line(report, "Bukkit version", server.getBukkitVersion());
        line(report, "Minecraft version", server.getMinecraftVersion());
        line(report, "Folia", yesNo(isFolia()));
        line(report, "Java", System.getProperty("java.version") + " (feature " + compatibility.javaFeature() + ")");
        line(report, "OS", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        line(report, "Max memory", bytes(Runtime.getRuntime().maxMemory()));
        line(report, "Required Paper", PlatformRequirements.REQUIRED_PAPER_VERSION);
        line(report, "Required Java", PlatformRequirements.REQUIRED_JAVA_FEATURE + "+");
        line(report, "Java supported", yesNo(compatibility.javaSupported()));
        line(report, "Paper supported", yesNo(compatibility.paperSupported()));
        line(report, "Runtime supported", yesNo(compatibility.supported()));

        section(report, "Database");
        line(report, "State", status.state());
        line(report, "Source", status.source());
        line(report, "Manifest ID", value(status.manifestId()));
        line(report, "Catalog ID", value(status.artifactId()));
        line(report, "Loaded at", formatInstant(status.loadedAt()));
        line(report, "Last database error", value(status.lastError()));

        section(report, "Remote stats");
        line(report, "Heads", databaseStats.heads());
        line(report, "Categories", databaseStats.categories());
        line(report, "Tags", databaseStats.tags());
        line(report, "Collections", databaseStats.collections());
        line(report, "Revocations", databaseStats.revocations());

        section(report, "Registry stats");
        line(report, "Effective heads", registryStats.heads());
        line(report, "Effective categories", registryStats.categories());
        line(report, "Effective tags", registryStats.tags());
        line(report, "Effective collections", registryStats.collections());
        line(report, "Hidden remote heads", plugin.headRegistry().hiddenHeads().size());
        line(report, "Remote overrides", plugin.headRegistry().overrides().list().size());
        line(report, "More Heads", plugin.headRegistry().customHeads().list().size());
        line(report, "Known player heads", plugin.headRegistry().playerHeads().knownPlayers().size());
        line(report, "More Categories", plugin.customCategories().list().size());

        section(report, "Refresh");
        line(report, "Running", yesNo(refresh.running()));
        line(report, "Current operation", refresh.running() ? refresh.currentOperation() : "none");
        line(report, "Started at", formatInstant(refresh.startedAt()));
        line(report, "Last outcome", refresh.lastOutcome());
        line(report, "Last operation", refresh.lastOutcome() == RefreshState.RefreshOutcome.NONE ? "none" : refresh.lastOperation());
        line(report, "Last successful operation", formatInstant(refresh.lastSuccessfulRefresh()));
        line(report, "Last failed operation", formatInstant(refresh.lastFailedRefresh()));
        line(report, "Last failure", value(refresh.lastFailureMessage()));

        section(report, "Remote config");
        line(report, "Manifest URL", config.remoteManifestUri());
        line(report, "Preferred mirror", value(config.preferredMirrorId()));
        line(report, "Connect timeout", config.connectTimeout());
        line(report, "Read timeout", config.readTimeout());

        section(report, "Cache and storage");
        line(report, "Data directory", dataDirectory);
        line(report, "Cache directory", config.cacheDirectory(dataDirectory));
        line(report, "Local store database", config.localStoreDatabase(dataDirectory));
        line(report, "Load cache on startup", yesNo(config.loadCacheOnStartup()));
        line(report, "Refresh on startup", yesNo(config.refreshOnStartup()));
        line(report, "Item cache enabled", yesNo(config.cacheItemEnabled()));
        line(report, "Item cache size", plugin.itemCacheSize());

        section(report, "Local features");
        line(report, "Remote overrides enabled", yesNo(config.remoteOverridesEnabled()));
        line(report, "More Heads enabled", yesNo(config.customHeadsEnabled()));
        line(report, "Player heads enabled", yesNo(config.playerHeadsEnabled()));
        line(report, "External player lookup", yesNo(config.playerHeadsAllowExternalLookup()));

        section(report, "Updater");
        appendUpdater(report, plugin);

        section(report, "Invocation");
        line(report, "Sender type", sender instanceof Player ? "player" : "console");

        if (sender instanceof Player player) {
            line(report, "Admin mode", yesNo(plugin.adminModes().enabled(player)));
        }

        return report.toString();
    }

    private static void appendUpdater(@NotNull StringBuilder report, @NotNull HeadDBPlugin plugin) {
        PluginConfig config = plugin.config();
        UpdateService updater = plugin.updater();
        UpdateCheckResult result = updater.lastResult();

        line(report, "Update checker enabled", yesNo(config.updateCheckerEnabled()));
        line(report, "Check on startup", yesNo(config.updateCheckerCheckOnStartup()));
        line(report, "Include prereleases", yesNo(config.updateCheckerIncludePrereleases()));
        line(report, "Include builds", yesNo(config.updateCheckerIncludeBuilds()));
        line(report, "Auto install", yesNo(config.autoUpdaterInstallUpdates()));
        line(report, "Running", yesNo(updater.running()));

        if (result == null) {
            line(report, "Last check", "never");
            return;
        }

        line(report, "Last check", formatInstant(result.checkedAt()));
        line(report, "Current version", result.currentVersion());
        line(report, "Update kind", result.kind());
        line(report, "Update available", yesNo(result.updateAvailable()));
        line(report, "Failed", yesNo(result.failed()));
        line(report, "Failure", value(result.failureMessage()));
        line(report, "Installed path", value(result.installedPath()));

        GitHubRelease release = result.release();
        if (release == null) {
            return;
        }

        line(report, "Latest version", release.version().raw());
        line(report, "Latest tag", release.tagName());
        line(report, "Latest URL", release.htmlUrl());
        line(report, "Latest prerelease", yesNo(release.prerelease()));
        line(report, "Latest asset", value(release.assetName()));
    }

    private static void section(@NotNull StringBuilder report, @NotNull String title) {
        if (!report.isEmpty()) {
            report.append(System.lineSeparator());
        }

        report.append("== ").append(title).append(" ==").append(System.lineSeparator());
    }

    private static void line(@NotNull StringBuilder report, @NotNull String key, @Nullable Object value) {
        report.append(key).append(": ").append(value(value)).append(System.lineSeparator());
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

    private static @NotNull String bytes(long bytes) {
        long mib = bytes / 1024L / 1024L;
        return bytes + " bytes (" + mib + " MiB)";
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

}
