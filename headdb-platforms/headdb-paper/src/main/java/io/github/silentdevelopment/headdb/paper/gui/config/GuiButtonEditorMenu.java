package io.github.silentdevelopment.headdb.paper.gui.config;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.material.MaterialSelectionMenu;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class GuiButtonEditorMenu {

    private static final int SIZE = 27;
    private static final String ACTION_NAME = "name";
    private static final String ACTION_MATERIAL = "material";
    private static final String ACTION_TYPE = "type";
    private static final String ACTION_HEAD_ID = "head-id";
    private static final String ACTION_LORE = "lore";
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_BACK = "back";
    private static final long GUARD_TTL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final Map<GuardKey, Long> EDIT_GUARD = new ConcurrentHashMap<>();

    private GuiButtonEditorMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void markEditedDrop(@NotNull Player player, @NotNull String key) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(key, "key");
        EDIT_GUARD.put(new GuardKey(player.getUniqueId(), key), System.nanoTime() + GUARD_TTL_NANOS);
    }

    public static boolean consumeEditedDrop(@NotNull UUID playerId, @NotNull String key) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(key, "key");

        Long expiresAt = EDIT_GUARD.remove(new GuardKey(playerId, key));
        if (expiresAt == null) {
            return false;
        }

        return expiresAt >= System.nanoTime();
    }

    public static @NotNull NamespacedKey iconKey(@NotNull HeadDBPlugin plugin) {
        return new NamespacedKey(plugin, "gui_icon_key");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String key) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(key, "key");

        if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_BUTTON_CONFIG)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        GuiIconConfig icon = plugin.guiConfig().iconOrDefault(key, fallbackKey(key));
        GuiButtonEditorHolder holder = new GuiButtonEditorHolder(key);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Button Config", displayKey(plugin, key), true));
        holder.inventory(inventory);

        fill(inventory);
        inventory.setItem(plugin.guiConfig().slot("gui-button.preview", 4), GuiHeadIcons.icon(plugin, icon));
        inventory.setItem(plugin.guiConfig().slot("gui-button.name", 10), action(plugin, ACTION_NAME, "gui-edit-name"));
        inventory.setItem(plugin.guiConfig().slot("gui-button.material", 11), action(plugin, ACTION_MATERIAL, "gui-edit-material"));
        inventory.setItem(plugin.guiConfig().slot("gui-button.type", 12), typeButton(plugin, icon));
        inventory.setItem(plugin.guiConfig().slot("gui-button.head-id", 13), action(plugin, ACTION_HEAD_ID, "gui-edit-head-id"));
        inventory.setItem(plugin.guiConfig().slot("gui-button.lore", 14), action(plugin, ACTION_LORE, "gui-edit-lore"));
        inventory.setItem(plugin.guiConfig().slot("gui-button.reset", 16), action(plugin, ACTION_RESET, "gui-edit-reset"));
        inventory.setItem(plugin.guiConfig().slot("gui-button.back", 18), action(plugin, ACTION_BACK, "back"));

        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof GuiButtonEditorHolder holder)) {
            return false;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return true;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || GuiMaterials.isAir(item.getType())) {
            return true;
        }

        Optional<String> action = readAction(plugin, item);
        if (action.isEmpty()) {
            return true;
        }

        plugin.sounds().playGuiAction(player, action.get());

        handleAction(plugin, player, holder.key(), action.get());
        return true;
    }


    private static @NotNull String fallbackKey(@NotNull String key) {
        Objects.requireNonNull(key, "key");

        if (key.startsWith("category.")) {
            return "category";
        }

        return key;
    }

    private static @NotNull String displayKey(@NotNull HeadDBPlugin plugin, @NotNull String key) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(key, "key");

        if (!key.startsWith("category.")) {
            return key;
        }

        String categoryId = key.substring("category.".length());
        return plugin.headRegistry().category(categoryId).map(category -> category.name()).orElse(categoryId);
    }

    public static @NotNull Optional<String> readIconKey(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(item, "item");

        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        String key = item.getItemMeta().getPersistentDataContainer().get(iconKey(plugin), PersistentDataType.STRING);
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(key);
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String key, @NotNull String action) {
        GuiIconConfigEditor editor = new GuiIconConfigEditor(plugin);

        if (action.equals(ACTION_BACK)) {
            plugin.guis().openMain(player);
            return;
        }

        if (action.equals(ACTION_NAME)) {
            prompt(plugin, player, "Enter MiniMessage name.", value -> editor.setName(key, value), key);
            return;
        }

        if (action.equals(ACTION_MATERIAL)) {
            MaterialSelectionMenu.openForButton(plugin, player, key);
            return;
        }

        if (action.equals(ACTION_TYPE)) {
            GuiIconConfig icon = plugin.guiConfig().iconOrDefault(key, fallbackKey(key));
            String next = icon.iconType() == GuiIconType.HEAD ? "ITEM" : "HEAD";
            editor.setType(key, next);
            player.sendMessage(Component.text("Button type set to ", NamedTextColor.GRAY).append(Component.text(next, NamedTextColor.GOLD)));
            open(plugin, player, key);
            return;
        }

        if (action.equals(ACTION_HEAD_ID)) {
            prompt(plugin, player, "Enter head id. Use none to clear.", value -> editor.setHeadId(key, value.equalsIgnoreCase("none") ? "" : value), key);
            return;
        }

        if (action.equals(ACTION_LORE)) {
            GuiButtonLoreEditorMenu.open(plugin, player, key);
            return;
        }

        if (action.equals(ACTION_RESET)) {
            editor.reset(key);
            player.sendMessage(Component.text("GUI button reset: ", NamedTextColor.GRAY).append(Component.text(key, NamedTextColor.GOLD)));
            open(plugin, player, key);
        }
    }

    private static @NotNull ItemStack typeButton(@NotNull HeadDBPlugin plugin, @NotNull GuiIconConfig icon) {
        boolean item = icon.iconType() == GuiIconType.ITEM;
        List<Component> lore = List.of(
                Component.text("ITEM", item ? NamedTextColor.GREEN : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("HEAD", item ? NamedTextColor.GRAY : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                GuiItems.lore("Click to switch button type.", NamedTextColor.YELLOW)
        );
        return action(plugin, ACTION_TYPE, plugin.guiConfig().icon("gui-edit-type"), GuiItems.name("Type", NamedTextColor.GOLD), lore);
    }

    private static void prompt(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String message, @NotNull java.util.function.Consumer<String> edit, @NotNull String key) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), value -> {
            edit.accept(value);
            player.sendMessage(Component.text("GUI button updated: ", NamedTextColor.GRAY).append(Component.text(key, NamedTextColor.GOLD)));
            open(plugin, player, key);
        }, () -> {
            player.sendMessage(Component.text("GUI edit cancelled.", NamedTextColor.GRAY));
            open(plugin, player, key);
        });
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        return action(plugin, action, plugin.guiConfig().icon(iconKey));
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull GuiIconConfig icon) {
        ItemStack item = GuiHeadIcons.icon(plugin, icon);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action));
        return item;
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull GuiIconConfig icon, @NotNull Component name, @NotNull List<Component> lore) {
        ItemStack item = GuiHeadIcons.icon(plugin, icon, name, lore);
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action));
        return item;
    }

    private static @NotNull Optional<String> readAction(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey(plugin), PersistentDataType.STRING);
        if (action == null || action.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(action);
    }

    private static @NotNull NamespacedKey actionKey(@NotNull HeadDBPlugin plugin) {
        return new NamespacedKey(plugin, "gui_button_editor_action");
    }

    private static void fill(@NotNull Inventory inventory) {
        ItemStack item = GuiItems.item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private record GuardKey(@NotNull UUID playerId, @NotNull String iconKey) {}

    private static final class GuiButtonEditorHolder implements InventoryHolder {

        private final String key;
        private Inventory inventory;

        private GuiButtonEditorHolder(@NotNull String key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("GUI button editor inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull String key() {
            return key;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
