package io.github.silentdevelopment.headdb.paper.gui.edit;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiLabels;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
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

import java.util.Objects;
import java.util.Optional;

public final class DeleteHeadConfirmMenu {

    private static final int SIZE = 27;
    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_CONFIRM = 11;
    private static final int SLOT_CANCEL = 15;
    private static final String ACTION_CONFIRM = "confirm";
    private static final String ACTION_CANCEL = "cancel";

    private DeleteHeadConfirmMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        if (!id.isCustom() || !Permissions.has(player, Permissions.CUSTOM_DELETE)) {
            noPermission(plugin, player);
            return;
        }

        Optional<Head> head = plugin.headRegistry().find(id);
        if (head.isEmpty()) {
            player.sendMessage(Component.text("Custom head no longer exists.", NamedTextColor.RED));
            return;
        }

        ConfirmHolder holder = new ConfirmHolder(id);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Delete Head", true));
        holder.inventory(inventory);
        fill(plugin, inventory);

        ItemStack preview = plugin.itemFactory().create(head.get());
        preview.editMeta(meta -> {
            java.util.List<Component> lore = meta.lore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(GuiItems.idDetail("ID", id.display()));
            lore.add(GuiItems.metaDetail("Name", head.get().name()));
            lore.add(GuiItems.lore("This action cannot be undone.", NamedTextColor.RED));
            meta.lore(lore);
        });
        inventory.setItem(SLOT_PREVIEW, preview);
        ItemStack confirm = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("confirm-yes"));
        stamp(plugin, confirm, ACTION_CONFIRM);
        inventory.setItem(SLOT_CONFIRM, confirm);

        ItemStack cancel = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("confirm-no"));
        stamp(plugin, cancel, ACTION_CANCEL);
        inventory.setItem(SLOT_CANCEL, cancel);

        player.openInventory(inventory);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof ConfirmHolder holder)) {
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
            HeadEditMenu.open(plugin, player, holder.id());
            return true;
        }

        if (!action.get().equals(ACTION_CONFIRM)) {
            return true;
        }

        if (!Permissions.has(player, Permissions.CUSTOM_DELETE)) {
            noPermission(plugin, player);
            return true;
        }

        String label = GuiLabels.head(plugin, player, holder.id());
        if (!plugin.headRegistry().customHeads().delete(holder.id())) {
            player.closeInventory();
            player.sendMessage(Component.text("Custom head no longer exists: ", NamedTextColor.RED).append(Component.text(holder.id().display(), NamedTextColor.GOLD)));
            return true;
        }

        plugin.headRegistry().onLocalMutation();
        plugin.clearItemCache();
        plugin.clearSearchCache();
        player.closeInventory();
        player.sendMessage(Component.text("Custom head deleted: ", NamedTextColor.GRAY).append(Component.text(label, NamedTextColor.GOLD)));
        return true;
    }

    private static void fill(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory) {
        if (!plugin.guiConfig().filler().enabled()) {
            return;
        }

        Material material = GuiMaterials.itemOr(plugin.guiConfig().filler().material(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = new ItemStack(material);
        filler.editMeta(meta -> {
            meta.displayName(GuiItems.mini(plugin.guiConfig().filler().name()));
            meta.lore(GuiItems.miniLore(plugin.guiConfig().filler().lore()));
        });

        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
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
        return new NamespacedKey(plugin, "delete_head_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
    }

    private static final class ConfirmHolder implements InventoryHolder {
        private final HeadId id;
        private Inventory inventory;

        private ConfirmHolder(@NotNull HeadId id) {
            this.id = id;
        }

        private @NotNull HeadId id() {
            return id;
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
