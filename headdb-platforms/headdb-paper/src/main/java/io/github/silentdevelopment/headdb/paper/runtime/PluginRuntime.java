package io.github.silentdevelopment.headdb.paper.runtime;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
import io.github.silentdevelopment.headdb.core.database.DefaultHeadDatabase;
import io.github.silentdevelopment.headdb.core.database.cache.FileDatabaseArtifactCache;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.database.refresh.DatabaseRefreshResult;
import io.github.silentdevelopment.headdb.core.database.refresh.DatabaseRefreshService;
import io.github.silentdevelopment.headdb.core.hash.Sha256Verifier;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.http.JdkRemoteHttpClient;
import io.github.silentdevelopment.headdb.core.remote.parse.GsonRemoteManifestParser;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class PluginRuntime {

    private final HeadDBPlugin plugin;
    private final PluginConfig config;
    private final DefaultHeadDatabase database;
    private final DatabaseRefreshService refreshService;
    private final RefreshState refreshState;
    private final List<ScheduledTask> scheduledTasks;
    private final AtomicBoolean closed;

    private PluginRuntime(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config, @NotNull DefaultHeadDatabase database, @NotNull DatabaseRefreshService refreshService, @NotNull RefreshState refreshState) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.database = Objects.requireNonNull(database, "database");
        this.refreshService = Objects.requireNonNull(refreshService, "refreshService");
        this.refreshState = Objects.requireNonNull(refreshState, "refreshState");
        this.scheduledTasks = new ArrayList<>();
        this.closed = new AtomicBoolean(false);
    }

    public static @NotNull PluginRuntime create(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(config, "config cannot be null");

        Path cacheDirectory = config.cacheDirectory(plugin.getDataFolder().toPath());
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        GsonRemoteManifestParser manifestParser = new GsonRemoteManifestParser();
        ArtifactSelector artifactSelector = new ArtifactSelector(config.preferredMirrorId());
        ZstdArtifactDecoder zstdDecoder = new ZstdArtifactDecoder();
        GsonCatalogIndexParser catalogIndexParser = new GsonCatalogIndexParser();
        GsonCatalogPartParser catalogPartParser = new GsonCatalogPartParser();
        GsonRevocationsIndexParser revocationsIndexParser = new GsonRevocationsIndexParser();
        GsonRevocationPartParser revocationPartParser = new GsonRevocationPartParser();
        FileDatabaseArtifactCache artifactCache = new FileDatabaseArtifactCache(cacheDirectory, manifestParser, artifactSelector, zstdDecoder, catalogIndexParser, revocationsIndexParser);
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(config.connectTimeout())
                .build();
        JdkRemoteHttpClient remoteHttpClient = new JdkRemoteHttpClient(httpClient, config.readTimeout(), 64 * 1024 * 1024);
        DatabaseRefreshService refreshService = new DatabaseRefreshService(database, config.remoteManifestUri(), remoteHttpClient, manifestParser, artifactSelector, new Sha256Verifier(), zstdDecoder, catalogIndexParser, catalogPartParser, revocationsIndexParser, revocationPartParser, Clock.systemUTC(), artifactCache);

        if (config.isDebug()) {
            plugin.getSLF4JLogger().debug("manifest URI: {}", config.remoteManifestUri());
            plugin.getSLF4JLogger().debug("artifact cache directory: {}", cacheDirectory.toAbsolutePath().normalize());
        }

        return new PluginRuntime(plugin, config, database, refreshService, new RefreshState());
    }

    public void start() {
        if (closed.get()) {
            throw new IllegalStateException("runtime is closed");
        }

        if (config.loadCacheOnStartup() || config.refreshOnStartup()) {
            runAsync("run startup refresh sequence", this::runStartupRefreshSequence);
        } else {
            plugin.getSLF4JLogger().info("startup cache load and remote refresh are both disabled.");
        }

        if (config.scheduledRefreshEnabled()) {
            schedulePeriodicRefresh();
        } else {
            plugin.getSLF4JLogger().info("scheduled remote database refresh is disabled.");
        }
    }

    public PluginConfig config() {
        return config;
    }

    public @NotNull DefaultHeadDatabase database() {
        return database;
    }

    public @NotNull RefreshState refreshState() {
        return refreshState;
    }

    public boolean closed() {
        return closed.get();
    }

    public boolean refreshAsync() {
        if (closed.get()) {
            return false;
        }

        if (!refreshState.begin("remote refresh")) {
            return false;
        }

        if (!runAsync("refresh remote database", this::refreshRemoteStarted)) {
            refreshState.finish();
            return false;
        }

        return true;
    }

    public boolean verifyRemoteAsync(@NotNull Consumer<DatabaseSnapshot> success, @NotNull Consumer<Throwable> failure) {
        Objects.requireNonNull(success, "success");
        Objects.requireNonNull(failure, "failure");

        if (closed.get()) {
            return false;
        }

        if (!refreshState.begin("remote verification")) {
            return false;
        }

        if (!runAsync("verify remote database", () -> verifyRemoteStarted(success, failure))) {
            refreshState.finish();
            return false;
        }

        return true;
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        synchronized (scheduledTasks) {
            for (ScheduledTask task : scheduledTasks) {
                task.cancel();
            }

            scheduledTasks.clear();
        }

        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
    }

    private void runStartupRefreshSequence() {
        if (config.loadCacheOnStartup()) {
            loadCached();
        }

        if (config.refreshOnStartup()) {
            refreshRemote();
        }
    }

    private void loadCached() {
        if (!refreshState.begin("cache load")) {
            plugin.getSLF4JLogger().warn("Skipped cache load because another refresh task is already running.");
            return;
        }

        loadCachedStarted();
    }

    private void loadCachedStarted() {
        try {
            boolean loaded = refreshService.loadCached();

            if (!loaded) {
                refreshState.finish();
                plugin.getSLF4JLogger().info("No cached artifacts found.");
                return;
            }

            plugin.invalidateHeadRegistry();
            plugin.warmHeadRegistry();
            plugin.clearItemCache();
            plugin.clearSearchCache();
            refreshState.markSuccess();
            plugin.getSLF4JLogger().info("Loaded database from cached artifacts.");
        } catch (Exception exception) {
            refreshState.markFailure(exception);
            logFailure("Failed to load cached artifacts.", exception);
        }
    }

    private void refreshRemote() {
        if (!refreshState.begin("remote refresh")) {
            plugin.getSLF4JLogger().warn("Skipped remote refresh because another refresh task is already running.");
            return;
        }

        refreshRemoteStarted();
    }

    private void refreshRemoteStarted() {
        try {
            DatabaseRefreshResult result = refreshService.refreshIfChanged();

            if (result == DatabaseRefreshResult.UNCHANGED) {
                refreshState.markSuccess();
                plugin.getSLF4JLogger().debug("Remote database artifacts are unchanged.");
                return;
            }

            plugin.invalidateHeadRegistry();
            plugin.warmHeadRegistry();
            plugin.clearItemCache();
            plugin.clearSearchCache();
            refreshState.markSuccess();
            plugin.getSLF4JLogger().info("Refreshed database from remote artifacts.");
        } catch (Exception exception) {
            refreshState.markFailure(exception);
            logFailure("Failed to refresh remote database.", exception);
        }
    }

    private void schedulePeriodicRefresh() {
        long intervalMillis = config.scheduledRefreshInterval().toMillis();
        ScheduledTask task = plugin.getServer().getAsyncScheduler().runAtFixedRate(
                plugin,
                scheduledTask -> runScheduledRefresh(),
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );

        synchronized (scheduledTasks) {
            scheduledTasks.add(task);
        }

        plugin.getSLF4JLogger().debug(
                "Scheduled remote database refresh every {}.",
                config.scheduledRefreshInterval()
        );
    }

    private void runScheduledRefresh() {
        if (closed.get()) {
            return;
        }

        if (!refreshState.begin("scheduled remote refresh")) {
            plugin.getSLF4JLogger().debug("Skipped scheduled remote database refresh because another refresh operation is running.");
            return;
        }

        refreshRemoteStarted();
    }

    private void verifyRemoteStarted(@NotNull Consumer<DatabaseSnapshot> success, @NotNull Consumer<Throwable> failure) {
        try {
            DatabaseSnapshot snapshot = refreshService.verifyRemote();
            refreshState.finish();
            success.accept(snapshot);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            refreshState.finish();
            failure.accept(exception);
        } catch (Exception exception) {
            refreshState.finish();
            failure.accept(exception);
        }
    }

    private void logFailure(@NotNull String message, @NotNull Throwable throwable) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(throwable, "throwable");

        if (config.isDebug()) {
            plugin.getSLF4JLogger().warn(message, throwable);
            return;
        }

        String detail = throwable.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = throwable.getClass().getSimpleName();
        }

        plugin.getSLF4JLogger().warn("{} {}", message, detail);
    }

    private boolean runAsync(@NotNull String operation, @NotNull Runnable runnable) {
        if (closed.get()) {
            return false;
        }

        ScheduledTask task = plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> {
            if (closed.get()) {
                refreshState.finish();
                return;
            }

            try {
                runnable.run();
            } catch (Throwable throwable) {
                refreshState.markFailure(throwable);
                plugin.getSLF4JLogger().error("Unexpected error while trying to {}.", operation, throwable);
            }
        });

        synchronized (scheduledTasks) {
            scheduledTasks.add(task);
        }

        return true;
    }
}