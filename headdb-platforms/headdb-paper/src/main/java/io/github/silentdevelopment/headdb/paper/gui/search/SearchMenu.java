package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.GuiKey;
import io.github.silentdevelopment.grafik.paper.gui.dynamic.PaperDynamicGuiConfigurator;
import io.github.silentdevelopment.grafik.paper.gui.dynamic.PaperDynamicGuiDefinition;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.item.HeadItemFactory;
import io.github.silentdevelopment.headdb.paper.search.SearchResultCache;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SearchMenu implements PaperDynamicGuiDefinition<SearchMenuState> {

    private static final GKey<GuiKey> KEY = GKey.of("headdb_search");
    private static final GKey<GuiKey> ADVANCED_KEY = GKey.of("headdb_search_advanced");

    private final GKey<GuiKey> key;
    private final HeadDBPlugin plugin;
    private final HeadItemFactory itemFactory;
    private final SearchResultCache searchResultCache;
    private final GKey<io.github.silentdevelopment.grafik.key.PageKey> initialPage;

    public SearchMenu(@NotNull HeadDBPlugin plugin, @NotNull HeadItemFactory itemFactory, @NotNull SearchResultCache searchResultCache) {
        this(KEY, plugin, itemFactory, searchResultCache, SearchPageFactory.KEY);
    }

    private SearchMenu(@NotNull GKey<GuiKey> key, @NotNull HeadDBPlugin plugin, @NotNull HeadItemFactory itemFactory, @NotNull SearchResultCache searchResultCache, @NotNull GKey<io.github.silentdevelopment.grafik.key.PageKey> initialPage) {
        this.key = Objects.requireNonNull(key, "key");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemFactory = Objects.requireNonNull(itemFactory, "itemFactory");
        this.searchResultCache = Objects.requireNonNull(searchResultCache, "searchResultCache");
        this.initialPage = Objects.requireNonNull(initialPage, "initialPage");
    }

    public static @NotNull SearchMenu advanced(@NotNull HeadDBPlugin plugin, @NotNull HeadItemFactory itemFactory, @NotNull SearchResultCache searchResultCache) {
        return new SearchMenu(ADVANCED_KEY, plugin, itemFactory, searchResultCache, SearchOptionsPageFactory.KEY);
    }

    @Override
    public @NotNull GKey<GuiKey> key() {
        return key;
    }

    @Override
    public void configure(@NotNull PaperDynamicGuiConfigurator<SearchMenuState> gui) {
        gui.initialPage(initialPage);
        gui.page(new SearchPageFactory(plugin, itemFactory, searchResultCache));
        gui.page(new SearchOptionsPageFactory(plugin));
        gui.page(new SearchFilterPageFactory(plugin, SearchFilterPageFactory.Mode.CATEGORY));
        gui.page(new SearchFilterPageFactory(plugin, SearchFilterPageFactory.Mode.TAGS));
        gui.page(new SearchFilterPageFactory(plugin, SearchFilterPageFactory.Mode.COLLECTIONS));
    }
}