package io.github.silentdevelopment.headdb.paper.updater;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class UpdaterUserAgent {

    private static final String USER_AGENT = "HeadDB-Updater";

    private UpdaterUserAgent() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull String create(@NotNull HeadDBPlugin plugin, @NotNull String pluginVersion) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(pluginVersion, "pluginVersion");

        String version = sanitize(pluginVersion);
        String osName = systemProperty("os.name");
        String osVersion = systemProperty("os.version");
        String osArchitecture = systemProperty("os.arch");
        String javaVersion = systemProperty("java.version");
        String serverName = sanitize(plugin.getServer().getName());
        String serverVersion = sanitize(plugin.getServer().getVersion());

        return USER_AGENT + "/" + version
                + " (" + osName + " " + osVersion
                + "; " + osArchitecture
                + "; Java " + javaVersion
                + "; " + serverName + " " + serverVersion
                + ")";
    }

    private static @NotNull String systemProperty(@NotNull String name) {
        return sanitize(System.getProperty(name, "unknown"));
    }

    private static @NotNull String sanitize(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean previousWhitespace = false;

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);

            if (character < 0x20 || character > 0x7E || character == '(' || character == ')' || character == ';') {
                result.append('_');
                previousWhitespace = false;
                continue;
            }

            if (Character.isWhitespace(character)) {
                if (!previousWhitespace) {
                    result.append(' ');
                }

                previousWhitespace = true;
                continue;
            }

            result.append(character);
            previousWhitespace = false;
        }

        String normalized = result.toString().trim();

        if (normalized.isBlank()) {
            return "unknown";
        }

        return normalized;
    }
}
