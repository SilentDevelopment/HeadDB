package io.github.silentdevelopment.headdb.paper.gui.favorites;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FavoritesMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private FavoritesMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        open(plugin, player, 0);
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_FAVORITES)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        List<Head> heads = favoriteHeads(plugin, player);
        int pages = pageCount(heads.size());
        int page = clampPage(requestedPage, pages);
        FavoritesHolder holder = new FavoritesHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Favorites " + (page + 1) + "/" + pages, plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderHeads(plugin, player, inventory, heads, page);
        renderControls(plugin, inventory, page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof FavoritesHolder holder)) {
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

        give(plugin, player, id.get());
        return true;
    }

    private static @NotNull List<Head> favoriteHeads(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        List<Head> heads = new ArrayList<>();
        for (HeadId id : plugin.favorites().favorites(player.getUniqueId())) {
            plugin.headRegistry().find(id).ifPresent(head -> {
                if (head.id().isRemote() && plugin.headRegistry().hidden(head.id()) && !plugin.adminModes().enabled(player)) {
                    return;
                }

                if (head.id().isRemote() && !Permissions.canViewCategory(player, head.category())) {
                    return;
                }

                heads.add(head);
            });
        }

        heads.sort(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(heads);
    }

    private static void renderHeads(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<Head> heads, int page) {
        int fromIndex = page * HEAD_SLOTS.length;
        int toIndex = Math.min(heads.size(), fromIndex + HEAD_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Head head = heads.get(index);
            ItemStack item = plugin.itemFactory().create(head);
            plugin.favorites().decorate(player.getUniqueId(), head.id(), item);
            if (plugin.adminModes().enabled(player)) {
                item.editMeta(meta -> {
                    List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                    lore.add(Component.empty());
                    lore.add(GuiItems.idDetail("ID", headIdLabel(head.id())));
                    lore.add(GuiItems.metaDetail("Category", head.category()));
                    meta.lore(lore);
                });
            }
            inventory.setItem(HEAD_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int page, int pages) {
        inventory.setItem(SLOT_BACK, actionItem(plugin, ACTION_BACK, "back"));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull FavoritesHolder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openMain(player);
            return;
        }
        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.page() - 1);
            return;
        }
        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.page() + 1);
        }
    }

    public static void give(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id) {
        if (!Permissions.has(player, Permissions.GUI_HEAD_TAKE)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        Optional<Head> head = plugin.headRegistry().find(id);
        if (head.isEmpty()) {
            player.sendMessage(Component.text("Favorite head no longer exists.", NamedTextColor.RED));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.messages().giveInventoryFull(player, player));
            return;
        }

        if (!plugin.economy().charge(player, head.get(), 1)) {
            return;
        }

        ItemStack item = plugin.itemFactory().create(head.get());
        if (!player.getInventory().addItem(item).isEmpty()) {
            player.sendMessage(plugin.messages().giveInventoryFull(player, player));
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
        ItemStack border = GuiItems.item(org.bukkit.Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
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
        return new org.bukkit.NamespacedKey(plugin, "favorites_menu_action");
    }


    private static @NotNull String headIdLabel(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        return id.display();
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

    private static final class FavoritesHolder implements InventoryHolder {

        private final int page;
        private Inventory inventory;

        private FavoritesHolder(int page) {
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Favorites inventory has not been assigned.");
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
