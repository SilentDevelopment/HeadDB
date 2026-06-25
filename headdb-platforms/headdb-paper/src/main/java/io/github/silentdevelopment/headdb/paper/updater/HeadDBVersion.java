package io.github.silentdevelopment.headdb.paper.updater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeadDBVersion implements Comparable<HeadDBVersion> {

    private static final Pattern BUILD_PATTERN = Pattern.compile("(?:^|[.\\-+])build[.\\-]?(\\d+)(?:$|[.\\-])", Pattern.CASE_INSENSITIVE);

    private final String raw;
    private final List<Integer> numbers;
    private final List<String> prereleaseParts;
    private final Integer buildNumber;

    private HeadDBVersion(@NotNull String raw, @NotNull List<Integer> numbers, @NotNull List<String> prereleaseParts, @Nullable Integer buildNumber) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.numbers = List.copyOf(numbers);
        this.prereleaseParts = List.copyOf(prereleaseParts);
        this.buildNumber = buildNumber;
    }

    public static @NotNull HeadDBVersion parse(@NotNull String value) {
        Objects.requireNonNull(value, "value");

        String normalized = normalize(value);
        String withoutBuild = normalized;
        String buildMetadata = "";
        int buildIndex = normalized.indexOf('+');

        if (buildIndex >= 0) {
            withoutBuild = normalized.substring(0, buildIndex);
            buildMetadata = normalized.substring(buildIndex + 1);
        }

        String numericPart = withoutBuild;
        String prereleasePart = "";
        int prereleaseIndex = withoutBuild.indexOf('-');

        if (prereleaseIndex >= 0) {
            numericPart = withoutBuild.substring(0, prereleaseIndex);
            prereleasePart = withoutBuild.substring(prereleaseIndex + 1);
        }

        List<Integer> numbers = parseNumbers(numericPart);
        List<String> prereleaseParts = parseIdentifiers(prereleasePart);
        Integer buildNumber = parseBuildNumber(buildMetadata);

        return new HeadDBVersion(normalized, numbers, prereleaseParts, buildNumber);
    }

    public @NotNull String raw() {
        return raw;
    }

    public boolean hasBuildNumber() {
        return buildNumber != null;
    }

    public @Nullable Integer buildNumber() {
        return buildNumber;
    }

    public @NotNull UpdateKind updateKindComparedTo(@NotNull HeadDBVersion current, boolean includeBuilds) {
        Objects.requireNonNull(current, "current");

        int versionComparison = compareVersionWithoutBuild(current);

        if (versionComparison > 0) {
            return UpdateKind.VERSION;
        }

        if (versionComparison < 0) {
            return UpdateKind.NONE;
        }

        if (!includeBuilds) {
            return UpdateKind.NONE;
        }

        if (current.hasBuildNumber() && !hasBuildNumber()) {
            return UpdateKind.VERSION;
        }

        if (!current.hasBuildNumber() || !hasBuildNumber()) {
            return UpdateKind.NONE;
        }

        if (buildNumber > current.buildNumber) {
            return UpdateKind.BUILD;
        }

        return UpdateKind.NONE;
    }

    @Override
    public int compareTo(@NotNull HeadDBVersion other) {
        Objects.requireNonNull(other, "other");

        int versionComparison = compareVersionWithoutBuild(other);

        if (versionComparison != 0) {
            return versionComparison;
        }

        if (buildNumber == null && other.buildNumber == null) {
            return 0;
        }

        if (buildNumber == null) {
            return -1;
        }

        if (other.buildNumber == null) {
            return 1;
        }

        return Integer.compare(buildNumber, other.buildNumber);
    }

    private int compareVersionWithoutBuild(@NotNull HeadDBVersion other) {
        int maxNumbers = Math.max(numbers.size(), other.numbers.size());

        for (int index = 0; index < maxNumbers; index++) {
            int left = numberAt(numbers, index);
            int right = numberAt(other.numbers, index);
            int comparison = Integer.compare(left, right);

            if (comparison != 0) {
                return comparison;
            }
        }

        if (prereleaseParts.isEmpty() && other.prereleaseParts.isEmpty()) {
            return 0;
        }

        if (prereleaseParts.isEmpty()) {
            return 1;
        }

        if (other.prereleaseParts.isEmpty()) {
            return -1;
        }

        int maxPrerelease = Math.max(prereleaseParts.size(), other.prereleaseParts.size());

        for (int index = 0; index < maxPrerelease; index++) {
            String left = partAt(prereleaseParts, index);
            String right = partAt(other.prereleaseParts, index);

            if (left.isEmpty() && right.isEmpty()) {
                return 0;
            }

            if (left.isEmpty()) {
                return -1;
            }

            if (right.isEmpty()) {
                return 1;
            }

            int comparison = compareIdentifier(left, right);

            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

    private static @NotNull String normalize(@NotNull String value) {
        String normalized = value.trim();

        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private static @NotNull List<Integer> parseNumbers(@NotNull String value) {
        List<Integer> numbers = new ArrayList<>();

        for (String part : value.split("\\.")) {
            if (part.isBlank()) {
                numbers.add(0);
                continue;
            }

            try {
                numbers.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                numbers.add(0);
            }
        }

        while (numbers.size() < 3) {
            numbers.add(0);
        }

        return numbers;
    }

    private static @NotNull List<String> parseIdentifiers(@NotNull String value) {
        List<String> identifiers = new ArrayList<>();

        if (value.isBlank()) {
            return identifiers;
        }

        for (String part : value.split("[.\\-]")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);

            if (!normalized.isBlank()) {
                identifiers.add(normalized);
            }
        }

        return identifiers;
    }

    private static @Nullable Integer parseBuildNumber(@NotNull String metadata) {
        if (metadata.isBlank()) {
            return null;
        }

        Matcher matcher = BUILD_PATTERN.matcher(metadata);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int numberAt(@NotNull List<Integer> numbers, int index) {
        if (index >= numbers.size()) {
            return 0;
        }

        return numbers.get(index);
    }

    private static @NotNull String partAt(@NotNull List<String> parts, int index) {
        if (index >= parts.size()) {
            return "";
        }

        return parts.get(index);
    }

    private static int compareIdentifier(@NotNull String left, @NotNull String right) {
        boolean leftNumeric = isNumeric(left);
        boolean rightNumeric = isNumeric(right);

        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }

        if (leftNumeric) {
            return -1;
        }

        if (rightNumeric) {
            return 1;
        }

        return left.compareTo(right);
    }

    private static boolean isNumeric(@NotNull String value) {
        if (value.isBlank()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }

        return true;
    }

}
