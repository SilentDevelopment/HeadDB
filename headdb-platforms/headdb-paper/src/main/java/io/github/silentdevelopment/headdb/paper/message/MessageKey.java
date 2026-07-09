package io.github.silentdevelopment.headdb.paper.message;

import org.jetbrains.annotations.NotNull;

public enum MessageKey {

    // Command

    COMMAND_ERROR_NO_PERMISSION("command.error.no-permission"),
    COMMAND_ERROR_UNKNOWN_COMMAND("command.error.unknown-command"),
    COMMAND_ERROR_NO_HANDLER("command.error.no-handler"),
    COMMAND_ERROR_INVALID_USAGE("command.error.invalid-usage"),
    COMMAND_ERROR_VALID_USAGES("command.error.valid-usages"),

    COMMAND_USAGE_SEARCH("command.usage.search"),
    COMMAND_USAGE_GIVE("command.usage.give"),
    COMMAND_USAGE_CONSOLE_GIVE("command.usage.console-give"),
    COMMAND_USAGE_INFO_CONSOLE("command.usage.info-console"),
    COMMAND_USAGE_OPEN_CONSOLE_SELF("command.usage.open-console-self"),
    COMMAND_USAGE_OPEN_CONSOLE_TARGET("command.usage.open-console-target"),
    COMMAND_USAGE_RANDOM_CONSOLE("command.usage.random-console"),
    COMMAND_USAGE_ITEMCACHE("command.usage.itemcache"),

    COMMAND_ERROR_INVALID_ARGUMENT("command.error.invalid-argument"),
    COMMAND_ERROR_UNKNOWN_HEAD("command.error.unknown-head"),
    COMMAND_ERROR_PLAYER_NOT_ONLINE("command.error.player-not-online"),
    COMMAND_ERROR_TARGET_EMPTY("command.error.target-empty"),
    COMMAND_ERROR_NO_GIVE_OTHERS("command.error.no-give-others"),
    COMMAND_ERROR_INVENTORY_FULL("command.error.inventory-full"),
    COMMAND_ERROR_SEARCH_GUI_NOT_READY("command.error.search-gui-not-ready"),
    COMMAND_ERROR_UNKNOWN_CATEGORY("command.error.unknown-category"),
    COMMAND_ERROR_HELD_HEAD_REQUIRED("command.error.held-head-required"),
    COMMAND_ERROR_RANDOM_EMPTY("command.error.random-empty"),

    COMMAND_GIVE_SUCCESS("command.give.success"),
    COMMAND_GIVE_RECEIVED("command.give.received"),

    COMMAND_RELOAD_STARTED("command.reload.started"),
    COMMAND_RELOAD_SUCCESS("command.reload.success"),
    COMMAND_RELOAD_FAILED("command.reload.failed"),

    COMMAND_REFRESH_ALREADY_RUNNING("command.refresh.already-running"),
    COMMAND_REFRESH_STARTED("command.refresh.started"),

    COMMAND_VERIFY_ALREADY_RUNNING("command.verify.already-running"),
    COMMAND_VERIFY_STARTED("command.verify.started"),
    COMMAND_VERIFY_SUCCESS("command.verify.success"),
    COMMAND_VERIFY_FAILED("command.verify.failed"),

    COMMAND_ITEMCACHE_CLEARED("command.itemcache.cleared"),

    // Economy

    ECONOMY_INVALID_FUNDS("economy.invalid-funds"),
    ECONOMY_PURCHASED("economy.purchased"),

    // GUI

    GUI_LANGUAGE_CHANGED("gui.language.changed"),
    GUI_LANGUAGE_RESET("gui.language.reset"),
    GUI_PRICE_INVALID("gui.price.invalid"),
    GUI_PRICE_UPDATED("gui.price.updated"),
    GUI_TAXONOMY_CREATED("gui.taxonomy.created"),
    GUI_TAXONOMY_DELETED("gui.taxonomy.deleted"),
    GUI_TAXONOMY_UNKNOWN("gui.taxonomy.unknown");

    private final String path;

    MessageKey(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String path() {
        return path;
    }
}