package io.github.silentdevelopment.headdb.paper.sound;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public enum SoundKey {

    MENU_OPEN("menu-open"),
    CLICK("click"),
    BACK("back"),
    PAGE_NEXT("page-next"),
    PAGE_PREVIOUS("page-previous"),
    SEARCH("search"),
    OPEN_INPUT("open-input"),
    SELECTOR_OPEN("selector-open"),
    TAKE_HEAD("take-head"),
    GIVE_HEAD("give-head"),
    PLAYER_HEAD("player-head"),
    FAVORITE_ADD("favorite-add"),
    FAVORITE_REMOVE("favorite-remove"),
    HIDE_HEAD("hide-head"),
    SHOW_HEAD("show-head"),
    TOGGLE("toggle"),
    ADMIN_MODE_ON("admin-mode-on"),
    ADMIN_MODE_OFF("admin-mode-off"),
    SETTINGS("settings"),
    LANGUAGE("language"),
    SAVE("save"),
    SAVE_DRAFT("save-draft"),
    PUBLISH("publish"),
    DELETE("delete"),
    DELETE_CONFIRM("delete-confirm"),
    CONFIRM("confirm"),
    CANCEL("cancel"),
    INVALID("invalid"),
    NO_PERMISSION("no-permission"),
    VALIDATION_ERROR("validation-error"),
    ECONOMY_PURCHASE("economy-purchase"),
    ECONOMY_INSUFFICIENT_FUNDS("economy-insufficient-funds"),
    REFRESH("refresh"),
    RELOAD("reload"),
    VERIFY("verify"),
    DEBUG("debug"),
    CUSTOM_MANAGEMENT("custom-management"),
    CATEGORY_CREATE("category-create"),
    CATEGORY_UPDATE("category-update"),
    CATEGORY_DELETE("category-delete"),
    TAG_CREATE("tag-create"),
    TAG_UPDATE("tag-update"),
    TAG_DELETE("tag-delete"),
    COLLECTION_CREATE("collection-create"),
    COLLECTION_UPDATE("collection-update"),
    COLLECTION_DELETE("collection-delete"),
    PRICE_SUCCESS("price-success"),
    PRICE_FAILURE("price-failure");

    private final String configKey;

    SoundKey(@NotNull String configKey) {
        this.configKey = Objects.requireNonNull(configKey, "configKey");
    }

    public @NotNull String configKey() {
        return configKey;
    }

    public static @NotNull Optional<SoundKey> fromConfigKey(@NotNull String key) {
        Objects.requireNonNull(key, "key");
        String normalized = normalize(key);

        for (SoundKey soundKey : values()) {
            if (soundKey.configKey.equals(normalized)) {
                return Optional.of(soundKey);
            }
        }

        return Optional.empty();
    }

    public static @NotNull SoundKey fromGuiIcon(@NotNull String iconKey) {
        Objects.requireNonNull(iconKey, "iconKey");
        String key = normalize(iconKey);

        if (key.equals("back")) {
            return BACK;
        }

        if (key.equals("previous")) {
            return PAGE_PREVIOUS;
        }

        if (key.equals("next")) {
            return PAGE_NEXT;
        }

        if (key.equals("search") || key.endsWith("-search") || key.startsWith("filter-")) {
            return SEARCH;
        }

        if (key.equals("settings") || key.startsWith("gui-edit-") || key.equals("reset-language")) {
            return SETTINGS;
        }

        if (key.equals("languages")) {
            return LANGUAGE;
        }

        if (key.equals("admin-mode-on")) {
            return ADMIN_MODE_ON;
        }

        if (key.equals("admin-mode-off")) {
            return ADMIN_MODE_OFF;
        }

        if (key.equals("confirm-yes")) {
            return CONFIRM;
        }

        if (key.equals("confirm-no") || key.equals("close")) {
            return CANCEL;
        }

        if (key.equals("no-permission") || key.equals("empty")) {
            return NO_PERMISSION;
        }

        if (key.equals("create-head-save") || key.equals("category-save-draft")) {
            return SAVE_DRAFT;
        }

        if (key.equals("taxonomy-save") || key.equals("category-save")) {
            return SAVE;
        }

        if (key.equals("category-publish") || key.equals("edit-publish")) {
            return PUBLISH;
        }

        if (key.equals("category-delete") || key.equals("edit-delete")) {
            return DELETE;
        }

        if (key.equals("category-price") || key.equals("edit-price")) {
            return OPEN_INPUT;
        }

        if (key.equals("category-add") || key.equals("create-category")) {
            return CATEGORY_CREATE;
        }

        if (key.equals("category-remove")) {
            return CATEGORY_DELETE;
        }

        if (key.equals("create-tag")) {
            return TAG_CREATE;
        }

        if (key.equals("create-collection")) {
            return COLLECTION_CREATE;
        }

        if (key.equals("show-all")) {
            return SHOW_HEAD;
        }

        if (key.equals("edit-visibility-visible")) {
            return SHOW_HEAD;
        }

        if (key.equals("edit-visibility-hidden")) {
            return HIDE_HEAD;
        }

        if (key.equals("sort-cycle") || key.equals("sort-direction") || key.equals("show-all") || key.equals("taxonomy-filter-all") || key.equals("taxonomy-filter-custom")) {
            return TOGGLE;
        }

        if (key.equals("refresh")) {
            return REFRESH;
        }

        if (key.equals("reload")) {
            return RELOAD;
        }

        if (key.equals("verify")) {
            return VERIFY;
        }

        if (key.equals("debug")) {
            return DEBUG;
        }

        if (key.startsWith("category-") || key.startsWith("taxonomy-") || key.equals("custom-heads") || key.equals("hidden-heads")) {
            return CUSTOM_MANAGEMENT;
        }

        if (key.startsWith("create-head-") || key.startsWith("edit-") || key.startsWith("lore-")) {
            return OPEN_INPUT;
        }

        return CLICK;
    }

    public static @NotNull SoundKey fromGuiAction(@NotNull String action) {
        Objects.requireNonNull(action, "action");
        String key = normalize(action);

        if (key.equals("back") || key.equals("back-main") || key.equals("back-edit") || key.equals("selector-back")) {
            return BACK;
        }

        if (key.equals("previous") || key.equals("selector-previous")) {
            return PAGE_PREVIOUS;
        }

        if (key.equals("next") || key.equals("selector-next")) {
            return PAGE_NEXT;
        }

        if (key.equals("search") || key.equals("filter")) {
            return SEARCH;
        }

        if (key.equals("confirm")) {
            return CONFIRM;
        }

        if (key.equals("cancel")) {
            return CANCEL;
        }

        if (key.equals("save")) {
            return SAVE;
        }

        if (key.equals("save-draft") || key.equals("create")) {
            return SAVE_DRAFT;
        }

        if (key.equals("publish")) {
            return PUBLISH;
        }

        if (key.equals("delete")) {
            return DELETE;
        }

        if (key.equals("price")) {
            return OPEN_INPUT;
        }

        if (key.equals("use-held")) {
            return TAKE_HEAD;
        }

        if (key.equals("show_all") || key.equals("show-all")) {
            return SHOW_HEAD;
        }

        if (key.equals("visibility")) {
            return TOGGLE;
        }

        if (key.equals("reset") || key.equals("start-fresh") || key.equals("selector-clear")) {
            return TOGGLE;
        }

        if (key.equals("id") || key.equals("name") || key.equals("description") || key.equals("permission") || key.equals("texture")) {
            return OPEN_INPUT;
        }

        if (key.equals("material") || key.equals("head-icon") || key.equals("heads") || key.equals("category") || key.equals("tags") || key.equals("collections") || key.equals("type")) {
            return SELECTOR_OPEN;
        }

        if (key.equals("add")) {
            return CATEGORY_UPDATE;
        }

        if (key.startsWith("remove:") || key.startsWith("category-remove:")) {
            return CATEGORY_DELETE;
        }

        if (key.startsWith("category:") || key.startsWith("collection:") || key.startsWith("tag:") || key.startsWith("selector-entry:")) {
            return MENU_OPEN;
        }

        if (key.startsWith("category-select:") || key.startsWith("tag-toggle:") || key.startsWith("collection-toggle:")) {
            return TOGGLE;
        }

        if (key.equals("lore-add") || key.startsWith("lore-set:")) {
            return OPEN_INPUT;
        }

        if (key.equals("lore-clear") || key.equals("lore-reset")) {
            return SAVE;
        }

        return CLICK;
    }

    private static @NotNull String normalize(@NotNull String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
