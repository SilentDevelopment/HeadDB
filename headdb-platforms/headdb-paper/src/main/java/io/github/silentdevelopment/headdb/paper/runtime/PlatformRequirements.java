package io.github.silentdevelopment.headdb.paper.runtime;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformRequirements {

    public static final int REQUIRED_JAVA_FEATURE = 25;
    public static final int REQUIRED_PAPER_MAJOR = 26;
    public static final String REQUIRED_PAPER_VERSION = "26.1.2+";

    private static final Pattern VERSION_TOKEN = Pattern.compile("(?<!\\d)(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?!\\d)");

    private PlatformRequirements() {
    }

    public static boolean supported(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        Compatibility compatibility = inspect(plugin);

        if (compatibility.supported()) {
            return true;
        }

        plugin.getSLF4JLogger().error(
                "HeadDB requires Paper {} and Java {}+.",
                REQUIRED_PAPER_VERSION,
                REQUIRED_JAVA_FEATURE
        );
        plugin.getSLF4JLogger().error(
                "Detected server: {}",
                compatibility.serverVersion()
        );
        plugin.getSLF4JLogger().error(
                "Detected Bukkit API: {}",
                compatibility.bukkitVersion()
        );
        plugin.getSLF4JLogger().error(
                "Detected Java: {}",
                compatibility.javaVersion()
        );

        if (!compatibility.javaSupported()) {
            plugin.getSLF4JLogger().error(
                    "Unsupported Java runtime. Install Java {} or newer.",
                    REQUIRED_JAVA_FEATURE
            );
        }

        if (!compatibility.paperSupported()) {
            plugin.getSLF4JLogger().error(
                    "Unsupported Paper runtime. Older Paper builds cannot safely load HeadDB's Java 25 runtime. Update to Paper {}.",
                    REQUIRED_PAPER_VERSION
            );
        }

        return false;
    }

    public static @NotNull Compatibility inspect(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        int javaFeature = Runtime.version().feature();
        String javaVersion = Runtime.version().toString();
        String serverVersion = value(plugin.getServer().getVersion());
        String bukkitVersion = value(plugin.getServer().getBukkitVersion());

        boolean javaSupported = javaFeature >= REQUIRED_JAVA_FEATURE;
        boolean paperSupported = paper26OrNewer(serverVersion + " " + bukkitVersion);

        return new Compatibility(
                javaFeature,
                javaVersion,
                serverVersion,
                bukkitVersion,
                javaSupported,
                paperSupported
        );
    }

    private static boolean paper26OrNewer(@NotNull String version) {
        Matcher matcher = VERSION_TOKEN.matcher(version);

        while (matcher.find()) {
            int major = parseInt(matcher.group(1));

            if (major >= REQUIRED_PAPER_MAJOR) {
                return true;
            }
        }

        return false;
    }

    private static int parseInt(@NotNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static @NotNull String value(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }

    public record Compatibility(
            int javaFeature,
            @NotNull String javaVersion,
            @NotNull String serverVersion,
            @NotNull String bukkitVersion,
            boolean javaSupported,
            boolean paperSupported
    ) {

        public boolean supported() {
            return javaSupported && paperSupported;
        }

    }

}