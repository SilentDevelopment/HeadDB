package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.material.MaterialSelectionMenu;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class MoreCategoriesMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_ADD = 49;
    private static final int SLOT_REMOVE = 50;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_REMOVE_MODE = "remove-mode";
    private static final String ACTION_SELECT = "select:";
    private static final String ACTION_REMOVE = "remove:";
    private static final String ACTION_EDIT_NAME = "edit-name";
    private static final String ACTION_EDIT_MATERIAL = "edit-material";
    private static final String ACTION_EDIT_HEAD_ICON = "edit-head-icon";
    private static final String ACTION_EDIT_VIEW_HEADS = "edit-view-heads";
    private static final String ACTION_EDIT_SAVE = "edit-save";
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private MoreCategoriesMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        open(plugin, player, MoreCategoryMode.BROWSE, 0);
    }

    public static void openEdit(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String id, @NotNull String name, @NotNull String material) {
        if (!canAdminCategories(plugin, player)) {
            noPermission(plugin, player);
            return;
        }

        openEditor(plugin, player, new CategoryEditorHolder(id, name, material));
    }

    public static void openCreate(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String name, @NotNull String categoryId, @NotNull String material) {
        openEdit(plugin, player, categoryId, name, material);
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MoreCategoryMode mode, int requestedPage) {
        if (!Permissions.has(player, Permissions.GUI_MORE_CATEGORIES)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        List<CustomCategory> categories = plugin.customCategories().listVisible(plugin.adminModes().enabled(player)).stream().filter(category -> Permissions.canViewCategory(player, category.id())).toList();
        int pages = pageCount(categories.size());
        int page = clampPage(requestedPage, pages);
        MoreCategoriesHolder holder = new MoreCategoriesHolder(mode, page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(mode == MoreCategoryMode.REMOVE ? "Remove Category" : "More Categories", plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderEntries(plugin, inventory, categories, mode, page, plugin.adminModes().enabled(player));
        renderControls(plugin, player, inventory, mode, page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    private static void openEditor(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryEditorHolder holder) {
        String title = holder.id().isBlank() ? "Create Category" : "Category: " + displayName(holder.name(), holder.id());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title, plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillAll(inventory);
        inventory.setItem(plugin.guiConfig().slot("more.category-create.name", 20), editAction(plugin, ACTION_EDIT_NAME, "category-name", "Name", holder.name().isBlank() ? "Not set" : holder.name()));
        inventory.setItem(plugin.guiConfig().slot("more.category-create.material", 23), materialAction(plugin, holder.material()));
        inventory.setItem(plugin.guiConfig().slot("more.category-create.head-icon", 24), headIconAction(plugin, holder.material()));
        inventory.setItem(plugin.guiConfig().slot("more.category-create.view-heads", 31), editAction(plugin, ACTION_EDIT_VIEW_HEADS, "category-view-heads", "View Heads", holder.id().isBlank() ? "Save this category before managing heads." : "View, add, or remove heads."));
        inventory.setItem(plugin.guiConfig().slot("more.category-create.save", 40), saveAction(plugin));
        inventory.setItem(plugin.guiConfig().slot("more.category-create.back", 45), actionItem(plugin, ACTION_BACK, "back"));

        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CategoryEditorHolder editor) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return true;
            }

            ItemStack item = event.getCurrentItem();
            if (item == null || GuiMaterials.isAir(item.getType())) {
                return true;
            }

            Optional<String> action = readAction(plugin, item);
            action.ifPresent(value -> {
                plugin.sounds().playGuiAction(player, value);
                handleEditorAction(plugin, player, editor, value);
            });
            return true;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof MoreCategoriesHolder holder)) {
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

        handleAction(plugin, player, holder, event.getClick(), action.get());
        return true;
    }

    private static void renderEntries(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull List<CustomCategory> categories, @NotNull MoreCategoryMode mode, int page, boolean adminMode) {
        int fromIndex = page * ENTRY_SLOTS.length;
        int toIndex = Math.min(categories.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            CustomCategory category = categories.get(index);
            String action = mode == MoreCategoryMode.REMOVE ? ACTION_REMOVE + category.id() : ACTION_SELECT + category.id();
            ItemStack item = categoryItem(plugin, category, mode, adminMode);
            stamp(plugin, item, action);
            inventory.setItem(ENTRY_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static @NotNull ItemStack categoryItem(@NotNull HeadDBPlugin plugin, @NotNull CustomCategory category, @NotNull MoreCategoryMode mode, boolean adminMode) {
        ItemStack item = categoryIcon(plugin, category);
        item.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            lore.add(GuiItems.idDetail("Heads", category.headIds().size()));
            if (category.draft()) {
                lore.add(GuiItems.idDetail("State", "DRAFT"));
            }
            lore.add(GuiItems.metaDetail("Icon", iconLabel(category)));
            lore.add(Component.empty());

            if (mode == MoreCategoryMode.REMOVE) {
                lore.add(GuiItems.lore("Left-click to delete this custom category.", NamedTextColor.RED));
            } else {
                lore.add(GuiItems.lore("Left-click to browse.", NamedTextColor.GREEN));
                if (adminMode) {
                    lore.add(GuiItems.lore("Right-click to edit.", NamedTextColor.YELLOW));
                }
            }

            meta.displayName(GuiItems.name(category.draft() ? "DRAFT - " + category.name() : category.name(), category.draft() ? NamedTextColor.YELLOW : mode == MoreCategoryMode.REMOVE ? NamedTextColor.RED : NamedTextColor.GOLD));
            meta.lore(lore);
        });
        return item;
    }

    private static @NotNull ItemStack categoryIcon(@NotNull HeadDBPlugin plugin, @NotNull CustomCategory category) {
        if (category.headIcon()) {
            try {
                HeadId id = parseHeadId(category.headIconId());
                Optional<Head> head = plugin.headRegistry().find(id);
                if (head.isPresent()) {
                    return plugin.itemFactory().create(head.get());
                }
            } catch (IllegalArgumentException ignored) {
                // Fall back to material below.
            }
        }

        return GuiItems.item(GuiMaterials.itemOr(category.material(), Material.CHEST), GuiItems.name(category.name(), NamedTextColor.GOLD), List.of());
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull MoreCategoryMode mode, int page, int pages) {
        inventory.setItem(SLOT_BACK, actionItem(plugin, ACTION_BACK, "back"));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }
        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN)) {
            inventory.setItem(SLOT_ADD, actionItem(plugin, ACTION_ADD, "category-add"));
            inventory.setItem(SLOT_REMOVE, actionItem(plugin, ACTION_REMOVE_MODE, mode == MoreCategoryMode.REMOVE ? "back" : "category-remove"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MoreCategoriesHolder holder, @NotNull ClickType click, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            if (holder.mode() != MoreCategoryMode.BROWSE) {
                open(plugin, player, MoreCategoryMode.BROWSE, holder.page());
                return;
            }
            plugin.guis().openMain(player);
            return;
        }
        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.mode(), holder.page() - 1);
            return;
        }
        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.mode(), holder.page() + 1);
            return;
        }
        if (action.equals(ACTION_ADD)) {
            CreateCategoryMenu.open(plugin, player);
            return;
        }
        if (action.equals(ACTION_REMOVE_MODE)) {
            if (!canAdminCategories(plugin, player)) {
                noPermission(plugin, player);
                return;
            }
            open(plugin, player, holder.mode() == MoreCategoryMode.REMOVE ? MoreCategoryMode.BROWSE : MoreCategoryMode.REMOVE, holder.page());
            return;
        }
        if (action.startsWith(ACTION_SELECT)) {
            String id = action.substring(ACTION_SELECT.length());
            Optional<CustomCategory> existing = plugin.customCategories().find(id);
            if (existing.isEmpty()) {
                open(plugin, player, MoreCategoryMode.BROWSE, 0);
                return;
            }
            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                if (!canAdminCategories(plugin, player)) {
                    noPermission(plugin, player);
                    return;
                }
                CustomCategory category = existing.get();
                CreateCategoryMenu.openExisting(plugin, player, category.id());
                return;
            }

            CustomCategory category = existing.get();
            CustomCategoryViewMenu.open(plugin, player, category.id(), 0);
            return;
        }
        if (action.startsWith(ACTION_REMOVE)) {
            String id = action.substring(ACTION_REMOVE.length());
            if (holder.mode() == MoreCategoryMode.CONFIRM_REMOVE) {
                if (!canAdminCategories(plugin, player)) {
                    noPermission(plugin, player);
                    return;
                }
                plugin.customCategories().delete(id);
                player.sendMessage(Component.text("Custom category deleted: ", NamedTextColor.GRAY).append(Component.text(id, NamedTextColor.GOLD)));
                open(plugin, player, MoreCategoryMode.BROWSE, holder.page());
                return;
            }
            openConfirm(plugin, player, id, holder.page());
        }
    }

    private static void handleEditorAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryEditorHolder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            open(plugin, player, MoreCategoryMode.BROWSE, 0);
            return;
        }
        if (action.equals(ACTION_EDIT_NAME)) {
            promptField(plugin, player, holder, "Enter the custom category display name.", value -> openEditor(plugin, player, holder.withName(value)));
            return;
        }
        if (action.equals(ACTION_EDIT_MATERIAL)) {
            MaterialSelectionMenu.openForCategoryCreate(plugin, player, holder.name(), holder.id(), holder.material());
            return;
        }
        if (action.equals(ACTION_EDIT_HEAD_ICON)) {
            promptHeadIcon(plugin, player, holder);
            return;
        }
        if (action.equals(ACTION_EDIT_VIEW_HEADS)) {
            if (holder.id().isBlank()) {
                player.sendMessage(Component.text("Save this custom category before managing heads.", NamedTextColor.RED));
                openEditor(plugin, player, holder);
                return;
            }
            CategoryMembersMenu.open(plugin, player, holder.id(), 0);
            return;
        }
        if (action.equals(ACTION_EDIT_SAVE)) {
            saveEdited(plugin, player, holder);
        }
    }

    private static void saveEdited(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryEditorHolder holder) {
        if (!canAdminCategories(plugin, player)) {
            noPermission(plugin, player);
            return;
        }
        if (holder.name().isBlank()) {
            player.sendMessage(Component.text("Set a name before saving.", NamedTextColor.RED));
            openEditor(plugin, player, holder);
            return;
        }

        String id = holder.id().isBlank() ? plugin.customCategories().nextId() : holder.id();
        Set<HeadId> existingMembers = plugin.customCategories().find(id).map(CustomCategory::headIds).orElse(Set.of());
        plugin.customCategories().save(new CustomCategory(id, holder.name(), holder.material(), existingMembers));
        player.sendMessage(Component.text("Custom category saved: ", NamedTextColor.GRAY).append(Component.text(holder.name(), NamedTextColor.GOLD)));
        openEdit(plugin, player, id, holder.name(), holder.material());
    }

    private static void promptHeadIcon(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryEditorHolder holder) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter a head id for the category icon, or none to clear.", NamedTextColor.GOLD), value -> {
            if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("clear")) {
                openEditor(plugin, player, holder.withMaterial("CHEST"));
                return;
            }

            HeadId id;
            try {
                id = parseHeadId(value);
            } catch (IllegalArgumentException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                openEditor(plugin, player, holder);
                return;
            }

            if (plugin.headRegistry().find(id).isEmpty()) {
                player.sendMessage(Component.text("Unknown head: ", NamedTextColor.RED).append(Component.text(id.display(), NamedTextColor.GOLD)));
                openEditor(plugin, player, holder);
                return;
            }

            openEditor(plugin, player, holder.withMaterial("HEAD:" + id));
        }, () -> openEditor(plugin, player, holder));
    }

    private static void promptField(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryEditorHolder holder, @NotNull String message, @NotNull java.util.function.Consumer<String> callback) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), callback, () -> openEditor(plugin, player, holder));
    }

    private static void openConfirm(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String id, int page) {
        MoreCategoriesHolder holder = new MoreCategoriesHolder(MoreCategoryMode.CONFIRM_REMOVE, page);
        Inventory inventory = Bukkit.createInventory(holder, 27, GuiTitles.title("Confirm Delete", plugin.adminModes().enabled(player)));
        holder.inventory(inventory);
        fillAll(inventory);
        inventory.setItem(11, actionItem(plugin, ACTION_REMOVE + id, "confirm-yes"));
        inventory.setItem(15, actionItem(plugin, ACTION_BACK, "confirm-no"));
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    private static boolean canAdminCategories(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        return plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN);
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
    }

    private static @NotNull ItemStack editAction(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey, @NotNull String label, @NotNull String value) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.name(label, NamedTextColor.GOLD), List.of(GuiItems.lore(value, NamedTextColor.GRAY), GuiItems.lore("Click to edit.", NamedTextColor.GREEN)));
        stamp(plugin, item, action);
        return item;
    }

    private static @NotNull ItemStack saveAction(@NotNull HeadDBPlugin plugin) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category-save"), GuiItems.name("Save", NamedTextColor.GREEN), List.of());
        stamp(plugin, item, ACTION_EDIT_SAVE);
        return item;
    }

    private static @NotNull ItemStack materialAction(@NotNull HeadDBPlugin plugin, @NotNull String iconValue) {
        String label = iconValue.startsWith("HEAD:") ? "Using head icon" : GuiMaterials.itemOr(iconValue, Material.CHEST).name();
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category-material"), GuiItems.name("Icon Material", NamedTextColor.GOLD), List.of(GuiItems.lore(label, NamedTextColor.GRAY), GuiItems.lore("Click to select a material icon.", NamedTextColor.GREEN)));
        stamp(plugin, item, ACTION_EDIT_MATERIAL);
        return item;
    }

    private static @NotNull ItemStack headIconAction(@NotNull HeadDBPlugin plugin, @NotNull String iconValue) {
        String label = iconValue.startsWith("HEAD:") ? iconValue.substring("HEAD:".length()) : "Not set";
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category-head-icon"), GuiItems.name("Icon Head", NamedTextColor.GOLD), List.of(GuiItems.lore(label, NamedTextColor.GRAY), GuiItems.lore("Click to use a head as the icon.", NamedTextColor.GREEN)));
        stamp(plugin, item, ACTION_EDIT_HEAD_ICON);
        return item;
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        return item;
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

    private static @NotNull HeadId parseHeadId(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.matches("[1-9][0-9]*")) {
            return HeadId.remote(trimmed);
        }
        return new HeadId(trimmed);
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

    private static void fillAll(@NotNull Inventory inventory) {
        ItemStack border = GuiItems.item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, border.clone());
        }
    }

    private static @NotNull String displayName(@NotNull String name, @NotNull String fallback) {
        if (!name.isBlank()) {
            return name;
        }
        return fallback;
    }

    private static @NotNull String iconLabel(@NotNull CustomCategory category) {
        if (category.headIcon()) {
            return category.headIconId();
        }

        return GuiMaterials.itemOr(category.material(), Material.CHEST).name();
    }

    private static @NotNull org.bukkit.NamespacedKey actionKey(@NotNull HeadDBPlugin plugin) {
        return new org.bukkit.NamespacedKey(plugin, "more_categories_action");
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) ENTRY_SLOTS.length);
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

    private enum MoreCategoryMode {
        BROWSE,
        REMOVE,
        CONFIRM_REMOVE
    }

    private static final class MoreCategoriesHolder implements InventoryHolder {

        private final MoreCategoryMode mode;
        private final int page;
        private Inventory inventory;

        private MoreCategoriesHolder(@NotNull MoreCategoryMode mode, int page) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("More categories inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull MoreCategoryMode mode() {
            return mode;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }

    private static final class CategoryEditorHolder implements InventoryHolder {

        private final String id;
        private final String name;
        private final String material;
        private Inventory inventory;

        private CategoryEditorHolder(@NotNull String id, @NotNull String name, @NotNull String material) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
            this.material = Objects.requireNonNull(material, "material");
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Category editor inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull String id() {
            return id;
        }

        private @NotNull String name() {
            return name;
        }

        private @NotNull String material() {
            return material;
        }

        private @NotNull CategoryEditorHolder withName(@NotNull String name) {
            return new CategoryEditorHolder(id, name, material);
        }

        private @NotNull CategoryEditorHolder withMaterial(@NotNull String material) {
            return new CategoryEditorHolder(id, name, material);
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
