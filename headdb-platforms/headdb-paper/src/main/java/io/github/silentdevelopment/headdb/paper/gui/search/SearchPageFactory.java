package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadTag;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.edit.HeadEditDropGuard;
import io.github.silentdevelopment.headdb.paper.gui.favorites.FavoriteClickGuard;
import io.github.silentdevelopment.headdb.paper.item.HeadItemFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.format.TextDecoration;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.search.SearchResultCache;
import io.github.silentdevelopment.headdb.paper.sound.SoundKey;
import io.github.silentdevelopment.headdb.query.HeadQueryResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class SearchPageFactory implements PaperPageFactory<SearchMenuState> {

    public static final GKey<PageKey> KEY = GKey.of("search_results");

    private static final int PAGE_SIZE = 28;
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_SUMMARY = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_SORT_FILTER = 52;
    private static final int SLOT_EMPTY = 22;
    private static final String NO_VISIBLE_CATEGORY = "__headdb_no_visible_category__";
    private static final int[] RESULT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final HeadDBPlugin plugin;
    private final HeadItemFactory itemFactory;
    private final SearchResultCache searchResultCache;

    public SearchPageFactory(@NotNull HeadDBPlugin plugin, @NotNull HeadItemFactory itemFactory, @NotNull SearchResultCache searchResultCache) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.searchResultCache = Objects.requireNonNull(searchResultCache, "searchResultCache");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return KEY;
    }

    @Override
    public @NotNull PaperPage<SearchMenuState> create(@NotNull GuiContext<SearchMenuState> context, @NotNull PaperPageBuilder<SearchMenuState> page) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        SearchRequest request = SearchState.request(context);
        int currentPage = SearchState.page(context);
        Player viewer = player(context);
        HeadQueryResult result = search(viewer, request, currentPage);

        if (result.total() > 0 && currentPage > result.totalPages()) {
            currentPage = result.totalPages();
            SearchState.page(context, currentPage);
            result = search(viewer, request, currentPage);
        }

        page.type(MenuType.GENERIC_9X6);
        page.title(title(viewer, request, result, currentPage));

        Set<Integer> reservedSlots = new HashSet<>();

        renderResults(page, result);
        renderControls(page, context, request, result, currentPage, reservedSlots);

        if (result.heads().isEmpty()) {
            set(page, reservedSlots, SLOT_EMPTY, emptyButton());
        }

        GuiItems.fillBorders(plugin, page, ROWS, reservedSlots);

        return page.build();
    }

    private void renderResults(@NotNull PaperPageBuilder<SearchMenuState> page, @NotNull HeadQueryResult result) {
        int index = 0;

        for (Head head : result.heads()) {
            if (index >= RESULT_SLOTS.length) {
                return;
            }

            int slot = RESULT_SLOTS[index];
            page.set(slot, headElement(slot, head));
            index++;
        }
    }

    private void renderControls(@NotNull PaperPageBuilder<SearchMenuState> page, @NotNull GuiContext<SearchMenuState> context, @NotNull SearchRequest request, @NotNull HeadQueryResult result, int currentPage, @NotNull Set<Integer> reservedSlots) {
        set(page, reservedSlots, SLOT_BACK, backButton());

        if (currentPage > 1) {
            set(page, reservedSlots, SLOT_PREVIOUS, previousButton(currentPage));
        }

        set(page, reservedSlots, SLOT_SUMMARY, summaryButton(request, result, currentPage));

        if (currentPage < result.totalPages()) {
            set(page, reservedSlots, SLOT_NEXT, nextButton(currentPage));
        }

        Player player = player(context);
        if (player != null && Permissions.has(player, Permissions.GUI_FILTER)) {
            set(page, reservedSlots, SLOT_SORT_FILTER, sortFilterButton(request));
        }
    }

    private @NotNull ItemElement<SearchMenuState> sortFilterButton(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        return GuiHeadIcons.<SearchMenuState>button(plugin, "sort_filter", "sort-filter", GuiItems.name("Sort / Filter", NamedTextColor.GOLD), List.of(
                GuiItems.metaDetail("Sort", request.sort() + " " + request.direction()),
                GuiItems.idDetail("IDs", request.ids().size()),
                GuiItems.idDetail("Categories", categoryLabel(request)),
                GuiItems.idDetail("Tags", request.tags().size()),
                GuiItems.idDetail("Collections", request.collections().size()),
                GuiItems.lore("Click to change options.", NamedTextColor.GREEN)
        ), context -> context.openPage(SearchOptionsPageFactory.KEY));
    }

    private @NotNull ItemElement<SearchMenuState> headElement(int slot, @NotNull Head head) {
        Objects.requireNonNull(head, "head");
        return ItemElement.<SearchMenuState>of(GKey.of("head_" + slot), context -> headItem(context, head), context -> giveSelf(context, head));
    }


    private @NotNull ItemStack headItem(@NotNull GuiContext<SearchMenuState> context, @NotNull Head head) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(head, "head");

        ItemStack item = itemFactory.create(head);
        Player player = player(context);
        if (player != null) {
            plugin.favorites().decorate(player.getUniqueId(), head.id(), item);
        }

        if (player == null || !plugin.adminModes().enabled(player)) {
            return item;
        }

        item.editMeta(meta -> {
            List<Component> lore = meta.lore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(meta.lore());
            lore.add(Component.empty());
            lore.add(GuiItems.idDetail("ID", headIdLabel(head.id())));
            lore.add(GuiItems.lore("Press Q to edit.", NamedTextColor.GRAY));
            meta.lore(lore);
        });
        return item;
    }

    private @NotNull ItemElement<SearchMenuState> backButton() {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "back", "back", context -> openBack(context, context.source().resultBackTarget()));
    }

    private void openBack(@NotNull GuiContext<SearchMenuState> context, @NotNull SearchMenuState.BackTarget target) {
        Player player = player(context);
        if (player == null) {
            return;
        }

        switch (target) {
            case BROWSE -> plugin.guis().openBrowse(player);
            case COLLECTIONS -> plugin.guis().openCollections(player);
            case TAGS -> plugin.guis().openTags(player);
            case RESULTS -> context.openPage(SearchPageFactory.KEY);
            case MAIN -> plugin.guis().openMain(player);
        }
    }

    private @NotNull ItemElement<SearchMenuState> previousButton(int currentPage) {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "previous", "previous", context -> {
            SearchState.page(context, currentPage - 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<SearchMenuState> nextButton(int currentPage) {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "next", "next", context -> {
            SearchState.page(context, currentPage + 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<SearchMenuState> summaryButton(@NotNull SearchRequest request, @NotNull HeadQueryResult result, int currentPage) {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "summary", "info", GuiItems.name(summaryTitle(request), NamedTextColor.GOLD), summaryLore(request, result, currentPage), ignored -> {});
    }

    private static @NotNull String summaryTitle(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        if (request.categoryLocked()) {
            return "Category Summary";
        }

        if (request.isEmpty()) {
            return "Browse Summary";
        }

        return "Search Summary";
    }

    private @NotNull ItemElement<SearchMenuState> emptyButton() {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "empty", "empty", GuiItems.name("No Heads Found", NamedTextColor.RED), List.of(GuiItems.lore("Try a different search query.", NamedTextColor.GRAY)), ignored -> {});
    }

    private void giveSelf(@NotNull GuiContext<SearchMenuState> context, @NotNull Head head) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(head, "head");

        Player player = player(context);

        if (player == null) {
            return;
        }

        if (HeadEditDropGuard.consume(player, head.id())) {
            return;
        }

        if (FavoriteClickGuard.consume(player, head.id())) {
            return;
        }

        if (!Permissions.has(player, Permissions.GUI_HEAD_TAKE)) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            return;
        }

        if (!Permissions.canViewCategory(player, head.category())) {
            player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
            plugin.sounds().play(player, SoundKey.NO_PERMISSION);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.messages().giveInventoryFull(player, player));
            plugin.sounds().play(player, SoundKey.INVALID);
            return;
        }

        if (!plugin.economy().charge(player, head, 1)) {
            return;
        }

        ItemStack item;

        try {
            item = itemFactory.create(head);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(plugin.messages().invalidArgument(player, exception.getMessage()));
            plugin.sounds().play(player, SoundKey.INVALID);
            return;
        }

        if (!player.getInventory().addItem(item).isEmpty()) {
            player.sendMessage(plugin.messages().giveInventoryFull(player, player));
            plugin.sounds().play(player, SoundKey.INVALID);
            return;
        }

        player.sendMessage(plugin.messages().giveSuccess(player, head, player));
        plugin.sounds().play(player, SoundKey.TAKE_HEAD);
    }

    private @NotNull HeadQueryResult search(Player player, @NotNull SearchRequest request, int page) {
        Objects.requireNonNull(request, "request");
        boolean includeHidden = player != null && plugin.adminModes().enabled(player);
        SearchRequest scopedRequest = scopedRequest(player, request);
        return searchResultCache.search(plugin.headRegistry(), scopedRequest, page, PAGE_SIZE, includeHidden);
    }

    private @NotNull SearchRequest scopedRequest(Player player, @NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        if (player == null || request.categoryLocked() || Permissions.canViewAllCategories(player)) {
            return request;
        }

        Set<String> visibleCategories = visibleCategories(player);
        if (request.categories().isEmpty()) {
            return request.withCategoryFilters(visibleCategories.isEmpty() ? Set.of(NO_VISIBLE_CATEGORY) : visibleCategories);
        }

        Set<String> scopedCategories = request.categories().stream().filter(category -> visibleCategories.contains(category)).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return request.withCategoryFilters(scopedCategories.isEmpty() ? Set.of(NO_VISIBLE_CATEGORY) : scopedCategories);
    }

    private @NotNull Set<String> visibleCategories(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return plugin.headRegistry().categories().stream().map(HeadCategory::id).filter(category -> Permissions.canViewCategory(player, category)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private @NotNull Component title(Player viewer, @NotNull SearchRequest request, @NotNull HeadQueryResult result, int currentPage) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");

        boolean adminMode = viewer != null && plugin.adminModes().enabled(viewer);
        return GuiTitles.title(resultTitle(request) + " " + currentPage + "/" + Math.max(1, result.totalPages()), adminMode);
    }

    private @NotNull String resultTitle(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        if (request.categoryLocked() && request.category() != null) {
            return categoryName(request.category());
        }

        if (!request.query().isBlank()) {
            return request.query();
        }

        if (request.tags().size() == 1 && request.ids().isEmpty() && request.categories().isEmpty() && request.collections().isEmpty()) {
            return tagName(request.tags().iterator().next());
        }

        if (request.collections().size() == 1 && request.ids().isEmpty() && request.categories().isEmpty() && request.tags().isEmpty()) {
            return collectionName(request.collections().iterator().next());
        }

        if (request.hasFilters()) {
            return "Filtered Heads";
        }

        return "All Heads";
    }

    private @NotNull String categoryName(@NotNull String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");

        return plugin.headRegistry().categories().stream().filter(category -> category.id().equalsIgnoreCase(categoryId)).map(HeadCategory::name).findFirst().orElse(categoryId);
    }

    private @NotNull String collectionName(@NotNull String collectionId) {
        Objects.requireNonNull(collectionId, "collectionId");

        return plugin.headRegistry().collections().stream().filter(collection -> collection.id().equalsIgnoreCase(collectionId)).map(HeadCollection::name).findFirst().orElse(collectionId);
    }

    private @NotNull String tagName(@NotNull String tagId) {
        Objects.requireNonNull(tagId, "tagId");

        return plugin.headRegistry().tags().stream().filter(tag -> tag.id().equalsIgnoreCase(tagId)).map(HeadTag::name).findFirst().orElse(tagId);
    }

    private @NotNull List<Component> summaryLore(@NotNull SearchRequest request, @NotNull HeadQueryResult result, int currentPage) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(result, "result");

        List<Component> lore = new java.util.ArrayList<>();

        if (!request.query().isBlank()) {
            lore.add(GuiItems.metaDetail("Query", request.query()));
        }

        lore.add(GuiItems.idDetail("Results", result.total()));
        lore.add(GuiItems.idDetail("Page", currentPage + " / " + Math.max(1, result.totalPages())));
        lore.add(GuiItems.idDetail("IDs", request.ids().size()));
        lore.add(GuiItems.idDetail("Categories", categoryLabel(request)));
        lore.add(GuiItems.idDetail("Tags", request.tags().size()));
        lore.add(GuiItems.idDetail("Collections", request.collections().size()));
        lore.add(GuiItems.metaDetail("Sort", request.sort() + " " + request.direction()));

        return List.copyOf(lore);
    }

    private static @NotNull String categoryLabel(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");

        if (request.categoryLocked()) {
            return request.category();
        }

        if (request.categories().isEmpty()) {
            return "all";
        }

        return String.valueOf(request.categories().size());
    }


    private static @NotNull String headIdLabel(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        return id.display();
    }

    private static Player player(@NotNull GuiContext<SearchMenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }

    private static void set(@NotNull PaperPageBuilder<SearchMenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<SearchMenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }
}