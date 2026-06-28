package io.github.silentdevelopment.headdb.paper.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public final class GitHubReleaseUpdateChecker {

    public static final String REPOSITORY = "SilentDevelopment/HeadDB";

    private static final int MAX_RELEASES = 50;

    private final HttpClient httpClient;
    private final Duration readTimeout;
    private final String userAgent;

    public GitHubReleaseUpdateChecker(
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            @NotNull String userAgent
    ) {
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(connectTimeout)
                .build();
    }

    public @NotNull UpdateCheckResult check(
            @NotNull String currentVersion,
            boolean includePrereleases,
            boolean includeBuilds
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(currentVersion, "currentVersion");

        HeadDBVersion current = HeadDBVersion.parse(currentVersion);
        JsonArray releases = fetchReleases();
        GitHubRelease bestRelease = null;
        UpdateKind bestKind = UpdateKind.NONE;

        for (JsonElement element : releases) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();

            if (booleanValue(object, "draft")) {
                continue;
            }

            boolean prerelease = booleanValue(object, "prerelease");

            if (prerelease && !includePrereleases) {
                continue;
            }

            String tagName = stringValue(object, "tag_name");

            if (tagName == null || tagName.isBlank()) {
                continue;
            }

            if (isBuildReleaseTag(tagName)) {
                continue;
            }

            HeadDBVersion candidateVersion = HeadDBVersion.parse(tagName);
            UpdateKind candidateKind = candidateVersion.updateKindComparedTo(current, false);

            if (candidateKind == UpdateKind.NONE) {
                continue;
            }

            GitHubRelease release = releaseFrom(object, tagName, prerelease, candidateVersion);

            if (bestRelease == null || release.version().compareTo(bestRelease.version()) > 0) {
                bestRelease = release;
                bestKind = candidateKind;
            }
        }

        if (bestRelease == null) {
            return UpdateCheckResult.upToDate(currentVersion, Instant.now());
        }

        return UpdateCheckResult.available(currentVersion, Instant.now(), bestRelease, bestKind);
    }

    private @NotNull JsonArray fetchReleases() throws IOException, InterruptedException {
        URI uri = URI.create("https://api.github.com/repos/" + encodeRepository(REPOSITORY) + "/releases?per_page=" + MAX_RELEASES);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub releases request failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonElement parsed = JsonParser.parseString(response.body());

        if (!parsed.isJsonArray()) {
            throw new IOException("GitHub releases response was not a JSON array.");
        }

        return parsed.getAsJsonArray();
    }

    private static @NotNull GitHubRelease releaseFrom(
            @NotNull JsonObject object,
            @NotNull String tagName,
            boolean prerelease,
            @NotNull HeadDBVersion version
    ) {
        String name = stringValue(object, "name");

        if (name == null || name.isBlank()) {
            name = tagName;
        }

        String htmlUrl = stringValue(object, "html_url");

        if (htmlUrl == null || htmlUrl.isBlank()) {
            htmlUrl = "https://github.com/" + REPOSITORY + "/releases/tag/" + URLEncoder.encode(tagName, StandardCharsets.UTF_8);
        }

        ReleaseAsset asset = pluginAsset(object);

        return new GitHubRelease(tagName, name, htmlUrl, prerelease, instantValue(object, "published_at"), version, asset.name(), asset.downloadUrl());
    }

    private static @NotNull ReleaseAsset pluginAsset(@NotNull JsonObject release) {
        JsonElement assetsElement = release.get("assets");

        if (assetsElement == null || !assetsElement.isJsonArray()) {
            return ReleaseAsset.empty();
        }

        ReleaseAsset fallback = ReleaseAsset.empty();
        JsonArray assets = assetsElement.getAsJsonArray();

        for (JsonElement assetElement : assets) {
            if (!assetElement.isJsonObject()) {
                continue;
            }

            JsonObject asset = assetElement.getAsJsonObject();
            String name = stringValue(asset, "name");
            String downloadUrl = stringValue(asset, "browser_download_url");

            if (name == null || downloadUrl == null) {
                continue;
            }

            String normalized = name.toLowerCase(Locale.ROOT);

            if (!normalized.endsWith(".jar")) {
                continue;
            }

            if (normalized.contains("sources") || normalized.contains("javadoc") || normalized.contains("original")) {
                continue;
            }

            ReleaseAsset candidate = new ReleaseAsset(name, downloadUrl);

            if (fallback.downloadUrl() == null) {
                fallback = candidate;
            }

            if (normalized.contains("headdb") || normalized.equals("headdb.jar")) {
                return candidate;
            }
        }

        return fallback;
    }

    private static boolean isBuildReleaseTag(@NotNull String tagName) {
        return tagName.toLowerCase(Locale.ROOT).contains("+build.");
    }

    private static @NotNull String encodeRepository(@NotNull String repository) {
        String normalized = repository.trim();
        int slash = normalized.indexOf('/');

        if (slash < 1 || slash == normalized.length() - 1) {
            return URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        }

        String owner = URLEncoder.encode(normalized.substring(0, slash), StandardCharsets.UTF_8);
        String name = URLEncoder.encode(normalized.substring(slash + 1), StandardCharsets.UTF_8);
        return owner + "/" + name;
    }

    private static boolean booleanValue(@NotNull JsonObject object, @NotNull String key) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return false;
        }

        return element.getAsBoolean();
    }

    private static @Nullable String stringValue(@NotNull JsonObject object, @NotNull String key) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return null;
        }

        return element.getAsString();
    }

    private static @Nullable Instant instantValue(@NotNull JsonObject object, @NotNull String key) {
        String value = stringValue(object, key);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record ReleaseAsset(@Nullable String name, @Nullable String downloadUrl) {

        private static @NotNull ReleaseAsset empty() {
            return new ReleaseAsset(null, null);
        }

    }

}