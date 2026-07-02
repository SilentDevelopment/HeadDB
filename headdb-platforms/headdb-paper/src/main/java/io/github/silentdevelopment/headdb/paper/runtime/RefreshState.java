package io.github.silentdevelopment.headdb.paper.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class RefreshState {

    private final AtomicBoolean running;
    private final AtomicReference<String> currentOperation;
    private final AtomicReference<Instant> startedAt;
    private final AtomicReference<RefreshOutcome> lastOutcome;
    private final AtomicReference<String> lastOperation;
    private final AtomicReference<Instant> lastSuccessfulRefresh;
    private final AtomicReference<Instant> lastFailedRefresh;
    private final AtomicReference<String> lastFailureMessage;

    public RefreshState() {
        this.running = new AtomicBoolean(false);
        this.currentOperation = new AtomicReference<>();
        this.startedAt = new AtomicReference<>();
        this.lastOutcome = new AtomicReference<>(RefreshOutcome.NONE);
        this.lastOperation = new AtomicReference<>();
        this.lastSuccessfulRefresh = new AtomicReference<>();
        this.lastFailedRefresh = new AtomicReference<>();
        this.lastFailureMessage = new AtomicReference<>();
    }

    public boolean begin() {
        return begin("remote refresh");
    }

    public boolean begin(@NotNull String operation) {
        Objects.requireNonNull(operation, "operation");

        if (!running.compareAndSet(false, true)) {
            return false;
        }

        currentOperation.set(normalizeOperation(operation));
        startedAt.set(Instant.now());
        return true;
    }

    public void markSuccess() {
        String operation = currentOperation();
        lastSuccessfulRefresh.set(Instant.now());
        lastFailureMessage.set(null);
        lastOperation.set(operation);
        lastOutcome.set(RefreshOutcome.SUCCESS);
        currentOperation.set(null);
        startedAt.set(null);
        running.set(false);
    }

    public void markFailure(@NotNull Throwable throwable) {
        String operation = currentOperation();
        lastFailedRefresh.set(Instant.now());
        lastFailureMessage.set(failureMessage(throwable));
        lastOperation.set(operation);
        lastOutcome.set(RefreshOutcome.FAILURE);
        currentOperation.set(null);
        startedAt.set(null);
        running.set(false);
    }

    public boolean running() {
        return running.get();
    }

    public @NotNull String currentOperation() {
        return value(currentOperation.get(), "database operation");
    }

    public @Nullable Instant startedAt() {
        return startedAt.get();
    }

    public @NotNull RefreshOutcome lastOutcome() {
        return lastOutcome.get();
    }

    public @NotNull String lastOperation() {
        return value(lastOperation.get(), "database operation");
    }

    public @Nullable Instant lastSuccessfulRefresh() {
        return lastSuccessfulRefresh.get();
    }

    public @Nullable Instant lastFailedRefresh() {
        return lastFailedRefresh.get();
    }

    public @Nullable String lastFailureMessage() {
        return lastFailureMessage.get();
    }

    public void finish() {
        currentOperation.set(null);
        startedAt.set(null);
        running.set(false);
    }

    private @NotNull String failureMessage(@NotNull Throwable throwable) {
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return throwable.getClass().getSimpleName();
        }

        return throwable.getMessage();
    }

    private static @NotNull String normalizeOperation(@NotNull String operation) {
        String normalized = operation.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return "database operation";
        }

        return normalized;
    }

    private static @NotNull String value(@Nullable String value, @NotNull String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    public enum RefreshOutcome {
        NONE,
        SUCCESS,
        FAILURE
    }

}
