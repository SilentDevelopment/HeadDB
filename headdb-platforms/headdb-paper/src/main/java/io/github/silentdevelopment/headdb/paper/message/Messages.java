package io.github.silentdevelopment.headdb.paper.message;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.hermes.id.LocaleId;
import io.github.silentdevelopment.hermes.paper.messenger.PaperMessenger;
import io.github.silentdevelopment.relay.text.CommandText;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final PaperMessenger messenger;
    private final Map<MessageKey, String> fallbackMessages;

    public Messages(@NotNull PaperMessenger messenger) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.fallbackMessages = fallbackMessages();
    }

    public @NotNull Component invalidArgument(@NotNull CommandSender receiver, @NotNull String message) {
        return render(receiver, MessageKey.COMMAND_ERROR_INVALID_ARGUMENT, Map.of("message", message));
    }

    public @NotNull Component unknownHead(@NotNull CommandSender receiver, @NotNull HeadId id) {
        return render(receiver, MessageKey.COMMAND_ERROR_UNKNOWN_HEAD, Map.of("id", id.toString()));
    }

    public @NotNull Component playerNotOnline(@NotNull CommandSender receiver, @NotNull String player) {
        return render(receiver, MessageKey.COMMAND_ERROR_PLAYER_NOT_ONLINE, Map.of("player", player));
    }

    public @NotNull Component targetEmpty(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_TARGET_EMPTY);
    }

    public @NotNull Component noGiveOthers(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_NO_GIVE_OTHERS);
    }

    public @NotNull Component giveInventoryFull(@NotNull CommandSender receiver, @NotNull Player target) {
        return render(receiver, MessageKey.COMMAND_ERROR_INVENTORY_FULL, Map.of("player", target.getName()));
    }

    public @NotNull Component giveSuccess(@NotNull CommandSender receiver, @NotNull Head head, @NotNull Player target) {
        return render(receiver, MessageKey.COMMAND_GIVE_SUCCESS, Map.of("head", head.name(), "player", target.getName()));
    }

    public @NotNull Component giveReceived(@NotNull Player receiver, @NotNull Head head) {
        return render(receiver, MessageKey.COMMAND_GIVE_RECEIVED, Map.of("head", head.name()));
    }

    public @NotNull Component economyInvalidFunds(@NotNull CommandSender receiver, @NotNull Head head, @NotNull String price) {
        return render(receiver, MessageKey.ECONOMY_INVALID_FUNDS, Map.of("head", head.name(), "price", price));
    }

    public @NotNull Component economyPurchased(@NotNull CommandSender receiver, @NotNull Head head, @NotNull String price) {
        return render(receiver, MessageKey.ECONOMY_PURCHASED, Map.of("head", head.name(), "price", price));
    }

    public @NotNull Component giveUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_GIVE);
    }

    public @NotNull Component consoleGiveUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_CONSOLE_GIVE);
    }

    public @NotNull Component searchUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_SEARCH);
    }

    public @NotNull Component infoConsoleUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_INFO_CONSOLE);
    }

    public @NotNull Component openConsoleSelfUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_OPEN_CONSOLE_SELF);
    }

    public @NotNull Component openConsoleTargetUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_OPEN_CONSOLE_TARGET);
    }

    public @NotNull Component randomConsoleUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_RANDOM_CONSOLE);
    }

    public @NotNull Component itemCacheUsage(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_USAGE_ITEMCACHE);
    }

    public @NotNull Component searchGuiNotReady(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_SEARCH_GUI_NOT_READY);
    }

    public @NotNull Component unknownCategory(@NotNull CommandSender receiver, @NotNull String category) {
        return render(receiver, MessageKey.COMMAND_ERROR_UNKNOWN_CATEGORY, Map.of("category", category));
    }

    public @NotNull Component heldHeadRequired(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_HELD_HEAD_REQUIRED);
    }

    public @NotNull Component randomEmpty(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_RANDOM_EMPTY);
    }

    public @NotNull Component reloadStarted(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_RELOAD_STARTED);
    }

    public @NotNull Component reloadSuccess(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_RELOAD_SUCCESS);
    }

    public @NotNull Component reloadFailed(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_RELOAD_FAILED);
    }

    public @NotNull Component refreshAlreadyRunning(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_REFRESH_ALREADY_RUNNING);
    }

    public @NotNull Component refreshStarted(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_REFRESH_STARTED);
    }

    public @NotNull Component verifyAlreadyRunning(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_VERIFY_ALREADY_RUNNING);
    }

    public @NotNull Component verifyStarted(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_VERIFY_STARTED);
    }

    public @NotNull Component verifySuccess(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_VERIFY_SUCCESS);
    }

    public @NotNull Component verifyFailed(@NotNull CommandSender receiver, @NotNull String message) {
        return render(receiver, MessageKey.COMMAND_VERIFY_FAILED, Map.of("message", message));
    }

    public @NotNull Component itemCacheCleared(@NotNull CommandSender receiver, int count) {
        return render(receiver, MessageKey.COMMAND_ITEMCACHE_CLEARED, Map.of("count", String.valueOf(count)));
    }

    public @NotNull Component relayUnknownCommand(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_UNKNOWN_COMMAND);
    }

    public @NotNull Component relayNoHandler(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_NO_HANDLER);
    }

    public @NotNull Component relayInvalidUsage(@NotNull CommandSender receiver, @NotNull CommandText text) {
        return render(receiver, MessageKey.COMMAND_ERROR_INVALID_USAGE).append(Component.text(" ")).append(Component.text(plain(text), NamedTextColor.GRAY));
    }

    public @NotNull Component relayValidUsages(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.COMMAND_ERROR_VALID_USAGES);
    }

    public @NotNull Component render(@NotNull CommandSender receiver, @NotNull CommandText text) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(text, "text");

        Component message = null;
        String key = text.key();

        if (key != null && !key.isBlank()) {
            message = messenger.getMessage((Audience) receiver, key);
        }

        if (message == null) {
            message = MINI_MESSAGE.deserialize(text.fallback());
        }

        return applyPlaceholders(message, placeholders(text));
    }

    public @NotNull Component render(@NotNull CommandSender receiver, @NotNull MessageKey key) {
        return render(receiver, key, Map.of());
    }

    public @NotNull Component render(@NotNull CommandSender receiver, @NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(placeholders, "placeholders");

        Component message = messenger.getMessage((Audience) receiver, key.path());

        if (message == null) {
            String fallback = fallbackMessages.get(key);
            if (fallback == null || fallback.isBlank()) {
                return Component.empty();
            }
            message = MINI_MESSAGE.deserialize(fallback);
        }

        return applyPlaceholders(message, placeholders);
    }

    public @NotNull List<LocaleOption> availableLocales() {
        return messenger.getLocaleManager().getAvailableLocales()
                .values()
                .stream()
                .map(locale -> {
                    LocaleId id = locale.getId();
                    String fallback = id.value();
                    String name = localeName(locale.getName(), fallback);
                    String nativeName = localeName(locale.getNativeName(), name);

                    return new LocaleOption(id, name, nativeName);
                })
                .sorted(Comparator.comparing(option -> option.id().value()))
                .toList();
    }

    public @NotNull LocaleId resolvedLocale(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        LocaleId resolved = messenger.getResolvedLocaleId(player.getUniqueId());

        if (resolved != null) {
            return resolved;
        }

        LocaleId fallback = messenger.getLocaleManager().getDefaultLocaleId();

        if (fallback != null) {
            return fallback;
        }

        return LocaleId.of("en-US");
    }

    public @NotNull LocaleOption resolvedLocaleOption(@NotNull Player player) {
        LocaleId resolved = resolvedLocale(player);

        for (LocaleOption option : availableLocales()) {
            if (option.id().equals(resolved)) {
                return option;
            }
        }

        return new LocaleOption(resolved, resolved.value(), resolved.value());
    }

    public void setLocale(@NotNull UUID uniqueId, @NotNull LocaleId localeId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(localeId, "localeId");

        if (messenger.getLocaleManager().findLocale(localeId).isEmpty()) {
            throw new IllegalArgumentException("Unknown locale: " + localeId.value());
        }

        messenger.getSelectionManager().putOverride(uniqueId, localeId);
    }

    public void clearLocale(@NotNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        messenger.getSelectionManager().removeOverride(uniqueId);
    }

    public @NotNull LocaleOption nextLocale(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        List<LocaleOption> locales = availableLocales();

        if (locales.isEmpty()) {
            throw new IllegalStateException("No HeadDB locales are available.");
        }

        LocaleId current = resolvedLocale(player);

        for (int index = 0; index < locales.size(); index++) {
            if (!locales.get(index).id().equals(current)) {
                continue;
            }

            int nextIndex = (index + 1) % locales.size();
            return locales.get(nextIndex);
        }

        return locales.getFirst();
    }

    public @NotNull Component languageChanged(@NotNull CommandSender receiver, @NotNull LocaleOption locale) {
        return render(receiver, MessageKey.GUI_LANGUAGE_CHANGED, Map.of("locale", locale.displayName()));
    }

    public @NotNull Component languageReset(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.GUI_LANGUAGE_RESET);
    }

    public @NotNull Component priceInvalid(@NotNull CommandSender receiver) {
        return render(receiver, MessageKey.GUI_PRICE_INVALID);
    }

    public @NotNull Component priceUpdated(@NotNull CommandSender receiver, @NotNull String target, @NotNull String price) {
        return render(receiver, MessageKey.GUI_PRICE_UPDATED, Map.of("target", target, "price", price));
    }

    public @NotNull Component taxonomyCreated(@NotNull CommandSender receiver, @NotNull String type, @NotNull String name, @NotNull String id) {
        return render(receiver, MessageKey.GUI_TAXONOMY_CREATED, Map.of("type", type, "name", name, "id", id));
    }

    public @NotNull Component taxonomyDeleted(@NotNull CommandSender receiver, @NotNull String type, @NotNull String id) {
        return render(receiver, MessageKey.GUI_TAXONOMY_DELETED, Map.of("type", type, "id", id));
    }

    public @NotNull Component taxonomyUnknown(@NotNull CommandSender receiver, @NotNull String type, @NotNull String id) {
        return render(receiver, MessageKey.GUI_TAXONOMY_UNKNOWN, Map.of("type", type, "id", id));
    }

    private static @NotNull String localeName(String value, @NotNull String fallback) {
        Objects.requireNonNull(fallback, "fallback");

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private static @NotNull String plain(@NotNull CommandText text) {
        Objects.requireNonNull(text, "text");

        String value = text.fallback();

        for (int index = 0; index < text.arguments().size(); index++) {
            value = value.replace("{" + index + "}", String.valueOf(text.arguments().get(index)));
        }

        for (Map.Entry<String, Object> entry : text.placeholders().entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }

        return value;
    }

    private static @NotNull Map<String, String> placeholders(@NotNull CommandText text) {
        Objects.requireNonNull(text, "text");

        Map<String, String> placeholders = new LinkedHashMap<>();

        for (int index = 0; index < text.arguments().size(); index++) {
            placeholders.put(String.valueOf(index), String.valueOf(text.arguments().get(index)));
        }

        for (Map.Entry<String, Object> entry : text.placeholders().entrySet()) {
            placeholders.put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        return placeholders;
    }

    private static @NotNull Component applyPlaceholders(@NotNull Component component, @NotNull Map<String, String> placeholders) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(placeholders, "placeholders");

        Component result = component;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replaceText(TextReplacementConfig.builder()
                    .matchLiteral("{" + entry.getKey() + "}")
                    .replacement(entry.getValue())
                    .build());
        }

        return result;
    }

    private static @NotNull Map<MessageKey, String> fallbackMessages() {
        Map<MessageKey, String> messages = new EnumMap<>(MessageKey.class);

        // Command

        messages.put(MessageKey.COMMAND_ERROR_NO_PERMISSION, "<red>You do not have permission to do that.");
        messages.put(MessageKey.COMMAND_ERROR_UNKNOWN_COMMAND, "<red>Unknown command.");
        messages.put(MessageKey.COMMAND_ERROR_NO_HANDLER, "<red>No handler is bound for this command.");
        messages.put(MessageKey.COMMAND_ERROR_INVALID_USAGE, "<red>Invalid usage.");
        messages.put(MessageKey.COMMAND_ERROR_VALID_USAGES, "<gray>Valid usages:");

        messages.put(MessageKey.COMMAND_USAGE_SEARCH, "<red>Usage: /hdb search <query> or /hdb search <text|tag|category|collection|head> ...");
        messages.put(MessageKey.COMMAND_USAGE_GIVE, "<red>Usage: /hdb give <id> [player] or /hdb give <player> <id>");
        messages.put(MessageKey.COMMAND_USAGE_CONSOLE_GIVE, "<red>Console usage: /hdb give <id> <player> or /hdb give <player> <id>");
        messages.put(MessageKey.COMMAND_USAGE_INFO_CONSOLE, "<red>Usage: /hdb info <id>. Console cannot inspect a held item.");
        messages.put(MessageKey.COMMAND_USAGE_OPEN_CONSOLE_SELF, "<red>Usage: /hdb open <category> <player>. Console cannot open its own GUI.");
        messages.put(MessageKey.COMMAND_USAGE_OPEN_CONSOLE_TARGET, "<red>Usage: /hdb open <category> <player>. Console must specify a player.");
        messages.put(MessageKey.COMMAND_USAGE_RANDOM_CONSOLE, "<red>Usage: /hdb random [amount] [category] <player>. Console must specify a player.");
        messages.put(MessageKey.COMMAND_USAGE_ITEMCACHE, "<red>Usage: /hdb itemcache clear");

        messages.put(MessageKey.COMMAND_ERROR_INVALID_ARGUMENT, "<red>{message}");
        messages.put(MessageKey.COMMAND_ERROR_UNKNOWN_HEAD, "<red>Unknown HeadDB head: <gold>{id}");
        messages.put(MessageKey.COMMAND_ERROR_PLAYER_NOT_ONLINE, "<red>Player is not online: <gold>{player}");
        messages.put(MessageKey.COMMAND_ERROR_TARGET_EMPTY, "<red>Target player cannot be empty.");
        messages.put(MessageKey.COMMAND_ERROR_NO_GIVE_OTHERS, "<red>You do not have permission to give heads to other players.");
        messages.put(MessageKey.COMMAND_ERROR_INVENTORY_FULL, "<red>Could not give head because <gold>{player}</gold><red>'s inventory is full.");
        messages.put(MessageKey.COMMAND_ERROR_SEARCH_GUI_NOT_READY, "<red>Search GUI is not implemented yet.");
        messages.put(MessageKey.COMMAND_ERROR_UNKNOWN_CATEGORY, "<red>Unknown category: <gold>{category}");
        messages.put(MessageKey.COMMAND_ERROR_HELD_HEAD_REQUIRED, "<red>Hold a HeadDB head or provide an ID.");
        messages.put(MessageKey.COMMAND_ERROR_RANDOM_EMPTY, "<red>No heads matched the random command filters.");

        messages.put(MessageKey.COMMAND_GIVE_SUCCESS, "<gray>Gave <gold>{head}</gold> to <gold>{player}</gold>.");
        messages.put(MessageKey.COMMAND_GIVE_RECEIVED, "<gray>You received <gold>{head}</gold> from HeadDB.");

        messages.put(MessageKey.ECONOMY_INVALID_FUNDS, "<red>You need <gold>{price}</gold><red> to buy <gold>{head}</gold><red>.");
        messages.put(MessageKey.ECONOMY_PURCHASED, "<gray>Purchased <gold>{head}</gold> for <gold>{price}</gold>.");

        messages.put(MessageKey.COMMAND_RELOAD_STARTED, "<gray>Reloading HeadDB...");
        messages.put(MessageKey.COMMAND_RELOAD_SUCCESS, "<gold>HeadDB reloaded.");
        messages.put(MessageKey.COMMAND_RELOAD_FAILED, "<red>Failed to reload HeadDB. Check console for details.");

        messages.put(MessageKey.COMMAND_REFRESH_ALREADY_RUNNING, "<red>HeadDB refresh is already running.");
        messages.put(MessageKey.COMMAND_REFRESH_STARTED, "<gold>HeadDB remote refresh started.");

        messages.put(MessageKey.COMMAND_VERIFY_ALREADY_RUNNING, "<red>HeadDB remote verification could not start because another database operation is running.");
        messages.put(MessageKey.COMMAND_VERIFY_STARTED, "<gold>Started HeadDB remote verification.");
        messages.put(MessageKey.COMMAND_VERIFY_SUCCESS, "<gold>HeadDB remote verification passed.");
        messages.put(MessageKey.COMMAND_VERIFY_FAILED, "<red>HeadDB remote verification failed: <gray>{message}");

        messages.put(MessageKey.COMMAND_ITEMCACHE_CLEARED, "<gold>Cleared HeadDB item cache. Removed <gray>{count}</gray><gold> cached item(s).");

        // GUI

        messages.put(MessageKey.GUI_LANGUAGE_CHANGED, "<gray>Language changed to <gold>{locale}</gold>.");
        messages.put(MessageKey.GUI_LANGUAGE_RESET, "<gray>Language reset to the default locale.");
        messages.put(MessageKey.GUI_PRICE_INVALID, "<red>Price must be a valid number.");
        messages.put(MessageKey.GUI_PRICE_UPDATED, "<gray>Updated price for <gold>{target}</gold><gray> to <gold>{price}</gold><gray>.");
        messages.put(MessageKey.GUI_TAXONOMY_CREATED, "<gray>Created custom {type}: <gold>{name}</gold><gray> (<gold>{id}</gold><gray>).");
        messages.put(MessageKey.GUI_TAXONOMY_DELETED, "<gray>Deleted custom {type}: <gold>{id}</gold><gray>.");
        messages.put(MessageKey.GUI_TAXONOMY_UNKNOWN, "<red>Unknown custom {type}: <gold>{id}</gold>");

        return Map.copyOf(messages);
    }
}