package io.github.silentdevelopment.headdb.paper.gui.hidden;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiLabels;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class HiddenHeadsMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_BACK = "back";
    private static final String ACTION_SHOW_ALL = "show_all";
    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private HiddenHeadsMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        open(plugin, player, 0);
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_HIDDEN_HEADS) || !plugin.adminModes().enabled(player)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        List<Head> heads = plugin.headRegistry().hiddenHeads();
        int pages = pageCount(heads.size());
        int page = clampPage(requestedPage, pages);
        HiddenHeadsHolder holder = new HiddenHeadsHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title(plugin, page, pages), true));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderHeads(plugin, player, inventory, heads, page);
        renderControls(plugin, inventory, heads.size(), page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof HiddenHeadsHolder holder)) {
            return false;
        }

        event.setCancelled(true);
        if (!Permissions.has(player, Permissions.GUI_HIDDEN_HEADS) || !plugin.adminModes().enabled(player)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
            player.closeInventory();
            return true;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return true;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || GuiMaterials.isAir(item.getType())) {
            return true;
        }

        Optional<String> action = readAction(plugin, item);
        if (action.isPresent()) {
            plugin.sounds().playGuiAction(player, action.get());
            handleAction(plugin, player, holder, action.get());
            return true;
        }

        Optional<HeadId> id = HeadItemIds.read(plugin, item);
        if (id.isEmpty()) {
            return true;
        }

        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (!Permissions.has(player, Permissions.FAVORITES_TOGGLE)) {
                player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
                plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
                return true;
            }

            boolean added = plugin.favorites().toggle(player.getUniqueId(), id.get());
            plugin.sounds().play(player, added ? io.github.silentdevelopment.headdb.paper.sound.SoundKey.FAVORITE_ADD : io.github.silentdevelopment.headdb.paper.sound.SoundKey.FAVORITE_REMOVE);
            open(plugin, player, holder.page());
            return true;
        }

        RemoteHeadOverride override = plugin.headRegistry().overrides().find(id.get()).orElse(RemoteHeadOverride.empty(id.get(), player.getUniqueId()));
        plugin.headRegistry().overrides().save(override.withHidden(false, player.getUniqueId()));
        plugin.headRegistry().onLocalMutation();
        plugin.clearItemCache();
        plugin.clearSearchCache();
        player.sendMessage(Component.text("Head shown: ", NamedTextColor.GRAY).append(Component.text(GuiLabels.head(plugin, player, id.get()), NamedTextColor.GOLD)));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.SHOW_HEAD);
        open(plugin, player, holder.page());
        return true;
    }

    private static void renderHeads(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<Head> heads, int page) {
        int fromIndex = page * HEAD_SLOTS.length;
        int toIndex = Math.min(heads.size(), fromIndex + HEAD_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Head head = heads.get(index);
            ItemStack item = plugin.itemFactory().create(head);
            plugin.favorites().decorate(player.getUniqueId(), head.id(), item);
            item.editMeta(meta -> {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                lore.add(GuiItems.idDetail("ID", headIdLabel(head.id())));
                lore.add(GuiItems.metaDetail("Category", head.category()));
                meta.lore(lore);
            });
            inventory.setItem(HEAD_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int total, int page, int pages) {
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }

        inventory.setItem(SLOT_INFO, GuiItems.item(Material.BOOK, GuiItems.name("Hidden Heads", NamedTextColor.GOLD), List.of(GuiItems.idDetail("Heads", total), GuiItems.lore("Click a head to show it.", NamedTextColor.GREEN))));
        inventory.setItem(SLOT_BACK, actionItem(plugin, ACTION_BACK, "back"));

        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }

        inventory.setItem(plugin.guiConfig().slot("hidden.show-all", 53), actionItem(plugin, ACTION_SHOW_ALL, "show-all"));
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HiddenHeadsHolder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openBrowse(player);
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.page() + 1);
            return;
        }

        if (action.equals(ACTION_SHOW_ALL)) {
            for (Head head : plugin.headRegistry().hiddenHeads()) {
                plugin.headRegistry().overrides().find(head.id()).ifPresent(override -> plugin.headRegistry().overrides().save(override.withHidden(false, player.getUniqueId())));
            }

            plugin.headRegistry().onLocalMutation();
            plugin.clearItemCache();
            plugin.clearSearchCache();
            player.sendMessage(Component.text("All hidden heads were shown.", NamedTextColor.GREEN));
            open(plugin, player, 0);
        }
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
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

    private static void fillBorder(@NotNull Inventory inventory) {
        ItemStack border = GuiItems.item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row != 0 && row != ROWS - 1 && column != 0 && column != 8) {
                continue;
            }

            inventory.setItem(slot, border.clone());
        }
    }

    private static @NotNull org.bukkit.NamespacedKey actionKey(@NotNull HeadDBPlugin plugin) {
        return new org.bukkit.NamespacedKey(plugin, "hidden_menu_action");
    }


    private static @NotNull String headIdLabel(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        return id.display();
    }

    private static @NotNull String title(@NotNull HeadDBPlugin plugin, int page, int pages) {
        return plugin.guiConfig().text("title.hidden-heads-page", "Hidden Heads %page%/%pages%").replace("%page%", String.valueOf(page + 1)).replace("%pages%", String.valueOf(Math.max(1, pages)));
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) entries / (double) HEAD_SLOTS.length);
    }

    private static int clampPage(int page, int pages) {
        if (page < 0) {
            return 0;
        }

        if (page >= pages) {
            return pages - 1;
        }

        return page;
    }

    private static final class HiddenHeadsHolder implements InventoryHolder {

        private final int page;
        private Inventory inventory;

        private HiddenHeadsHolder(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Hidden heads inventory has not been assigned.");
            }

            return currentInventory;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
