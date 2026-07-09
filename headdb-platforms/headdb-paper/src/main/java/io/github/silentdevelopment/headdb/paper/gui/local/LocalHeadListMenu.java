package io.github.silentdevelopment.headdb.paper.gui.local;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.item.HeadItemIds;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadEntry;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class LocalHeadListMenu {

    public static final String CUSTOM_TITLE = "More Heads";
    public static final String PLAYER_TITLE = "Player Heads";

    private static final int SIZE = 54;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final HeadTexture PLAYER_FALLBACK_TEXTURE = new HeadTexture("0");
    private static final String ACTION_BACK = "back";
    private static final String ACTION_PREVIOUS = "previous";
    private static final String ACTION_NEXT = "next";

    private LocalHeadListMenu() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void openCustom(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        openCustom(plugin, player, 0);
    }

    public static void openCustom(@NotNull HeadDBPlugin plugin, @NotNull Player player, int page) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        List<Head> heads = (plugin.adminModes().enabled(player) ? plugin.headRegistry().customHeads().listStored().stream().map(StoredCustomHead::toHead).toList() : plugin.headRegistry().customHeads().list()).stream().sorted(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER)).toList();
        open(plugin, player, LocalHeadListType.CUSTOM, heads, page);
    }

    public static void openPlayers(@NotNull HeadDBPlugin plugin, @NotNull Player player) {
        openPlayers(plugin, player, 0);
    }

    public static void openPlayers(@NotNull HeadDBPlugin plugin, @NotNull Player player, int page) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");

        List<Head> heads = playerHeads(plugin).stream().sorted(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER)).toList();
        open(plugin, player, LocalHeadListType.PLAYER, heads, page);
    }

    public static boolean handleClick(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull InventoryClickEvent event, @NotNull Consumer<HeadId> edit, @NotNull Consumer<ItemStack> give) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(edit, "edit");
        Objects.requireNonNull(give, "give");

        if (!(event.getView().getTopInventory().getHolder() instanceof LocalHeadListHolder holder)) {
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
                player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
                plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.NO_PERMISSION);
                return true;
            }

            boolean added = plugin.favorites().toggle(player.getUniqueId(), id.get());
            plugin.sounds().play(player, added ? io.github.silentdevelopment.headdb.paper.sound.SoundKey.FAVORITE_ADD : io.github.silentdevelopment.headdb.paper.sound.SoundKey.FAVORITE_REMOVE);
            open(plugin, player, holder.type(), holder.page());
            return true;
        }

        give.accept(item);
        return true;
    }

    public static boolean isLocalListTitle(@NotNull String title) {
        Objects.requireNonNull(title, "title");
        return title.equals(CUSTOM_TITLE) || title.equals(PLAYER_TITLE) || title.startsWith(CUSTOM_TITLE + " ") || title.startsWith(PLAYER_TITLE + " ");
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull LocalHeadListType type, @NotNull List<Head> heads, int requestedPage) {
        int pages = pageCount(heads.size());
        int page = clampPage(requestedPage, pages);
        LocalHeadListHolder holder = new LocalHeadListHolder(type, page);
        boolean adminMode = plugin.adminModes().enabled(player);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, GuiTitles.title(title(plugin, type, page, pages), adminMode));
        holder.inventory(inventory);

        fillBorder(inventory);
        renderHeads(plugin, player, inventory, heads, page, adminMode);
        renderControls(plugin, inventory, type, page, pages, heads.size(), adminMode);

        player.openInventory(inventory);
        plugin.sounds().play(player, io.github.silentdevelopment.headdb.paper.sound.SoundKey.MENU_OPEN);
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
                    lore.add(GuiItems.idDetail("ID", headIdLabel(head.id())));
                    lore.add(GuiItems.metaDetail("Category", head.category()));
                    lore.add(Component.text("Press ", NamedTextColor.GRAY).append(Component.keybind("key.drop", NamedTextColor.GOLD)).append(Component.text(" to edit.", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);
                });
            }
            inventory.setItem(HEAD_SLOTS[slotIndex], item);
            slotIndex++;
        }
    }

    private static void renderControls(@NotNull HeadDBPlugin plugin, @NotNull Inventory inventory, @NotNull LocalHeadListType type, int page, int pages, int totalHeads, boolean adminMode) {
        if (page > 0) {
            inventory.setItem(SLOT_PREVIOUS, control(plugin, ACTION_PREVIOUS, "previous"));
        }

        inventory.setItem(SLOT_BACK, control(plugin, ACTION_BACK, "back"));
        inventory.setItem(SLOT_INFO, info(type, page, pages, totalHeads, adminMode));

        if (page + 1 < pages) {
            inventory.setItem(SLOT_NEXT, control(plugin, ACTION_NEXT, "next"));
        }
    }

    private static void handleAction(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull LocalHeadListHolder holder, @NotNull String action) {
        if (action.equals(ACTION_BACK)) {
            if (holder.type() == LocalHeadListType.CUSTOM) {
                plugin.guis().openBrowse(player);
                return;
            }

            plugin.guis().openMain(player);
            return;
        }

        if (action.equals(ACTION_PREVIOUS)) {
            open(plugin, player, holder.type(), holder.page() - 1);
            return;
        }

        if (action.equals(ACTION_NEXT)) {
            open(plugin, player, holder.type(), holder.page() + 1);
        }
    }

    private static void open(@NotNull HeadDBPlugin plugin, @NotNull Player player, @NotNull LocalHeadListType type, int page) {
        if (type == LocalHeadListType.PLAYER) {
            openPlayers(plugin, player, page);
            return;
        }

        openCustom(plugin, player, page);
    }

    private static @NotNull List<Head> playerHeads(@NotNull HeadDBPlugin plugin) {
        List<Head> heads = new ArrayList<>();

        for (PlayerHeadEntry entry : plugin.headRegistry().playerHeads().knownPlayers()) {
            heads.add(head(entry));
        }

        return List.copyOf(heads);
    }

    private static @NotNull Head head(@NotNull PlayerHeadEntry entry) {
        HeadId id = entry.uuid() == null ? new HeadId("player:" + entry.name()) : HeadId.player(entry.uuid());
        return new Head(id, entry.name(), PLAYER_FALLBACK_TEXTURE, "players", Set.of("player"), Set.of());
    }

    private static @NotNull ItemStack control(@NotNull HeadDBPlugin plugin, @NotNull String action, @NotNull String iconKey) {
        ItemStack item = GuiHeadIcons.icon(plugin, plugin.guiConfig().icon(iconKey));
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey(plugin), PersistentDataType.STRING, action));
        return item;
    }

    private static @NotNull ItemStack info(@NotNull LocalHeadListType type, int page, int pages, int totalHeads, boolean adminMode) {
        List<Component> lore = new ArrayList<>();
        lore.add(GuiItems.lore("Page: " + (page + 1) + " / " + pages, NamedTextColor.GRAY));
        lore.add(GuiItems.idDetail("Heads", totalHeads));
        lore.add(Component.empty());
        lore.add(GuiItems.lore("Click a head to receive it.", NamedTextColor.GREEN));
        if (adminMode) {
            lore.add(Component.text("Press ", NamedTextColor.GRAY).append(Component.keybind("key.drop", NamedTextColor.GOLD)).append(Component.text(" to edit.", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
        }
        return GuiItems.item(Material.BOOK, GuiItems.name(type.displayName(), NamedTextColor.GOLD), lore);
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
        return new org.bukkit.NamespacedKey(plugin, "local_menu_action");
    }

    private static @NotNull String title(@NotNull HeadDBPlugin plugin, @NotNull LocalHeadListType type, int page, int pages) {
        String key = type == LocalHeadListType.CUSTOM ? "title.custom-heads-page" : "title.player-heads-page";
        String fallback = type.displayName() + " %page%/%pages%";
        return plugin.guiConfig().text(key, fallback).replace("%page%", String.valueOf(page + 1)).replace("%pages%", String.valueOf(Math.max(1, pages)));
    }


    private static @NotNull String headIdLabel(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        return id.display();
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

    private enum LocalHeadListType {
        CUSTOM(CUSTOM_TITLE),
        PLAYER(PLAYER_TITLE);

        private final String displayName;

        LocalHeadListType(@NotNull String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        private @NotNull String displayName() {
            return displayName;
        }
    }

    private static final class LocalHeadListHolder implements InventoryHolder {

        private final LocalHeadListType type;
        private final int page;
        private Inventory inventory;

        private LocalHeadListHolder(@NotNull LocalHeadListType type, int page) {
            this.type = Objects.requireNonNull(type, "type");
            this.page = page;
        }

        @Override
        public @NotNull Inventory getInventory() {
            Inventory currentInventory = inventory;
            if (currentInventory == null) {
                throw new IllegalStateException("Local head list inventory has not been assigned.");
            }
            return currentInventory;
        }

        private @NotNull LocalHeadListType type() {
            return type;
        }

        private int page() {
            return page;
        }

        private void inventory(@NotNull Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }
    }
}
