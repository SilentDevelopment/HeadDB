package io.github.silentdevelopment.headdb.paper.runtime;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildInfoTest {

    @Test
    void readsCiMetadataWithoutChangingPluginVersion() {
        Properties buildProperties = new Properties();
        buildProperties.setProperty("headdb.build.channel", "ci");
        buildProperties.setProperty("headdb.build.number", "412");
        buildProperties.setProperty("headdb.build.attempt", "2");
        buildProperties.setProperty("headdb.build.run-id", "22018457391");
        buildProperties.setProperty("headdb.build.commit", "45733cd4e64b227ade4f42c30f6b4bdd27e5d88d");
        buildProperties.setProperty("headdb.build.branch", "master");
        buildProperties.setProperty("headdb.build.timestamp", "2026-07-24T21:15:00Z");

        Properties gitProperties = new Properties();
        gitProperties.setProperty("git.commit.id.abbrev", "45733cd");
        gitProperties.setProperty("git.commit.id.full", "45733cd4e64b227ade4f42c30f6b4bdd27e5d88d");

        BuildInfo buildInfo = BuildInfo.from("7.0.0-rc.7", buildProperties, gitProperties);

        assertEquals("7.0.0-rc.7", buildInfo.version());
        assertEquals("CI", buildInfo.channel());
        assertEquals("412", buildInfo.buildNumber());
        assertEquals("2", buildInfo.buildAttempt());
        assertEquals("22018457391", buildInfo.runId());
        assertEquals("412.2", buildInfo.displayBuild());
        assertEquals("45733cd", buildInfo.commit());
        assertEquals("45733cd4e64b227ade4f42c30f6b4bdd27e5d88d", buildInfo.fullCommit());
        assertEquals("master", buildInfo.branch());
        assertEquals("2026-07-24T21:15:00Z", buildInfo.buildTime());
        assertTrue(buildInfo.hasCiBuildInfo());
        assertTrue(buildInfo.hasGitInfo());
    }

    @Test
    void usesSafeLocalDefaults() {
        Properties buildProperties = new Properties();
        buildProperties.setProperty("headdb.build.channel", "local");
        buildProperties.setProperty("headdb.build.number", "local");
        buildProperties.setProperty("headdb.build.attempt", "1");
        buildProperties.setProperty("headdb.build.run-id", "local");
        buildProperties.setProperty("headdb.build.commit", "unknown");
        buildProperties.setProperty("headdb.build.branch", "local");
        buildProperties.setProperty("headdb.build.timestamp", "unknown");

        BuildInfo buildInfo = BuildInfo.from("7.0.0-rc.7", buildProperties, new Properties());

        assertEquals("7.0.0-rc.7", buildInfo.version());
        assertEquals("Local", buildInfo.channel());
        assertEquals("Local", buildInfo.displayBuild());
        assertNull(buildInfo.buildNumber());
        assertNull(buildInfo.buildAttempt());
        assertNull(buildInfo.runId());
        assertFalse(buildInfo.hasCiBuildInfo());
        assertFalse(buildInfo.hasGitInfo());
    }

    @Test
    void fallsBackToGitMetadata() {
        Properties gitProperties = new Properties();
        gitProperties.setProperty("git.commit.id.abbrev", "abcdef1");
        gitProperties.setProperty("git.commit.id.full", "abcdef1234567890");
        gitProperties.setProperty("git.branch", "feature/build-info");
        gitProperties.setProperty("git.build.time", "2026-07-24T22:00:00Z");

        BuildInfo buildInfo = BuildInfo.from("7.0.0", new Properties(), gitProperties);

        assertEquals("abcdef1", buildInfo.commit());
        assertEquals("abcdef1234567890", buildInfo.fullCommit());
        assertEquals("feature/build-info", buildInfo.branch());
        assertEquals("2026-07-24T22:00:00Z", buildInfo.buildTime());
    }
}
