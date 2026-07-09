package io.github.silentdevelopment.headdb.paper.gui.material;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.category.MoreCategoriesMenu;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiIconConfigEditor;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class MaterialSelectionMenu {

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
    private static final String ACTION_SELECT = "select:";
    private static final int[] MATERIAL_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final List<Material> MATERIALS = Arrays.stream(Material.values())
            .filter(GuiMaterials::modernUsableMaterial)
            .sorted(Comparator.comparing(Material::name))
            .toList();

    private MaterialSelectionMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void openForButton(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String buttonKey) {
        open(plugin, player, MaterialTarget.button(buttonKey), 0);
    }

    public static void openForCategoryCreate(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String name, @NotNull String categoryId, @NotNull String material) {
        open(plugin, player, MaterialTarget.categoryCreate(name, categoryId, material), 0);
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MaterialTarget target, int requestedPage) {
        int pages = pageCount(MATERIALS.size());
        int page = clampPage(requestedPage, pages);
        MaterialSelectionHolder holder = new MaterialSelectionHolder(target, page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Select Material", plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderMaterials(plugin, inventory, page);
        renderControls(plugin, inventory, page, pages);

        player.openInventory(inventory);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");

        if (!(event.getView().getTopInventory().getHolder() instanceof MaterialSelectionHolder holder)) {
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

        handleAction(plugin, player, holder, action.get());
        return true;
    }

    private static void renderMaterials(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, int page) {
        int fromIndex = page * MATERIAL_SLOTS.length;
        int toIndex = Math.min(MATERIALS.size(), fromIndex + MATERIAL_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            Material material = MATERIALS.get(index);
            ItemStack item = GuiItems.item(material, GuiItems.name(displayName(material), NamedTextColor.GOLD), List.of(GuiItems.metaDetail("Material", material.name()), GuiItems.lore("Click to select this material.", NamedTextColor.GREEN)));
            stamp(plugin, item, ACTION_SELECT + material.name());
            inventory.setItem(MATERIAL_SLOTS[slotIndex], item);
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

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MaterialSelectionHolder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            reopenTarget(plugin, player, holder.target());
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.target(), holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.target(), holder.page() + 1);
            return;
        }

        if (action.equals(ACTION_TYPE)) {
            promptMaterial(plugin, player, holder.target());
            return;
        }

        if (!action.startsWith(ACTION_SELECT)) {
            return;
        }

        apply(plugin, player, holder.target(), action.substring(ACTION_SELECT.length()));
    }

    private static void promptMaterial(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MaterialTarget target) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text("Type a modern item material name.", NamedTextColor.GOLD), value -> {
            Optional<Material> material = GuiMaterials.item(value);
            if (material.isEmpty()) {
                player.sendMessage(Component.text("Unknown modern item material: ", NamedTextColor.RED).append(Component.text(value, NamedTextColor.GOLD)));
                reopenTarget(plugin, player, target);
                return;
            }

            apply(plugin, player, target, material.get().name());
        }, () -> reopenTarget(plugin, player, target));
    }

    private static void apply(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MaterialTarget target, @NotNull String material) {
        if (target.kind() == MaterialTargetKind.GUI_BUTTON) {
            new GuiIconConfigEditor(plugin).setMaterial(target.buttonKey(), material);
            player.sendMessage(Component.text("Button material set to ", NamedTextColor.GRAY).append(Component.text(material, NamedTextColor.GOLD)));
            io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu.open(plugin, player, target.buttonKey());
            return;
        }

        MoreCategoriesMenu.openEdit(plugin, player, target.categoryId(), target.categoryName(), material);
    }

    private static void reopenTarget(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull MaterialTarget target) {
        if (target.kind() == MaterialTargetKind.GUI_BUTTON) {
            io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu.open(plugin, player, target.buttonKey());
            return;
        }

        MoreCategoriesMenu.openEdit(plugin, player, target.categoryId(), target.categoryName(), target.material());
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
        return new org.bukkit.NamespacedKey(plugin, "material_selection_action");
    }

    private static @NotNull String displayName(@NotNull Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) entries / (double) MATERIAL_SLOTS.length);
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

    private enum MaterialTargetKind {
        GUI_BUTTON,
        CATEGORY_CREATE
    }

    private record MaterialTarget(@NotNull MaterialTargetKind kind, @NotNull String buttonKey, @NotNull String categoryName, @NotNull String categoryId, @NotNull String material) {

        private MaterialTarget {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(buttonKey, "buttonKey");
            Objects.requireNonNull(categoryName, "categoryName");
            Objects.requireNonNull(categoryId, "categoryId");
            Objects.requireNonNull(material, "material");
        }

        private static @NotNull MaterialTarget button(@NotNull String buttonKey) {
            return new MaterialTarget(MaterialTargetKind.GUI_BUTTON, buttonKey, "", "", "");
        }

        private static @NotNull MaterialTarget categoryCreate(@NotNull String name, @NotNull String categoryId, @NotNull String material) {
            return new MaterialTarget(MaterialTargetKind.CATEGORY_CREATE, "", name, categoryId, material);
        }
    }

    private static final class MaterialSelectionHolder implements InventoryHolder {

        private final MaterialTarget target;
        private final int page;
        private Inventory inventory;

        private MaterialSelectionHolder(@NotNull MaterialTarget target, int page) {
            this.target = Objects.requireNonNull(target, "target");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Material selection inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull MaterialTarget target() {
            return target;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
