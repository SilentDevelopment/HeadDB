package io.github.silentdevelopment.headdb.paper.local.storage;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.local.custom.CustomHeadStore;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverrideStore;
import io.github.silentdevelopment.headdb.paper.local.player.CachedPlayerHead;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadCache;
import io.github.silentdevelopment.strata.BlockingStack;
import io.github.silentdevelopment.strata.Key;
import io.github.silentdevelopment.strata.Namespace;
import io.github.silentdevelopment.strata.Stack;
import io.github.silentdevelopment.strata.Strata;
import io.github.silentdevelopment.strata.Type;
import io.github.silentdevelopment.strata.codec.gson.GsonCodec;
import io.github.silentdevelopment.strata.durable.jdbc.JdbcDialect;
import io.github.silentdevelopment.strata.durable.jdbc.JdbcLayer;
import io.github.silentdevelopment.strata.entry.Entry;
import io.github.silentdevelopment.strata.operation.SaveOptions;
import io.github.silentdevelopment.strata.query.Query;
import io.github.silentdevelopment.strata.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class StrataLocalStores {

    private static final Namespace NAMESPACE = Namespace.of("headdb");
    private static final Type<StoredCustomHeadRow> CUSTOM_HEAD = Type.of("custom_head", StoredCustomHeadRow.class);
    private static final Type<RemoteHeadOverrideRow> REMOTE_OVERRIDE = Type.of("remote_override", RemoteHeadOverrideRow.class);
    private static final Type<CachedPlayerHeadRow> PLAYER_HEAD = Type.of("player_head", CachedPlayerHeadRow.class);
    private static final Duration BLOCKING_TIMEOUT = Duration.ofSeconds(10);

    private final BlockingStack stack;

    private StrataLocalStores(@NotNull BlockingStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    public static @NotNull StrataLocalStores sqlite(@NotNull Path databaseFile, @NotNull Executor executor) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        Objects.requireNonNull(executor, "executor");

        try {
            Path parent = databaseFile.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create HeadDB storage directory for " + databaseFile, exception);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databaseFile.toAbsolutePath().normalize());

        JdbcLayer layer = JdbcLayer.named("headdb-sqlite", dataSource, "headdb_local", JdbcDialect.GENERIC);
        try {
            layer.createSchema();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create HeadDB Strata SQLite schema.", exception);
        }

        Stack stack = Strata.stack()
                .executor(executor)
                .requireDurable()
                .durable(layer)
                .codec(CUSTOM_HEAD, GsonCodec.of(StoredCustomHeadRow.class))
                .codec(REMOTE_OVERRIDE, GsonCodec.of(RemoteHeadOverrideRow.class))
                .codec(PLAYER_HEAD, GsonCodec.of(CachedPlayerHeadRow.class))
                .build();

        return new StrataLocalStores(stack.blocking(BLOCKING_TIMEOUT));
    }

    public @NotNull CustomHeadStore customHeads() {
        return new StrataCustomHeadStore(stack);
    }

    public @NotNull RemoteHeadOverrideStore remoteOverrides() {
        return new StrataRemoteHeadOverrideStore(stack);
    }

    public @NotNull PlayerHeadCache playerHeadCache(@NotNull Duration ttl) {
        return new StrataPlayerHeadCache(stack, ttl);
    }

    private static final class StrataCustomHeadStore implements CustomHeadStore {

        private final BlockingStack stack;

        private StrataCustomHeadStore(@NotNull BlockingStack stack) {
            this.stack = Objects.requireNonNull(stack, "stack");
        }

        @Override
        public @NotNull Optional<StoredCustomHead> findStored(@NotNull HeadId id) {
            Objects.requireNonNull(id, "id");

            if (!id.isCustom()) {
                return Optional.empty();
            }

            return load(stack, key(CUSTOM_HEAD, "custom", id.key())).map(StoredCustomHeadRow::toDomain);
        }

        @Override
        public @NotNull Collection<StoredCustomHead> listStored() {
            return all(stack, CUSTOM_HEAD).stream().map(StoredCustomHeadRow::toDomain).toList();
        }

        @Override
        public void save(@NotNull StoredCustomHead head) {
            Objects.requireNonNull(head, "head");
            saveValue(stack, key(CUSTOM_HEAD, "custom", head.id()), StoredCustomHeadRow.fromDomain(head));
        }

        @Override
        public boolean delete(@NotNull HeadId id) {
            Objects.requireNonNull(id, "id");

            if (!id.isCustom()) {
                return false;
            }

            return deleteKey(stack, key(CUSTOM_HEAD, "custom", id.key()));
        }
    }

    private static final class StrataRemoteHeadOverrideStore implements RemoteHeadOverrideStore {

        private final BlockingStack stack;

        private StrataRemoteHeadOverrideStore(@NotNull BlockingStack stack) {
            this.stack = Objects.requireNonNull(stack, "stack");
        }

        @Override
        public @NotNull Optional<RemoteHeadOverride> find(@NotNull HeadId id) {
            Objects.requireNonNull(id, "id");

            if (!id.isRemote()) {
                return Optional.empty();
            }

            return load(stack, key(REMOTE_OVERRIDE, "remote", id.key())).map(RemoteHeadOverrideRow::toDomain);
        }

        @Override
        public @NotNull Collection<RemoteHeadOverride> list() {
            return all(stack, REMOTE_OVERRIDE).stream().map(RemoteHeadOverrideRow::toDomain).toList();
        }

        @Override
        public void save(@NotNull RemoteHeadOverride override) {
            Objects.requireNonNull(override, "override");

            if (override.empty()) {
                delete(override.headId());
                return;
            }

            saveValue(stack, key(REMOTE_OVERRIDE, "remote", override.headId().key()), RemoteHeadOverrideRow.fromDomain(override));
        }

        @Override
        public boolean delete(@NotNull HeadId id) {
            Objects.requireNonNull(id, "id");

            if (!id.isRemote()) {
                return false;
            }

            return deleteKey(stack, key(REMOTE_OVERRIDE, "remote", id.key()));
        }

        @Override
        public int deleteOrphans(@NotNull Set<HeadId> validRemoteIds) {
            Objects.requireNonNull(validRemoteIds, "validRemoteIds");

            int deleted = 0;
            for (RemoteHeadOverride override : list()) {
                if (validRemoteIds.contains(override.headId())) {
                    continue;
                }

                if (delete(override.headId())) {
                    deleted++;
                }
            }

            return deleted;
        }
    }

    private static final class StrataPlayerHeadCache implements PlayerHeadCache {

        private final BlockingStack stack;
        private final Duration ttl;

        private StrataPlayerHeadCache(@NotNull BlockingStack stack, @NotNull Duration ttl) {
            this.stack = Objects.requireNonNull(stack, "stack");
            this.ttl = Objects.requireNonNull(ttl, "ttl");
        }

        @Override
        public @NotNull Optional<CachedPlayerHead> find(@NotNull String lookupKey) {
            return load(stack, key(PLAYER_HEAD, "player", normalize(lookupKey))).map(CachedPlayerHeadRow::toDomain);
        }

        @Override
        public @NotNull Collection<CachedPlayerHead> list() {
            return all(stack, PLAYER_HEAD).stream().map(CachedPlayerHeadRow::toDomain).toList();
        }

        @Override
        public void save(@NotNull CachedPlayerHead head) {
            Objects.requireNonNull(head, "head");
            saveValue(stack, key(PLAYER_HEAD, "player", normalize(head.lookupKey())), CachedPlayerHeadRow.fromDomain(head), SaveOptions.defaults().withTtl(ttl));
        }

        @Override
        public boolean delete(@NotNull String lookupKey) {
            return deleteKey(stack, key(PLAYER_HEAD, "player", normalize(lookupKey)));
        }
    }

    private static <T> @NotNull Key<T> key(@NotNull Type<T> type, @NotNull String first, @NotNull String second) {
        return NAMESPACE.key(type, first, second);
    }

    private static <T> @NotNull Optional<T> load(@NotNull BlockingStack stack, @NotNull Key<T> key) {
        Result<Entry<T>> result = stack.load(key);
        if (!result.successful()) {
            return Optional.empty();
        }

        return result.optional().map(Entry::value);
    }

    private static <T> @NotNull List<T> all(@NotNull BlockingStack stack, @NotNull Type<T> type) {
        Result<List<Entry<T>>> result = stack.query(type, Query.all());
        if (!result.successful()) {
            return List.of();
        }

        return result.valueOrThrow().stream().map(Entry::value).toList();
    }

    private static <T> void saveValue(@NotNull BlockingStack stack, @NotNull Key<T> key, @NotNull T value) {
        saveValue(stack, key, value, SaveOptions.defaults());
    }

    private static <T> void saveValue(@NotNull BlockingStack stack, @NotNull Key<T> key, @NotNull T value, @NotNull SaveOptions options) {
        Result<Void> result = stack.save(key, value, options);
        if (!result.successful()) {
            throw new IllegalStateException("Failed to save HeadDB local value " + key.external() + ": " + result.failures());
        }
    }

    private static <T> boolean deleteKey(@NotNull BlockingStack stack, @NotNull Key<T> key) {
        Result<Void> result = stack.delete(key);
        return result.successful();
    }

    private static @NotNull String normalize(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String instant(@NotNull Instant instant) {
        Objects.requireNonNull(instant, "instant");
        return instant.toString();
    }

    private static @NotNull Instant instant(@Nullable String value, @NotNull Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static @Nullable String uuid(@Nullable UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    private static @Nullable UUID uuid(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @NotNull List<String> list(@Nullable Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private static @NotNull Set<String> set(@Nullable Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            result.add(value.trim());
        }

        return Set.copyOf(result);
    }

    private record StoredCustomHeadRow(
            @NotNull String id,
            @NotNull String name,
            @NotNull String textureHash,
            @Nullable String textureSignature,
            @NotNull List<String> lore,
            @NotNull List<String> tags,
            @NotNull List<String> collections,
            @NotNull String category,
            @NotNull String createdAt,
            @NotNull String updatedAt,
            @Nullable String createdBy,
            boolean draft
    ) {

        private StoredCustomHeadRow {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(textureHash, "textureHash");

            if (lore == null) {
                lore = List.of();
            }

            if (tags == null) {
                tags = List.of();
            }

            if (collections == null) {
                collections = List.of();
            }

            if (category == null || category.isBlank()) {
                category = "custom";
            }

            if (createdAt == null || createdAt.isBlank()) {
                createdAt = Instant.EPOCH.toString();
            }

            if (updatedAt == null || updatedAt.isBlank()) {
                updatedAt = createdAt;
            }
        }

        private static @NotNull StoredCustomHeadRow fromDomain(@NotNull StoredCustomHead head) {
            return new StoredCustomHeadRow(
                    head.id(),
                    head.name(),
                    head.textureHash(),
                    head.textureSignature(),
                    list(head.lore()),
                    list(head.tags()),
                    list(head.collections()),
                    head.category(),
                    instant(head.createdAt()),
                    instant(head.updatedAt()),
                    uuid(head.createdBy()),
                    head.draft()
            );
        }

        private @NotNull StoredCustomHead toDomain() {
            Instant created = instant(createdAt, Instant.EPOCH);
            Instant updated = instant(updatedAt, created);

            return new StoredCustomHead(
                    id,
                    name,
                    textureHash,
                    textureSignature,
                    list(lore),
                    set(tags),
                    set(collections),
                    category,
                    created,
                    updated,
                    uuid(createdBy),
                    draft
            );
        }
    }

    private record RemoteHeadOverrideRow(
            @NotNull String headId,
            @Nullable String name,
            @Nullable List<String> lore,
            @NotNull List<String> addTags,
            @NotNull List<String> removeTags,
            @Nullable List<String> replaceTags,
            @NotNull List<String> addCollections,
            @NotNull List<String> removeCollections,
            @Nullable String category,
            @Nullable Boolean hidden,
            @NotNull String createdAt,
            @NotNull String updatedAt,
            @Nullable String updatedBy
    ) {

        private RemoteHeadOverrideRow {
            Objects.requireNonNull(headId, "headId");

            if (addTags == null) {
                addTags = List.of();
            }

            if (removeTags == null) {
                removeTags = List.of();
            }

            if (addCollections == null) {
                addCollections = List.of();
            }

            if (removeCollections == null) {
                removeCollections = List.of();
            }

            if (createdAt == null || createdAt.isBlank()) {
                createdAt = Instant.EPOCH.toString();
            }

            if (updatedAt == null || updatedAt.isBlank()) {
                updatedAt = createdAt;
            }
        }

        private static @NotNull RemoteHeadOverrideRow fromDomain(@NotNull RemoteHeadOverride override) {
            return new RemoteHeadOverrideRow(
                    override.headId().toString(),
                    override.name(),
                    override.lore() == null ? null : list(override.lore()),
                    list(override.addTags()),
                    list(override.removeTags()),
                    override.replaceTags() == null ? null : list(override.replaceTags()),
                    list(override.addCollections()),
                    list(override.removeCollections()),
                    override.category(),
                    override.hidden(),
                    instant(override.createdAt()),
                    instant(override.updatedAt()),
                    uuid(override.updatedBy())
            );
        }

        private @NotNull RemoteHeadOverride toDomain() {
            Instant created = instant(createdAt, Instant.EPOCH);
            Instant updated = instant(updatedAt, created);

            return new RemoteHeadOverride(
                    new HeadId(headId),
                    name,
                    lore == null ? null : list(lore),
                    set(addTags),
                    set(removeTags),
                    replaceTags == null ? null : set(replaceTags),
                    set(addCollections),
                    set(removeCollections),
                    category,
                    hidden,
                    created,
                    updated,
                    uuid(updatedBy)
            );
        }
    }

    private record CachedPlayerHeadRow(
            @NotNull String lookupKey,
            @Nullable String uuid,
            @NotNull String name,
            @Nullable String textureHash,
            @Nullable String textureSignature,
            @NotNull String resolvedAt,
            @Nullable String failedAt
    ) {

        private CachedPlayerHeadRow {
            Objects.requireNonNull(lookupKey, "lookupKey");

            if (name == null || name.isBlank()) {
                name = lookupKey;
            }

            if (resolvedAt == null || resolvedAt.isBlank()) {
                resolvedAt = Instant.EPOCH.toString();
            }
        }

        private static @NotNull CachedPlayerHeadRow fromDomain(@NotNull CachedPlayerHead head) {
            return new CachedPlayerHeadRow(
                    head.lookupKey(),
                    StrataLocalStores.uuid(head.uuid()),
                    head.name(),
                    head.textureHash(),
                    head.textureSignature(),
                    instant(head.resolvedAt()),
                    head.failedAt() == null ? null : instant(head.failedAt())
            );
        }

        private @NotNull CachedPlayerHead toDomain() {
            Instant resolved = instant(resolvedAt, Instant.EPOCH);
            Instant failed = failedAt == null ? null : instant(failedAt, Instant.EPOCH);

            return new CachedPlayerHead(
                    lookupKey,
                    StrataLocalStores.uuid(uuid),
                    name,
                    textureHash,
                    textureSignature,
                    resolved,
                    failed
            );
        }
    }
}