package io.github.silentdevelopment.headdb.paper.local.custom;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface CustomHeadStore {

    @NotNull Optional<StoredCustomHead> findStored(@NotNull HeadId id);

    default @NotNull Optional<Head> find(@NotNull HeadId id) {
        return findStored(id).filter(head -> !head.draft()).map(StoredCustomHead::toHead);
    }

    @NotNull Collection<StoredCustomHead> listStored();

    default @NotNull Collection<Head> list() {
        return listStored().stream().filter(head -> !head.draft()).map(StoredCustomHead::toHead).toList();
    }

    void save(@NotNull StoredCustomHead head);

    boolean delete(@NotNull HeadId id);

    default boolean exists(@NotNull HeadId id) {
        return findStored(id).isPresent();
    }
}
