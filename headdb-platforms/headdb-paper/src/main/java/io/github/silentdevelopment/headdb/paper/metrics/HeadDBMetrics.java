package io.github.silentdevelopment.headdb.paper.metrics;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class HeadDBMetrics {

    private static final int BSTATS_PLUGIN_ID = 9152;
    private static Metrics metrics;

    private HeadDBMetrics() {}

    public static synchronized void register(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (metrics != null) {
            metrics.shutdown();
        }

        metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
    }

    public static synchronized void unregister(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (metrics == null) {
            return;
        }

        metrics.shutdown();
        metrics = null;
    }

}
