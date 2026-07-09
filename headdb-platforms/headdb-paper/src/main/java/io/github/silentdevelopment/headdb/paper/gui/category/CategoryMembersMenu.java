package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiLabels;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CategoryMembersMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_ADD = 49;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_REMOVE = "remove:";
    private static final int[] MEMBER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private CategoryMembersMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(categoryId, "categoryId");

        Optional<CustomCategory> existing = plugin.customCategories().find(categoryId);
        if (existing.isEmpty()) {
            MoreCategoriesMenu.open(plugin, player);
            return;
        }

        CustomCategory category = existing.get();
        List<HeadId> ids = new ArrayList<>(category.headIds());
        int pages = pageCount(ids.size());
        int page = clampPage(requestedPage, pages);
        Holder holder = new Holder(category.id(), page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Heads: " + category.name(), plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderMembers(plugin, inventory, ids, page);
        renderControls(plugin, inventory, page, pages);
        player.openInventory(inventory);
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

        handleAction(plugin, player, holder, action.get());
        return true;
    }

    private static void renderMembers(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull List<HeadId> ids, int page) {
        int fromIndex = page * MEMBER_SLOTS.length;
        int toIndex = Math.min(ids.size(), fromIndex + MEMBER_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            HeadId id = ids.get(index);
            ItemStack item = memberItem(plugin, id);
            stamp(plugin, item, ACTION_REMOVE + id);
            inventory.setItem(MEMBER_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static @NotNull ItemStack memberItem(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        Optional<Head> head = id.isCustom() ? plugin.headRegistry().customHeads().findStored(id).map(stored -> stored.toHead()) : plugin.headRegistry().find(id);
        if (head.isPresent()) {
            ItemStack item = plugin.itemFactory().create(head.get());
            item.editMeta(meta -> {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                lore.add(GuiItems.idDetail("ID", id.display()));
                lore.add(GuiItems.lore("Click to remove from this custom category.", NamedTextColor.RED));
                meta.lore(lore);
            });
            return item;
        }

        return GuiItems.item(Material.PAPER, GuiItems.name(id.display(), NamedTextColor.RED), List.of(GuiItems.lore("Missing head.", NamedTextColor.GRAY), GuiItems.lore("Click to remove this stale entry.", NamedTextColor.RED)));
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int page, int pages) {
        inventory.setItem(SLOT_BACK, actionItem(plugin, ACTION_BACK, "back"));
        inventory.setItem(SLOT_ADD, actionItem(plugin, ACTION_ADD, "category-add"));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.customCategories().find(holder.categoryId()).ifPresentOrElse(
                    category -> CreateCategoryMenu.openExisting(plugin, player, category.id()),
                    () -> MoreCategoriesMenu.open(plugin, player)
            );
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.categoryId(), holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.categoryId(), holder.page() + 1);
            return;
        }

        if (action.equals(ACTION_ADD)) {
            CategoryHeadPickerMenu.open(plugin, player, holder.categoryId(), 0);
            return;
        }

        if (!action.startsWith(ACTION_REMOVE)) {
            return;
        }

        HeadId id;
        try {
            id = new HeadId(action.substring(ACTION_REMOVE.length()));
        } catch (IllegalArgumentException exception) {
            open(plugin, player, holder.categoryId(), holder.page());
            return;
        }

        plugin.customCategories().removeHead(holder.categoryId(), id);
        player.sendMessage(Component.text("Head removed: ", NamedTextColor.GRAY).append(Component.text(GuiLabels.head(plugin, player, id), NamedTextColor.GOLD)));
        open(plugin, player, holder.categoryId(), holder.page());
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        return item;
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

    private static @NotNull org.bukkit.NamespacedKey actionKey(@NotNull HeadDBPlugin plugin) {
        return new org.bukkit.NamespacedKey(plugin, "category_members_action");
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) MEMBER_SLOTS.length);
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

    private static final class Holder implements InventoryHolder {

        private final String categoryId;
        private final int page;
        private Inventory inventory;

        private Holder(@NotNull String categoryId, int page) {
            this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Category members inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull String categoryId() {
            return categoryId;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
