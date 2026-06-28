package io.github.silentdevelopment.headdb.paper.config;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ConfigDefaultsMerger {

    private static final List<NestedDefault> DEFAULTS = List.of(
            new NestedDefault("update-checker", "enabled", "true"),
            new NestedDefault("update-checker", "check-on-startup", "true"),
            new NestedDefault("update-checker", "notify-console", "true"),
            new NestedDefault("update-checker", "notify-admins", "true"),
            new NestedDefault("update-checker", "include-prereleases", "true"),
            new NestedDefault("update-checker", "include-builds", "true"),
            new NestedDefault("auto-updater", "install-updates", "false")
    );

    private ConfigDefaultsMerger() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    static @NotNull String mergeMissingDefaults(@NotNull String content) {
        Objects.requireNonNull(content, "content");

        String lineSeparator = detectLineSeparator(content);
        boolean trailingLineSeparator = content.endsWith("\n") || content.endsWith("\r");
        List<String> lines = splitLines(content);
        boolean changed = false;

        for (NestedDefault nestedDefault : DEFAULTS) {
            changed |= addMissingNestedKey(lines, nestedDefault.section(), nestedDefault.key(), nestedDefault.value());
        }

        if (!changed) {
            return content;
        }

        String merged = String.join(lineSeparator, lines);

        if (!trailingLineSeparator) {
            return merged;
        }

        return merged + lineSeparator;
    }

    private static @NotNull String detectLineSeparator(@NotNull String content) {
        if (content.contains("\r\n")) {
            return "\r\n";
        }

        return "\n";
    }

    private static @NotNull List<String> splitLines(@NotNull String content) {
        String[] split = content.split("\\R", -1);
        int length = split.length;

        if (length > 0 && split[length - 1].isEmpty() && (content.endsWith("\n") || content.endsWith("\r"))) {
            length--;
        }

        List<String> lines = new ArrayList<>(length);

        for (int index = 0; index < length; index++) {
            lines.add(split[index]);
        }

        return lines;
    }

    private static boolean addMissingNestedKey(@NotNull List<String> lines, @NotNull String section, @NotNull String key, @NotNull String value) {
        if (hasNestedKey(lines, section, key)) {
            return false;
        }

        addNestedKey(lines, section, key, value);
        return true;
    }

    private static boolean hasNestedKey(@NotNull List<String> lines, @NotNull String section, @NotNull String key) {
        boolean inSection = false;

        for (String line : lines) {
            String stripped = stripComment(line);

            if (stripped.isBlank()) {
                continue;
            }

            if (isTopLevel(line)) {
                inSection = stripped.trim().equals(section + ":");
                continue;
            }

            if (!inSection) {
                continue;
            }

            if (stripped.trim().startsWith(key + ":")) {
                return true;
            }
        }

        return false;
    }

    private static void addNestedKey(@NotNull List<String> lines, @NotNull String section, @NotNull String key, @NotNull String value) {
        int sectionIndex = findTopLevelSection(lines, section);

        if (sectionIndex < 0) {
            ensureBlankLine(lines);
            lines.add(section + ":");
            lines.add("  " + key + ": " + value);
            return;
        }

        int insertIndex = findSectionEnd(lines, sectionIndex);
        lines.add(insertIndex, "  " + key + ": " + value);
    }

    private static int findTopLevelSection(@NotNull List<String> lines, @NotNull String section) {
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);

            if (!isTopLevel(line)) {
                continue;
            }

            if (stripComment(line).trim().equals(section + ":")) {
                return index;
            }
        }

        return -1;
    }

    private static int findSectionEnd(@NotNull List<String> lines, int sectionIndex) {
        for (int index = sectionIndex + 1; index < lines.size(); index++) {
            String line = lines.get(index);

            if (line.isBlank()) {
                continue;
            }

            if (isTopLevel(line)) {
                return index;
            }
        }

        return lines.size();
    }

    private static boolean isTopLevel(@NotNull String line) {
        return !line.startsWith(" ") && !line.startsWith("\t");
    }

    private static void ensureBlankLine(@NotNull List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }

        String last = lines.get(lines.size() - 1);

        if (!last.isBlank()) {
            lines.add("");
        }
    }

    private static @NotNull String stripComment(@NotNull String line) {
        int commentIndex = line.indexOf('#');

        if (commentIndex < 0) {
            return line;
        }

        return line.substring(0, commentIndex);
    }

    private record NestedDefault(@NotNull String section, @NotNull String key, @NotNull String value) {
    }

}
