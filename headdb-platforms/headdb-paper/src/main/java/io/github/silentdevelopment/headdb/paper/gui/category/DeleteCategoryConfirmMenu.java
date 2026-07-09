package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.Objects;
import java.util.Optional;

public final class DeleteCategoryConfirmMenu {

    private static final int SIZE = 27;
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CANCEL = "cancel";

    private DeleteCategoryConfirmMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(categoryId, "categoryId");

        if (!canDelete(plugin, player)) {
            noPermission(plugin, player);
            return;
        }

        Optional<CustomCategory> category = plugin.customCategories().find(categoryId);
        if (category.isEmpty()) {
            plugin.guis().openBrowse(player);
            return;
        }

        Holder holder = new Holder(category.get().id());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Component.text("Delete Category", NamedTextColor.RED));
        holder.inventory(inventory);
        fill(inventory);
        inventory.setItem(11, action(plugin, "confirm-yes", ACTION_CONFIRM, category.get()));
        inventory.setItem(15, action(plugin, "confirm-no", ACTION_CANCEL, category.get()));
        player.openInventory(inventory);
        plugin.sounds().play(player, SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
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

        if (action.get().equals(ACTION_CANCEL)) {
            plugin.sounds().play(player, SoundKey.CANCEL);
            CreateCategoryMenu.openExisting(plugin, player, holder.categoryId());
            return true;
        }

        if (!action.get().equals(ACTION_CONFIRM)) {
            return true;
        }

        if (!canDelete(plugin, player)) {
            noPermission(plugin, player);
            return true;
        }

        Optional<CustomCategory> category = plugin.customCategories().find(holder.categoryId());
        String label = category.map(value -> categoryLabel(plugin, player, value)).orElse(holder.categoryId());
        boolean deleted = plugin.customCategories().delete(holder.categoryId());
        plugin.clearSearchCache();
        player.sendMessage(Component.text(deleted ? "Category deleted: " : "Category already deleted: ", deleted ? NamedTextColor.GRAY : NamedTextColor.RED).append(Component.text(label, NamedTextColor.GOLD)));
        plugin.sounds().play(player, deleted ? SoundKey.CATEGORY_DELETE : SoundKey.INVALID);
        plugin.guis().openBrowse(player);
        return true;
    }

    private static @NotNull String categoryLabel(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category) {
        return plugin.adminModes().enabled(player) || Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN) ? category.name() + " (" + category.id() + ")" : category.name();
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String iconKey, @NotNull String action, @NotNull CustomCategory category) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.mini(plugin.guiConfig().icon(iconKey).name()), List.of(GuiItems.idDetail("Category", category.name()), GuiItems.idDetail("ID", category.id())));
        stamp(plugin, item, action);
        return item;
    }

    private static boolean canDelete(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        return plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN);
    }

    private static void fill(@NotNull Inventory inventory) {
        ItemStack item = GuiItems.item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private static void stamp(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item, @NotNull String action) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action));
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
        return new NamespacedKey(plugin, "delete_category_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, SoundKey.NO_PERMISSION);
    }

    private static final class Holder implements InventoryHolder {
        private final String categoryId;
        private Inventory inventory;

        private Holder(@NotNull String categoryId) {
            this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        }

        private @NotNull String categoryId() {
            return categoryId;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
