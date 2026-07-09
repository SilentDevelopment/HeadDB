package io.github.silentdevelopment.headdb.paper.gui.edit;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiLabels;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.hidden.HiddenHeadsMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.MoreCategoriesMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.BrowseMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CollectionsMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.TagsMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CreateHeadMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CreateCategoryMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CreateTaxonomyMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.DeleteCategoryConfirmMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CustomCategoryViewMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CategoryMembersMenu;
import io.github.silentdevelopment.headdb.paper.gui.category.CategoryHeadPickerMenu;
import io.github.silentdevelopment.headdb.paper.gui.settings.CommandConfirmMenu;
import io.github.silentdevelopment.headdb.paper.gui.favorites.FavoritesMenu;
import io.github.silentdevelopment.headdb.paper.gui.favorites.FavoriteClickGuard;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonEditorMenu;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiButtonLoreEditorMenu;
import io.github.silentdevelopment.headdb.paper.gui.material.MaterialSelectionMenu;
import io.github.silentdevelopment.headdb.paper.gui.local.LocalHeadListMenu;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.github.silentdevelopment.headdb.paper.gui.favorites.FavoritesMenu.give;

public final class HeadEditListener implements Listener {

    private final HeadDBPlugin plugin;

    public HeadEditListener(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        CreateHeadMenu.handleClose(plugin, player, holder);
        CreateCategoryMenu.handleClose(plugin, player, holder);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (GuiButtonEditorMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (GuiButtonLoreEditorMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (MaterialSelectionMenu.handleClick(plugin, player, event)) {
            return;
        }

        ItemStack earlyItem = event.getCurrentItem();
        if (earlyItem != null && !GuiMaterials.isAir(earlyItem.getType()) && (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) && plugin.adminModes().enabled(player)) {
            Optional<HeadEditMenu.ActionTarget> earlyAction = HeadEditMenu.actionTarget(plugin, earlyItem);
            if (earlyAction.isEmpty() || !HeadEditMenu.isLoreLineAction(earlyAction.get().action())) {
                Optional<String> guiIconKey = GuiButtonEditorMenu.readIconKey(plugin, earlyItem);
                if (guiIconKey.isPresent()) {
                    deny(event);
                    GuiButtonEditorMenu.markEditedDrop(player, guiIconKey.get());
                    GuiButtonEditorMenu.open(plugin, player, guiIconKey.get());
                    return;
                }
            }
        }

        if (BrowseMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CollectionsMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (TagsMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CommandConfirmMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CreateHeadMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CreateCategoryMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CreateTaxonomyMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CustomCategoryViewMenu.handleClick(plugin, player, event, id -> openEdit(player, id), item -> giveListedHead(player, item))) {
            return;
        }

        if (DeleteCategoryConfirmMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (DeleteHeadConfirmMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (handleAdvancedSearchClick(player, event)) {
            return;
        }

        if (HiddenHeadsMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (FavoritesMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CategoryHeadPickerMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (CategoryMembersMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (MoreCategoriesMenu.handleClick(plugin, player, event)) {
            return;
        }

        if (LocalHeadListMenu.handleClick(plugin, player, event, id -> openEdit(player, id), item -> giveListedHead(player, item))) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || GuiMaterials.isAir(item.getType())) {
            return;
        }

        Optional<HeadEditMenu.ActionTarget> actionTarget = HeadEditMenu.actionTarget(plugin, item);
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) && plugin.adminModes().enabled(player)) {
            if (actionTarget.isEmpty() || !HeadEditMenu.isLoreLineAction(actionTarget.get().action())) {
                Optional<String> guiIconKey = GuiButtonEditorMenu.readIconKey(plugin, item);
                if (guiIconKey.isPresent()) {
                    deny(event);
                    GuiButtonEditorMenu.markEditedDrop(player, guiIconKey.get());
                    GuiButtonEditorMenu.open(plugin, player, guiIconKey.get());
                    return;
                }
            }
        }

        if (actionTarget.isPresent()) {
            deny(event);
            handleAction(player, event, actionTarget.get());
            return;
        }

        Optional<HeadId> clickedHeadId = io.github.silentdevelopment.headdb.paper.item.HeadItemIds.read(plugin, item);
        if (clickedHeadId.isPresent() && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            deny(event);
            FavoriteClickGuard.mark(player, clickedHeadId.get());
            toggleFavorite(player, clickedHeadId.get());
            return;
        }

        Optional<HeadId> dropTarget = dropTarget(event, item);
        if (dropTarget.isPresent() && plugin.adminModes().enabled(player)) {
            deny(event);
            HeadEditDropGuard.mark(player, dropTarget.get());
            openEdit(player, dropTarget.get());
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof HeadEditMenu.EditHolder) {
            deny(event);
        }
    }

    private boolean handleAdvancedSearchClick(@NotNull Player player, @NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return false;
        }

        if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.SHIFT_RIGHT) {
            return false;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || GuiMaterials.isAir(item.getType())) {
            return false;
        }

        Optional<String> iconKey = GuiButtonEditorMenu.readIconKey(plugin, item);
        if (iconKey.isEmpty() || !iconKey.get().equals("search")) {
            return false;
        }

        deny(event);
        plugin.guis().openAdvancedSearch(player);
        return true;
    }


    private void toggleFavorite(@NotNull Player player, @NotNull HeadId id) {
        if (!Permissions.has(player, Permissions.FAVORITES_TOGGLE)) {
            noPermission(player);
            return;
        }

        boolean added = plugin.favorites().toggle(player.getUniqueId(), id);
        player.sendMessage(Component.text(added ? "Added favorite: " : "Removed favorite: ", added ? NamedTextColor.YELLOW : NamedTextColor.GRAY).append(Component.text(GuiLabels.head(plugin, player, id), NamedTextColor.GOLD)));
    }

    private void giveListedHead(@NotNull Player player, @NotNull ItemStack item) {
        Optional<HeadId> headId = io.github.silentdevelopment.headdb.paper.item.HeadItemIds.read(plugin, item);
        if (headId.isEmpty()) {
            return;
        }

        HeadId id = headId.get();
        if (!Permissions.has(player, Permissions.GUI_HEAD_TAKE)) {
            noPermission(player);
            return;
        }

        Optional<Head> head = plugin.headRegistry().find(id);
        if (head.isEmpty()) {
            player.sendMessage(Component.text("Head no longer exists.", NamedTextColor.RED));
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Your inventory is full.", NamedTextColor.RED));
            return;
        }

        if (!plugin.economy().charge(player, head.get(), 1)) {
            return;
        }

        java.util.Map<Integer, ItemStack> remaining = player.getInventory().addItem(item.clone());
        if (!remaining.isEmpty()) {
            player.sendMessage(Component.text("Your inventory is full.", NamedTextColor.RED));
        }
    }

    private @NotNull Optional<HeadId> dropTarget(@NotNull InventoryClickEvent event, @NotNull ItemStack item) {
        if (event.getClick() != ClickType.DROP && event.getClick() != ClickType.CONTROL_DROP) {
            return Optional.empty();
        }

        return io.github.silentdevelopment.headdb.paper.item.HeadItemIds.read(plugin, item);
    }

    private void openEdit(@NotNull Player player, @NotNull HeadId id) {
        if (!plugin.adminModes().enabled(player)) {
            player.sendMessage(Component.text("Enable Admin Mode to edit heads.", NamedTextColor.RED));
            return;
        }

        if (!Permissions.has(player, Permissions.GUI_EDIT) && !Permissions.has(player, Permissions.EDIT)) {
            noPermission(player);
            return;
        }

        HeadEditMenu.open(plugin, player, id);
    }

    private void handleAction(@NotNull Player player, @NotNull InventoryClickEvent event, @NotNull HeadEditMenu.ActionTarget target) {
        String action = target.action();
        HeadId id = target.id();
        HeadEditMenu.EditHolder holder = event.getView().getTopInventory().getHolder() instanceof HeadEditMenu.EditHolder editHolder ? editHolder : null;

        if (action.equals(HeadEditMenu.ACTION_PREVIEW)) {
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_BACK_MAIN)) {
            plugin.guis().openMain(player);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_BACK_EDIT)) {
            HeadEditMenu.open(plugin, player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_PREVIOUS) && holder != null) {
            openSameMenu(player, holder, holder.page() - 1);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_NEXT) && holder != null) {
            openSameMenu(player, holder, holder.page() + 1);
            return;
        }

        if (!plugin.adminModes().enabled(player)) {
            player.sendMessage(Component.text("Enable Admin Mode to edit heads.", NamedTextColor.RED));
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_NAME)) {
            promptName(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_LORE_MENU)) {
            HeadEditMenu.openLore(plugin, player, id, 0);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_CATEGORY_MENU)) {
            HeadEditMenu.openCategories(plugin, player, id, 0);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_TAGS_MENU)) {
            HeadEditMenu.openTags(plugin, player, id, 0);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_COLLECTIONS_MENU)) {
            HeadEditMenu.openCollections(plugin, player, id, 0);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_VISIBILITY)) {
            toggleVisibility(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_RESET)) {
            reset(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_DELETE)) {
            confirmDeleteCustom(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_PUBLISH)) {
            publishCustom(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_PRICE)) {
            promptPrice(player, id);
            return;
        }

        if (action.startsWith(HeadEditMenu.ACTION_CATEGORY_SELECT)) {
            setCategory(player, id, action.substring(HeadEditMenu.ACTION_CATEGORY_SELECT.length()));
            return;
        }

        if (action.startsWith(HeadEditMenu.ACTION_TAG_TOGGLE)) {
            toggleTag(player, id, action.substring(HeadEditMenu.ACTION_TAG_TOGGLE.length()), holder == null ? 0 : holder.page());
            return;
        }

        if (action.startsWith(HeadEditMenu.ACTION_COLLECTION_TOGGLE)) {
            toggleCollection(player, id, action.substring(HeadEditMenu.ACTION_COLLECTION_TOGGLE.length()), holder == null ? 0 : holder.page());
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_LORE_ADD)) {
            promptLoreAdd(player, id);
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_LORE_CLEAR)) {
            setLore(player, id, List.of(), "Lore cleared.");
            return;
        }

        if (action.equals(HeadEditMenu.ACTION_LORE_RESET)) {
            resetLore(player, id);
            return;
        }

        if (action.startsWith(HeadEditMenu.ACTION_LORE_SET)) {
            int line = parseIndex(action.substring(HeadEditMenu.ACTION_LORE_SET.length()));
            if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
                removeLoreLine(player, id, line);
                return;
            }

            promptLoreSet(player, id, line);
            return;
        }

        player.sendMessage(Component.text("Unknown edit action.", NamedTextColor.RED));
    }

    private void openSameMenu(@NotNull Player player, @NotNull HeadEditMenu.EditHolder holder, int page) {
        switch (holder.type()) {
            case CATEGORY -> HeadEditMenu.openCategories(plugin, player, holder.id(), page);
            case TAGS -> HeadEditMenu.openTags(plugin, player, holder.id(), page);
            case COLLECTIONS -> HeadEditMenu.openCollections(plugin, player, holder.id(), page);
            case LORE -> HeadEditMenu.openLore(plugin, player, holder.id(), page);
            case ROOT -> HeadEditMenu.open(plugin, player, holder.id());
        }
    }

    private void promptName(@NotNull Player player, @NotNull HeadId id) {
        if (!editable(player, id, Permissions.EDIT_NAME)) {
            return;
        }

        String oldName = plugin.headRegistry().find(id).map(Head::name).orElse(id.display());
        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter the new name.", NamedTextColor.GOLD), value -> {
            if (id.isRemote()) {
                RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
                plugin.headRegistry().overrides().save(override.withName(value, player.getUniqueId()));
            } else if (id.isCustom()) {
                StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
                plugin.headRegistry().customHeads().save(stored.withName(value));
            }

            mutated(player, id);
            player.sendMessage(Component.text("Name Updated", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text(oldName, NamedTextColor.GRAY).append(Component.text(" > ", NamedTextColor.DARK_GRAY)).append(Component.text(value, NamedTextColor.GREEN)));
            HeadEditMenu.open(plugin, player, id);
        });
    }

    private void setCategory(@NotNull Player player, @NotNull HeadId id, @NotNull String category) {
        if (!editable(player, id, Permissions.EDIT_CATEGORY)) {
            return;
        }

        if (id.isRemote()) {
            RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
            plugin.headRegistry().overrides().save(override.withCategory(category, player.getUniqueId()));
        } else if (id.isCustom()) {
            StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
            plugin.headRegistry().customHeads().save(stored.withCategory(category));
        }

        mutated(player, id);
        player.sendMessage(Component.text("Category set to ", NamedTextColor.GRAY).append(Component.text(GuiLabels.category(plugin, player, category), NamedTextColor.GOLD)));
        HeadEditMenu.open(plugin, player, id);
    }

    private void toggleTag(@NotNull Player player, @NotNull HeadId id, @NotNull String tag, int page) {
        if (!editable(player, id, Permissions.EDIT_TAGS)) {
            return;
        }

        boolean selected = plugin.headRegistry().find(id).map(head -> head.tags().contains(tag)).orElse(false);
        if (id.isRemote()) {
            RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
            plugin.headRegistry().overrides().save(selected ? override.withTagRemoved(tag, player.getUniqueId()) : override.withTagAdded(tag, player.getUniqueId()));
        } else if (id.isCustom()) {
            StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
            Set<String> tags = toggle(stored.tags(), tag);
            plugin.headRegistry().customHeads().save(stored.withTags(tags));
        }

        mutated(player, id);
        HeadEditMenu.openTags(plugin, player, id, page);
    }

    private void toggleCollection(@NotNull Player player, @NotNull HeadId id, @NotNull String collection, int page) {
        if (!editable(player, id, Permissions.EDIT_TAGS)) {
            return;
        }

        boolean selected = plugin.headRegistry().find(id).map(head -> head.collections().contains(collection)).orElse(false);
        if (id.isRemote()) {
            RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
            plugin.headRegistry().overrides().save(selected ? override.withCollectionRemoved(collection, player.getUniqueId()) : override.withCollectionAdded(collection, player.getUniqueId()));
        } else if (id.isCustom()) {
            StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
            Set<String> collections = toggle(stored.collections(), collection);
            plugin.headRegistry().customHeads().save(stored.withCollections(collections));
        }

        mutated(player, id);
        HeadEditMenu.openCollections(plugin, player, id, page);
    }

    private void toggleVisibility(@NotNull Player player, @NotNull HeadId id) {
        if (!id.isRemote() || !Permissions.has(player, Permissions.EDIT_VISIBILITY)) {
            noPermission(player);
            return;
        }

        boolean hidden = plugin.headRegistry().hidden(id);
        RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
        plugin.headRegistry().overrides().save(override.withHidden(!hidden, player.getUniqueId()));
        mutated(player, id);
        player.sendMessage(Component.text(hidden ? "Head is now visible." : "Head is now hidden.", hidden ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        HeadEditMenu.open(plugin, player, id);
    }

    private void reset(@NotNull Player player, @NotNull HeadId id) {
        if (!id.isRemote() || !Permissions.has(player, Permissions.EDIT_RESET)) {
            noPermission(player);
            return;
        }

        plugin.headRegistry().overrides().delete(id);
        mutated(player, id);
        player.sendMessage(Component.text("Local override reset.", NamedTextColor.GRAY));
        HeadEditMenu.open(plugin, player, id);
    }

    private void confirmDeleteCustom(@NotNull Player player, @NotNull HeadId id) {
        if (!id.isCustom() || !Permissions.has(player, Permissions.CUSTOM_DELETE)) {
            noPermission(player);
            return;
        }

        DeleteHeadConfirmMenu.open(plugin, player, id);
    }


    private void publishCustom(@NotNull Player player, @NotNull HeadId id) {
        if (!id.isCustom() || !Permissions.has(player, Permissions.CUSTOM_CREATE)) {
            noPermission(player);
            return;
        }

        StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
        plugin.headRegistry().customHeads().save(stored.withDraft(false));
        mutated(player, id);
        player.sendMessage(Component.text("Draft published: ", NamedTextColor.GRAY).append(Component.text(GuiLabels.head(plugin, player, id), NamedTextColor.GOLD)));
        HeadEditMenu.open(plugin, player, id);
    }

    private void promptPrice(@NotNull Player player, @NotNull HeadId id) {
        if (!plugin.economy().enabled()) {
            HeadEditMenu.open(plugin, player, id);
            return;
        }

        if (!editable(player, id, Permissions.EDIT)) {
            return;
        }

        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter the new head price, or 0 to clear.", NamedTextColor.GOLD), value -> {
            try {
                double price = Double.parseDouble(value.trim());
                plugin.economy().setHeadPrice(id, price);
                player.sendMessage(plugin.messages().priceUpdated(player, GuiLabels.head(plugin, player, id), plugin.economy().format(price)));
            } catch (NumberFormatException exception) {
                player.sendMessage(plugin.messages().priceInvalid(player));
            } catch (RuntimeException exception) {
                player.sendMessage(Component.text("Failed to update head price: " + exception.getMessage(), NamedTextColor.RED));
            }

            HeadEditMenu.open(plugin, player, id);
        }, () -> HeadEditMenu.open(plugin, player, id));
    }

    private void promptLoreAdd(@NotNull Player player, @NotNull HeadId id) {
        if (!editable(player, id, Permissions.EDIT_LORE)) {
            return;
        }

        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter the lore line to add.", NamedTextColor.GOLD), value -> {
            List<String> lore = new ArrayList<>(HeadEditMenu.loreLines(plugin, id));
            lore.add(value);
            setLore(player, id, lore, "Lore line added.");
        });
    }

    private void promptLoreSet(@NotNull Player player, @NotNull HeadId id, int line) {
        if (!editable(player, id, Permissions.EDIT_LORE)) {
            return;
        }

        List<String> lore = new ArrayList<>(HeadEditMenu.loreLines(plugin, id));
        if (line < 0 || line >= lore.size()) {
            HeadEditMenu.openLore(plugin, player, id, 0);
            return;
        }

        player.closeInventory();
        plugin.prompts().request(player, Component.text("Enter replacement text for lore line " + (line + 1) + ".", NamedTextColor.GOLD), value -> {
            lore.set(line, value);
            setLore(player, id, lore, "Lore line updated.");
        });
    }

    private void removeLoreLine(@NotNull Player player, @NotNull HeadId id, int line) {
        if (!editable(player, id, Permissions.EDIT_LORE)) {
            return;
        }

        List<String> lore = new ArrayList<>(HeadEditMenu.loreLines(plugin, id));
        if (line < 0 || line >= lore.size()) {
            HeadEditMenu.openLore(plugin, player, id, 0);
            return;
        }

        lore.remove(line);
        setLore(player, id, lore, "Lore line removed.");
    }

    private void setLore(@NotNull Player player, @NotNull HeadId id, @NotNull List<String> lore, @NotNull String message) {
        if (!editable(player, id, Permissions.EDIT_LORE)) {
            return;
        }

        List<String> cleaned = lore.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList();
        if (id.isRemote()) {
            RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
            plugin.headRegistry().overrides().save(override.withLore(cleaned, player.getUniqueId()));
        } else if (id.isCustom()) {
            StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
            plugin.headRegistry().customHeads().save(stored.withLore(cleaned));
        }

        mutated(player, id);
        player.sendMessage(Component.text(message, NamedTextColor.GRAY));
        HeadEditMenu.openLore(plugin, player, id, 0);
    }


    private void resetLore(@NotNull Player player, @NotNull HeadId id) {
        if (!editable(player, id, Permissions.EDIT_LORE)) {
            return;
        }

        if (id.isRemote()) {
            RemoteHeadOverride override = plugin.headRegistry().overrides().find(id).orElse(RemoteHeadOverride.empty(id, player.getUniqueId()));
            plugin.headRegistry().overrides().save(override.withLore(null, player.getUniqueId()));
        } else if (id.isCustom()) {
            StoredCustomHead stored = plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new IllegalArgumentException("Unknown custom head: " + id));
            plugin.headRegistry().customHeads().save(stored.withLore(List.of()));
        }

        mutated(player, id);
        player.sendMessage(Component.text("Lore reset to default.", NamedTextColor.GRAY));
        HeadEditMenu.openLore(plugin, player, id, 0);
    }

    private boolean editable(@NotNull Player player, @NotNull HeadId id, @NotNull String permission) {
        if (!plugin.adminModes().enabled(player)) {
            player.sendMessage(Component.text("Enable Admin Mode to edit heads.", NamedTextColor.RED));
            return false;
        }

        if (id.isPlayer()) {
            noPermission(player);
            return false;
        }

        if (!Permissions.has(player, Permissions.EDIT) && !Permissions.has(player, permission)) {
            noPermission(player);
            return false;
        }

        return true;
    }

    private void mutated(@NotNull Player player, @NotNull HeadId id) {
        plugin.headRegistry().onLocalMutation();
        plugin.clearItemCache();
        plugin.clearSearchCache();
    }

    private void noPermission(@NotNull Player player) {
        player.sendMessage(plugin.messages().render(player, io.github.silentdevelopment.headdb.paper.message.MessageKey.COMMAND_ERROR_NO_PERMISSION));
    }

    private static @NotNull Set<String> toggle(@NotNull Set<String> values, @NotNull String value) {
        LinkedHashSet<String> updated = new LinkedHashSet<>(values);
        if (!updated.remove(value)) {
            updated.add(value);
        }

        return Set.copyOf(updated);
    }

    private static int parseIndex(@NotNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static void deny(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);
    }
}
