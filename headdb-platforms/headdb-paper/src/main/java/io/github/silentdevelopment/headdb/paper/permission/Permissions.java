package io.github.silentdevelopment.headdb.paper.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class Permissions {

    public static final String ADMIN = "headdb.admin";

    public static final String HELP = "headdb.command.help";
    public static final String VERSION = "headdb.command.version";
    public static final String STATUS = "headdb.command.status";
    public static final String DEBUG = "headdb.command.debug";
    public static final String REPORT = "headdb.command.report";
    public static final String VERIFY = "headdb.command.verify";
    public static final String REFRESH = "headdb.command.refresh";
    public static final String RELOAD = "headdb.command.reload";
    public static final String UPDATE = "headdb.admin.update";
    public static final String SEARCH = "headdb.command.search";
    public static final String INFO = "headdb.command.info";
    public static final String GIVE = "headdb.command.give";
    public static final String GIVE_OTHERS = "headdb.command.give.others";
    public static final String OPEN = "headdb.command.open";
    public static final String OPEN_OTHERS = "headdb.command.open.others";
    public static final String ITEM_CACHE = "headdb.command.itemcache";

    public static final String PLAYER = "headdb.command.player";
    public static final String PLAYER_OTHERS = "headdb.command.player.others";

    public static final String CUSTOM_LIST = "headdb.command.custom.list";
    public static final String CUSTOM_INFO = "headdb.command.custom.info";
    public static final String CUSTOM_CREATE = "headdb.command.custom.create";
    public static final String CUSTOM_DELETE = "headdb.command.custom.delete";
    public static final String CUSTOM_RENAME = "headdb.command.custom.rename";
    public static final String CUSTOM_GIVE = "headdb.command.custom.give";
    public static final String CUSTOM_GIVE_OTHERS = "headdb.command.custom.give.others";

    public static final String EDIT = "headdb.command.edit";
    public static final String EDIT_NAME = "headdb.command.edit.name";
    public static final String EDIT_LORE = "headdb.command.edit.lore";
    public static final String EDIT_TAGS = "headdb.command.edit.tags";
    public static final String EDIT_CATEGORY = "headdb.command.edit.category";
    public static final String EDIT_VISIBILITY = "headdb.command.edit.visibility";
    public static final String EDIT_RESET = "headdb.command.edit.reset";

    public static final String GUI_MAIN = "headdb.gui.main";
    public static final String GUI_BROWSE = "headdb.gui.browse";
    public static final String GUI_SEARCH = "headdb.gui.search";
    public static final String GUI_FILTER = "headdb.gui.filter";
    public static final String GUI_HEAD_TAKE = "headdb.gui.head.take";
    public static final String GUI_CATEGORY_VIEW = "headdb.gui.category.view";
    public static final String GUI_CATEGORY_OPEN = "headdb.gui.category.open";
    public static final String GUI_PLAYER_HEADS = "headdb.gui.player-heads";
    public static final String GUI_CUSTOM_HEADS = "headdb.gui.custom-heads";
    public static final String GUI_FAVORITES = "headdb.gui.favorites";
    public static final String FAVORITES_TOGGLE = "headdb.gui.favorites.toggle";
    public static final String GUI_MORE_CATEGORIES = "headdb.gui.more-categories";
    public static final String GUI_CUSTOM_CATEGORIES_ADMIN = "headdb.gui.more-categories.admin";
    public static final String GUI_HIDDEN_HEADS = "headdb.gui.hidden-heads";
    public static final String GUI_EDIT = "headdb.gui.edit";
    public static final String GUI_BUTTON_CONFIG = "headdb.gui.button-config";
    public static final String GUI_SETTINGS = "headdb.gui.settings";
    public static final String GUI_SETTINGS_LANGUAGE = "headdb.gui.settings.language";
    public static final String GUI_ADMIN_MODE = "headdb.gui.admin-mode";

    public static final String CATEGORY_ALL = "headdb.category.*";
    public static final String CATEGORY_PREFIX = "headdb.category.";

    private Permissions() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static boolean has(@NotNull CommandSender sender, @NotNull String permission) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(permission, "permission");

        if (sender.hasPermission(ADMIN)) {
            return true;
        }

        return sender.hasPermission(permission);
    }

    public static boolean canGiveTo(@NotNull CommandSender sender, @NotNull Player target) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(target, "target");

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            return has(sender, GIVE);
        }

        return has(sender, GIVE) && has(sender, GIVE_OTHERS);
    }


    public static boolean canPlayerHeadFor(@NotNull CommandSender sender, @NotNull Player target) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(target, "target");

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            return has(sender, PLAYER);
        }

        return has(sender, PLAYER) && has(sender, PLAYER_OTHERS);
    }

    public static boolean canCustomGiveTo(@NotNull CommandSender sender, @NotNull Player target) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(target, "target");

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            return has(sender, CUSTOM_GIVE);
        }

        return has(sender, CUSTOM_GIVE) && has(sender, CUSTOM_GIVE_OTHERS);
    }

    public static boolean canOpenFor(@NotNull CommandSender sender, @NotNull Player target) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(target, "target");

        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) {
            return has(sender, OPEN);
        }

        return has(sender, OPEN) && has(sender, OPEN_OTHERS);
    }

    public static boolean canViewAllCategories(@NotNull CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        return has(sender, CATEGORY_ALL);
    }

    public static boolean canViewCategory(@NotNull CommandSender sender, @NotNull String categoryId) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(categoryId, "categoryId");

        if (canViewAllCategories(sender)) {
            return true;
        }

        return has(sender, category(categoryId));
    }

    public static @NotNull String category(@NotNull String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");

        String normalized = categoryId.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("categoryId cannot be blank.");
        }

        return CATEGORY_PREFIX + normalized;
    }
}
