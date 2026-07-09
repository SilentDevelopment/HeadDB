package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiLabels;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadEntry;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CategoryHeadPickerMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_TYPE = 49;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_TYPE = "type";
    private static final String ACTION_SELECT = "select";
    private static final HeadTexture PLAYER_FALLBACK_TEXTURE = new HeadTexture("0");
    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private CategoryHeadPickerMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(categoryId, "categoryId");

        if (!plugin.adminModes().enabled(player) || !Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN)) {
            player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        List<Head> heads = heads(plugin, player);
        int pages = pageCount(heads.size());
        int page = clampPage(requestedPage, pages);
        Holder holder = new Holder(categoryId, page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Add Head", plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderHeads(plugin, inventory, heads, page);
        renderControls(plugin, inventory, page, pages);
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

        handleAction(plugin, player, holder, action.get(), item);
        return true;
    }

    private static void renderHeads(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull List<Head> heads, int page) {
        int fromIndex = page * HEAD_SLOTS.length;
        int toIndex = Math.min(heads.size(), fromIndex + HEAD_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Head head = heads.get(index);
            ItemStack item = plugin.itemFactory().create(head);
            item.editMeta(meta -> {
                List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                lore.add(Component.empty());
                lore.add(GuiItems.idDetail("ID", head.id().display()));
                lore.add(GuiItems.metaDetail("Category", head.category()));
                lore.add(GuiItems.lore("Click to add this head.", NamedTextColor.GREEN));
                meta.lore(lore);
            });
            stamp(plugin, item, ACTION_SELECT);
            inventory.setItem(HEAD_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int page, int pages) {
        inventory.setItem(SLOT_BACK, actionItem(plugin, ACTION_BACK, "back"));
        inventory.setItem(SLOT_TYPE, actionItem(plugin, ACTION_TYPE, "material-type-chat"));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull String action, @NotNull ItemStack item) {
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

        if (action.equals(ACTION_TYPE)) {
            promptId(plugin, player, holder.categoryId());
            return;
        }

        if (!action.equals(ACTION_SELECT)) {
            return;
        }

        Optional<HeadId> id = HeadItemIds.read(plugin, item);
        if (id.isEmpty()) {
            return;
        }

        add(plugin, player, holder.categoryId(), id.get());
    }

    private static void promptId(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter a head id to add.", NamedTextColor.GOLD), value -> {
            HeadId id;
            try {
                id = parseHeadId(value);
            } catch (IllegalArgumentException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                open(plugin, player, categoryId, 0);
                return;
            }

            boolean exists = id.isCustom() ? plugin.headRegistry().customHeads().findStored(id).isPresent() : plugin.headRegistry().find(id).isPresent();
            if (!exists) {
                player.sendMessage(Component.text("Unknown head: ", NamedTextColor.RED).append(Component.text(id.display(), NamedTextColor.GOLD)));
                open(plugin, player, categoryId, 0);
                return;
            }

            add(plugin, player, categoryId, id);
        }, () -> open(plugin, player, categoryId, 0));
    }

    private static void add(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId, @NotNull HeadId id) {
        plugin.customCategories().addHead(categoryId, id);
        player.sendMessage(Component.text("Head added: ", NamedTextColor.GRAY).append(Component.text(GuiLabels.head(plugin, player, id), NamedTextColor.GOLD)));
        CategoryMembersMenu.open(plugin, player, categoryId, 0);
    }

    private static @NotNull List<Head> heads(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        boolean adminMode = plugin.adminModes().enabled(player);
        List<Head> heads = new ArrayList<>(plugin.headRegistry().heads(adminMode));
        if (adminMode) {
            for (io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead head : plugin.headRegistry().customHeads().listStored()) {
                if (head.draft()) {
                    heads.add(head.toHead());
                }
            }
        }
        for (PlayerHeadEntry entry : plugin.headRegistry().playerHeads().knownPlayers()) {
            heads.add(playerHead(entry));
        }
        heads.sort(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(heads);
    }

    private static @NotNull Head playerHead(@NotNull PlayerHeadEntry entry) {
        HeadId id = entry.uuid() == null ? new HeadId("player:" + entry.name()) : HeadId.player(entry.uuid());
        return new Head(id, entry.name(), PLAYER_FALLBACK_TEXTURE, "players", Set.of("player"), Set.of());
    }

    private static @NotNull HeadId parseHeadId(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.matches("[1-9][0-9]*")) {
            return HeadId.remote(trimmed);
        }
        return SearchParser.headId(trimmed);
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
        return new org.bukkit.NamespacedKey(plugin, "category_head_picker_action");
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
                throw new IllegalStateException("Category head picker inventory has not been assigned.");
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
