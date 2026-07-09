package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CustomCategoryService {

    private final SQLiteDataSource dataSource;

    public CustomCategoryService(@NotNull Path databaseFile) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        this.dataSource = dataSource(databaseFile);
        createSchema();
    }

    public synchronized @NotNull List<CustomCategory> list() {
        List<CustomCategory> categories = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT id, name, material, description, draft FROM headdb_custom_categories ORDER BY lower(name) ASC")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String id = result.getString("id");
                    categories.add(categoryFromRow(result, headIds(connection, id)));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list HeadDB custom categories.", exception);
        }

        categories.sort(Comparator.comparing(CustomCategory::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(categories);
    }


    public synchronized @NotNull List<CustomCategory> listPublished() {
        return list().stream().filter(category -> !category.draft()).toList();
    }

    public synchronized @NotNull List<CustomCategory> listVisible(boolean includeDrafts) {
        if (includeDrafts) {
            return list();
        }

        return listPublished();
    }

    private static @NotNull CustomCategory categoryFromRow(@NotNull ResultSet result, @NotNull Set<HeadId> headIds) throws SQLException {
        String description = result.getString("description");
        boolean draft = result.getBoolean("draft");
        return new CustomCategory(result.getString("id"), result.getString("name"), result.getString("material"), description == null ? "Local custom category." : description, draft, headIds);
    }

    public synchronized @NotNull String nextId() {
        int next = 1;
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT id FROM headdb_custom_categories")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        next = Math.max(next, Integer.parseInt(result.getString("id")) + 1);
                    } catch (NumberFormatException ignored) {
                        // Non-numeric legacy IDs are ignored for automatic assignment.
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to calculate next HeadDB custom category id.", exception);
        }

        while (find(String.valueOf(next)).isPresent()) {
            next++;
        }

        return String.valueOf(next);
    }

    public synchronized @NotNull Optional<CustomCategory> find(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = normalize(id);

        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT id, name, material, description, draft FROM headdb_custom_categories WHERE id = ?")) {
            statement.setString(1, normalized);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(categoryFromRow(result, headIds(connection, normalized)));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read HeadDB custom category " + normalized + ".", exception);
        }
    }

    public synchronized void save(@NotNull CustomCategory category) {
        Objects.requireNonNull(category, "category");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO headdb_custom_categories(id, name, material, description, draft) VALUES (?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET name = excluded.name, material = excluded.material, description = excluded.description, draft = excluded.draft")) {
                statement.setString(1, category.id());
                statement.setString(2, category.name());
                statement.setString(3, category.material());
                statement.setString(4, category.description());
                statement.setBoolean(5, category.draft());
                statement.executeUpdate();
            }

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM headdb_custom_category_heads WHERE category_id = ?")) {
                delete.setString(1, category.id());
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO headdb_custom_category_heads(category_id, head_id) VALUES (?, ?)")) {
                for (HeadId headId : category.headIds()) {
                    insert.setString(1, category.id());
                    insert.setString(2, headId.toString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save HeadDB custom category " + category.id() + ".", exception);
        }
    }

    public synchronized void addHead(@NotNull String categoryId, @NotNull HeadId headId) {
        Objects.requireNonNull(headId, "headId");
        CustomCategory category = find(categoryId).orElseThrow(() -> new IllegalArgumentException("Unknown custom category: " + categoryId));
        LinkedHashSet<HeadId> ids = new LinkedHashSet<>(category.headIds());
        ids.add(headId);
        save(category.withHeadIds(ids));
    }

    public synchronized void removeHead(@NotNull String categoryId, @NotNull HeadId headId) {
        Objects.requireNonNull(headId, "headId");
        CustomCategory category = find(categoryId).orElseThrow(() -> new IllegalArgumentException("Unknown custom category: " + categoryId));
        LinkedHashSet<HeadId> ids = new LinkedHashSet<>(category.headIds());
        ids.remove(headId);
        save(category.withHeadIds(ids));
    }

    public synchronized boolean delete(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = normalize(id);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement heads = connection.prepareStatement("DELETE FROM headdb_custom_category_heads WHERE category_id = ?")) {
                heads.setString(1, normalized);
                heads.executeUpdate();
            }
            int updated;
            try (PreparedStatement category = connection.prepareStatement("DELETE FROM headdb_custom_categories WHERE id = ?")) {
                category.setString(1, normalized);
                updated = category.executeUpdate();
            }
            connection.commit();
            return updated > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete HeadDB custom category " + normalized + ".", exception);
        }
    }

    private @NotNull Set<HeadId> headIds(@NotNull Connection connection, @NotNull String categoryId) throws SQLException {
        LinkedHashSet<HeadId> ids = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT head_id FROM headdb_custom_category_heads WHERE category_id = ? ORDER BY rowid ASC")) {
            statement.setString(1, categoryId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        ids.add(new HeadId(result.getString("head_id")));
                    } catch (IllegalArgumentException ignored) {
                        // Ignore malformed rows.
                    }
                }
            }
        }

        return Set.copyOf(ids);
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement categories = connection.prepareStatement("CREATE TABLE IF NOT EXISTS headdb_custom_categories (id TEXT PRIMARY KEY, name TEXT NOT NULL, material TEXT NOT NULL, description TEXT NOT NULL DEFAULT 'Local custom category.', draft INTEGER NOT NULL DEFAULT 0)")) {
                categories.executeUpdate();
            }
            addColumnIfMissing(connection, "headdb_custom_categories", "description", "TEXT NOT NULL DEFAULT 'Local custom category.'");
            addColumnIfMissing(connection, "headdb_custom_categories", "draft", "INTEGER NOT NULL DEFAULT 0");
            try (PreparedStatement heads = connection.prepareStatement("CREATE TABLE IF NOT EXISTS headdb_custom_category_heads (category_id TEXT NOT NULL, head_id TEXT NOT NULL, PRIMARY KEY (category_id, head_id))")) {
                heads.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create HeadDB custom category tables.", exception);
        }
    }


    private static void addColumnIfMissing(@NotNull Connection connection, @NotNull String table, @NotNull String column, @NotNull String definition) throws SQLException {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, table, column)) {
            if (result.next()) {
                return;
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition)) {
            statement.executeUpdate();
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

    private static @NotNull String normalize(@NotNull String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("ID cannot be blank.");
        }
        return normalized;
    }
}
