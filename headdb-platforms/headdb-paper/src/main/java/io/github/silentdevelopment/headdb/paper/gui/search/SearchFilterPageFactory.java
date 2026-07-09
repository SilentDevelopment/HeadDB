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
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class SearchFilterPageFactory implements PaperPageFactory<SearchMenuState> {

    public static final GKey<PageKey> CATEGORY_KEY = GKey.of("search_filter_category");
    public static final GKey<PageKey> TAGS_KEY = GKey.of("search_filter_tags");
    public static final GKey<PageKey> COLLECTIONS_KEY = GKey.of("search_filter_collections");

    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_CLEAR = 53;

    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final HeadDBPlugin plugin;
    private final Mode mode;

    public SearchFilterPageFactory(@NotNull HeadDBPlugin plugin, @NotNull Mode mode) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return mode.key();
    }

    @Override
    public @NotNull PaperPage<SearchMenuState> create(@NotNull GuiContext<SearchMenuState> context, @NotNull PaperPageBuilder<SearchMenuState> page) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        page.type(MenuType.GENERIC_9X6);
        Set<Integer> reservedSlots = new HashSet<>();
        Player player = player(context);
        page.title(GuiTitles.title(mode.title(), player != null && plugin.adminModes().enabled(player)));

        set(page, reservedSlots, SLOT_BACK, backButton());

        if (player == null || !Permissions.has(player, Permissions.GUI_FILTER) || !Permissions.has(player, mode.permission())) {
            set(page, reservedSlots, 22, deniedButton());
            GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
            return page.build();
        }

        SearchRequest request = SearchState.request(context);

        if (mode == Mode.CATEGORY && request.categoryLocked()) {
            set(page, reservedSlots, 22, lockedCategoryButton(request));
            GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
            return page.build();
        }

        List<FilterEntry> entries = entries(player);
        int pageIndex = pageIndex(context);
        int fromIndex = pageIndex * ENTRY_SLOTS.length;

        if (fromIndex >= entries.size() && pageIndex > 0) {
            pageIndex = Math.max(0, pageCount(entries.size()) - 1);
            setPageIndex(context, pageIndex);
            fromIndex = pageIndex * ENTRY_SLOTS.length;
        }

        int toIndex = Math.min(entries.size(), fromIndex + ENTRY_SLOTS.length);
        int slotIndex = 0;

        for (int index = fromIndex; index < toIndex; index++) {
            FilterEntry entry = entries.get(index);
            int slot = ENTRY_SLOTS[slotIndex];

            set(page, reservedSlots, slot, entryButton(entry, isSelected(request, entry)));
            slotIndex++;
        }

        renderControls(context, page, reservedSlots, request, entries.size(), pageIndex);

        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);

        return page.build();
    }

    private void renderControls(
            @NotNull GuiContext<SearchMenuState> context,
            @NotNull PaperPageBuilder<SearchMenuState> page,
            @NotNull Set<Integer> reservedSlots,
            @NotNull SearchRequest request,
            int entries,
            int currentPage
    ) {
        int pages = pageCount(entries);

        if (currentPage > 0) {
            set(page, reservedSlots, SLOT_PREVIOUS, previousButton(currentPage));
        }

        set(page, reservedSlots, SLOT_INFO, infoButton(request, entries, currentPage, pages));

        if (currentPage + 1 < pages) {
            set(page, reservedSlots, SLOT_NEXT, nextButton(currentPage));
        }

        if (hasSelectedFilter(request)) {
            set(page, reservedSlots, SLOT_CLEAR, clearButton());
        }
    }

    private @NotNull ItemElement<SearchMenuState> entryButton(@NotNull FilterEntry entry, boolean selected) {
        String iconKey = selected ? "filter-selected" : "filter-unselected";

        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "filter_" + mode.name().toLowerCase(Locale.ROOT) + "_" + keyPart(entry.id()),
                iconKey,
                GuiItems.name(entry.name(), selected ? NamedTextColor.GREEN : NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("ID", entry.id()),
                        GuiItems.lore(selected ? "Selected. Click to remove." : "Click to select.", selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
                ),
                context -> {
                    Player player = player(context);

                    if (player == null) {
                        return;
                    }

                    if (mode == Mode.CATEGORY && !Permissions.canViewCategory(player, entry.id())) {
                        player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                        return;
                    }

                    SearchState.request(context, toggle(SearchState.request(context), entry.id()));
                    context.refresh();
                }
        );
    }

    private @NotNull SearchRequest toggle(@NotNull SearchRequest request, @NotNull String id) {
        return switch (mode) {
            case CATEGORY -> request.toggleCategoryFilter(id);
            case TAGS -> request.toggleTag(id);
            case COLLECTIONS -> request.toggleCollection(id);
        };
    }

    private boolean isSelected(@NotNull SearchRequest request, @NotNull FilterEntry entry) {
        return switch (mode) {
            case CATEGORY -> request.categories().contains(entry.id());
            case TAGS -> request.tags().contains(entry.id());
            case COLLECTIONS -> request.collections().contains(entry.id());
        };
    }

    private boolean hasSelectedFilter(@NotNull SearchRequest request) {
        return switch (mode) {
            case CATEGORY -> !request.categories().isEmpty() && !request.categoryLocked();
            case TAGS -> !request.tags().isEmpty();
            case COLLECTIONS -> !request.collections().isEmpty();
        };
    }

    private @NotNull ItemElement<SearchMenuState> clearButton() {
        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "clear_" + mode.name().toLowerCase(Locale.ROOT),
                "clear-filters",
                GuiItems.name("Clear " + mode.label(), NamedTextColor.RED),
                List.of(GuiItems.lore("Remove selected " + mode.label().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GRAY)),
                context -> {
                    SearchRequest request = SearchState.request(context);
                    SearchRequest updated = switch (mode) {
                        case CATEGORY -> request.withCategoryFilters(Set.of());
                        case TAGS -> request.withTags(Set.of());
                        case COLLECTIONS -> request.withCollections(Set.of());
                    };

                    SearchState.request(context, updated);
                    context.refresh();
                }
        );
    }

    private @NotNull ItemElement<SearchMenuState> infoButton(@NotNull SearchRequest request, int entries, int pageIndex, int pages) {
        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                mode.name().toLowerCase(Locale.ROOT) + "_info",
                "info",
                GuiItems.name(mode.title(), NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("Entries", entries),
                        GuiItems.idDetail("Selected", selectedCount(request)),
                        GuiItems.idDetail("Page", (pageIndex + 1) + " / " + Math.max(1, pages))
                ),
                ignored -> {}
        );
    }

    private int selectedCount(@NotNull SearchRequest request) {
        return switch (mode) {
            case CATEGORY -> request.categoryLocked() ? 0 : request.categories().size();
            case TAGS -> request.tags().size();
            case COLLECTIONS -> request.collections().size();
        };
    }

    private @NotNull ItemElement<SearchMenuState> lockedCategoryButton(@NotNull SearchRequest request) {
        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "locked_category_filter",
                "filter-category",
                GuiItems.name("Category Scope", NamedTextColor.GOLD),
                List.of(
                        GuiItems.idDetail("Current", request.category()),
                        GuiItems.lore("Use tag or collection filters inside this category.", NamedTextColor.DARK_GRAY)
                ),
                ignored -> {}
        );
    }

    private @NotNull ItemElement<SearchMenuState> deniedButton() {
        return GuiHeadIcons.<SearchMenuState>button(
                plugin,
                "filter_denied",
                "no-permission",
                GuiItems.name("No Permission", NamedTextColor.RED),
                List.of(GuiItems.lore("You cannot change search filters.", NamedTextColor.GRAY)),
                ignored -> {}
        );
    }

    private @NotNull ItemElement<SearchMenuState> backButton() {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "back", "back", context -> context.openPage(SearchPageFactory.KEY));
    }

    private @NotNull ItemElement<SearchMenuState> previousButton(int currentPage) {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "previous_" + mode.name().toLowerCase(Locale.ROOT), "previous", context -> {
            setPageIndex(context, currentPage - 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<SearchMenuState> nextButton(int currentPage) {
        return GuiHeadIcons.<SearchMenuState>button(plugin, "next_" + mode.name().toLowerCase(Locale.ROOT), "next", context -> {
            setPageIndex(context, currentPage + 1);
            context.refresh();
        });
    }

    private @NotNull List<FilterEntry> entries(@NotNull Player player) {
        if (mode == Mode.CATEGORY) {
            return plugin.headRegistry().categories().stream()
                    .filter(category -> Permissions.canViewCategory(player, category.id()))
                    .sorted(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER))
                    .map(category -> new FilterEntry(category.id(), category.name()))
                    .toList();
        }

        if (mode == Mode.TAGS) {
            Set<String> visibleTags = visibleTagIds(player);
            return plugin.headRegistry().tags().stream()
                    .filter(tag -> plugin.adminModes().enabled(player) || visibleTags.contains(tag.id()))
                    .sorted(Comparator.comparing(HeadTag::name, String.CASE_INSENSITIVE_ORDER))
                    .map(tag -> new FilterEntry(tag.id(), tag.name()))
                    .toList();
        }

        Set<String> visibleCollections = visibleCollectionIds(player);
        return plugin.headRegistry().collections().stream()
                .filter(collection -> plugin.adminModes().enabled(player) || visibleCollections.contains(collection.id()))
                .sorted(Comparator.comparing(HeadCollection::name, String.CASE_INSENSITIVE_ORDER))
                .map(collection -> new FilterEntry(collection.id(), collection.name()))
                .toList();
    }


    private @NotNull Set<String> visibleTagIds(@NotNull Player player) {
        Set<String> tags = new HashSet<>();
        for (Head head : visibleHeads(player)) {
            tags.addAll(head.tags());
        }
        return Set.copyOf(tags);
    }

    private @NotNull Set<String> visibleCollectionIds(@NotNull Player player) {
        Set<String> collections = new HashSet<>();
        for (Head head : visibleHeads(player)) {
            collections.addAll(head.collections());
        }
        return Set.copyOf(collections);
    }

    private @NotNull List<Head> visibleHeads(@NotNull Player player) {
        boolean includeHidden = plugin.adminModes().enabled(player);
        return plugin.headRegistry().heads(includeHidden).stream().filter(head -> Permissions.canViewCategory(player, head.category())).toList();
    }

    private int pageIndex(@NotNull GuiContext<SearchMenuState> context) {
        Integer value = context.state().get(mode.stateKey(), Integer.class);

        if (value == null || value < 0) {
            return 0;
        }

        return value;
    }

    private void setPageIndex(@NotNull GuiContext<SearchMenuState> context, int page) {
        context.state().put(mode.stateKey(), Math.max(0, page));
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) entries / (double) ENTRY_SLOTS.length);
    }

    private static Player player(@NotNull GuiContext<SearchMenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }

    private static @NotNull String keyPart(@NotNull String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private static void set(@NotNull PaperPageBuilder<SearchMenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<SearchMenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }

    private record FilterEntry(@NotNull String id, @NotNull String name) {}

    public enum Mode {

        CATEGORY(CATEGORY_KEY, "Category Filters", "Categories", "headdb.search.filter.category-page", Permissions.GUI_FILTER_CATEGORIES),
        TAGS(TAGS_KEY, "Tag Filters", "Tags", "headdb.search.filter.tags-page", Permissions.GUI_FILTER_TAGS),
        COLLECTIONS(COLLECTIONS_KEY, "Collection Filters", "Collections", "headdb.search.filter.collections-page", Permissions.GUI_FILTER_COLLECTIONS);

        private final GKey<PageKey> key;
        private final String title;
        private final String label;
        private final String stateKey;
        private final String permission;

        Mode(@NotNull GKey<PageKey> key, @NotNull String title, @NotNull String label, @NotNull String stateKey, @NotNull String permission) {
            this.key = key;
            this.title = title;
            this.label = label;
            this.stateKey = stateKey;
            this.permission = permission;
        }

        public @NotNull GKey<PageKey> key() {
            return key;
        }

        public @NotNull String title() {
            return title;
        }

        public @NotNull String label() {
            return label;
        }

        public @NotNull String stateKey() {
            return stateKey;
        }

        public @NotNull String permission() {
            return permission;
        }
    }
}