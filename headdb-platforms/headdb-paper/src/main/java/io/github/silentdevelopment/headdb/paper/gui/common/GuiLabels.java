package io.github.silentdevelopment.headdb.paper.gui.common;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.category.CustomCategory;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GuiLabels {

    private GuiLabels() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull String head(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        return plugin.headRegistry().find(id).map(head -> head(plugin, player, head)).orElse(id.display());
    }

    public static @NotNull String head(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Head head) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(head, "head");

        if (adminLabel(plugin, player)) {
            return head.name() + " (" + head.id().display() + ")";
        }

        return head.name();
    }

    public static @NotNull String category(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String id) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        return plugin.customCategories().find(id)
                .map(category -> category(plugin, player, category))
                .or(() -> plugin.headRegistry().category(id).map(category -> category(plugin, player, category)))
                .orElse(id);
    }

    public static @NotNull String category(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadCategory category) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(category, "category");

        if (adminLabel(plugin, player)) {
            return category.name() + " (" + category.id() + ")";
        }

        return category.name();
    }

    public static @NotNull String category(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(category, "category");

        if (adminLabel(plugin, player)) {
            return category.name() + " (" + category.id() + ")";
        }

        return category.name();
    }

    public static boolean adminLabel(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        return plugin.adminModes().enabled(player) || Permissions.has(player, Permissions.GUI_EDIT) || Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN);
    }
}
