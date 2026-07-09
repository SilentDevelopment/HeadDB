package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.local.taxonomy.CustomTaxonomyEntry;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CreateTaxonomyMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_ID = 20;
    private static final int SLOT_NAME = 22;
    private static final int SLOT_DESCRIPTION = 24;
    private static final int SLOT_SAVE = 40;
    private static final int SLOT_BACK = 45;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_ID = "id";
    private static final String ACTION_NAME = "name";
    private static final String ACTION_DESCRIPTION = "description";
    private static final String ACTION_SAVE = "save";
    private static final Map<DraftKey, Draft> DRAFTS = new ConcurrentHashMap<>();

    private CreateTaxonomyMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void openTag(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        Objects.requireNonNull(player, "player");
        DRAFTS.put(new DraftKey(player.getUniqueId(), Mode.TAG), Draft.empty(Mode.TAG));
        open(plugin, player, Mode.TAG);
    }

    public static void openCollection(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        Objects.requireNonNull(player, "player");
        DRAFTS.put(new DraftKey(player.getUniqueId(), Mode.COLLECTION), Draft.empty(Mode.COLLECTION));
        open(plugin, player, Mode.COLLECTION);
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(mode, "mode");

        if (!canUse(plugin, player, mode)) {
            noPermission(plugin, player);
            return;
        }

        Draft draft = DRAFTS.computeIfAbsent(new DraftKey(player.getUniqueId(), mode), ignored -> Draft.empty(mode));
        Holder holder = new Holder(mode);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(plugin.guiConfig().text(mode.titleKey(), mode.titleFallback()), true));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        render(plugin, inventory, draft, mode);
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
        action.ifPresent(value -> handleAction(plugin, player, holder.mode(), value));
        return true;
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode, @NotNull String action) {
        DraftKey key = new DraftKey(player.getUniqueId(), mode);
        Draft draft = DRAFTS.computeIfAbsent(key, ignored -> Draft.empty(mode));

        if (action.equals(ACTION_BACK)) {
            back(plugin, player, mode);
            return;
        }

        if (action.equals(ACTION_ID)) {
            prompt(plugin, player, mode, "Enter the " + mode.displayName() + " id.", value -> draft.withId(value));
            return;
        }

        if (action.equals(ACTION_NAME)) {
            prompt(plugin, player, mode, "Enter the " + mode.displayName() + " display name.", value -> draft.withName(value));
            return;
        }

        if (action.equals(ACTION_DESCRIPTION)) {
            prompt(plugin, player, mode, "Enter the " + mode.displayName() + " description.", value -> draft.withDescription(value));
            return;
        }

        if (action.equals(ACTION_SAVE)) {
            save(plugin, player, mode, key, draft);
        }
    }

    private static void prompt(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode, @NotNull String message, @NotNull Function<String, Draft> update) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), value -> {
            DRAFTS.put(new DraftKey(player.getUniqueId(), mode), update.apply(value));
            open(plugin, player, mode);
        }, () -> open(plugin, player, mode));
    }

    private static void save(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode, @NotNull DraftKey key, @NotNull Draft draft) {
        if (!canUse(plugin, player, mode)) {
            noPermission(plugin, player);
            return;
        }

        try {
            CustomTaxonomyEntry entry = new CustomTaxonomyEntry(draft.id(), draft.name(), draft.description(), player.getUniqueId());
            switch (mode) {
                case TAG -> plugin.customTags().save(entry);
                case COLLECTION -> plugin.customCollections().save(entry);
            }
            DRAFTS.remove(key);
            plugin.headRegistry().onLocalMutation();
            plugin.clearSearchCache();
            player.sendMessage(plugin.messages().taxonomyCreated(player, mode.displayName().toLowerCase(java.util.Locale.ROOT), entry.name(), entry.id()));
            back(plugin, player, mode);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            open(plugin, player, mode);
        }
    }

    private static void back(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode) {
        switch (mode) {
            case TAG -> TagsMenu.open(plugin, player);
            case COLLECTION -> CollectionsMenu.open(plugin, player);
        }
    }

    private static void render(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull Draft draft, @NotNull Mode mode) {
        inventory.setItem(SLOT_PREVIEW, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(mode.iconKey()), GuiItems.name(mode.titleFallback(), NamedTextColor.GOLD), List.of(GuiItems.idDetail("ID", valueOrUnset(draft.id())), GuiItems.idDetail("Name", valueOrUnset(draft.name())))));
        inventory.setItem(plugin.guiConfig().slot("taxonomy.id", SLOT_ID), actionItem(plugin, "taxonomy-id", ACTION_ID, "ID", valueOrUnset(draft.id())));
        inventory.setItem(plugin.guiConfig().slot("taxonomy.name", SLOT_NAME), actionItem(plugin, "taxonomy-name", ACTION_NAME, "Name", valueOrUnset(draft.name())));
        inventory.setItem(plugin.guiConfig().slot("taxonomy.description", SLOT_DESCRIPTION), actionItem(plugin, "taxonomy-description", ACTION_DESCRIPTION, "Description", valueOrUnset(draft.description())));
        inventory.setItem(plugin.guiConfig().slot("taxonomy.save", SLOT_SAVE), actionOnly(plugin, "taxonomy-save", ACTION_SAVE));
        inventory.setItem(plugin.guiConfig().slot("taxonomy.back", SLOT_BACK), actionOnly(plugin, "back", ACTION_BACK));
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String iconKey, @NotNull String action, @NotNull String key, @NotNull Object value) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey), GuiItems.mini(plugin.guiConfig().icon(iconKey).name()), List.of(GuiItems.idDetail(key, value), GuiItems.lore("Click to edit.", NamedTextColor.GREEN)));
        stamp(plugin, item, action);
        return item;
    }

    private static @NotNull ItemStack actionOnly(@NotNull HeadDBPlugin plugin, @NotNull String iconKey, @NotNull String action) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        return item;
    }

    private static boolean canUse(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Mode mode) {
        return plugin.adminModes().enabled(player) && Permissions.has(player, mode.permission());
    }

    private static @NotNull String valueOrUnset(@NotNull String value) {
        return value.isBlank() ? "Not set" : value;
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
        return new NamespacedKey(plugin, "taxonomy_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
    }

    private enum Mode {
        TAG("tag", "Tag", "create-tag", "title.create-tag", "Create Tag", Permissions.GUI_CREATE_TAG),
        COLLECTION("collection", "Collection", "create-collection", "title.create-collection", "Create Collection", Permissions.GUI_CREATE_COLLECTION);

        private final String id;
        private final String displayName;
        private final String iconKey;
        private final String titleKey;
        private final String titleFallback;
        private final String permission;

        Mode(@NotNull String id, @NotNull String displayName, @NotNull String iconKey, @NotNull String titleKey, @NotNull String titleFallback, @NotNull String permission) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            this.iconKey = Objects.requireNonNull(iconKey, "iconKey");
            this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
            this.titleFallback = Objects.requireNonNull(titleFallback, "titleFallback");
            this.permission = Objects.requireNonNull(permission, "permission");
        }

        private @NotNull String displayName() { return displayName; }
        private @NotNull String iconKey() { return iconKey; }
        private @NotNull String titleKey() { return titleKey; }
        private @NotNull String titleFallback() { return titleFallback; }
        private @NotNull String permission() { return permission; }
    }

    private record Draft(@NotNull String id, @NotNull String name, @NotNull String description) {
        private static @NotNull Draft empty(@NotNull Mode mode) {
            return new Draft("", "", "Local custom " + mode.displayName().toLowerCase(java.util.Locale.ROOT) + ".");
        }

        private @NotNull Draft withId(@NotNull String id) { return new Draft(id, name, description); }
        private @NotNull Draft withName(@NotNull String name) { return new Draft(id, name, description); }
        private @NotNull Draft withDescription(@NotNull String description) { return new Draft(id, name, description); }
    }

    private record DraftKey(@NotNull UUID playerId, @NotNull Mode mode) {}

    private static final class Holder implements InventoryHolder {
        private final Mode mode;
        private Inventory inventory;

        private Holder(@NotNull Mode mode) {
            this.mode = Objects.requireNonNull(mode, "mode");
        }

        private @NotNull Mode mode() {
            return mode;
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
