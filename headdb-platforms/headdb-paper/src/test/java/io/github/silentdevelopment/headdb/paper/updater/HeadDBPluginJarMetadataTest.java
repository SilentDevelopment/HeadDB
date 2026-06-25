package io.github.silentdevelopment.headdb.paper.updater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeadDBPluginJarMetadataTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void readsPaperPluginAndGitMetadata() throws Exception {
        Path jar = writeJar("HeadDB", HeadDBPluginJarMetadata.PLUGIN_MAIN_CLASS, "7.0.0-rc.2", "7.0.0-rc.2+build.5");
        HeadDBPluginJarMetadata metadata = HeadDBPluginJarMetadata.read(jar);

        assertEquals("HeadDB", metadata.name());
        assertEquals(HeadDBPluginJarMetadata.PLUGIN_MAIN_CLASS, metadata.mainClass());
        assertEquals("7.0.0-rc.2", metadata.paperPluginVersion());
        assertEquals("7.0.0-rc.2+build.5", metadata.buildVersion());
        assertEquals("7.0.0-rc.2+build.5", metadata.preferredVersion());
    }

    @Test
    void validatesMatchingDownloadedUpdate() throws Exception {
        Path jar = writeJar("HeadDB", HeadDBPluginJarMetadata.PLUGIN_MAIN_CLASS, "7.0.0-rc.2", "7.0.0-rc.2");
        HeadDBPluginJarMetadata.read(jar).validateDownloadedUpdate("v7.0.0-rc.2");
    }

    @Test
    void rejectsWrongPluginName() throws Exception {
        Path jar = writeJar("OtherPlugin", HeadDBPluginJarMetadata.PLUGIN_MAIN_CLASS, "7.0.0-rc.2", "7.0.0-rc.2");
        HeadDBPluginJarMetadata metadata = HeadDBPluginJarMetadata.read(jar);

        assertThrows(IOException.class, () -> metadata.validateDownloadedUpdate("7.0.0-rc.2"));
    }

    @Test
    void rejectsWrongMainClass() throws Exception {
        Path jar = writeJar("HeadDB", "example.Plugin", "7.0.0-rc.2", "7.0.0-rc.2");
        HeadDBPluginJarMetadata metadata = HeadDBPluginJarMetadata.read(jar);

        assertThrows(IOException.class, () -> metadata.validateDownloadedUpdate("7.0.0-rc.2"));
    }

    @Test
    void rejectsMismatchedVersion() throws Exception {
        Path jar = writeJar("HeadDB", HeadDBPluginJarMetadata.PLUGIN_MAIN_CLASS, "7.0.0-rc.1", "7.0.0-rc.1");
        HeadDBPluginJarMetadata metadata = HeadDBPluginJarMetadata.read(jar);

        assertThrows(IOException.class, () -> metadata.validateDownloadedUpdate("7.0.0-rc.2"));
    }

    private Path writeJar(String name, String mainClass, String paperVersion, String buildVersion) throws IOException {
        Path jar = tempDirectory.resolve(name + "-" + paperVersion.replace('+', '-') + ".jar");

        try (JarOutputStream output = new JarOutputStream(java.nio.file.Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("paper-plugin.yml"));
            output.write(("name: \"" + name + "\"\nmain: " + mainClass + "\nversion: '" + paperVersion + "'\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();

            if (buildVersion == null) {
                return jar;
            }

            output.putNextEntry(new JarEntry("git.properties"));
            output.write(("headdb.build.version=" + buildVersion + "\n").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        return jar;
    }

}
