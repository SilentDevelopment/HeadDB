package io.github.silentdevelopment.headdb.paper.gui.favorites;

import io.github.silentdevelopment.headdb.model.HeadId;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FavoriteHeadService {

    private static final Component STAR = Component.text(" ★", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);

    private final SQLiteDataSource dataSource;
    private final Map<UUID, Set<HeadId>> cache;

    public FavoriteHeadService(@NotNull Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        this.dataSource = dataSource(databaseFile);
        this.cache = new HashMap<>();
        createSchema();
    }

    public synchronized boolean isFavorite(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");
        return favorites(playerId).contains(headId);
    }

    public synchronized boolean toggle(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");

        Set<HeadId> favorites = new LinkedHashSet<>(favorites(playerId));
        if (favorites.contains(headId)) {
            remove(playerId, headId);
            favorites.remove(headId);
            cache.put(playerId, immutable(favorites));
            return false;
        }

        add(playerId, headId);
        favorites.add(headId);
        cache.put(playerId, immutable(favorites));
        return true;
    }

    public synchronized @NotNull Set<HeadId> favorites(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Set<HeadId> cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }
        Set<HeadId> loaded = loadFavorites(playerId);
        cache.put(playerId, loaded);
        return loaded;
    }

    public @NotNull ItemStack decorate(@NotNull UUID playerId, @NotNull HeadId headId, @NotNull ItemStack item) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");
        Objects.requireNonNull(item, "item");

        if (!isFavorite(playerId, headId)) {
            return item;
        }

        item.editMeta(meta -> {
            Component name = meta.displayName();
            if (name == null) {
                return;
            }

            meta.displayName(name.append(STAR));
        });

        return item;
    }

    public synchronized void invalidate(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        cache.remove(playerId);
    }

    public synchronized void invalidateAll() {
        cache.clear();
    }

    private @NotNull Set<HeadId> loadFavorites(@NotNull UUID playerId) {
        LinkedHashSet<HeadId> ids = new LinkedHashSet<>();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT head_id FROM headdb_favorites WHERE player_id = ? ORDER BY created_at ASC")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        ids.add(new HeadId(result.getString("head_id")));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore stale malformed rows.
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list HeadDB favorites.", exception);
        }

        return immutable(ids);
    }

    private static @NotNull Set<HeadId> immutable(@NotNull Set<HeadId> ids) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(ids));
    }

    private void add(@NotNull UUID playerId, @NotNull HeadId headId) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO headdb_favorites(player_id, head_id, created_at) VALUES (?, ?, strftime('%s','now'))")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, headId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save HeadDB favorite.", exception);
        }
    }

    private void remove(@NotNull UUID playerId, @NotNull HeadId headId) {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM headdb_favorites WHERE player_id = ? AND head_id = ?")) {
            statement.setString(1, playerId.toString());
            statement.setString(2, headId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to remove HeadDB favorite.", exception);
        }
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS headdb_favorites (player_id TEXT NOT NULL, head_id TEXT NOT NULL, created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')), PRIMARY KEY (player_id, head_id))")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create HeadDB favorites table.", exception);
        }
    }

    private static @NotNull SQLiteDataSource dataSource(@NotNull Path databaseFile) {
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
        return dataSource;
    }
}
