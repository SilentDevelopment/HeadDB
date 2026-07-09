package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class CustomCategoryViewMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private CustomCategoryViewMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String categoryId, int requestedPage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(categoryId, "categoryId");

        Optional<CustomCategory> existing = plugin.customCategories().find(categoryId);
        if (existing.isEmpty()) {
            plugin.guis().openBrowse(player);
            return;
        }

        CustomCategory category = existing.get();
        boolean adminMode = plugin.adminModes().enabled(player);
        if (category.draft() && !adminMode) {
            noPermission(plugin, player);
            return;
        }

        if (!Permissions.canViewCategory(player, category.id())) {
            noPermission(plugin, player);
            return;
        }

        List<Head> heads = heads(plugin, category, adminMode);
        int pages = pageCount(heads.size());
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Holder holder = new Holder(category.id(), page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(category.draft() ? "DRAFT - " + category.name() : category.name(), adminMode));
        holder.inventory(inventory);

        fillBorder(plugin, inventory);
        renderHeads(plugin, player, inventory, heads, page, adminMode);
        renderControls(plugin, player, inventory, category, heads.size(), page, pages);
        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event, @NotNull Consumer<HeadId> edit, @NotNull Consumer<ItemStack> give) {
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
        if (action.isPresent()) {
            plugin.sounds().playGuiAction(player, action.get());
            handleAction(plugin, player, holder, action.get());
            return true;
        }

        Optional<HeadId> id = HeadItemIds.read(plugin, item);
        if (id.isEmpty()) {
            return true;
        }

        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) && plugin.adminModes().enabled(player)) {
            edit.accept(id.get());
            return true;
        }

        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (!Permissions.has(player, Permissions.FAVORITES_TOGGLE)) {
                noPermission(plugin, player);
                return true;
            }

            plugin.favorites().toggle(player.getUniqueId(), id.get());
            open(plugin, player, holder.categoryId(), holder.page());
            return true;
        }

        give.accept(item);
        return true;
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Holder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            plugin.guis().openBrowse(player);
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
    }

    private static void renderHeads(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull List<Head> heads, int page, boolean adminMode) {
        int fromIndex = page * HEAD_SLOTS.length;
        int toIndex = Math.min(heads.size(), fromIndex + HEAD_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Head head = heads.get(index);
            ItemStack item = plugin.itemFactory().create(head);
            plugin.favorites().decorate(player.getUniqueId(), head.id(), item);
            if (adminMode) {
                item.editMeta(meta -> {
                    List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
                    lore.add(Component.empty());
                    lore.add(GuiItems.idDetail("ID", head.id().display()));
                    lore.add(GuiItems.metaDetail("Category", head.category()));
                    lore.add(Component.text("Press ", NamedTextColor.GRAY).append(Component.keybind("key.drop", NamedTextColor.GOLD)).append(Component.text(" to edit.", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                });
            }
            inventory.setItem(HEAD_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull Inventory inventory, @NotNull CustomCategory category, int totalHeads, int page, int pages) {
        inventory.setItem(SLOT_BACK, action(plugin, ACTION_BACK, "back"));
        inventory.setItem(SLOT_INFO, GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("info"), GuiItems.name("Info", NamedTextColor.GOLD), List.of(GuiItems.idDetail("Heads", totalHeads), GuiItems.idDetail("ID", category.id()), GuiItems.idDetail("Page", (page + 1) + " / " + Math.max(1, pages)))));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, action(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, action(plugin, ACTION_NEXT, "next"));
        }
    }

    private static @NotNull List<Head> heads(@NotNull HeadDBPlugin plugin, @NotNull CustomCategory category, boolean adminMode) {
        List<Head> heads = new ArrayList<>();
        for (HeadId id : category.headIds()) {
            Optional<Head> head = id.isCustom() && adminMode ? plugin.headRegistry().customHeads().findStored(id).map(stored -> stored.toHead()) : plugin.headRegistry().find(id);
            head.ifPresent(heads::add);
        }
        heads.sort(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(heads);
    }

    private static @NotNull ItemStack action(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        return item;
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
        return new NamespacedKey(plugin, "custom_category_view_action");
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) HEAD_SLOTS.length);
    }

    private static void noPermission(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
    }

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
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
