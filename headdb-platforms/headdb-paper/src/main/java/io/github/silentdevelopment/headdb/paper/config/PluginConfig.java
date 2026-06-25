package io.github.silentdevelopment.headdb.paper.config;

import io.github.silentdevelopment.atlas.bind.annotation.Comment;
import io.github.silentdevelopment.atlas.bind.annotation.Key;
import io.github.silentdevelopment.atlas.bind.annotation.Range;
import io.github.silentdevelopment.atlas.bind.annotation.Required;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

@Comment("HeadDB configuration.")
public final class PluginConfig {

    @Required
    @Key("remote.manifest-url")
    @Comment({
            "Remote database settings.",
            "URL to the remote HeadDB manifest.json."
    })
    private String remoteManifestUrl = "https://data.headsdb.com/manifest.json";

    @Required
    @Key("remote.preferred-mirror-id")
    @Comment("Preferred remote mirror ID. If missing from the manifest, HeadDB falls back to the first mirror.")
    private String preferredMirrorId = ArtifactSelector.DEFAULT_PREFERRED_MIRROR_ID;

    @Required
    @Key("cache.directory")
    @Comment({
            "Artifact and item cache settings.",
            "Directory inside the plugin data folder used for cached remote artifacts."
    })
    private String cacheDirectory = "cache";

    @Key("cache.item.enabled")
    @Comment("Caches created head item prototypes and returns cloned ItemStacks.")
    private boolean cacheItemEnabled = true;

    @Key("cache.item.max-size")
    @Range(min = 0)
    @Comment("Max cache size for head item prototypes. Set to 0 for unbounded lazy caching.")
    private int cacheItemMaxSize = 4096;

    @Key("refresh.load-cache-on-startup")
    @Comment({
            "Database refresh settings.",
            "Loads the last verified cached artifacts before remote refresh."
    })
    private boolean loadCacheOnStartup = true;

    @Key("refresh.refresh-on-startup")
    @Comment("Refreshes the remote database during startup.")
    private boolean refreshOnStartup = true;

    @Key("http.connect-timeout-seconds")
    @Range(min = 1, max = 60)
    @Comment({
            "HTTP client settings.",
            "HTTP connect timeout in seconds."
    })
    private int connectTimeoutSeconds = 10;

    @Key("http.read-timeout-seconds")
    @Range(min = 1, max = 300)
    @Comment("HTTP read timeout in seconds.")
    private int readTimeoutSeconds = 30;

    @Required
    @Key("storage.sqlite.file")
    @Comment({
            "Mutable storage settings.",
            "SQLite database file inside the plugin data folder used by Strata-backed local data.",
            "This is separate from the immutable remote artifact cache."
    })
    private String localStoreSqliteFile = "storage/headdb.db";

    @Key("remote-overrides.enabled")
    @Comment("Enables local metadata overrides for immutable remote heads.")
    private boolean remoteOverridesEnabled = true;

    @Key("custom-heads.enabled")
    @Comment("Enables durable per-server custom heads.")
    private boolean customHeadsEnabled = true;

    @Key("player-heads.enabled")
    @Comment("Enables player heads derived from known/offline player profiles.")
    private boolean playerHeadsEnabled = true;

    @Key("player-heads.cache-ttl-hours")
    @Range(min = 1, max = 720)
    @Comment("TTL for successful player head cache entries.")
    private int playerHeadCacheTtlHours = 12;

    @Key("player-heads.failed-cache-ttl-minutes")
    @Range(min = 1, max = 1440)
    @Comment("TTL for failed player head lookups.")
    private int playerHeadFailedCacheTtlMinutes = 10;

    @Key("player-heads.allow-external-lookup")
    @Comment("Allows explicit player:<name|uuid> lookups outside the local known-player list.")
    private boolean playerHeadsAllowExternalLookup = true;

    @Required
    @Key("messages.directory")
    @Comment({
            "Message and localization settings.",
            "Directory inside the plugin data folder used for locale message files."
    })
    private String messagesDirectory = "messages";

    @Required
    @Key("messages.default-locale")
    @Comment("Default locale used when no player-specific locale is selected.")
    private String defaultLocale = "en-US";

    @Required
    @Key("messages.console-locale")
    @Comment("Locale used for console messages.")
    private String consoleLocale = "en-US";

    @Key("gui.open-main-command")
    @Comment({
            "GUI runtime settings.",
            "If true, /hdb opens the main GUI for players with headdb.gui.main."
    })
    private boolean guiOpenMainCommand = true;

    @Key("update-checker.enabled")
    @Comment({
            "Update checker settings.",
            "Checks the official HeadDB GitHub Releases feed for newer versions and builds.",
            "The GitHub repository is hardcoded by the plugin."
    })
    private boolean updateCheckerEnabled = true;

    @Key("update-checker.check-on-startup")
    @Comment("Checks for updates shortly after HeadDB starts or reloads.")
    private boolean updateCheckerCheckOnStartup = true;

    @Key("update-checker.notify-console")
    @Comment("Logs update notifications to console when an update is available.")
    private boolean updateCheckerNotifyConsole = true;

    @Key("update-checker.notify-admins")
    @Comment("Notifies online players with headdb.admin.update when an update is available.")
    private boolean updateCheckerNotifyAdmins = true;

    @Key("update-checker.include-prereleases")
    @Comment("Includes prerelease GitHub Releases such as alpha, beta, and rc versions.")
    private boolean updateCheckerIncludePrereleases = true;

    @Key("update-checker.include-builds")
    @Comment("Includes newer build metadata releases such as 7.0.0-rc.2+build.5.")
    private boolean updateCheckerIncludeBuilds = true;

    @Key("auto-updater.install-updates")
    @Comment({
            "Auto updater settings.",
            "If true, the newest plugin jar is installed in place when possible or staged in the server update folder.",
            "The server is not restarted automatically. Restart the server to load the new version.",
            "Disabled by default."
    })
    private boolean autoUpdaterInstallUpdates = false;

    @Key("debug")
    @Comment({
            "Diagnostics.",
            "Runs extra startup diagnostics and logs config/runtime validation details."
    })
    private boolean debug = false;

    public PluginConfig() {}

    public void validate() {
        validateManifestUrl();
        validateRequired("remote.preferred-mirror-id", preferredMirrorId);

        validateRelativeDirectory("cache.directory", cacheDirectory);
        validateRelativePath("storage.sqlite.file", localStoreSqliteFile);
        validateRelativeDirectory("messages.directory", messagesDirectory);

        if (cacheItemMaxSize < 0) {
            throw new ConfigException("cache.item.max-size must be greater than or equal to 0");
        }

        validateRequired("messages.default-locale", defaultLocale);
        validateRequired("messages.console-locale", consoleLocale);
    }

    public @NotNull URI remoteManifestUri() {
        return URI.create(remoteManifestUrl);
    }

    public @NotNull String preferredMirrorId() {
        return preferredMirrorId;
    }

    public @NotNull Path cacheDirectory(@NotNull Path pluginDataDirectory) {
        Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory");
        return pluginDataDirectory.resolve(cacheDirectory).normalize();
    }

    public boolean cacheItemEnabled() {
        return cacheItemEnabled;
    }

    public int cacheItemMaxSize() {
        return cacheItemMaxSize;
    }

    public int getCacheItemMaxSize() {
        return cacheItemMaxSize;
    }

    public boolean loadCacheOnStartup() {
        return loadCacheOnStartup;
    }

    public boolean refreshOnStartup() {
        return refreshOnStartup;
    }

    public @NotNull Duration connectTimeout() {
        return Duration.ofSeconds(connectTimeoutSeconds);
    }

    public @NotNull Duration readTimeout() {
        return Duration.ofSeconds(readTimeoutSeconds);
    }

    public @NotNull Path localStoreDatabase(@NotNull Path pluginDataDirectory) {
        Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory");
        return pluginDataDirectory.resolve(localStoreSqliteFile).normalize();
    }

    public boolean remoteOverridesEnabled() {
        return remoteOverridesEnabled;
    }

    public boolean customHeadsEnabled() {
        return customHeadsEnabled;
    }

    public boolean playerHeadsEnabled() {
        return playerHeadsEnabled;
    }

    public @NotNull Duration playerHeadCacheTtl() {
        return Duration.ofHours(playerHeadCacheTtlHours);
    }

    public @NotNull Duration playerHeadFailedCacheTtl() {
        return Duration.ofMinutes(playerHeadFailedCacheTtlMinutes);
    }

    public boolean playerHeadsAllowExternalLookup() {
        return playerHeadsAllowExternalLookup;
    }

    public @NotNull String messagesDirectory() {
        return messagesDirectory;
    }

    public @NotNull Path messagesDirectory(@NotNull Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        return dataDirectory.resolve(messagesDirectory).normalize();
    }

    public @NotNull String defaultLocale() {
        return defaultLocale;
    }

    public @NotNull String consoleLocale() {
        return consoleLocale;
    }

    public boolean guiOpenMainCommand() {
        return guiOpenMainCommand;
    }

    public boolean updateCheckerEnabled() {
        return updateCheckerEnabled;
    }

    public boolean updateCheckerCheckOnStartup() {
        return updateCheckerCheckOnStartup;
    }

    public boolean updateCheckerNotifyConsole() {
        return updateCheckerNotifyConsole;
    }

    public boolean updateCheckerNotifyAdmins() {
        return updateCheckerNotifyAdmins;
    }

    public boolean updateCheckerIncludePrereleases() {
        return updateCheckerIncludePrereleases;
    }

    public boolean updateCheckerIncludeBuilds() {
        return updateCheckerIncludeBuilds;
    }

    public boolean autoUpdaterInstallUpdates() {
        return autoUpdaterInstallUpdates;
    }

    public boolean isDebug() {
        return debug;
    }

    private void validateManifestUrl() {
        validateRequired("remote.manifest-url", remoteManifestUrl);

        URI uri;

        try {
            uri = new URI(remoteManifestUrl);
        } catch (URISyntaxException exception) {
            throw new ConfigException("remote.manifest-url must be a valid URI", exception);
        }

        String scheme = uri.getScheme();

        if (scheme == null) {
            throw new ConfigException("remote.manifest-url must include a scheme");
        }

        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new ConfigException("remote.manifest-url must use http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ConfigException("remote.manifest-url must include a host");
        }
    }

    private static void validateRequired(@NotNull String key, String value) {
        Objects.requireNonNull(key, "key");

        if (value == null || value.isBlank()) {
            throw new ConfigException(key + " cannot be blank");
        }
    }

    private static void validateRelativePath(@NotNull String key, String value) {
        validateRequired(key, value);

        Path path;

        try {
            path = Path.of(value).normalize();
        } catch (InvalidPathException exception) {
            throw new ConfigException(key + " must be a valid relative path", exception);
        }

        if (path.isAbsolute()) {
            throw new ConfigException(key + " must be relative to the plugin data folder");
        }

        if (path.startsWith("..")) {
            throw new ConfigException(key + " cannot escape the plugin data folder");
        }
    }

    private static void validateRelativeDirectory(@NotNull String key, String value) {
        validateRequired(key, value);

        Path path;

        try {
            path = Path.of(value).normalize();
        } catch (InvalidPathException exception) {
            throw new ConfigException(key + " must be a valid relative path", exception);
        }

        if (path.isAbsolute()) {
            throw new ConfigException(key + " must be relative to the plugin data folder");
        }

        if (path.startsWith("..")) {
            throw new ConfigException(key + " cannot escape the plugin data folder");
        }
    }
}
