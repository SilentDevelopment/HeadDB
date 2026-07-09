package io.github.silentdevelopment.headdb.paper.gui.config;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class GuiButtonLoreEditorMenu {

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_PREVIOUS = 46;
    private static final int SLOT_NEXT = 52;
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";
    private static final String ACTION_ADD = "add";
    private static final String ACTION_CLEAR = "clear";
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_LINE = "line:";
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private GuiButtonLoreEditorMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String buttonKey) {
        open(plugin, player, buttonKey, 0);
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String buttonKey, int requestedPage) {
        GuiIconConfig icon = plugin.guiConfig().iconOrDefault(buttonKey, fallbackKey(buttonKey));
        List<String> lines = icon.lore();
        int pages = pageCount(lines.size());
        int page = clampPage(requestedPage, pages);
        GuiButtonLoreHolder holder = new GuiButtonLoreHolder(buttonKey, page);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title("Button Lore", buttonKey, plugin.adminModes().enabled(player)));
        holder.inventory(inventory);

        fill(inventory);
        inventory.setItem(plugin.guiConfig().slot("gui-button-lore.preview", 4), GuiHeadIcons.icon(plugin, icon));
        renderLines(plugin, inventory, lines, page);
        renderControls(plugin, inventory, buttonKey, page, pages);

        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiButtonLoreHolder holder)) {
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

        handleAction(plugin, player, holder, event.getClick(), action.get());
        return true;
    }

    private static void renderLines(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull List<String> lines, int page) {
        int fromIndex = page * ENTRY_SLOTS.length;
        int toIndex = Math.min(lines.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon("button-lore-line"), GuiItems.name("Line " + (index + 1), NamedTextColor.GOLD), List.of(GuiItems.miniOrWhite(lines.get(index)), Component.empty(), GuiItems.lore("Click to edit this line.", NamedTextColor.GREEN), GuiItems.lore("Press drop to remove this line.", NamedTextColor.RED)));
            stamp(plugin, item, ACTION_LINE + index);
            inventory.setItem(ENTRY_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull String key, int page, int pages) {
        inventory.setItem(plugin.guiConfig().slot("gui-button-lore.back", 45), actionItem(plugin, ACTION_BACK, "back"));
        inventory.setItem(plugin.guiConfig().slot("gui-button-lore.add", 48), actionItem(plugin, ACTION_ADD, "lore-add"));
        inventory.setItem(plugin.guiConfig().slot("gui-button-lore.clear", 50), actionItem(plugin, ACTION_CLEAR, "lore-clear"));
        inventory.setItem(plugin.guiConfig().slot("gui-button-lore.reset", 53), actionItem(plugin, ACTION_RESET, "lore-reset"));
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, actionItem(plugin, ACTION_PREVIOUS, "previous"));
        }
        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, actionItem(plugin, ACTION_NEXT, "next"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull GuiButtonLoreHolder holder, @NotNull ClickType click, @NotNull String action) {
        GuiIconConfigEditor editor = new GuiIconConfigEditor(plugin);

        if (action.equals(ACTION_BACK)) {
            GuiButtonEditorMenu.open(plugin, player, holder.key());
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.key(), holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.key(), holder.page() + 1);
            return;
        }

        if (action.equals(ACTION_ADD)) {
            prompt(plugin, player, holder.key(), "Enter the lore line to add.", value -> {
                List<String> lines = new ArrayList<>(plugin.guiConfig().iconOrDefault(holder.key(), fallbackKey(holder.key())).lore());
                lines.add(value);
                editor.setLore(holder.key(), lines);
            });
            return;
        }

        if (action.equals(ACTION_CLEAR)) {
            editor.clearLore(holder.key());
            player.sendMessage(Component.text("Button lore cleared.", NamedTextColor.GRAY));
            open(plugin, player, holder.key(), 0);
            return;
        }

        if (action.equals(ACTION_RESET)) {
            editor.clearLore(holder.key());
            player.sendMessage(Component.text("Button lore reset.", NamedTextColor.GRAY));
            open(plugin, player, holder.key(), 0);
            return;
        }

        if (!action.startsWith(ACTION_LINE)) {
            return;
        }

        int index = parseIndex(action.substring(ACTION_LINE.length()));
        List<String> lines = new ArrayList<>(plugin.guiConfig().iconOrDefault(holder.key(), fallbackKey(holder.key())).lore());
        if (index < 0 || index >= lines.size()) {
            open(plugin, player, holder.key(), holder.page());
            return;
        }

        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            lines.remove(index);
            editor.setLore(holder.key(), lines);
            player.sendMessage(Component.text("Button lore line removed.", NamedTextColor.GRAY));
            open(plugin, player, holder.key(), holder.page());
            return;
        }

        prompt(plugin, player, holder.key(), "Enter replacement text for lore line " + (index + 1) + ".", value -> {
            List<String> updated = new ArrayList<>(plugin.guiConfig().iconOrDefault(holder.key(), fallbackKey(holder.key())).lore());
            if (index >= 0 && index < updated.size()) {
                updated.set(index, value);
            }
            editor.setLore(holder.key(), updated);
        });
    }

    private static void prompt(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull String key, @NotNull String message, @NotNull java.util.function.Consumer<String> edit) {
        player.closeInventory();
        plugin.prompts().request(player, Component.text(message, NamedTextColor.GOLD), value -> {
            edit.accept(value);
            player.sendMessage(Component.text("Button lore updated: ", NamedTextColor.GRAY).append(Component.text(key, NamedTextColor.GOLD)));
            open(plugin, player, key, 0);
        }, () -> open(plugin, player, key, 0));
    }


    private static @NotNull String fallbackKey(@NotNull String key) {
        Objects.requireNonNull(key, "key");

        if (key.startsWith("category.")) {
            return "category";
        }

        return key;
    }

    private static @NotNull ItemStack actionItem(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        stamp(plugin, item, action);
        return item;
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
        return new org.bukkit.NamespacedKey(plugin, "gui_button_lore_action");
    }

    private static void fill(@NotNull Inventory inventory) {
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

    private static int parseIndex(@NotNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static final class GuiButtonLoreHolder implements InventoryHolder {

        private final String key;
        private final int page;
        private Inventory inventory;

        private GuiButtonLoreHolder(@NotNull String key, int page) {
            this.key = Objects.requireNonNull(key, "key");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("GUI button lore inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull String key() {
            return key;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
