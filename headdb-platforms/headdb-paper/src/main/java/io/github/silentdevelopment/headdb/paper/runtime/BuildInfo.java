package io.github.silentdevelopment.headdb.paper.runtime;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public record BuildInfo(@NotNull String version, @NotNull String channel, @Nullable String buildNumber, @Nullable String buildAttempt, @Nullable String runId, @Nullable String commit, @Nullable String fullCommit, @Nullable String branch, @Nullable String buildTime) {

    private static final String BUILD_PROPERTIES_RESOURCE = "META-INF/headdb-build.properties";
    private static final String GIT_PROPERTIES_RESOURCE = "git.properties";

    public static @NotNull BuildInfo read(@NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        Properties buildProperties = readProperties(plugin, BUILD_PROPERTIES_RESOURCE);
        Properties gitProperties = readProperties(plugin, GIT_PROPERTIES_RESOURCE);
        return from(plugin.getPluginMeta().getVersion(), buildProperties, gitProperties);
    }

    static @NotNull BuildInfo from(@NotNull String pluginVersion, @NotNull Properties buildProperties, @NotNull Properties gitProperties) {
        Objects.requireNonNull(pluginVersion, "pluginVersion");
        Objects.requireNonNull(buildProperties, "buildProperties");
        Objects.requireNonNull(gitProperties, "gitProperties");

        String channel = channelLabel(buildProperties.getProperty("headdb.build.channel"));
        String buildNumber = optionalValue(buildProperties.getProperty("headdb.build.number"));
        String buildAttempt = buildNumber == null ? null : optionalValue(buildProperties.getProperty("headdb.build.attempt"));
        String runId = buildNumber == null ? null : optionalValue(buildProperties.getProperty("headdb.build.run-id"));
        String fullCommit = firstPresent(buildProperties.getProperty("headdb.build.commit"), gitProperties.getProperty("git.commit.id.full"));
        String commit = firstPresent(gitProperties.getProperty("git.commit.id.abbrev"), abbreviate(fullCommit));
        String branch = firstPresent(buildProperties.getProperty("headdb.build.branch"), gitProperties.getProperty("git.branch"));
        String buildTime = firstPresent(buildProperties.getProperty("headdb.build.timestamp"), gitProperties.getProperty("git.build.time"));

        return new BuildInfo(pluginVersion, channel, buildNumber, buildAttempt, runId, commit, fullCommit, branch, buildTime);
    }

    public @NotNull String displayBuild() {
        if (buildNumber == null) {
            return channel;
        }

        if (buildAttempt == null || buildAttempt.equals("1")) {
            return buildNumber;
        }

        return buildNumber + "." + buildAttempt;
    }

    public boolean hasGitInfo() {
        return commit != null || fullCommit != null || branch != null || buildTime != null;
    }

    public boolean hasCiBuildInfo() {
        return channel.equalsIgnoreCase("ci") && buildNumber != null;
    }

    private static @NotNull Properties readProperties(@NotNull JavaPlugin plugin, @NotNull String resource) {
        Properties properties = new Properties();

        try (InputStream input = plugin.getResource(resource)) {
            if (input == null) {
                return properties;
            }

            properties.load(input);
        } catch (IOException ignored) {
            // Build information is optional.
        }

        return properties;
    }

    private static @NotNull String channelLabel(@Nullable String value) {
        String normalized = optionalValue(value);
        if (normalized == null) {
            return "Local";
        }

        if (normalized.equalsIgnoreCase("ci")) {
            return "CI";
        }

        if (normalized.length() == 1) {
            return normalized.toUpperCase(Locale.ROOT);
        }

        return Character.toUpperCase(normalized.charAt(0))
                + normalized.substring(1).toLowerCase(Locale.ROOT);
    }

    private static @Nullable String firstPresent(@Nullable String first, @Nullable String second) {
        String normalizedFirst = optionalValue(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }

        return optionalValue(second);
    }

    private static @Nullable String abbreviate(@Nullable String value) {
        String normalized = optionalValue(value);
        if (normalized == null) {
            return null;
        }

        if (normalized.length() <= 7) {
            return normalized;
        }

        return normalized.substring(0, 7);
    }

    private static @Nullable String optionalValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("unknown") || normalized.equalsIgnoreCase("local")) {
            return null;
        }

        return normalized;
    }
}
