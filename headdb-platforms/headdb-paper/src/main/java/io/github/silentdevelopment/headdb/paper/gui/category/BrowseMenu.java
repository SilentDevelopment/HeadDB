package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiIconConfig;
import io.github.silentdevelopment.headdb.paper.gui.hidden.HiddenHeadsMenu;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenuState;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class BrowseMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_BROWSE_ALL = "browse-all";
    private static final String ACTION_HIDDEN = "hidden";
    private static final String ACTION_TAGS = "tags";
    private static final String ACTION_COLLECTIONS = "collections";
    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_CREATE_HEAD = "create-head";
    private static final String ACTION_CREATE_CATEGORY = "create-category";
    private static final String ACTION_CUSTOM_HEADS = "custom-heads";
    private static final String ACTION_CATEGORY = "category:";
    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private BrowseMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        open(plugin, player, 0);
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_BROWSE_MENU)) {
            noPermission(plugin, player);
            return;
        }

        List<HeadCategory> categories = categories(plugin, player);
        int pages = pageCount(categories.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        BrowseHolder holder = new BrowseHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(plugin.guiConfig().text("title.browse", "Browse"), plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        renderTopActions(plugin, player, inventory);
        renderCategories(plugin, player, inventory, categories, page);
        renderControls(plugin, player, inventory, categories.size(), page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof BrowseHolder holder)) {
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

        handleAction(plugin, player, holder, action.get(), event.getClick());
        return true;
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull BrowseHolder holder, @NotNull String action, @NotNull ClickType click) {
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
            return;
        }

        if (action.equals(ACTION_BROWSE_ALL)) {
            if (!Permissions.has(player, Permissions.GUI_BROWSE) || !Permissions.canViewAllCategories(player)) {
                noPermission(plugin, player);
                return;
            }

            plugin.guis().openSearch(player, browseRequest(), SearchMenuState.BackTarget.BROWSE);
            return;
        }

        if (action.equals(ACTION_HIDDEN)) {
            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_HIDDEN_HEADS)) {
                noPermission(plugin, player);
                return;
            }

            HiddenHeadsMenu.open(plugin, player);
            return;
        }

        if (action.equals(ACTION_TAGS)) {
            if (!Permissions.has(player, Permissions.GUI_TAGS)) {
                noPermission(plugin, player);
                return;
            }

            TagsMenu.open(plugin, player);
            return;
        }

        if (action.equals(ACTION_COLLECTIONS)) {
            if (!Permissions.has(player, Permissions.GUI_COLLECTIONS)) {
                noPermission(plugin, player);
                return;
            }

            CollectionsMenu.open(plugin, player);
            return;
        }

        if (action.equals(ACTION_SEARCH)) {
            if (!Permissions.has(player, Permissions.GUI_SEARCH) || !Permissions.has(player, Permissions.SEARCH)) {
                noPermission(plugin, player);
                return;
            }

            if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
                plugin.guis().openAdvancedSearch(player, browseRequest(), SearchMenuState.BackTarget.BROWSE);
                return;
            }

            player.closeInventory();
            player.getScheduler().run(plugin, task -> plugin.prompts().request(player, Component.text("Enter a search query.", NamedTextColor.GOLD), query -> {
                plugin.guis().openSearch(player, new SearchRequest(query, Set.of(), Set.of(), Set.of(), Set.of(), HeadSort.RELEVANCE, SortDirection.DESCENDING, 1, 28, false), SearchMenuState.BackTarget.BROWSE);
            }, () -> {
                player.sendMessage(Component.text("Search cancelled.", NamedTextColor.GRAY));
                open(plugin, player, holder.page());
            }), () -> {});
            return;
        }

        if (action.equals(ACTION_CREATE_HEAD)) {
            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CREATE_HEAD)) {
                noPermission(plugin, player);
                return;
            }

            CreateHeadMenu.openNew(plugin, player);
            return;
        }

        if (action.equals(ACTION_CUSTOM_HEADS)) {
            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CUSTOM_HEADS)) {
                noPermission(plugin, player);
                return;
            }

            plugin.guis().openCustomHeads(player);
            return;
        }

        if (action.equals(ACTION_CREATE_CATEGORY)) {
            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN)) {
                noPermission(plugin, player);
                return;
            }

            CreateCategoryMenu.open(plugin, player);
            return;
        }

        if (!action.startsWith(ACTION_CATEGORY)) {
            return;
        }

        String category = action.substring(ACTION_CATEGORY.length());
        Optional<CustomCategory> customCategory = plugin.customCategories().find(category);
        if (customCategory.isPresent() && (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) && plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN)) {
            CreateCategoryMenu.openExisting(plugin, player, customCategory.get().id());
            return;
        }

        if (!Permissions.has(player, Permissions.GUI_CATEGORY_OPEN) || !Permissions.canViewCategory(player, category)) {
            noPermission(plugin, player);
            return;
        }

        if (customCategory.isPresent()) {
            CustomCategoryViewMenu.open(plugin, player, customCategory.get().id(), 0);
            return;
        }

        plugin.guis().openSearch(player, categoryRequest(category), SearchMenuState.BackTarget.BROWSE);
    }

    private static void renderTopActions(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory) {
        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_HIDDEN_HEADS)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.hidden-heads", 35), "hidden-heads", ACTION_HIDDEN);
        }

        if (Permissions.has(player, Permissions.GUI_TAGS)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.tags", 2), "tags", ACTION_TAGS);
        }

        if (Permissions.has(player, Permissions.GUI_BROWSE) && Permissions.canViewAllCategories(player)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.all", 4), "browse-all", ACTION_BROWSE_ALL);
        }

        if (Permissions.has(player, Permissions.GUI_COLLECTIONS)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.collections", 6), "collections", ACTION_COLLECTIONS);
        }

        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.create-category", 18), "create-category", ACTION_CREATE_CATEGORY);
        }

        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_HEADS)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.custom-heads", 26), "custom-heads", ACTION_CUSTOM_HEADS);
        }

        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CREATE_HEAD)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.create-head", 27), "create-head", ACTION_CREATE_HEAD);
        }

        if (Permissions.has(player, Permissions.GUI_SEARCH)) {
            setAction(plugin, inventory, plugin.guiConfig().slot("browse.search", 53), "search", ACTION_SEARCH);
        }
    }

    private static void setAction(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int slot, @NotNull String iconKey, @NotNull String action) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        inventory.setItem(slot, item);
    }

    private static void renderCategories(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<HeadCategory> categories, int page) {
        boolean adminMode = plugin.adminModes().enabled(player);
        Map<String, Integer> counts = plugin.headRegistry().categoryCounts(adminMode);
        Map<String, Head> iconHeads = categoryIconHeads(plugin, categories, adminMode);
        Map<String, CustomCategory> customCategories = customCategories(plugin);
        int fromIndex = page * CATEGORY_SLOTS.length;
        int toIndex = Math.min(categories.size(), fromIndex + CATEGORY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            HeadCategory category = categories.get(index);
            CustomCategory customCategory = customCategories.get(category.id());
            int amount = customCategory == null ? counts.getOrDefault(category.id(), 0) : customCategoryCount(plugin, customCategory, adminMode);
            ItemStack item = customCategory == null ? categoryItem(plugin, category, iconHeads.get(category.id()), amount, adminMode) : customCategoryItem(plugin, category, customCategory, amount, adminMode);
            stamp(plugin, item, ACTION_CATEGORY + category.id());
            inventory.setItem(CATEGORY_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }


    private static int customCategoryCount(@NotNull HeadDBPlugin plugin, @NotNull CustomCategory category, boolean adminMode) {
        int count = 0;
        for (HeadId id : category.headIds()) {
            boolean visible = id.isCustom() && adminMode ? plugin.headRegistry().customHeads().findStored(id).isPresent() : plugin.headRegistry().find(id).isPresent();
            if (visible) {
                count++;
            }
        }
        return count;
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, int categoryCount, int page, int pages) {
        ItemStack back = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("back"));
        stamp(plugin, back, ACTION_BACK);
        inventory.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack previous = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("previous"));
            stamp(plugin, previous, ACTION_PREVIOUS);
            inventory.setItem(SLOT_PREVIOUS, previous);
        }

        ItemStack info = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name("Info", NamedTextColor.GOLD), List.of(
                GuiItems.idDetail("Categories", categoryCount),
                GuiItems.idDetail("Page", (page + 1) + " / " + Math.max(1, pages))
        ));
        inventory.setItem(SLOT_INFO, info);

        if (page + 1 < pages) {
            ItemStack next = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("next"));
            stamp(plugin, next, ACTION_NEXT);
            inventory.setItem(SLOT_NEXT, next);
        }
    }


    private static @NotNull ItemStack customCategoryItem(@NotNull HeadDBPlugin plugin, @NotNull HeadCategory category, @NotNull CustomCategory customCategory, int amount, boolean adminMode) {
        ItemStack item;
        if (customCategory.headIcon()) {
            item = plugin.headRegistry().find(new HeadId(customCategory.headIconId())).map(plugin.itemFactory()::create).orElseGet(() -> GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category")));
        } else {
            item = GuiItems.item(GuiMaterials.itemOr(customCategory.material(), Material.CHEST), Component.empty(), List.of());
        }

        item.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();
            if (!customCategory.description().isBlank()) {
                lore.add(GuiItems.lore(customCategory.description(), NamedTextColor.GRAY));
                lore.add(Component.empty());
            }
            if (adminMode) {
                lore.add(GuiItems.idDetail("Heads", amount));
                lore.add(GuiItems.idDetail("ID", customCategory.id()));
                lore.add(GuiItems.idDetail("Permission", Permissions.category(customCategory.id())));
                if (customCategory.draft()) {
                    lore.add(GuiItems.idDetail("State", "DRAFT"));
                }
                lore.add(Component.empty());
            }
            lore.add(GuiItems.lore("Click to browse this category.", NamedTextColor.GREEN));
            if (adminMode) {
                lore.add(GuiItems.lore("Right-click to edit this custom category.", NamedTextColor.YELLOW));
            }

            meta.displayName(GuiItems.name(customCategory.draft() ? "DRAFT - " + customCategory.name() : customCategory.name(), customCategory.draft() ? NamedTextColor.YELLOW : NamedTextColor.GOLD));
            meta.lore(lore);
            meta.getPersistentDataContainer().remove(HeadItemIds.key(plugin));
        });
        return item;
    }

    private static @NotNull ItemStack categoryItem(@NotNull HeadDBPlugin plugin, @NotNull HeadCategory category, Head iconHead, int amount, boolean adminMode) {
        String iconKey = categoryIconKey(category.id());
        boolean configured = plugin.guiConfig().hasIcon(iconKey);
        GuiIconConfig icon = plugin.guiConfig().iconOrDefault(iconKey, "category");
        ItemStack item = categoryIcon(plugin, iconHead, configured, icon);
        item.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();

            if (configured) {
                lore.addAll(GuiItems.miniLore(replaceAll(icon.lore(), category, amount)));
            } else if (category.description() != null && !category.description().isBlank()) {
                lore.add(Component.text(category.description(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }

            if (adminMode) {
                lore.add(GuiItems.idDetail("Heads", amount));
                lore.add(GuiItems.idDetail("ID", category.id()));
                lore.add(Component.empty());
            }

            lore.add(GuiItems.lore("Click to browse this category.", NamedTextColor.GREEN));
            Component name = configured ? GuiItems.mini(replace(icon.name(), category, amount)) : GuiItems.name(category.name(), NamedTextColor.GOLD);
            meta.displayName(name);
            meta.lore(lore);
            meta.getPersistentDataContainer().remove(HeadItemIds.key(plugin));
            meta.getPersistentDataContainer().set(GuiButtonEditorMenu.iconKey(plugin), PersistentDataType.STRING, iconKey);
        });
        return item;
    }

    private static @NotNull ItemStack categoryIcon(@NotNull HeadDBPlugin plugin, Head iconHead, boolean configured, @NotNull GuiIconConfig icon) {
        if (configured) {
            return GuiHeadIcons.icon(plugin, icon);
        }

        if (iconHead == null) {
            return GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category"));
        }

        return plugin.itemFactory().create(iconHead);
    }

    private static @NotNull String categoryIconKey(@NotNull String categoryId) {
        return "category." + categoryId.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static @NotNull String replace(@NotNull String value, @NotNull HeadCategory category, int amount) {
        return value.replace("%name%", category.name()).replace("%id%", category.id()).replace("%count%", String.valueOf(amount));
    }

    private static @NotNull List<String> replaceAll(@NotNull List<String> values, @NotNull HeadCategory category, int amount) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(replace(value, category, amount));
        }
        return List.copyOf(result);
    }

    private static @NotNull Map<String, Head> categoryIconHeads(@NotNull HeadDBPlugin plugin, @NotNull List<HeadCategory> categories, boolean includeHidden) {
        Map<String, Head> icons = new LinkedHashMap<>();
        Set<String> missing = new HashSet<>();
        for (HeadCategory category : categories) {
            missing.add(category.id());
        }

        if (missing.isEmpty()) {
            return icons;
        }

        for (Head head : plugin.headRegistry().heads(includeHidden)) {
            String category = head.category();
            if (!missing.contains(category)) {
                continue;
            }

            icons.put(category, head);
            missing.remove(category);
            if (missing.isEmpty()) {
                return icons;
            }
        }

        return icons;
    }

    private static @NotNull List<HeadCategory> categories(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        boolean adminMode = plugin.adminModes().enabled(player);
        Map<String, CustomCategory> customById = customCategories(plugin);
        List<HeadCategory> remoteCategories = plugin.headRegistry().categories().stream()
                .filter(category -> !customById.containsKey(category.id()))
                .filter(category -> Permissions.canViewCategory(player, category.id()))
                .sorted(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<HeadCategory> customCategories = plugin.customCategories().listVisible(adminMode).stream()
                .filter(category -> Permissions.canViewCategory(player, category.id()))
                .sorted(Comparator.comparing(CustomCategory::name, String.CASE_INSENSITIVE_ORDER))
                .map(CustomCategory::toHeadCategory)
                .toList();

        List<HeadCategory> result = new ArrayList<>();
        result.addAll(remoteCategories);
        result.addAll(customCategories);
        return List.copyOf(result);
    }

    private static @NotNull Map<String, CustomCategory> customCategories(@NotNull HeadDBPlugin plugin) {
        Map<String, CustomCategory> categories = new LinkedHashMap<>();
        for (CustomCategory category : plugin.customCategories().list()) {
            categories.put(category.id(), category);
        }
        return Map.copyOf(categories);
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) CATEGORY_SLOTS.length);
    }

    private static void fillBorder(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory) {
        if (!plugin.guiConfig().filler().enabled()) {
            return;
        }

        Material material = GuiMaterials.itemOr(plugin.guiConfig().filler().material(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = new ItemStack(material);
        filler.editMeta(meta -> {
            meta.displayName(GuiItems.mini(plugin.guiConfig().filler().name()));
            meta.lore(GuiItems.miniLore(plugin.guiConfig().filler().lore()));
        });
        for (int slot = 0; slot < ROWS * 9; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row != 0 && row != ROWS - 1 && column != 0 && column != 8) {
                continue;
            }
            inventory.setItem(slot, filler);
        }
    }

    private static void stamp(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item, @NotNull String action) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action));
    }

    private static @NotNull Optional<String> readAction(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item) {
        if (GuiMaterials.isAir(item.getType())) {
            return Optional.empty();
        }
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
        return new NamespacedKey(plugin, "browse_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
    }

    private static @NotNull SearchRequest browseRequest() {
        return new SearchRequest("", Set.of(), Set.of(), Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28, false);
    }

    private static @NotNull SearchRequest categoryRequest(@NotNull String category) {
        return new SearchRequest("", Set.of(), Set.of(category), Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28, true);
    }

    private static final class BrowseHolder implements InventoryHolder {

        private final int page;
        private Inventory inventory;

        private BrowseHolder(int page) {
            this.page = page;
        }

        private int page() {
            return page;
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
