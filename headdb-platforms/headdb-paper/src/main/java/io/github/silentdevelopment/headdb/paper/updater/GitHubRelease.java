package io.github.silentdevelopment.headdb.paper.updater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

public record GitHubRelease(
        @NotNull String tagName,
        @NotNull String name,
        @NotNull String htmlUrl,
        boolean prerelease,
        @Nullable Instant publishedAt,
        @NotNull HeadDBVersion version,
        @Nullable String assetName,
        @Nullable String assetDownloadUrl
) {

    public GitHubRelease {
        Objects.requireNonNull(tagName, "tagName");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(htmlUrl, "htmlUrl");
        Objects.requireNonNull(version, "version");
    }

    public boolean hasPluginAsset() {
        return assetDownloadUrl != null && !assetDownloadUrl.isBlank();
    }

}
