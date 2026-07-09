package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.edit.HeadEditMenu;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.texture.TextureInputParser;
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
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CreateHeadMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_USE_HELD = 12;
    private static final int SLOT_START_FRESH = 14;
    private static final int SLOT_ID = 20;
    private static final int SLOT_NAME = 22;
    private static final int SLOT_TEXTURE = 24;
    private static final int SLOT_CATEGORY = 29;
    private static final int SLOT_TAGS = 31;
    private static final int SLOT_COLLECTIONS = 33;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_SELECTOR_BACK = 45;
    private static final int SLOT_SELECTOR_PREVIOUS = 48;
    private static final int SLOT_SELECTOR_INFO = 49;
    private static final int SLOT_SELECTOR_NEXT = 50;
    private static final int SLOT_SELECTOR_CLEAR = 53;
    private static final Map<UUID, Draft> DRAFTS = new ConcurrentHashMap<>();
    private static final String ACTION_BACK = "back";
    private static final String ACTION_USE_HELD = "use-held";
    private static final String ACTION_START_FRESH = "start-fresh";
    private static final String ACTION_ID = "id";
    private static final String ACTION_NAME = "name";
    private static final String ACTION_TEXTURE = "texture";
    private static final String ACTION_CATEGORY = "category";
    private static final String ACTION_TAGS = "tags";
    private static final String ACTION_COLLECTIONS = "collections";
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_SELECTOR_BACK = "selector-back";
    private static final String ACTION_SELECTOR_PREVIOUS = "selector-previous";
    private static final String ACTION_SELECTOR_NEXT = "selector-next";
    private static final String ACTION_SELECTOR_CLEAR = "selector-clear";
    private static final String ACTION_SELECTOR_ENTRY = "selector-entry:";
    private static final int[] SELECTOR_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private CreateHeadMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void openNew(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        DRAFTS.put(player.getUniqueId(), Draft.empty());
        open(plugin, player);
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CREATE_HEAD)) {
            noPermission(plugin, player);
            return;
        }

        Draft draft = DRAFTS.computeIfAbsent(player.getUniqueId(), ignored -> Draft.empty());
        CreateHeadHolder holder = new CreateHeadHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(plugin.guiConfig().text("title.create-head", "Create Head"), true));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        render(plugin, inventory, draft);
        player.openInventory(inventory);
        plugin.sounds().play(player, SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof CreateHeadHolder) && !(holder instanceof CreateHeadSelectorHolder)) {
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

        if (!action.get().equals(ACTION_CREATE)) {
            plugin.sounds().playGuiAction(player, action.get());
        }

        if (holder instanceof CreateHeadSelectorHolder selectorHolder) {
            handleSelectorAction(plugin, player, selectorHolder, action.get());
            return true;
        }

        handleAction(plugin, player, action.get());
        return true;
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String action) {
        Draft draft = DRAFTS.computeIfAbsent(player.getUniqueId(), ignored -> Draft.empty());

        if (action.equals(ACTION_BACK)) {
            plugin.guis().openBrowse(player);
            return;
        }

        if (action.equals(ACTION_START_FRESH)) {
            DRAFTS.put(player.getUniqueId(), Draft.empty());
            open(plugin, player);
            return;
        }

        if (action.equals(ACTION_USE_HELD)) {
            useHeld(plugin, player, draft);
            return;
        }

        if (action.equals(ACTION_ID)) {
            prompt(plugin, player, "Enter the custom head id.", value -> update(player, draft.withId(value)));
            return;
        }

        if (action.equals(ACTION_NAME)) {
            prompt(plugin, player, "Enter the custom head name.", value -> update(player, draft.withName(value)));
            return;
        }

        if (action.equals(ACTION_TEXTURE)) {
            prompt(plugin, player, "Enter a texture hash, URL, or base64 value.", value -> {
                try {
                    HeadTexture texture = new TextureInputParser().parse(value);
                    update(player, draft.withTexture(texture.hash()));
                } catch (IllegalArgumentException exception) {
                    player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                    plugin.sounds().play(player, SoundKey.VALIDATION_ERROR);
                }
            });
            return;
        }

        if (action.equals(ACTION_CATEGORY)) {
            openSelector(plugin, player, SelectorMode.CATEGORY, 0);
            return;
        }

        if (action.equals(ACTION_TAGS)) {
            openSelector(plugin, player, SelectorMode.TAGS, 0);
            return;
        }

        if (action.equals(ACTION_COLLECTIONS)) {
            openSelector(plugin, player, SelectorMode.COLLECTIONS, 0);
            return;
        }

        if (action.equals(ACTION_CREATE)) {
            saveDraft(plugin, player, draft, true);
        }
    }


    private static void openSelector(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull SelectorMode mode, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");

        if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CREATE_HEAD)) {
            noPermission(plugin, player);
            return;
        }

        Draft draft = DRAFTS.computeIfAbsent(player.getUniqueId(), ignored -> Draft.empty());
        List<SelectionEntry> entries = selectorEntries(plugin, player, draft, mode);
        int pages = pageCount(entries.size(), SELECTOR_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        CreateHeadSelectorHolder holder = new CreateHeadSelectorHolder(mode, page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(mode.title(), true));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        renderSelectorEntries(plugin, inventory, draft, mode, entries, page);
        renderSelectorControls(plugin, inventory, draft, mode, entries.size(), page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, SoundKey.MENU_OPEN);
    }

    private static void renderSelectorEntries(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull Draft draft, @NotNull SelectorMode mode, @NotNull List<SelectionEntry> entries, int page) {
        int fromIndex = page * SELECTOR_SLOTS.length;
        int toIndex = Math.min(entries.size(), fromIndex + SELECTOR_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            SelectionEntry entry = entries.get(index);
            ItemStack item = selectorItem(plugin, entry, isSelected(draft, mode, entry.id()));
            stamp(plugin, item, ACTION_SELECTOR_ENTRY + entry.id());
            inventory.setItem(SELECTOR_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderSelectorControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull Draft draft, @NotNull SelectorMode mode, int entries, int page, int pages) {
        setAction(plugin, inventory, SLOT_SELECTOR_BACK, "back", ACTION_SELECTOR_BACK);

        if (page > 0) {
            setAction(plugin, inventory, SLOT_SELECTOR_PREVIOUS, "previous", ACTION_SELECTOR_PREVIOUS);
        }

        inventory.setItem(SLOT_SELECTOR_INFO, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name(mode.title(), NamedTextColor.GOLD), List.of(GuiItems.idDetail("Entries", entries), GuiItems.idDetail("Selected", selectedCount(draft, mode)), GuiItems.idDetail("Page", (page + 1) + " / " + Math.max(1, pages)))));

        if (page + 1 < pages) {
            setAction(plugin, inventory, SLOT_SELECTOR_NEXT, "next", ACTION_SELECTOR_NEXT);
        }

        if (selectedCount(draft, mode) > 0 || mode == SelectorMode.CATEGORY) {
            setAction(plugin, inventory, SLOT_SELECTOR_CLEAR, "clear-filters", ACTION_SELECTOR_CLEAR);
        }
    }

    private static @NotNull ItemStack selectorItem(@NotNull HeadDBPlugin plugin, @NotNull SelectionEntry entry, boolean selected) {
        String iconKey = selected ? "filter-selected" : "filter-unselected";
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.name(entry.name(), selected ? NamedTextColor.GREEN : NamedTextColor.GOLD), List.of(GuiItems.idDetail("ID", entry.id()), GuiItems.lore(selected ? "Selected. Click to remove." : "Click to select.", selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        return item;
    }

    private static void handleSelectorAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CreateHeadSelectorHolder holder, @NotNull String action) {
        if (action.equals(ACTION_SELECTOR_BACK)) {
            open(plugin, player);
            return;
        }

        if (action.equals(ACTION_SELECTOR_PREVIOUS)) {
            openSelector(plugin, player, holder.mode(), holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_SELECTOR_NEXT)) {
            openSelector(plugin, player, holder.mode(), holder.page() + 1);
            return;
        }

        Draft draft = DRAFTS.computeIfAbsent(player.getUniqueId(), ignored -> Draft.empty());
        if (action.equals(ACTION_SELECTOR_CLEAR)) {
            DRAFTS.put(player.getUniqueId(), clearSelection(draft, holder.mode()));
            openSelector(plugin, player, holder.mode(), holder.page());
            return;
        }

        if (!action.startsWith(ACTION_SELECTOR_ENTRY)) {
            return;
        }

        String id = action.substring(ACTION_SELECTOR_ENTRY.length());
        DRAFTS.put(player.getUniqueId(), toggleSelection(draft, holder.mode(), id));
        if (holder.mode() == SelectorMode.CATEGORY) {
            open(plugin, player);
            return;
        }

        openSelector(plugin, player, holder.mode(), holder.page());
    }

    private static @NotNull Draft toggleSelection(@NotNull Draft draft, @NotNull SelectorMode mode, @NotNull String id) {
        return switch (mode) {
            case CATEGORY -> draft.withCategory(id);
            case TAGS -> draft.withTags(toggle(draft.tags(), id));
            case COLLECTIONS -> draft.withCollections(toggle(draft.collections(), id));
        };
    }

    private static @NotNull Draft clearSelection(@NotNull Draft draft, @NotNull SelectorMode mode) {
        return switch (mode) {
            case CATEGORY -> draft.withCategory("custom");
            case TAGS -> draft.withTags(Set.of());
            case COLLECTIONS -> draft.withCollections(Set.of());
        };
    }

    private static @NotNull Set<String> toggle(@NotNull Set<String> current, @NotNull String id) {
        LinkedHashSet<String> updated = new LinkedHashSet<>(current);
        if (updated.contains(id)) {
            updated.remove(id);
        } else {
            updated.add(id);
        }
        return Set.copyOf(updated);
    }

    private static boolean isSelected(@NotNull Draft draft, @NotNull SelectorMode mode, @NotNull String id) {
        return switch (mode) {
            case CATEGORY -> draft.category().equals(id);
            case TAGS -> draft.tags().contains(id);
            case COLLECTIONS -> draft.collections().contains(id);
        };
    }

    private static int selectedCount(@NotNull Draft draft, @NotNull SelectorMode mode) {
        return switch (mode) {
            case CATEGORY -> draft.category().isBlank() ? 0 : 1;
            case TAGS -> draft.tags().size();
            case COLLECTIONS -> draft.collections().size();
        };
    }

    private static @NotNull List<SelectionEntry> selectorEntries(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Draft draft, @NotNull SelectorMode mode) {
        Map<String, SelectionEntry> entries = new java.util.LinkedHashMap<>();

        switch (mode) {
            case CATEGORY -> {
                entries.put("custom", new SelectionEntry("custom", "Custom"));
                plugin.headRegistry().categories().stream()
                        .filter(category -> Permissions.canViewCategory(player, category.id()))
                        .sorted(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER))
                        .forEach(category -> entries.put(category.id(), new SelectionEntry(category.id(), category.name())));
                boolean adminMode = plugin.adminModes().enabled(player);
                plugin.customCategories().listVisible(adminMode).stream()
                        .filter(category -> Permissions.canViewCategory(player, category.id()))
                        .sorted(Comparator.comparing(CustomCategory::name, String.CASE_INSENSITIVE_ORDER))
                        .forEach(category -> entries.put(category.id(), new SelectionEntry(category.id(), category.draft() ? "DRAFT - " + category.name() : category.name())));
                if (!draft.category().isBlank()) {
                    entries.putIfAbsent(draft.category(), new SelectionEntry(draft.category(), displayName(draft.category())));
                }
            }
            case TAGS -> {
                entries.put("custom", new SelectionEntry("custom", "Custom"));
                plugin.headRegistry().tags().stream().sorted(Comparator.comparing(HeadTag::name, String.CASE_INSENSITIVE_ORDER)).forEach(tag -> entries.put(tag.id(), new SelectionEntry(tag.id(), tag.name())));
                for (String tag : draft.tags()) {
                    entries.putIfAbsent(tag, new SelectionEntry(tag, displayName(tag)));
                }
            }
            case COLLECTIONS -> {
                plugin.headRegistry().collections().stream().sorted(Comparator.comparing(HeadCollection::name, String.CASE_INSENSITIVE_ORDER)).forEach(collection -> entries.put(collection.id(), new SelectionEntry(collection.id(), collection.name())));
                for (String collection : draft.collections()) {
                    entries.putIfAbsent(collection, new SelectionEntry(collection, displayName(collection)));
                }
            }
        }

        return List.copyOf(entries.values());
    }

    private static void render(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull Draft draft) {
        inventory.setItem(slot(plugin, "create-head.preview", SLOT_PREVIEW), preview(plugin, draft));
        setAction(plugin, inventory, slot(plugin, "create-head.use-held", SLOT_USE_HELD), "create-head-held", ACTION_USE_HELD);
        setAction(plugin, inventory, slot(plugin, "create-head.reset", SLOT_START_FRESH), "create-head-fresh", ACTION_START_FRESH);
        inventory.setItem(slot(plugin, "create-head.id", SLOT_ID), actionItem(plugin, "create-head-id", ACTION_ID, "ID", draft.id().isBlank() ? "Not set" : draft.id()));
        inventory.setItem(slot(plugin, "create-head.name", SLOT_NAME), actionItem(plugin, "create-head-name", ACTION_NAME, "Name", draft.name().isBlank() ? "Not set" : draft.name()));
        inventory.setItem(slot(plugin, "create-head.texture", SLOT_TEXTURE), actionItem(plugin, "create-head-texture", ACTION_TEXTURE, "Texture", draft.texture().isBlank() ? "Not set" : "set"));
        inventory.setItem(slot(plugin, "create-head.category", SLOT_CATEGORY), actionItem(plugin, "create-head-category", ACTION_CATEGORY, "Category", draft.category().isBlank() ? "custom" : draft.category()));
        inventory.setItem(slot(plugin, "create-head.tags", SLOT_TAGS), actionItem(plugin, "create-head-tags", ACTION_TAGS, "Tags", draft.tags().isEmpty() ? "none" : draft.tags().size()));
        inventory.setItem(slot(plugin, "create-head.collections", SLOT_COLLECTIONS), actionItem(plugin, "create-head-collections", ACTION_COLLECTIONS, "Collections", draft.collections().isEmpty() ? "none" : draft.collections().size()));
        setAction(plugin, inventory, slot(plugin, "create-head.save", SLOT_CREATE), "create-head-save", ACTION_CREATE);
        setAction(plugin, inventory, slot(plugin, "create-head.back", SLOT_BACK), "back", ACTION_BACK);
    }

    private static int slot(@NotNull HeadDBPlugin plugin, @NotNull String key, int fallback) {
        return plugin.guiConfig().slot(key, fallback);
    }

    private static @NotNull ItemStack preview(@NotNull HeadDBPlugin plugin, @NotNull Draft draft) {
        ItemStack item = null;
        if (!draft.texture().isBlank()) {
            try {
                String previewId = draft.id().trim().isBlank() ? "preview" : draft.id().trim();
                String previewName = draft.name().trim().isBlank() ? displayName(previewId) : draft.name().trim();
                String previewCategory = draft.category().trim().isBlank() ? "custom" : draft.category().trim();
                StoredCustomHead head = new StoredCustomHead(previewId, previewName, draft.texture().trim(), null, List.of(), draft.tags(), draft.collections(), previewCategory, Instant.now(), Instant.now(), null, true);
                item = plugin.itemFactory().create(head.toHead());
            } catch (IllegalArgumentException ignored) {
                item = null;
            }
        }

        if (item == null) {
            item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("create-head"));
        }

        item.editMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.add(GuiItems.idDetail("ID", draft.id().isBlank() ? "Not set" : "custom:" + draft.id()));
            lore.add(GuiItems.idDetail("Name", draft.name().isBlank() ? "Not set" : draft.name()));
            lore.add(GuiItems.idDetail("Category", draft.category().isBlank() ? "custom" : draft.category()));
            lore.add(GuiItems.idDetail("Texture", draft.texture().isBlank() ? "Not set" : "set"));
            meta.lore(lore);
        });
        return item;
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String iconKey, @NotNull String action, @NotNull String key, @NotNull Object value) {
        java.util.List<Component> lore = new java.util.ArrayList<>(GuiItems.miniLore(plugin.guiConfig().icon(iconKey).lore()));
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }
        lore.add(GuiItems.idDetail("Current", value));
        lore.add(GuiItems.lore("Click to edit.", NamedTextColor.GREEN));

        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.mini(plugin.guiConfig().icon(iconKey).name()), lore);
        stamp(plugin, item, action);
        return item;
    }

    private static void setAction(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int slot, @NotNull String iconKey, @NotNull String action) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        inventory.setItem(slot, item);
    }

    private static void useHeld(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Draft draft) {
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<HeadId> headId = HeadItemIds.read(plugin, held);
        if (headId.isPresent()) {
            Optional<io.github.silentdevelopment.headdb.model.Head> head = plugin.headRegistry().find(headId.get());
            if (head.isPresent()) {
                Draft updated = draft.withTexture(head.get().texture().hash());
                if (draft.id().isBlank() && !headId.get().isPlayer()) {
                    updated = updated.withId(headId.get().key());
                }
                if (draft.name().isBlank()) {
                    updated = updated.withName(head.get().name());
                }
                DRAFTS.put(player.getUniqueId(), updated);
                player.getScheduler().run(plugin, task -> open(plugin, player), () -> {});
                return;
            }
        }

        try {
            HeadTexture texture = new TextureInputParser().fromItem(held);
            DRAFTS.put(player.getUniqueId(), draft.withTexture(texture.hash()));
            player.getScheduler().run(plugin, task -> open(plugin, player), () -> {});
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            plugin.sounds().play(player, SoundKey.INVALID);
        }
    }

    public static void handleClose(@NotNull HeadDBPlugin plugin, @NotNull Player player, @Nullable InventoryHolder holder) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!(holder instanceof CreateHeadHolder) && !(holder instanceof CreateHeadSelectorHolder)) {
            return;
        }

        Draft draft = DRAFTS.get(player.getUniqueId());
        if (draft == null || !draft.canPersist()) {
            return;
        }

        saveDraft(plugin, player, draft, false);
    }

    private static void saveDraft(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Draft draft, boolean reopenEdit) {
        if (!Permissions.has(player, Permissions.CUSTOM_CREATE)) {
            noPermission(plugin, player);
            return;
        }

        try {
            StoredCustomHead head = draft.toStored(player.getUniqueId()).withDraft(true);
            plugin.headRegistry().customHeads().save(head);
            plugin.headRegistry().onLocalMutation();
            plugin.clearItemCache();
            plugin.clearSearchCache();
            if (!reopenEdit) {
                return;
            }

            DRAFTS.remove(player.getUniqueId());
            player.sendMessage(Component.text("Saved draft head: ", NamedTextColor.GRAY).append(Component.text(head.name() + " (" + head.headId().display() + ")", NamedTextColor.GOLD)));
            plugin.sounds().play(player, SoundKey.SAVE_DRAFT);
            HeadEditMenu.open(plugin, player, HeadId.custom(head.id()));
        } catch (IllegalArgumentException exception) {
            if (!reopenEdit) {
                return;
            }

            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            plugin.sounds().play(player, SoundKey.VALIDATION_ERROR);
            open(plugin, player);
        }
    }

    private static void prompt(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String message, @NotNull Consumer<String> input) {
        player.closeInventory();
        player.getScheduler().run(plugin, task -> plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), value -> {
            input.accept(value);
            open(plugin, player);
        }, () -> open(plugin, player)), () -> {});
    }

    private static void update(@NotNull Player player, @NotNull Draft draft) {
        DRAFTS.put(player.getUniqueId(), draft);
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

    private static int pageCount(int entries, int pageSize) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) pageSize);
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
        return new NamespacedKey(plugin, "create_head_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, SoundKey.NO_PERMISSION);
    }

    private record SelectionEntry(@NotNull String id, @NotNull String name) {}

    private enum SelectorMode {
        CATEGORY("Select Category"),
        TAGS("Select Tags"),
        COLLECTIONS("Select Collections");

        private final String title;

        SelectorMode(@NotNull String title) {
            this.title = Objects.requireNonNull(title, "title");
        }

        private @NotNull String title() {
            return title;
        }
    }

    private record Draft(@NotNull String id, @NotNull String name, @NotNull String texture, @NotNull String category, @NotNull Set<String> tags, @NotNull Set<String> collections) {
        private static @NotNull Draft empty() {
            return new Draft("", "", "", "custom", Set.of("custom"), Set.of());
        }

        private @NotNull Draft withId(@NotNull String id) { return new Draft(id, name, texture, category, tags, collections); }
        private @NotNull Draft withName(@NotNull String name) { return new Draft(id, name, texture, category, tags, collections); }
        private @NotNull Draft withTexture(@NotNull String texture) { return new Draft(id, name, texture, category, tags, collections); }
        private @NotNull Draft withCategory(@NotNull String category) { return new Draft(id, name, texture, category, tags, collections); }
        private @NotNull Draft withTags(@NotNull Set<String> tags) { return new Draft(id, name, texture, category, tags, collections); }
        private @NotNull Draft withCollections(@NotNull Set<String> collections) { return new Draft(id, name, texture, category, tags, collections); }

        private boolean canPersist() {
            return !id.trim().isBlank() && !texture.trim().isBlank();
        }

        private @NotNull StoredCustomHead toStored(@NotNull UUID createdBy) {
            String cleanId = id.trim();
            if (cleanId.isBlank()) {
                throw new IllegalArgumentException("ID is required.");
            }

            if (texture.trim().isBlank()) {
                throw new IllegalArgumentException("Texture is required.");
            }

            String cleanName = name.trim().isBlank() ? displayName(cleanId) : name.trim();
            String cleanCategory = category.trim().isBlank() ? "custom" : category.trim();
            return new StoredCustomHead(cleanId, cleanName, texture.trim(), null, List.of(), tags, collections, cleanCategory, Instant.now(), Instant.now(), createdBy);
        }
    }

    private static @NotNull String displayName(@NotNull String id) {
        String normalized = id.trim().replace('_', '-');
        if (normalized.isBlank()) {
            return "Custom Head";
        }

        String[] parts = normalized.split("-");
        java.util.List<String> words = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return words.isEmpty() ? normalized : String.join(" ", words);
    }

    private static final class CreateHeadSelectorHolder implements InventoryHolder {
        private final SelectorMode mode;
        private final int page;
        private Inventory inventory;

        private CreateHeadSelectorHolder(@NotNull SelectorMode mode, int page) {
            this.mode = Objects.requireNonNull(mode, "mode");
            this.page = page;
        }

        private @NotNull SelectorMode mode() {
            return mode;
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

    private static final class CreateHeadHolder implements InventoryHolder {
        private Inventory inventory;

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
