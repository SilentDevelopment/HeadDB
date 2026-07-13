package io.github.silentdevelopment.headdb.paper.runtime;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RuntimeDiagnostics {

    private RuntimeDiagnostics() {
    }

    public static void logConfig(@NotNull HeadDBPlugin plugin, @NotNull PluginConfig config) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");

        plugin.getSLF4JLogger().debug(
                "Config: manifest={}, preferredMirror={}, loadCacheOnStartup={}, refreshOnStartup={}, scheduledRefreshEnabled={}, scheduledRefreshInterval={}, startupChecks={}",
                config.remoteManifestUri(),
                config.preferredMirrorId(),
                config.loadCacheOnStartup(),
                config.refreshOnStartup(),
                config.scheduledRefreshEnabled(),
                config.scheduledRefreshInterval(),
                config.isDebug()
        );
    }

    public static void logRuntimeState(@NotNull HeadDBPlugin plugin, @NotNull PluginRuntime runtime) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(runtime, "runtime");

        DatabaseStatus status = runtime.database().status();
        DatabaseStats stats = runtime.database().stats();

        plugin.getSLF4JLogger().debug(
                "Database: state={}, source={}, heads={}, categories={}, tags={}, collections={}, revocations={}",
                status.state(),
                status.source(),
                stats.heads(),
                stats.categories(),
                stats.tags(),
                stats.collections(),
                stats.revocations()
        );

        if (status.lastError() != null && !status.lastError().isBlank()) {
            plugin.getSLF4JLogger().warn("Database last error: {}", status.lastError());
        }
    }
}