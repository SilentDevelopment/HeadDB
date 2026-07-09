package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.local.taxonomy.CustomTaxonomyEntry;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.gui.search.SearchMenuState;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class TagsMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CREATE = 27;
    private static final int SLOT_FILTER = 52;
    private static final int SLOT_SEARCH = 53;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_FILTER = "filter";
    private static final String ACTION_SEARCH = "search";
    private static final String ACTION_ENTRY = "tag:";
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private TagsMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        open(plugin, player, 0, false, "");
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, int requestedPage, boolean customOnly, @NotNull String query) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(query, "query");

        if (!Permissions.has(player, Permissions.GUI_TAGS)) {
            noPermission(plugin, player);
            return;
        }

        boolean adminMode = plugin.adminModes().enabled(player);
        Set<String> customIds = customIds(plugin);
        Map<String, Integer> counts = counts(visibleHeads(plugin, player));
        List<HeadTag> entries = plugin.headRegistry().tags().stream()
                .filter(entry -> adminMode || counts.containsKey(entry.id()))
                .filter(entry -> !customOnly || customIds.contains(entry.id()))
                .filter(entry -> matches(entry.id(), entry.name(), query))
                .sorted(Comparator.comparing(HeadTag::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int pages = pageCount(entries.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Holder holder = new Holder(page, customOnly, query.trim());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title(plugin, page, pages, customOnly, query), adminMode));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        renderEntries(plugin, player, inventory, entries, counts, customIds, page);
        renderControls(plugin, player, inventory, entries.size(), page, pages, customOnly, query);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
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

        plugin.sounds().playGuiAction(player, action.get());

        handleAction(plugin, player, holder, action.get());
        return true;
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openBrowse(player);
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.page() - 1, holder.customOnly(), holder.query());
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.page() + 1, holder.customOnly(), holder.query());
            return;
        }

        if (action.equals(ACTION_CREATE)) {
            if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CREATE_TAG)) {
                noPermission(plugin, player);
                return;
            }

            CreateTaxonomyMenu.openTag(plugin, player);
            return;
        }

        if (action.equals(ACTION_FILTER)) {
            if (!plugin.adminModes().enabled(player)) {
                return;
            }

            open(plugin, player, 0, !holder.customOnly(), holder.query());
            return;
        }

        if (action.equals(ACTION_SEARCH)) {
            player.closeInventory();
            player.getScheduler().run(plugin, task -> plugin.prompts().request(player, Component.text("Enter tag search text.", NamedTextColor.GOLD), value -> open(plugin, player, 0, holder.customOnly(), value), () -> open(plugin, player, holder.page(), holder.customOnly(), holder.query())), () -> {});
            return;
        }

        if (!action.startsWith(ACTION_ENTRY)) {
            return;
        }

        String id = action.substring(ACTION_ENTRY.length());
        plugin.guis().openSearch(player, new SearchRequest("", Set.of(), Set.of(), Set.of(id), Set.of(), HeadSort.NAME, SortDirection.ASCENDING, 1, 28, false), SearchMenuState.BackTarget.TAGS);
    }

    private static void renderEntries(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<HeadTag> entries, @NotNull Map<String, Integer> counts, @NotNull Set<String> customIds, int page) {
        int fromIndex = page * ENTRY_SLOTS.length;
        int toIndex = Math.min(entries.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            HeadTag entry = entries.get(index);
            ItemStack item = entryItem(plugin, player, entry, counts.getOrDefault(entry.id(), 0), customIds.contains(entry.id()));
            stamp(plugin, item, ACTION_ENTRY + entry.id());
            inventory.setItem(ENTRY_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static @NotNull ItemStack entryItem(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadTag entry, int heads, boolean custom) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("tags"));
        item.editMeta(meta -> {
            java.util.List<Component> lore = new java.util.ArrayList<>();
            if (entry.description() != null && !entry.description().isBlank()) {
                lore.add(GuiItems.lore(entry.description(), NamedTextColor.GRAY));
                lore.add(Component.empty());
            }
            if (plugin.adminModes().enabled(player)) {
                lore.add(GuiItems.idDetail("Heads", heads));
                lore.add(GuiItems.idDetail("ID", entry.id()));
                lore.add(GuiItems.idDetail("Type", custom ? "Custom" : "Remote"));
                lore.add(Component.empty());
            }
            lore.add(GuiItems.lore("Click to browse this tag.", NamedTextColor.GREEN));
            meta.displayName(GuiItems.name(entry.name(), custom ? NamedTextColor.YELLOW : NamedTextColor.GOLD));
            meta.lore(lore);
        });
        return item;
    }

    private static @NotNull List<Head> visibleHeads(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        boolean includeHidden = plugin.adminModes().enabled(player);
        return plugin.headRegistry().heads(includeHidden).stream().filter(head -> Permissions.canViewCategory(player, head.category())).toList();
    }

    private static @NotNull Map<String, Integer> counts(@NotNull List<Head> heads) {
        Map<String, Integer> counts = new HashMap<>();
        for (Head head : heads) {
            for (String value : head.tags()) {
                counts.merge(value, 1, Integer::sum);
            }
        }
        return Map.copyOf(counts);
    }

    private static @NotNull Set<String> customIds(@NotNull HeadDBPlugin plugin) {
        Set<String> ids = new HashSet<>();
        for (CustomTaxonomyEntry entry : plugin.customTags().list()) {
            ids.add(entry.id());
        }
        return Set.copyOf(ids);
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, int entries, int page, int pages, boolean customOnly, @NotNull String query) {
        ItemStack back = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("back"));
        stamp(plugin, back, ACTION_BACK);
        inventory.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack previous = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("previous"));
            stamp(plugin, previous, ACTION_PREVIOUS);
            inventory.setItem(SLOT_PREVIOUS, previous);
        }

        inventory.setItem(SLOT_INFO, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name("Info", NamedTextColor.GOLD), List.of(
                GuiItems.idDetail("Tags", entries),
                GuiItems.idDetail("Page", (page + 1) + " / " + Math.max(1, pages)),
                GuiItems.idDetail("Filter", customOnly ? "Custom" : "All"),
                GuiItems.idDetail("Search", query.isBlank() ? "None" : query)
        )));

        if (page + 1 < pages) {
            ItemStack next = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("next"));
            stamp(plugin, next, ACTION_NEXT);
            inventory.setItem(SLOT_NEXT, next);
        }

        if (plugin.adminModes().enabled(player)) {
            ItemStack filter = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(customOnly ? "taxonomy-filter-custom" : "taxonomy-filter-all"));
            stamp(plugin, filter, ACTION_FILTER);
            inventory.setItem(plugin.guiConfig().slot("tags.filter", SLOT_FILTER), filter);
        }

        ItemStack search = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("taxonomy-search"));
        stamp(plugin, search, ACTION_SEARCH);
        inventory.setItem(plugin.guiConfig().slot("tags.search", SLOT_SEARCH), search);

        if (plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CREATE_TAG)) {
            ItemStack create = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("create-tag"));
            stamp(plugin, create, ACTION_CREATE);
            inventory.setItem(plugin.guiConfig().slot("tags.create", SLOT_CREATE), create);
        }
    }

    private static boolean matches(@NotNull String id, @NotNull String name, @NotNull String query) {
        String trimmed = query.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isBlank()) {
            return true;
        }

        String haystack = id.toLowerCase(Locale.ROOT) + " " + name.toLowerCase(Locale.ROOT);
        for (String part : trimmed.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!haystack.contains(part)) {
                return false;
            }
        }
        return true;
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

    private static @NotNull String title(@NotNull HeadDBPlugin plugin, int page, int pages, boolean customOnly, @NotNull String query) {
        String key = customOnly ? "title.tags-custom-page" : "title.tags-page";
        String fallback = customOnly ? "Custom Tags %page%/%pages%" : "Tags %page%/%pages%";
        return plugin.guiConfig().text(key, fallback)
                .replace("%page%", String.valueOf(page + 1))
                .replace("%pages%", String.valueOf(Math.max(1, pages)))
                .replace("%query%", query.trim());
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) ENTRY_SLOTS.length);
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
        return new NamespacedKey(plugin, "tags_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
    }

    private static final class Holder implements InventoryHolder {
        private final int page;
        private final boolean customOnly;
        private final String query;
        private Inventory inventory;

        private Holder(int page, boolean customOnly, @NotNull String query) {
            this.page = page;
            this.customOnly = customOnly;
            this.query = Objects.requireNonNull(query, "query");
        }

        private int page() { return page; }
        private boolean customOnly() { return customOnly; }
        private @NotNull String query() { return query; }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
