package io.github.silentdevelopment.headdb.paper.updater;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public final class UpdateCheckResult {

    private final String currentVersion;
    private final Instant checkedAt;
    private final GitHubRelease release;
    private final UpdateKind kind;
    private final String failureMessage;
    private volatile Path installedPath;

    private UpdateCheckResult(@NotNull String currentVersion, @NotNull Instant checkedAt, @Nullable GitHubRelease release, @NotNull UpdateKind kind, @Nullable String failureMessage) {
        this.currentVersion = Objects.requireNonNull(currentVersion, "currentVersion");
        this.checkedAt = Objects.requireNonNull(checkedAt, "checkedAt");
        this.release = release;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.failureMessage = failureMessage;
    }

    public static @NotNull UpdateCheckResult upToDate(@NotNull String currentVersion, @NotNull Instant checkedAt) {
        return new UpdateCheckResult(currentVersion, checkedAt, null, UpdateKind.NONE, null);
    }

    public static @NotNull UpdateCheckResult available(@NotNull String currentVersion, @NotNull Instant checkedAt, @NotNull GitHubRelease release, @NotNull UpdateKind kind) {
        return new UpdateCheckResult(currentVersion, checkedAt, Objects.requireNonNull(release, "release"), kind, null);
    }

    public static @NotNull UpdateCheckResult failed(@NotNull String currentVersion, @NotNull Instant checkedAt, @NotNull String failureMessage) {
        return new UpdateCheckResult(currentVersion, checkedAt, null, UpdateKind.NONE, Objects.requireNonNull(failureMessage, "failureMessage"));
    }

    public @NotNull String currentVersion() {
        return currentVersion;
    }

    public @NotNull Instant checkedAt() {
        return checkedAt;
    }

    public @Nullable GitHubRelease release() {
        return release;
    }

    public @NotNull UpdateKind kind() {
        return kind;
    }

    public @Nullable String failureMessage() {
        return failureMessage;
    }

    public @Nullable Path installedPath() {
        return installedPath;
    }

    public void markInstalled(@NotNull Path installedPath) {
        this.installedPath = Objects.requireNonNull(installedPath, "installedPath");
    }

    public boolean updateAvailable() {
        return release != null && kind != UpdateKind.NONE;
    }

    public boolean failed() {
        return failureMessage != null && !failureMessage.isBlank();
    }

}
