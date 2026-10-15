package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenuState;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CategoryFacetMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_EMPTY = 22;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_STYLE = "style:";
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private CategoryFacetMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryFacet facet) {
        open(plugin, player, facet, 0);
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryFacet facet, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(facet, "facet");

        if (!canOpen(player, facet)) {
            noPermission(plugin, player);
            return;
        }

        open(plugin, player, facet, requestedPage, entries(plugin, player, facet));
    }

    public static void openForTag(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryFacet facet, @NotNull String tagId) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(facet, "facet");
        Objects.requireNonNull(tagId, "tagId");

        if (!canOpen(player, facet)) {
            noPermission(plugin, player);
            return;
        }

        List<FacetEntry> entries = entries(plugin, player, facet);
        int requestedPage = 0;

        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).tag().id().equalsIgnoreCase(tagId)) {
                requestedPage = index / ENTRY_SLOTS.length;
                break;
            }
        }

        open(plugin, player, facet, requestedPage, entries);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder(false) instanceof Holder holder)) {
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

        Optional<CategoryFacet> facet = CategoryFacetRegistry.find(holder.categoryId());
        if (facet.isEmpty()) {
            plugin.guis().openBrowse(player);
            return true;
        }

        plugin.sounds().playGuiAction(player, action.get());
        handleAction(plugin, player, holder, facet.get(), action.get());
        return true;
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryFacet facet, int requestedPage, @NotNull List<FacetEntry> entries) {
        int pages = pageCount(entries.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Holder holder = new Holder(facet.categoryId(), page);
        String title = plugin.guiConfig().text("title.category-styles-page", "%name% %page%/%pages%")
                .replace("%name%", facet.title())
                .replace("%page%", String.valueOf(page + 1))
                .replace("%pages%", String.valueOf(Math.max(1, pages)));
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title, plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        renderEntries(plugin, player, inventory, entries, page);

        if (entries.isEmpty()) {
            inventory.setItem(SLOT_EMPTY, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("empty"), GuiItems.name("No Styles Found", NamedTextColor.RED), List.of(
                    GuiItems.lore("No matching styles are available.", NamedTextColor.GRAY)
            )));
        }

        renderControls(plugin, inventory, entries.size(), page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull CategoryFacet facet, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openSearch(player, categoryRequest(facet.categoryId()), SearchMenuState.BackTarget.BROWSE);
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, facet, holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, facet, holder.page() + 1);
            return;
        }

        if (!action.startsWith(ACTION_STYLE)) {
            return;
        }

        String tagId = action.substring(ACTION_STYLE.length());
        if (!facet.matchesTag(tagId)) {
            return;
        }

        plugin.guis().openSearch(player, styleRequest(facet.categoryId(), tagId), SearchMenuState.BackTarget.CATEGORY_FACET);
    }

    private static void renderEntries(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<FacetEntry> entries, int page) {
        boolean adminMode = plugin.adminModes().enabled(player);
        int fromIndex = page * ENTRY_SLOTS.length;
        int toIndex = Math.min(entries.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            FacetEntry entry = entries.get(index);
            ItemStack item = entryItem(plugin, entry, adminMode);
            stamp(plugin, item, ACTION_STYLE + entry.tag().id());
            inventory.setItem(ENTRY_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static @NotNull ItemStack entryItem(@NotNull HeadDBPlugin plugin, @NotNull FacetEntry entry, boolean adminMode) {
        ItemStack item;

        try {
            item = plugin.itemFactory().create(entry.iconHead());
        } catch (IllegalArgumentException exception) {
            item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("category-style"));
        }

        item.editMeta(meta -> {
            List<Component> lore = new ArrayList<>();

            if (adminMode) {
                lore.add(GuiItems.idDetail("Heads", entry.count()));
                lore.add(GuiItems.idDetail("Tag", entry.tag().id()));
                lore.add(Component.empty());
            }

            lore.add(GuiItems.lore("Click to browse this style.", NamedTextColor.GREEN));
            meta.displayName(GuiItems.name(entry.name(), NamedTextColor.GOLD));
            meta.lore(lore);
            meta.getPersistentDataContainer().remove(HeadItemIds.key(plugin));
        });

        return item;
    }

    private static @NotNull List<FacetEntry> entries(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CategoryFacet facet) {
        Map<String, HeadTag> tags = new HashMap<>();

        for (HeadTag tag : plugin.headRegistry().tags()) {
            if (facet.matchesTag(tag.id())) {
                tags.put(tag.id(), tag);
            }
        }

        Map<String, Integer> counts = new HashMap<>();
        Map<String, Head> iconHeads = new HashMap<>();
        boolean includeHidden = plugin.adminModes().enabled(player);

        for (Head head : plugin.headRegistry().heads(includeHidden)) {
            if (!head.category().equalsIgnoreCase(facet.categoryId())) {
                continue;
            }

            for (String tagId : head.tags()) {
                if (!tags.containsKey(tagId)) {
                    continue;
                }

                counts.merge(tagId, 1, Integer::sum);
                iconHeads.putIfAbsent(tagId, head);
            }
        }

        List<FacetEntry> result = new ArrayList<>();

        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            HeadTag tag = tags.get(count.getKey());
            Head iconHead = iconHeads.get(count.getKey());

            if (tag == null || iconHead == null) {
                continue;
            }

            result.add(new FacetEntry(tag, facet.displayName(tag.id(), tag.name()), count.getValue(), iconHead));
        }

        result.sort(Comparator.comparing(FacetEntry::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int entryCount, int page, int pages) {
        ItemStack back = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("back"));
        stamp(plugin, back, ACTION_BACK);
        inventory.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack previous = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("previous"));
            stamp(plugin, previous, ACTION_PREVIOUS);
            inventory.setItem(SLOT_PREVIOUS, previous);
        }

        inventory.setItem(SLOT_INFO, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name("Info", NamedTextColor.GOLD), List.of(
                GuiItems.idDetail("Styles", entryCount),
                GuiItems.idDetail("Page", (page + 1) + " / " + Math.max(1, pages))
        )));

        if (page + 1 < pages) {
            ItemStack next = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("next"));
            stamp(plugin, next, ACTION_NEXT);
            inventory.setItem(SLOT_NEXT, next);
        }
    }

    private static boolean canOpen(@NotNull Player player, @NotNull CategoryFacet facet) {
        return Permissions.has(player, Permissions.GUI_CATEGORY_OPEN) && Permissions.canViewCategory(player, facet.categoryId());
    }

    private static @NotNull SearchRequest categoryRequest(@NotNull String categoryId) {
        return SearchRequest.lockedCategory("", Set.of(), categoryId, Set.of(), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28);
    }

    private static @NotNull SearchRequest styleRequest(@NotNull String categoryId, @NotNull String tagId) {
        return SearchRequest.lockedCategory("", Set.of(), categoryId, Set.of(tagId), Set.of(), HeadSort.ID, SortDirection.ASCENDING, 1, 28);
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) entries / (double) ENTRY_SLOTS.length);
    }

    private static void fillBorder(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory) {
        if (!plugin.guiConfig().filler().enabled()) {
            return;
        }

        Material material = GuiMaterials.itemOr(plugin.guiConfig().filler().material(), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = GuiItems.item(material, GuiItems.mini(plugin.guiConfig().filler().name()), GuiItems.miniLore(plugin.guiConfig().filler().lore()));

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
        return new NamespacedKey(plugin, "category_facet_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
    }

    private record FacetEntry(@NotNull HeadTag tag, @NotNull String name, int count, @NotNull Head iconHead) {}

    private static final class Holder implements InventoryHolder {

        private final String categoryId;
        private final int page;
        private Inventory inventory;

        private Holder(@NotNull String categoryId, int page) {
            this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
            this.page = page;
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

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
