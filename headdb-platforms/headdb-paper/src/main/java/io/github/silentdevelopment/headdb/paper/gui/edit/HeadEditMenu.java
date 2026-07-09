package io.github.silentdevelopment.headdb.paper.gui.edit;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.category.CustomCategory;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class HeadEditMenu {

    public static final int SIZE = 54;
    public static final String ACTION_PREVIEW = "preview";
    public static final String ACTION_NAME = "name";
    public static final String ACTION_LORE_MENU = "lore-menu";
    public static final String ACTION_CATEGORY_MENU = "category-menu";
    public static final String ACTION_TAGS_MENU = "tags-menu";
    public static final String ACTION_COLLECTIONS_MENU = "collections-menu";
    public static final String ACTION_VISIBILITY = "visibility";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_PUBLISH = "publish";
    public static final String ACTION_PRICE = "price";
    public static final String ACTION_BACK_MAIN = "back-main";
    public static final String ACTION_BACK_EDIT = "back-edit";
    public static final String ACTION_PREVIOUS = "previous";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_CATEGORY_SELECT = "category-select:";
    public static final String ACTION_TAG_TOGGLE = "tag-toggle:";
    public static final String ACTION_COLLECTION_TOGGLE = "collection-toggle:";
    public static final String ACTION_LORE_ADD = "lore-add";
    public static final String ACTION_LORE_CLEAR = "lore-clear";
    public static final String ACTION_LORE_RESET = "lore-reset";
    public static final String ACTION_LORE_SET = "lore-set:";

    private static final int ROWS = 6;
    private static final int SLOT_PREVIOUS = 46;
    private static final int SLOT_NEXT = 52;
    private static final HeadTexture PLAYER_FALLBACK_TEXTURE = new HeadTexture("0");
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private HeadEditMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull NamespacedKey actionKey(@NotNull HeadDBPlugin plugin) {
        return new NamespacedKey(plugin, "edit_action");
    }

    public static @NotNull NamespacedKey targetKey(@NotNull HeadDBPlugin plugin) {
        return new NamespacedKey(plugin, "edit_head_id");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id) {
        openRoot(plugin, player, id);
    }

    public static void openRoot(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id) {
        Optional<Head> head = previewHead(plugin, id);
        String titleName = head.map(Head::name).orElse(id.display());
        EditHolder holder = new EditHolder(id, EditMenuType.ROOT, 0);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Edit", titleName, true));
        holder.inventory(inventory);

        fillBorder(inventory);
        head.ifPresent(value -> inventory.setItem(plugin.guiConfig().slot("edit.preview", 13), stamp(plugin, plugin.itemFactory().create(value), id, ACTION_PREVIEW)));
        inventory.setItem(plugin.guiConfig().slot("edit.back", 45), button(plugin, id, ACTION_BACK_MAIN, "back"));

        if (id.isPlayer()) {
            inventory.setItem(31, stamp(plugin, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name("Edit Options", NamedTextColor.GOLD), List.of(
                    GuiItems.metaDetail("Type", "Player Head"),
                    GuiItems.idDetail("ID", id.display()),
                    Component.empty(),
                    GuiItems.lore("Player heads are generated from Minecraft profiles.", NamedTextColor.GRAY),
                    GuiItems.lore("There are no local override options for player heads.", NamedTextColor.DARK_GRAY)
            )), id, ACTION_PREVIEW));
            player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
            return;
        }

        inventory.setItem(plugin.guiConfig().slot("edit.name", 21), button(plugin, id, ACTION_NAME, "edit-name"));
        inventory.setItem(plugin.guiConfig().slot("edit.lore", 23), button(plugin, id, ACTION_LORE_MENU, "edit-lore"));
        inventory.setItem(plugin.guiConfig().slot("edit.category", 29), button(plugin, id, ACTION_CATEGORY_MENU, "edit-category"));
        inventory.setItem(plugin.guiConfig().slot("edit.tags", 31), button(plugin, id, ACTION_TAGS_MENU, "edit-tags"));
        inventory.setItem(plugin.guiConfig().slot("edit.collections", 33), button(plugin, id, ACTION_COLLECTIONS_MENU, "edit-collections"));
        if (plugin.economy().enabled()) {
            inventory.setItem(plugin.guiConfig().slot("edit.price", 35), button(plugin, id, ACTION_PRICE, "edit-price"));
        }

        if (id.isRemote()) {
            inventory.setItem(plugin.guiConfig().slot("edit.visibility", 39), visibilityButton(plugin, id));
            inventory.setItem(plugin.guiConfig().slot("edit.reset", 41), button(plugin, id, ACTION_RESET, "edit-reset"));
        } else if (id.isCustom()) {
            boolean draft = plugin.headRegistry().customHeads().findStored(id).map(StoredCustomHead::draft).orElse(false);
            if (draft) {
                inventory.setItem(plugin.guiConfig().slot("edit.publish", 39), button(plugin, id, ACTION_PUBLISH, "edit-publish"));
            }
            inventory.setItem(plugin.guiConfig().slot("edit.delete", 41), button(plugin, id, ACTION_DELETE, "edit-delete"));
        }

        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static void openCategories(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id, int page) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        plugin.headRegistry().categories().stream()
                .filter(category -> Permissions.canViewCategory(player, category.id()))
                .sorted(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(category -> entries.put(category.id(), new Entry(category.id(), category.name())));
        boolean adminMode = plugin.adminModes().enabled(player);
        plugin.customCategories().listVisible(adminMode).stream()
                .filter(category -> Permissions.canViewCategory(player, category.id()))
                .sorted(Comparator.comparing(CustomCategory::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(category -> entries.put(category.id(), new Entry(category.id(), category.draft() ? "DRAFT - " + category.name() : category.name())));
        String current = previewHead(plugin, id).map(Head::category).orElse("unknown");
        openSelection(plugin, player, id, EditMenuType.CATEGORY, page, List.copyOf(entries.values()), "Category: " + current, ACTION_CATEGORY_SELECT, selectedCategory(plugin, id));
    }

    public static void openTags(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id, int page) {
        List<Entry> entries = plugin.headRegistry().tags().stream().sorted(Comparator.comparing(HeadTag::name, String.CASE_INSENSITIVE_ORDER)).map(tag -> new Entry(tag.id(), tag.name())).toList();
        openSelection(plugin, player, id, EditMenuType.TAGS, page, entries, "Tags", ACTION_TAG_TOGGLE, selectedTags(plugin, id));
    }

    public static void openCollections(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id, int page) {
        List<Entry> entries = plugin.headRegistry().collections().stream().sorted(Comparator.comparing(HeadCollection::name, String.CASE_INSENSITIVE_ORDER)).map(collection -> new Entry(collection.id(), collection.name())).toList();
        openSelection(plugin, player, id, EditMenuType.COLLECTIONS, page, entries, "Collections", ACTION_COLLECTION_TOGGLE, selectedCollections(plugin, id));
    }

    public static void openLore(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id, int page) {
        Optional<Head> head = previewHead(plugin, id);
        List<String> lines = loreLines(plugin, id, head.orElse(null));
        int pages = pageCount(lines.size());
        int safePage = clampPage(page, pages);
        EditHolder holder = new EditHolder(id, EditMenuType.LORE, safePage);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Lore", head.map(Head::name).orElse(id.display()), true));
        holder.inventory(inventory);

        fillBorder(inventory);
        head.ifPresent(value -> inventory.setItem(plugin.guiConfig().slot("edit.lore.preview", 4), stamp(plugin, plugin.itemFactory().create(value), id, ACTION_PREVIEW)));

        int fromIndex = safePage * ENTRY_SLOTS.length;
        int toIndex = Math.min(lines.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;
        for (int index = fromIndex; index < toIndex; index++) {
            ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("lore-line"), GuiItems.name("Line " + (index + 1), NamedTextColor.GOLD), List.of(GuiItems.miniOrWhite(lines.get(index)), Component.empty(), GuiItems.lore("Click to edit this line.", NamedTextColor.GREEN), GuiItems.lore("Press drop to remove this line.", NamedTextColor.RED)));
            inventory.setItem(ENTRY_SLOTS[slotIndex], stamp(plugin, item, id, ACTION_LORE_SET + index));
            slotIndex++;
        }

        inventory.setItem(plugin.guiConfig().slot("edit.lore.back", 45), button(plugin, id, ACTION_BACK_EDIT, "back"));
        inventory.setItem(SLOT_PREVIOUS, safePage > 0 ? button(plugin, id, ACTION_PREVIOUS, "previous") : inventory.getItem(SLOT_PREVIOUS));
        inventory.setItem(SLOT_NEXT, safePage + 1 < pages ? button(plugin, id, ACTION_NEXT, "next") : inventory.getItem(SLOT_NEXT));
        inventory.setItem(plugin.guiConfig().slot("edit.lore.add", 48), button(plugin, id, ACTION_LORE_ADD, "lore-add"));
        inventory.setItem(plugin.guiConfig().slot("edit.lore.clear", 50), button(plugin, id, ACTION_LORE_CLEAR, "lore-clear"));
        inventory.setItem(plugin.guiConfig().slot("edit.lore.reset", 53), button(plugin, id, ACTION_LORE_RESET, "lore-reset"));
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static @NotNull Optional<ActionTarget> actionTarget(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }

        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey(plugin), PersistentDataType.STRING);
        String rawId = item.getItemMeta().getPersistentDataContainer().get(targetKey(plugin), PersistentDataType.STRING);
        if (action == null || rawId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(new ActionTarget(action, new HeadId(rawId)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static boolean isLoreLineAction(@NotNull String action) {
        return action.startsWith(ACTION_LORE_SET);
    }

    private static void openSelection(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull HeadId id, @NotNull EditMenuType type, int page, @NotNull List<Entry> entries, @NotNull String label, @NotNull String actionPrefix, @NotNull Set<String> selected) {
        Optional<Head> head = previewHead(plugin, id);
        int pages = pageCount(entries.size());
        int safePage = clampPage(page, pages);
        EditHolder holder = new EditHolder(id, type, safePage);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(label, head.map(Head::name).orElse(id.display()), true));
        holder.inventory(inventory);

        fillBorder(inventory);
        int fromIndex = safePage * ENTRY_SLOTS.length;
        int toIndex = Math.min(entries.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Entry entry = entries.get(index);
            boolean enabled = selected.contains(entry.id());
            String iconKey = enabled ? "filter-selected" : "filter-unselected";
            ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.name(entry.name(), enabled ? NamedTextColor.GREEN : NamedTextColor.GOLD), List.of(GuiItems.idDetail("ID", entry.id()), GuiItems.lore(enabled ? "Selected. Click to remove." : "Click to select.", enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            inventory.setItem(ENTRY_SLOTS[slotIndex], stamp(plugin, item, id, actionPrefix + entry.id()));
            slotIndex++;
        }

        inventory.setItem(45, button(plugin, id, ACTION_BACK_EDIT, "back"));
        inventory.setItem(SLOT_PREVIOUS, safePage > 0 ? button(plugin, id, ACTION_PREVIOUS, "previous") : inventory.getItem(SLOT_PREVIOUS));
        inventory.setItem(SLOT_NEXT, safePage + 1 < pages ? button(plugin, id, ACTION_NEXT, "next") : inventory.getItem(SLOT_NEXT));
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    private static @NotNull Optional<Head> previewHead(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        if (id.isPlayer()) {
            return Optional.of(playerHead(id));
        }

        if (id.isCustom()) {
            return plugin.headRegistry().customHeads().findStored(id).map(StoredCustomHead::toHead);
        }

        return plugin.headRegistry().find(id);
    }

    private static @NotNull Head playerHead(@NotNull HeadId id) {
        String key = id.key();
        String name = key;

        try {
            UUID uuid = UUID.fromString(key);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.getName() != null && !player.getName().isBlank()) {
                name = player.getName();
            }
        } catch (IllegalArgumentException ignored) {
            name = key;
        }

        return new Head(id, name, PLAYER_FALLBACK_TEXTURE, "players", Set.of("player"), Set.of());
    }

    public static @NotNull List<String> loreLines(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        return loreLines(plugin, id, previewHead(plugin, id).orElse(null));
    }

    private static @NotNull List<String> loreLines(@NotNull HeadDBPlugin plugin, @NotNull HeadId id, Head head) {
        Optional<List<String>> local = plugin.headRegistry().lore(id);
        if (local.isPresent()) {
            return local.get();
        }

        if (head == null) {
            return List.of();
        }

        ItemStack item = plugin.itemFactory().create(head);
        if (!item.hasItemMeta() || item.getItemMeta().lore() == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (Component line : item.getItemMeta().lore()) {
            String text = PLAIN.serialize(line).trim();
            if (!text.isBlank()) {
                lines.add(text);
            }
        }
        return List.copyOf(lines);
    }

    private static @NotNull Set<String> selectedTags(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        return previewHead(plugin, id).map(Head::tags).orElse(Set.of());
    }

    private static @NotNull Set<String> selectedCollections(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        return previewHead(plugin, id).map(Head::collections).orElse(Set.of());
    }

    private static @NotNull Set<String> selectedCategory(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        return previewHead(plugin, id).map(Head::category).map(category -> Set.of(category)).orElse(Set.of());
    }


    private static @NotNull ItemStack visibilityButton(@NotNull HeadDBPlugin plugin, @NotNull HeadId id) {
        boolean hidden = plugin.headRegistry().hidden(id);
        String iconKey = hidden ? "edit-visibility-hidden" : "edit-visibility-visible";
        List<Component> lore = List.of(
                Component.text("SHOW", hidden ? NamedTextColor.GRAY : NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                Component.text("HIDE", hidden ? NamedTextColor.GREEN : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                GuiItems.lore("Click to toggle visibility.", NamedTextColor.YELLOW)
        );
        return stamp(plugin, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.name("Visibility", NamedTextColor.GOLD), lore), id, ACTION_VISIBILITY);
    }

    private static @NotNull ItemStack button(@NotNull HeadDBPlugin plugin, @NotNull HeadId id, @NotNull String action, @NotNull String iconKey) {
        return stamp(plugin, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey)), id, action);
    }

    private static @NotNull ItemStack stamp(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item, @NotNull HeadId id, @NotNull String action) {
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(HeadItemIds.key(plugin), PersistentDataType.STRING, id.toString());
            meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(targetKey(plugin), PersistentDataType.STRING, id.toString());
        });
        return item;
    }

    private static void fillBorder(@NotNull Inventory inventory) {
        ItemStack filler = GuiItems.item(Material.BLACK_STAINED_GLASS_PANE, Component.empty(), List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler.clone());
        }
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

    private record Entry(@NotNull String id, @NotNull String name) {}

    public record ActionTarget(@NotNull String action, @NotNull HeadId id) {}

    public enum EditMenuType {
        ROOT,
        CATEGORY,
        TAGS,
        COLLECTIONS,
        LORE
    }

    public static final class EditHolder implements InventoryHolder {

        private final HeadId id;
        private final EditMenuType type;
        private final int page;
        private Inventory inventory;

        private EditHolder(@NotNull HeadId id, @NotNull EditMenuType type, int page) {
            this.id = Objects.requireNonNull(id, "id");
            this.type = Objects.requireNonNull(type, "type");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Edit inventory has not been assigned.");
            }

            return currentInventory;
        }

        public @NotNull HeadId id() {
            return id;
        }

        public @NotNull EditMenuType type() {
            return type;
        }

        public int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
