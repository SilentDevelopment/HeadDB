package io.github.silentdevelopment.headdb.paper.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsMergerTest {

    @Test
    void appendsMissingUpdaterSections() {
        String input = "remote:\n  manifest-url: https://data.headsdb.com/manifest.json\n";
        String merged = ConfigDefaultsMerger.mergeMissingDefaults(input);

        assertTrue(merged.contains("update-checker:\n"));
        assertTrue(merged.contains("  enabled: true\n"));
        assertTrue(merged.contains("  include-builds: true\n"));
        assertTrue(merged.contains("auto-updater:\n"));
        assertTrue(merged.contains("  install-updates: false\n"));
    }

    @Test
    void completesPartialExistingSectionWithoutDuplicatingKeys() {
        String input = "update-checker:\n  enabled: false\n\ndebug: false\n";
        String merged = ConfigDefaultsMerger.mergeMissingDefaults(input);

        assertEquals(1, count(merged, "  enabled:"));
        assertTrue(merged.contains("  check-on-startup: true\n"));
        assertTrue(merged.indexOf("  include-builds: true") < merged.indexOf("debug: false"));
    }

    @Test
    void preservesCrlfLineSeparators() {
        String input = "update-checker:\r\n  enabled: false\r\n";
        String merged = ConfigDefaultsMerger.mergeMissingDefaults(input);

        assertTrue(merged.contains("\r\n  check-on-startup: true\r\n"));
    }

    @Test
    void leavesCompleteConfigUnchanged() {
        String input = "update-checker:\n  enabled: true\n  check-on-startup: true\n  notify-console: true\n  notify-admins: true\n  include-prereleases: true\n  include-builds: true\nauto-updater:\n  install-updates: false\n";
        String merged = ConfigDefaultsMerger.mergeMissingDefaults(input);

        assertEquals(input, merged);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);

        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }

        return count;
    }

}
