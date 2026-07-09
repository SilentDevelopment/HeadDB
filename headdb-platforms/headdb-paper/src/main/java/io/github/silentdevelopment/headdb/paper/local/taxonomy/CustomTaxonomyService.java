package io.github.silentdevelopment.headdb.paper.local.taxonomy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CustomTaxonomyService {

    private final SQLiteDataSource dataSource;
    private final String type;
    private final String defaultDescription;

    public CustomTaxonomyService(@NotNull Path databaseFile, @NotNull String type, @NotNull String defaultDescription) {
        Objects.requireNonNull(databaseFile, "databaseFile");
        this.type = normalizeType(type);
        this.defaultDescription = Objects.requireNonNull(defaultDescription, "defaultDescription");
        this.dataSource = dataSource(databaseFile);
        createSchema();
    }

    public synchronized @NotNull List<CustomTaxonomyEntry> list() {
        List<CustomTaxonomyEntry> entries = new ArrayList<>();
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT id, name, description, created_at, updated_at, created_by FROM headdb_custom_taxonomy WHERE type = ? ORDER BY lower(name) ASC")) {
            statement.setString(1, type);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(entry(result));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to list HeadDB custom " + type + " entries.", exception);
        }

        entries.sort(Comparator.comparing(CustomTaxonomyEntry::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    public synchronized @NotNull Optional<CustomTaxonomyEntry> find(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = CustomTaxonomyEntry.normalizeId(id);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT id, name, description, created_at, updated_at, created_by FROM headdb_custom_taxonomy WHERE type = ? AND id = ?")) {
            statement.setString(1, type);
            statement.setString(2, normalized);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(entry(result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read HeadDB custom " + type + " entry " + normalized + ".", exception);
        }
    }

    public synchronized void save(@NotNull CustomTaxonomyEntry entry) {
        Objects.requireNonNull(entry, "entry");
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("INSERT INTO headdb_custom_taxonomy(type, id, name, description, created_at, updated_at, created_by) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT(type, id) DO UPDATE SET name = excluded.name, description = excluded.description, updated_at = excluded.updated_at, created_by = excluded.created_by")) {
            statement.setString(1, type);
            statement.setString(2, entry.id());
            statement.setString(3, entry.name());
            statement.setString(4, entry.description());
            statement.setString(5, entry.createdAt().toString());
            statement.setString(6, entry.updatedAt().toString());
            statement.setString(7, entry.createdBy() == null ? null : entry.createdBy().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save HeadDB custom " + type + " entry " + entry.id() + ".", exception);
        }
    }

    public synchronized @NotNull CustomTaxonomyEntry create(@NotNull String id, @NotNull String name, @Nullable UUID createdBy) {
        CustomTaxonomyEntry entry = new CustomTaxonomyEntry(id, name, defaultDescription, createdBy);
        save(entry);
        return entry;
    }

    public synchronized boolean delete(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        String normalized = CustomTaxonomyEntry.normalizeId(id);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM headdb_custom_taxonomy WHERE type = ? AND id = ?")) {
            statement.setString(1, type);
            statement.setString(2, normalized);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to delete HeadDB custom " + type + " entry " + normalized + ".", exception);
        }
    }

    private @NotNull CustomTaxonomyEntry entry(@NotNull ResultSet result) throws SQLException {
        String description = result.getString("description");
        return new CustomTaxonomyEntry(
                result.getString("id"),
                result.getString("name"),
                description == null || description.isBlank() ? defaultDescription : description,
                instant(result.getString("created_at"), Instant.EPOCH),
                instant(result.getString("updated_at"), Instant.EPOCH),
                uuid(result.getString("created_by"))
        );
    }

    private void createSchema() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS headdb_custom_taxonomy(type TEXT NOT NULL, id TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, created_by TEXT, PRIMARY KEY(type, id))")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to create HeadDB custom taxonomy schema.", exception);
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

    private static @NotNull String normalizeType(@NotNull String type) {
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Taxonomy type cannot be blank.");
        }
        return normalized;
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
}
