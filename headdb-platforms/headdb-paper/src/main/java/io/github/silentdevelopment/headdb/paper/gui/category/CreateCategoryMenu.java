package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class CreateCategoryMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_PREVIEW = 4;
    private static final int SLOT_NAME = 20;
    private static final int SLOT_DESCRIPTION = 22;
    private static final int SLOT_MATERIAL = 24;
    private static final int SLOT_HEAD_ICON = 29;
    private static final int SLOT_PERMISSION = 31;
    private static final int SLOT_HEADS = 33;
    private static final int SLOT_SAVE_DRAFT = 38;
    private static final int SLOT_PUBLISH = 40;
    private static final int SLOT_DELETE = 42;
    private static final int SLOT_PRICE = 44;
    private static final int SLOT_BACK = 45;
    private static final Map<UUID, String> ACTIVE_DRAFTS = new ConcurrentHashMap<>();
    private static final String ACTION_BACK = "back";
    private static final String ACTION_NAME = "name";
    private static final String ACTION_DESCRIPTION = "description";
    private static final String ACTION_MATERIAL = "material";
    private static final String ACTION_HEAD_ICON = "head-icon";
    private static final String ACTION_PERMISSION = "permission";
    private static final String ACTION_HEADS = "heads";
    private static final String ACTION_SAVE_DRAFT = "save-draft";
    private static final String ACTION_PUBLISH = "publish";
    private static final String ACTION_DELETE = "delete";
    private static final String ACTION_PRICE = "price";

    private CreateCategoryMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        if (!canUse(plugin, player)) {
            noPermission(plugin, player);
            return;
        }

        String id = createDraft(plugin, player);
        ACTIVE_DRAFTS.put(player.getUniqueId(), id);
        openExisting(plugin, player, id);
    }

    public static void openExisting(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String id) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        if (!canUse(plugin, player)) {
            noPermission(plugin, player);
            return;
        }

        Optional<CustomCategory> existing = plugin.customCategories().find(id);
        if (existing.isEmpty()) {
            ACTIVE_DRAFTS.remove(player.getUniqueId());
            plugin.guis().openBrowse(player);
            return;
        }

        ACTIVE_DRAFTS.put(player.getUniqueId(), existing.get().id());
        Holder holder = new Holder(existing.get().id());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(existing.get().draft() ? "Create Category" : "Edit Category", true));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        render(plugin, inventory, existing.get());
        player.openInventory(inventory);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
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
        action.ifPresent(value -> handleAction(plugin, player, holder, value));
        return true;
    }

    public static void handleClose(@NotNull HeadDBPlugin plugin, @NotNull Player player, @Nullable InventoryHolder holder) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        if (!(holder instanceof Holder createHolder)) {
            return;
        }

        plugin.customCategories().find(createHolder.id()).ifPresent(plugin.customCategories()::save);
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull String action) {
        Optional<CustomCategory> existing = plugin.customCategories().find(holder.id());
        if (existing.isEmpty()) {
            plugin.guis().openBrowse(player);
            return;
        }

        CustomCategory category = existing.get();
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openBrowse(player);
            return;
        }

        if (action.equals(ACTION_NAME)) {
            prompt(plugin, player, category, "Enter the category display name.", value -> category.withName(value));
            return;
        }

        if (action.equals(ACTION_DESCRIPTION)) {
            prompt(plugin, player, category, "Enter the category description.", value -> category.withDescription(value));
            return;
        }

        if (action.equals(ACTION_MATERIAL)) {
            prompt(plugin, player, category, "Enter a Bukkit material name for the icon.", value -> category.withMaterial(value));
            return;
        }

        if (action.equals(ACTION_HEAD_ICON)) {
            prompt(plugin, player, category, "Enter a head id for the icon, or none to clear.", value -> headIcon(plugin, player, category, value));
            return;
        }

        if (action.equals(ACTION_PERMISSION)) {
            player.sendMessage(Component.text("Permission: ", NamedTextColor.GRAY).append(Component.text(Permissions.category(category.id()), NamedTextColor.GOLD)));
            openExisting(plugin, player, category.id());
            return;
        }

        if (action.equals(ACTION_HEADS)) {
            CategoryMembersMenu.open(plugin, player, category.id(), 0);
            return;
        }

        if (action.equals(ACTION_SAVE_DRAFT)) {
            plugin.customCategories().save(category.withDraft(true));
            player.sendMessage(Component.text("Category draft saved: ", NamedTextColor.GRAY).append(Component.text(categoryLabel(plugin, player, category), NamedTextColor.GOLD)));
            openExisting(plugin, player, category.id());
            return;
        }

        if (action.equals(ACTION_PUBLISH)) {
            plugin.customCategories().save(category.withDraft(false));
            ACTIVE_DRAFTS.remove(player.getUniqueId());
            plugin.clearSearchCache();
            player.sendMessage(Component.text("Category published: ", NamedTextColor.GRAY).append(Component.text(categoryLabel(plugin, player, category), NamedTextColor.GOLD)));
            openExisting(plugin, player, category.id());
            return;
        }

        if (action.equals(ACTION_DELETE)) {
            DeleteCategoryConfirmMenu.open(plugin, player, category.id());
            return;
        }

        if (action.equals(ACTION_PRICE)) {
            promptPrice(plugin, player, category);
        }
    }

    private static @NotNull CustomCategory headIcon(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category, @NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("none") || trimmed.equalsIgnoreCase("clear")) {
            return category.withMaterial("CHEST");
        }

        HeadId id = trimmed.matches("[1-9][0-9]*") ? HeadId.remote(trimmed) : new HeadId(trimmed);
        if (plugin.headRegistry().find(id).isEmpty()) {
            player.sendMessage(Component.text("Unknown head: ", NamedTextColor.RED).append(Component.text(id.display(), NamedTextColor.GOLD)));
            return category;
        }

        return category.withMaterial("HEAD:" + id);
    }

    private static void prompt(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category, @NotNull String message, @NotNull java.util.function.Function<String, CustomCategory> update) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), value -> {
            try {
                CustomCategory updated = update.apply(value).withDraft(true);
                plugin.customCategories().save(updated);
                openExisting(plugin, player, updated.id());
            } catch (IllegalArgumentException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                openExisting(plugin, player, category.id());
            }
        }, () -> openExisting(plugin, player, category.id()));
    }

    private static void render(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull CustomCategory category) {
        inventory.setItem(SLOT_PREVIEW, preview(plugin, category));
        inventory.setItem(plugin.guiConfig().slot("create-category.name", SLOT_NAME), actionItem(plugin, "category-name", ACTION_NAME, "Name", category.name()));
        inventory.setItem(plugin.guiConfig().slot("create-category.description", SLOT_DESCRIPTION), actionItem(plugin, "category-description", ACTION_DESCRIPTION, "Description", category.description()));
        inventory.setItem(plugin.guiConfig().slot("create-category.material", SLOT_MATERIAL), actionItem(plugin, "category-material", ACTION_MATERIAL, "Material", category.material()));
        inventory.setItem(plugin.guiConfig().slot("create-category.head-icon", SLOT_HEAD_ICON), actionItem(plugin, "category-head-icon", ACTION_HEAD_ICON, "Head Icon", category.headIcon() ? category.headIconId() : "Not set"));
        inventory.setItem(plugin.guiConfig().slot("create-category.permission", SLOT_PERMISSION), actionItem(plugin, "category-permission", ACTION_PERMISSION, "Permission", Permissions.category(category.id())));
        inventory.setItem(plugin.guiConfig().slot("create-category.heads", SLOT_HEADS), actionItem(plugin, "category-view-heads", ACTION_HEADS, "Heads", category.headIds().size()));
        inventory.setItem(plugin.guiConfig().slot("create-category.save-draft", SLOT_SAVE_DRAFT), actionOnly(plugin, "category-save-draft", ACTION_SAVE_DRAFT));
        inventory.setItem(plugin.guiConfig().slot("create-category.publish", SLOT_PUBLISH), actionOnly(plugin, "category-publish", ACTION_PUBLISH));
        inventory.setItem(plugin.guiConfig().slot("create-category.delete", SLOT_DELETE), actionOnly(plugin, "category-delete", ACTION_DELETE));
        if (plugin.economy().enabled()) {
            inventory.setItem(plugin.guiConfig().slot("create-category.price", SLOT_PRICE), actionOnly(plugin, "category-price", ACTION_PRICE));
        }
        inventory.setItem(plugin.guiConfig().slot("create-category.back", SLOT_BACK), actionOnly(plugin, "back", ACTION_BACK));
    }

    private static @NotNull String categoryLabel(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category) {
        return plugin.adminModes().enabled(player) || Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN) ? category.name() + " (" + category.id() + ")" : category.name();
    }

    private static @NotNull ItemStack preview(@NotNull HeadDBPlugin plugin, @NotNull CustomCategory category) {
        ItemStack item;
        if (category.headIcon()) {
            item = plugin.headRegistry().find(new HeadId(category.headIconId())).map(plugin.itemFactory()::create).orElseGet(() -> GuiItems.item(Material.CHEST, Component.empty(), List.of()));
        } else {
            item = GuiItems.item(GuiMaterials.itemOr(category.material(), Material.CHEST), Component.empty(), List.of());
        }

        item.editMeta(meta -> {
            meta.displayName(GuiItems.name(category.draft() ? "DRAFT - " + category.name() : category.name(), category.draft() ? NamedTextColor.YELLOW : NamedTextColor.GOLD));
            meta.lore(List.of(GuiItems.idDetail("ID", category.id()), GuiItems.idDetail("Heads", category.headIds().size()), GuiItems.idDetail("Permission", Permissions.category(category.id()))));
        });
        return item;
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

    private static @NotNull String createDraft(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        String id = plugin.customCategories().nextId();
        CustomCategory category = new CustomCategory(id, "Draft Category " + id, "CHEST", "Local custom category.", true, Set.of());
        plugin.customCategories().save(category);
        return category.id();
    }

    private static void promptPrice(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull CustomCategory category) {
        if (!plugin.economy().enabled()) {
            openExisting(plugin, player, category.id());
            return;
        }

        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter the new category price, or 0 to clear.", NamedTextColor.GOLD), value -> {
            try {
                double price = Double.parseDouble(value.trim());
                plugin.economy().setCustomCategoryPrice(category.id(), price);
                player.sendMessage(plugin.messages().priceUpdated(player, categoryLabel(plugin, player, category), plugin.economy().format(price)));
            } catch (NumberFormatException exception) {
                player.sendMessage(plugin.messages().priceInvalid(player));
            } catch (RuntimeException exception) {
                player.sendMessage(Component.text("Failed to update category price: " + exception.getMessage(), NamedTextColor.RED));
            }

            openExisting(plugin, player, category.id());
        }, () -> openExisting(plugin, player, category.id()));
    }

    private static boolean canUse(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        return plugin.adminModes().enabled(player) && Permissions.has(player, Permissions.GUI_CUSTOM_CATEGORIES_ADMIN);
    }

    private static void fillBorder(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory) {
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
        return new NamespacedKey(plugin, "create_category_action");
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
    }

    private static final class Holder implements InventoryHolder {
        private final String id;
        private Inventory inventory;

        private Holder(@NotNull String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        private @NotNull String id() {
            return id;
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
