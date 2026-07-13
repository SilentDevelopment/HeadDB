package io.github.silentdevelopment.headdb.paper.updater;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.format.VersionFormatter;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateService {

    private static final String UPDATE_STATE_FILE = "updater-state.properties";
    private static final String STATE_EXPECTED_VERSION = "expected-version";
    private static final String STATE_ACTIVE_JAR = "active-jar";
    private static final String STATE_BACKUP_JAR = "backup-jar";

    private final HeadDBPlugin plugin;
    private final PluginConfig config;
    private final GitHubReleaseUpdateChecker checker;
    private final HttpClient downloadClient;
    private final String userAgent;
    private final AtomicBoolean running;
    private final AtomicBoolean closed;
    private volatile UpdateCheckResult lastResult;

    public UpdateService(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.userAgent = UpdaterUserAgent.create(plugin, BuildInfo.read(plugin).version());
        this.checker = new GitHubReleaseUpdateChecker(config.connectTimeout(), config.readTimeout(), userAgent);
        this.downloadClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.running = new AtomicBoolean(false);
        this.closed = new AtomicBoolean(false);
    }

    public void start() {
        if (!config.updateCheckerEnabled() || !config.updateCheckerCheckOnStartup()) {
            plugin.getComponentLogger().info(VersionFormatter.startup(plugin, null));
            return;
        }

        if (!runAsync(null, true, false)) {
            plugin.getComponentLogger().info(VersionFormatter.startup(plugin, null));
        }
    }

    /**
     * Performs an explicit administrator-requested update check and installs an available update.
     * This intentionally bypasses the automatic-install configuration flag.
     */
    public boolean checkAndInstallAsync(@NotNull CommandSender requester) {
        Objects.requireNonNull(requester, "requester");
        return runAsync(requester, false, true);
    }

    public boolean running() {
        return running.get();
    }

    public @Nullable UpdateCheckResult lastResult() {
        return lastResult;
    }

    public void close() {
        closed.set(true);
    }

    /**
     * Called only after the plugin has completed its startup sequence. A backup is removed only when the
     * running build is at least the version recorded when the update was staged.
     */
    public void cleanupBackupAfterSuccessfulLoad() {
        PendingUpdate pendingUpdate = readPendingUpdate();

        if (pendingUpdate != null) {
            cleanupPendingUpdate(currentPluginJar(), pendingUpdate);
            return;
        }

        Path currentJar = currentPluginJar();

        if (currentJar != null) {
            cleanupLegacyBackup(currentJar);
        }
    }

    private void cleanupPendingUpdate(@Nullable Path currentJar, @NotNull PendingUpdate pendingUpdate) {
        String runningVersion = BuildInfo.read(plugin).version();
        HeadDBVersion running = HeadDBVersion.parse(runningVersion);
        HeadDBVersion expected = HeadDBVersion.parse(pendingUpdate.expectedVersion());

        if (running.compareTo(expected) < 0) {
            plugin.getSLF4JLogger().info(
                    "Update backup retained because the loaded version is {} while the pending update expects {}. Restart is still required.",
                    runningVersion,
                    pendingUpdate.expectedVersion()
            );
            return;
        }

        if (currentJar != null && config.isDebug()) {
            Path normalizedCurrentJar = currentJar.toAbsolutePath().normalize();
            Path recordedActiveJar = pendingUpdate.activeJar().toAbsolutePath().normalize();

            if (!normalizedCurrentJar.equals(recordedActiveJar)) {
                plugin.getSLF4JLogger().debug(
                        "Loaded plugin jar path differs from the update record. Loaded={}, recorded={}.",
                        normalizedCurrentJar,
                        recordedActiveJar
                );
            }
        }

        Path backup = pendingUpdate.backupJar().toAbsolutePath().normalize();

        if (Files.exists(backup) && !deleteBackup(backup)) {
            return;
        }

        deleteUpdateState();

        if (config.isDebug()) {
            plugin.getSLF4JLogger().debug(
                    "Removed the previous update backup after successfully loading version {}.",
                    runningVersion
            );
        }
    }

    /**
     * Cleans up backups created by older updater builds that did not persist an update state file.
     */
    private void cleanupLegacyBackup(@NotNull Path currentJar) {
        Path normalizedCurrentJar = currentJar.toAbsolutePath().normalize();
        Path backup = backupPath(normalizedCurrentJar);

        if (!Files.isRegularFile(backup)) {
            return;
        }

        String runningVersion = BuildInfo.read(plugin).version();
        String diskVersion = readJarVersion(normalizedCurrentJar);
        String backupVersion = readJarVersion(backup);

        if (diskVersion == null || diskVersion.isBlank()) {
            plugin.getSLF4JLogger().warn("Update backup retained because the active jar version could not be verified.");
            return;
        }

        if (backupVersion == null || backupVersion.isBlank()) {
            plugin.getSLF4JLogger().warn("Update backup retained because the backup jar version could not be verified.");
            return;
        }

        HeadDBVersion running = HeadDBVersion.parse(runningVersion);
        HeadDBVersion installed = HeadDBVersion.parse(diskVersion);
        HeadDBVersion previous = HeadDBVersion.parse(backupVersion);

        if (running.compareTo(installed) != 0) {
            plugin.getSLF4JLogger().info(
                    "Update backup retained because the jar on disk is version {} while the loaded version is {}. Restart is still required.",
                    diskVersion,
                    runningVersion
            );
            return;
        }

        if (installed.compareTo(previous) <= 0) {
            plugin.getSLF4JLogger().warn(
                    "Update backup retained because its version ({}) is not older than the active version ({}).",
                    backupVersion,
                    diskVersion
            );
            return;
        }

        if (deleteBackup(backup) && config.isDebug()) {
            plugin.getSLF4JLogger().debug(
                    "Removed the previous update backup after successfully loading version {}.",
                    runningVersion
            );
        }
    }

    private boolean deleteBackup(@NotNull Path backup) {
        try {
            return Files.deleteIfExists(backup);
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("The previous update backup could not be deleted: {}", backup);
            debugFailure("Backup cleanup failure details.", exception);
            return false;
        }
    }

    private boolean deleteUnneededBackup(@NotNull Path backup) {
        try {
            boolean deleted = Files.deleteIfExists(backup);

            if (deleted && config.isDebug()) {
                plugin.getSLF4JLogger().debug(
                        "Removed unneeded update backup because the active plugin jar was not replaced: {}.",
                        backup.toAbsolutePath().normalize()
                );
            }

            return true;
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("An unneeded update backup could not be deleted: {}", backup);
            debugFailure("Unneeded update backup cleanup failure details.", exception);
            return false;
        }
    }

    private boolean runAsync(@Nullable CommandSender requester, boolean startup, boolean forceInstall) {
        if (closed.get()) {
            return false;
        }

        if (!running.compareAndSet(false, true)) {
            if (requester != null) {
                reply(requester, Component.text("An update check is already running.", NamedTextColor.RED));
            }

            return false;
        }

        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> check(requester, startup, forceInstall));
        return true;
    }

    private void check(@Nullable CommandSender requester, boolean startup, boolean forceInstall) {
        try {
            BuildInfo buildInfo = BuildInfo.read(plugin);
            UpdateCheckResult result = checker.check(
                    buildInfo.version(),
                    config.updateCheckerIncludePrereleases(),
                    config.updateCheckerIncludeBuilds()
            );

            lastResult = result;

            if (result.updateAvailable() && (forceInstall || config.autoUpdaterInstallUpdates())) {
                installUpdate(result);
            }

            notifyResult(requester, result, startup);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            UpdateCheckResult result = UpdateCheckResult.failed(currentVersion(), java.time.Instant.now(), "Update check was interrupted.");
            lastResult = result;
            notifyResult(requester, result, startup);
        } catch (Exception exception) {
            String message = exception.getMessage();

            if (message == null || message.isBlank()) {
                message = exception.getClass().getSimpleName();
            }

            UpdateCheckResult result = UpdateCheckResult.failed(currentVersion(), java.time.Instant.now(), message);
            lastResult = result;
            notifyResult(requester, result, startup);
            debugFailure("Update check failure details.", exception);
        } finally {
            running.set(false);
        }
    }

    private void installUpdate(@NotNull UpdateCheckResult result) throws IOException, InterruptedException {
        GitHubRelease release = result.release();

        if (release == null) {
            return;
        }

        if (!release.hasPluginAsset()) {
            throw new IOException("Release " + release.tagName() + " does not contain a downloadable jar asset.");
        }

        String downloadUrl = release.assetDownloadUrl();

        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("Release " + release.tagName() + " has no downloadable jar asset.");
        }

        Path currentJar = currentPluginJar();

        if (currentJar == null) {
            Path fallback = downloadToUpdateDirectory(downloadUrl, safeFileName(release.assetName()), release.version().raw());
            result.markInstalled(fallback);
            return;
        }

        InstallResult installResult = downloadAndInstall(downloadUrl, currentJar, release.version().raw());
        result.markInstalled(installResult.installedPath());

        if (installResult.backupPath() != null && Files.isRegularFile(installResult.backupPath())) {
            writePendingUpdate(release.version().raw(), currentJar, installResult.backupPath());
        }
    }

    private @NotNull InstallResult downloadAndInstall(@NotNull String downloadUrl, @NotNull Path currentJar, @NotNull String expectedVersion) throws IOException, InterruptedException {
        Path normalizedCurrentJar = currentJar.toAbsolutePath().normalize();
        Path pluginDirectory = normalizedCurrentJar.getParent();

        if (pluginDirectory == null) {
            Path fallback = downloadToUpdateDirectory(downloadUrl, normalizedCurrentJar.getFileName().toString(), expectedVersion);
            return new InstallResult(fallback, null);
        }

        Files.createDirectories(pluginDirectory);

        String currentFileName = normalizedCurrentJar.getFileName().toString();
        Path temporary = pluginDirectory.resolve(currentFileName + ".download");
        Files.deleteIfExists(temporary);
        downloadJar(downloadUrl, temporary, expectedVersion);

        Path backup = backupPath(normalizedCurrentJar);

        try {
            Files.copy(normalizedCurrentJar, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn(
                    "Could not create an update backup at {}. The update will be staged in the server update folder instead.",
                    backup
            );
            debugFailure("Update backup creation failure details.", exception);
            Path fallback = moveDownloadedJarToUpdateDirectory(temporary, currentFileName);
            return new InstallResult(fallback, null);
        }

        try {
            moveReplacing(temporary, normalizedCurrentJar);
            return new InstallResult(normalizedCurrentJar, backup);
        } catch (IOException exception) {
            if (config.isDebug()) {
                plugin.getSLF4JLogger().debug(
                        "The active plugin jar could not be replaced while running; staging the update in the server update folder instead. Active jar: {}.",
                        normalizedCurrentJar,
                        exception
                );
            }

            Path fallback = moveDownloadedJarToUpdateDirectory(temporary, currentFileName);

            if (deleteUnneededBackup(backup)) {
                return new InstallResult(fallback, null);
            }

            return new InstallResult(fallback, backup);
        }
    }

    private @NotNull Path downloadToUpdateDirectory(@NotNull String downloadUrl, @NotNull String fileName, @NotNull String expectedVersion) throws IOException, InterruptedException {
        Path updateDirectory = updateDirectory();
        Files.createDirectories(updateDirectory);

        Path target = updateDirectory.resolve(safeFileName(fileName));
        Path temporary = updateDirectory.resolve(target.getFileName() + ".download");
        Files.deleteIfExists(temporary);

        downloadJar(downloadUrl, temporary, expectedVersion);
        moveReplacing(temporary, target);
        return target;
    }

    private @NotNull Path moveDownloadedJarToUpdateDirectory(@NotNull Path downloadedJar, @NotNull String activeFileName) throws IOException {
        Path updateDirectory = updateDirectory();
        Files.createDirectories(updateDirectory);

        Path target = updateDirectory.resolve(safeFileName(activeFileName));
        moveReplacing(downloadedJar, target);
        return target;
    }

    private void writePendingUpdate(@NotNull String expectedVersion, @NotNull Path activeJar, @NotNull Path backupJar) {
        Path stateFile = updateStateFile();
        Path temporary = stateFile.resolveSibling(stateFile.getFileName() + ".download");
        Properties properties = new Properties();
        properties.setProperty(STATE_EXPECTED_VERSION, expectedVersion);
        properties.setProperty(STATE_ACTIVE_JAR, activeJar.toAbsolutePath().normalize().toString());
        properties.setProperty(STATE_BACKUP_JAR, backupJar.toAbsolutePath().normalize().toString());

        try {
            Files.createDirectories(stateFile.getParent());
            Files.deleteIfExists(temporary);

            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Pending plugin update state");
            }

            moveReplacing(temporary, stateFile);
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("Could not persist updater state; backup cleanup will use metadata fallback checks.");
            debugFailure("Updater state write failure details.", exception);

            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                debugFailure("Updater state temporary-file cleanup failure details.", cleanupException);
            }
        }
    }

    private @Nullable PendingUpdate readPendingUpdate() {
        Path stateFile = updateStateFile();

        if (!Files.isRegularFile(stateFile)) {
            return null;
        }

        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(stateFile)) {
            properties.load(input);

            String expectedVersion = requiredProperty(properties, STATE_EXPECTED_VERSION);
            Path activeJar = Path.of(requiredProperty(properties, STATE_ACTIVE_JAR)).toAbsolutePath().normalize();
            Path backupJar = Path.of(requiredProperty(properties, STATE_BACKUP_JAR)).toAbsolutePath().normalize();
            return new PendingUpdate(expectedVersion, activeJar, backupJar);
        } catch (IOException | IllegalArgumentException exception) {
            plugin.getSLF4JLogger().warn("Could not read updater state; backup cleanup will use metadata fallback checks.");
            debugFailure("Updater state read failure details.", exception);
            return null;
        }
    }

    private void deleteUpdateState() {
        Path stateFile = updateStateFile();

        try {
            Files.deleteIfExists(stateFile);
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("The updater state file could not be deleted: {}", stateFile);
            debugFailure("Updater state cleanup failure details.", exception);
        }
    }

    private @NotNull Path updateStateFile() {
        return plugin.getDataFolder().toPath().resolve(UPDATE_STATE_FILE).toAbsolutePath().normalize();
    }

    private static @NotNull String requiredProperty(@NotNull Properties properties, @NotNull String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing updater state property: " + key);
        }

        return value.trim();
    }

    private @NotNull Path backupPath(@NotNull Path currentJar) {
        Path normalizedCurrentJar = currentJar.toAbsolutePath().normalize();
        Path parent = normalizedCurrentJar.getParent();

        if (parent == null) {
            return Path.of(normalizedCurrentJar.getFileName().toString() + ".backup").toAbsolutePath().normalize();
        }

        return parent.resolve(normalizedCurrentJar.getFileName().toString() + ".backup").toAbsolutePath().normalize();
    }

    private @Nullable String readJarVersion(@NotNull Path jar) {
        try {
            return HeadDBPluginJarMetadata.read(jar).preferredVersion();
        } catch (IOException exception) {
            plugin.getSLF4JLogger().warn("Could not inspect plugin jar metadata at {}.", jar.toAbsolutePath().normalize());
            debugFailure("Plugin jar metadata inspection failure details.", exception);
            return null;
        }
    }

    private void downloadJar(@NotNull String downloadUrl, @NotNull Path temporary, @NotNull String expectedVersion) throws IOException, InterruptedException {
        Path parent = temporary.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.deleteIfExists(temporary);

        HttpRequest request = HttpRequest.newBuilder(URI.create(downloadUrl))
                .timeout(config.readTimeout())
                .header("User-Agent", userAgent)
                .GET()
                .build();

        HttpResponse<Path> response = downloadClient.send(request, HttpResponse.BodyHandlers.ofFile(temporary));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(temporary);
            throw new IOException("Failed to download update. HTTP " + response.statusCode());
        }

        validateDownloadedJar(temporary, expectedVersion);
    }

    private void validateDownloadedJar(@NotNull Path jar, @NotNull String expectedVersion) throws IOException {
        if (!Files.isRegularFile(jar)) {
            throw new IOException("Downloaded update is not a regular file: " + jar);
        }

        if (Files.size(jar) <= 0) {
            Files.deleteIfExists(jar);
            throw new IOException("Downloaded update jar is empty.");
        }

        try {
            HeadDBPluginJarMetadata.read(jar).validateDownloadedUpdate(expectedVersion);
        } catch (IOException exception) {
            Files.deleteIfExists(jar);
            throw exception;
        }
    }

    private void moveReplacing(@NotNull Path source, @NotNull Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private @Nullable Path currentPluginJar() {
        CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();

        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }

        try {
            Path path = Path.of(codeSource.getLocation().toURI()).toAbsolutePath().normalize();

            if (!Files.isRegularFile(path)) {
                return null;
            }

            String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
            return fileName.endsWith(".jar") ? path : null;
        } catch (IllegalArgumentException | URISyntaxException exception) {
            plugin.getSLF4JLogger().warn("Could not resolve the currently loaded plugin jar path.");
            debugFailure("Loaded jar path resolution failure details.", exception);
            return null;
        }
    }

    private void notifyResult(@Nullable CommandSender requester, @NotNull UpdateCheckResult result, boolean startup) {
        if (closed.get()) {
            return;
        }

        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (closed.get()) {
                return;
            }

            if (requester != null) {
                plugin.messages().send(requester, formatResult(result));
                return;
            }

            if (startup) {
                plugin.getComponentLogger().info(VersionFormatter.startup(plugin, result));
            }

            logResult(result);

            if (!startup || !result.updateAvailable() || !config.updateCheckerNotifyAdmins()) {
                return;
            }

            notifyAdmins(result);
        });
    }

    private void logResult(@NotNull UpdateCheckResult result) {
        if (result.failed()) {
            if (config.updateCheckerNotifyConsole()) {
                plugin.getSLF4JLogger().warn("Update check failed: {}", result.failureMessage());
            }

            return;
        }

        if (!result.updateAvailable()) {
            return;
        }

        GitHubRelease release = result.release();

        if (release == null) {
            return;
        }

        if (result.installedPath() != null) {
            plugin.getComponentLogger().info(downloadedMessage(release));

            if (config.isDebug()) {
                plugin.getSLF4JLogger().debug("Update staged at {}.", result.installedPath().toAbsolutePath().normalize());
            }

            return;
        }

        if (config.updateCheckerNotifyConsole()) {
            plugin.getComponentLogger().info(availableMessage(release));
        }
    }

    private void notifyAdmins(@NotNull UpdateCheckResult result) {
        Component message = formatResult(result);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (Permissions.has(player, Permissions.UPDATE)) {
                player.sendMessage(message);
            }
        }
    }

    private @NotNull Component formatResult(@NotNull UpdateCheckResult result) {
        if (result.failed()) {
            return Component.text("Update check failed: ", NamedTextColor.RED)
                    .append(Component.text(result.failureMessage(), NamedTextColor.GRAY));
        }

        if (!result.updateAvailable()) {
            return Component.text("Latest version is already installed: ", NamedTextColor.GRAY)
                    .append(Component.text(result.currentVersion(), NamedTextColor.GOLD));
        }

        GitHubRelease release = result.release();

        if (release == null) {
            return Component.text("Update state is unavailable.", NamedTextColor.RED);
        }

        if (result.installedPath() != null) {
            return downloadedMessage(release);
        }

        return availableMessage(release);
    }

    private @NotNull Component downloadedMessage(@NotNull GitHubRelease release) {
        return Component.text("Downloaded new version: ", NamedTextColor.GREEN)
                .append(Component.text(release.tagName(), NamedTextColor.GOLD))
                .append(Component.text(" | ", NamedTextColor.GRAY))
                .append(Component.text("Restart the server to load it.", NamedTextColor.GREEN));
    }

    private @NotNull Component availableMessage(@NotNull GitHubRelease release) {
        String url = downloadLink(release);

        return Component.text("New version available: ", NamedTextColor.GRAY)
                .append(Component.text(release.tagName(), NamedTextColor.GOLD))
                .append(Component.text(" | Run /hdb reload or download: ", NamedTextColor.GRAY))
                .append(link(url, url));
    }

    private static @NotNull String downloadLink(@NotNull GitHubRelease release) {
        String assetDownloadUrl = release.assetDownloadUrl();
        return assetDownloadUrl == null || assetDownloadUrl.isBlank() ? release.htmlUrl() : assetDownloadUrl;
    }

    private @NotNull Component link(@NotNull String label, @NotNull String url) {
        return Component.text(label, NamedTextColor.GOLD)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(url, NamedTextColor.GRAY)));
    }

    private void reply(@NotNull CommandSender sender, @NotNull Component message) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> plugin.messages().send(sender, message));
    }

    private @NotNull String currentVersion() {
        return BuildInfo.read(plugin).version();
    }

    private @NotNull Path updateDirectory() {
        return plugin.getServer().getUpdateFolderFile().toPath().toAbsolutePath().normalize();
    }

    private void debugFailure(@NotNull String message, @NotNull Throwable throwable) {
        if (config.isDebug()) {
            plugin.getSLF4JLogger().debug(message, throwable);
        }
    }

    private static @NotNull String safeFileName(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "HeadDB.jar";
        }

        String cleaned = value.replace('\\', '_').replace('/', '_').trim();

        if (cleaned.isBlank() || !cleaned.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return "HeadDB.jar";
        }

        return cleaned;
    }

    private record InstallResult(@NotNull Path installedPath, @Nullable Path backupPath) {
    }

    private record PendingUpdate(@NotNull String expectedVersion, @NotNull Path activeJar, @NotNull Path backupJar) {
    }

}
