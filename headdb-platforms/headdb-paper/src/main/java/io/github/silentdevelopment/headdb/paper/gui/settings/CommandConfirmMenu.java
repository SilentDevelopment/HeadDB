package io.github.silentdevelopment.headdb.paper.gui.settings;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
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

public final class CommandConfirmMenu {

    private static final int SIZE = 27;
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CANCEL = "cancel";

    private CommandConfirmMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String title, @NotNull String command, @NotNull String permission) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(permission, "permission");

        if (!Permissions.has(player, permission)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            plugin.sounds().play(player, SoundKey.NO_PERMISSION);
            return;
        }

        Holder holder = new Holder(command, permission);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title, false));
        holder.inventory(inventory);
        fill(inventory);
        inventory.setItem(11, action(plugin, "confirm-yes", ACTION_CONFIRM, command));
        inventory.setItem(15, action(plugin, "confirm-no", ACTION_CANCEL, command));
        player.openInventory(inventory);
        plugin.sounds().play(player, SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

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
            plugin.guis().openMain(player);
            return true;
        }

        if (!action.get().equals(ACTION_CONFIRM)) {
            return true;
        }

        if (!Permissions.has(player, holder.permission())) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            plugin.sounds().play(player, SoundKey.NO_PERMISSION);
            return true;
        }

        plugin.sounds().play(player, soundFor(holder.command()));
        player.closeInventory();
        player.performCommand(holder.command());
        return true;
    }

    private static @NotNull SoundKey soundFor(@NotNull String command) {
        String normalized = command.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("hdb reload") || normalized.equals("hdb reload")) {
            return SoundKey.RELOAD;
        }
        if (normalized.startsWith("hdb refresh") || normalized.equals("hdb refresh")) {
            return SoundKey.REFRESH;
        }
        if (normalized.startsWith("hdb verify") || normalized.equals("hdb verify")) {
            return SoundKey.VERIFY;
        }
        if (normalized.startsWith("hdb debug") || normalized.equals("hdb debug")) {
            return SoundKey.DEBUG;
        }
        return SoundKey.CONFIRM;
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String iconKey, @NotNull String action, @NotNull String command) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.mini(plugin.guiConfig().icon(iconKey).name()), List.of(GuiItems.idDetail("Command", "/" + command)));
        stamp(plugin, item, action);
        return item;
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
        return new NamespacedKey(plugin, "command_confirm_action");
    }

    private static final class Holder implements InventoryHolder {
        private final String command;
        private final String permission;
        private Inventory inventory;

        private Holder(@NotNull String command, @NotNull String permission) {
            this.command = Objects.requireNonNull(command, "command");
            this.permission = Objects.requireNonNull(permission, "permission");
        }

        private @NotNull String command() {
            return command;
        }

        private @NotNull String permission() {
            return permission;
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
