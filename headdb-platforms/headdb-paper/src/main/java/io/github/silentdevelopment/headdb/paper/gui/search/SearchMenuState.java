package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record SearchMenuState(
        @NotNull UUID viewerId,
        @NotNull SearchRequest request,
        @NotNull BackTarget resultBackTarget,
        @NotNull BackTarget optionsBackTarget
) {

    public SearchMenuState(@NotNull UUID viewerId, @NotNull SearchRequest request) {
        this(viewerId, request, BackTarget.MAIN, BackTarget.RESULTS);
    }

    public SearchMenuState {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(resultBackTarget, "resultBackTarget");
        Objects.requireNonNull(optionsBackTarget, "optionsBackTarget");
    }

    public enum BackTarget {
        MAIN,
        BROWSE,
        COLLECTIONS,
        TAGS,
        RESULTS
    }
}
