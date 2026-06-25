package io.github.silentdevelopment.headdb.paper.updater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

final class HeadDBPluginJarMetadata {

    static final String PLUGIN_NAME = "HeadDB";
    static final String PLUGIN_MAIN_CLASS = "io.github.silentdevelopment.headdb.paper.HeadDBPlugin";

    private final String name;
    private final String mainClass;
    private final String paperPluginVersion;
    private final String buildVersion;

    private HeadDBPluginJarMetadata(@Nullable String name, @Nullable String mainClass, @Nullable String paperPluginVersion, @Nullable String buildVersion) {
        this.name = name;
        this.mainClass = mainClass;
        this.paperPluginVersion = paperPluginVersion;
        this.buildVersion = buildVersion;
    }

    static @NotNull HeadDBPluginJarMetadata read(@NotNull Path jar) throws IOException {
        Objects.requireNonNull(jar, "jar");

        if (!Files.isRegularFile(jar)) {
            throw new IOException("Plugin jar is not a regular file: " + jar);
        }

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return read(jarFile);
        }
    }

    static @NotNull HeadDBPluginJarMetadata read(@NotNull JarFile jarFile) throws IOException {
        Objects.requireNonNull(jarFile, "jarFile");

        PaperPluginYaml paperPluginYaml = readPaperPluginYaml(jarFile);
        String buildVersion = readGitPropertiesVersion(jarFile);
        return new HeadDBPluginJarMetadata(paperPluginYaml.name(), paperPluginYaml.mainClass(), paperPluginYaml.version(), buildVersion);
    }

    void validateDownloadedUpdate(@NotNull String expectedVersion) throws IOException {
        Objects.requireNonNull(expectedVersion, "expectedVersion");

        if (!PLUGIN_NAME.equals(name)) {
            throw new IOException("Downloaded jar plugin name is not " + PLUGIN_NAME + ".");
        }

        if (!PLUGIN_MAIN_CLASS.equals(mainClass)) {
            throw new IOException("Downloaded jar main class is not " + PLUGIN_MAIN_CLASS + ".");
        }

        String actualVersion = preferredVersion();

        if (actualVersion == null || actualVersion.isBlank()) {
            throw new IOException("Downloaded jar version could not be verified.");
        }

        HeadDBVersion expected = HeadDBVersion.parse(expectedVersion);
        HeadDBVersion actual = HeadDBVersion.parse(actualVersion);

        if (actual.compareTo(expected) == 0) {
            return;
        }

        throw new IOException("Downloaded jar version " + actualVersion + " does not match expected release version " + expectedVersion + ".");
    }

    @Nullable String preferredVersion() {
        if (buildVersion != null && !buildVersion.isBlank()) {
            return buildVersion;
        }

        return paperPluginVersion;
    }

    @Nullable String name() {
        return name;
    }

    @Nullable String mainClass() {
        return mainClass;
    }

    @Nullable String paperPluginVersion() {
        return paperPluginVersion;
    }

    @Nullable String buildVersion() {
        return buildVersion;
    }

    private static @NotNull PaperPluginYaml readPaperPluginYaml(@NotNull JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry("paper-plugin.yml");

        if (entry == null) {
            throw new IOException("Plugin jar does not contain paper-plugin.yml.");
        }

        String name = null;
        String mainClass = null;
        String version = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String stripped = stripComment(line).trim();

                if (stripped.isBlank()) {
                    continue;
                }

                if (stripped.startsWith("name:")) {
                    name = scalarValue(stripped, "name");
                    continue;
                }

                if (stripped.startsWith("main:")) {
                    mainClass = scalarValue(stripped, "main");
                    continue;
                }

                if (stripped.startsWith("version:")) {
                    version = scalarValue(stripped, "version");
                }
            }
        }

        return new PaperPluginYaml(name, mainClass, version);
    }

    private static @Nullable String readGitPropertiesVersion(@NotNull JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry("git.properties");

        if (entry == null) {
            return null;
        }

        Properties properties = new Properties();

        try (InputStream input = jarFile.getInputStream(entry)) {
            properties.load(input);
        }

        String version = properties.getProperty("headdb.build.version");
        return version == null || version.isBlank() ? null : version.trim();
    }

    private static @NotNull String scalarValue(@NotNull String line, @NotNull String key) {
        String value = line.substring((key + ":").length()).trim();

        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1).trim();
        }

        return value;
    }

    private static @NotNull String stripComment(@NotNull String line) {
        int commentIndex = line.indexOf('#');

        if (commentIndex < 0) {
            return line;
        }

        return line.substring(0, commentIndex);
    }

    private record PaperPluginYaml(@Nullable String name, @Nullable String mainClass, @Nullable String version) {
    }

}
