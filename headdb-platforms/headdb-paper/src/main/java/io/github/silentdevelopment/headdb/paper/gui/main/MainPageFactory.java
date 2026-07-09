package io.github.silentdevelopment.headdb.paper.gui.main;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.settings.SettingsPageFactory;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class MainPageFactory implements PaperPageFactory<MenuState> {

    public static final GKey<PageKey> KEY = GKey.of("main");

    private static final int ROWS = 6;
    private static final int SLOT_CLOSE = 45;

    private final HeadDBPlugin plugin;

    public MainPageFactory(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return KEY;
    }

    @Override
    public @NotNull PaperPage<MenuState> create(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        Player player = player(context);
        boolean adminMode = player != null && plugin.adminModes().enabled(player);
        Set<Integer> reservedSlots = new HashSet<>();

        page.type(MenuType.GENERIC_9X6);
        page.title(GuiTitles.title(plugin.guiConfig().text("title.main", "HeadDB"), adminMode));
        renderControls(context, page, reservedSlots, adminMode);
        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
        return page.build();
    }

    private void renderControls(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, boolean adminMode) {
        Player player = player(context);
        if (player == null) {
            return;
        }

        set(page, reservedSlots, SLOT_CLOSE, closeButton());

        if (Permissions.has(player, Permissions.GUI_PLAYER_HEADS)) {
            set(page, reservedSlots, slot("main.player-heads", 11), playerHeadsButton());
        }

        if (Permissions.has(player, Permissions.GUI_BROWSE_MENU)) {
            set(page, reservedSlots, slot("main.browse", 13), browseButton());
        }

        if (Permissions.has(player, Permissions.GUI_FAVORITES)) {
            set(page, reservedSlots, slot("main.favorites", 15), favoritesButton());
        }

        if (Permissions.has(player, Permissions.GUI_SEARCH)) {
            set(page, reservedSlots, slot("main.search", 31), searchButton());
        }

        set(page, reservedSlots, slot("main.info", 49), infoButton(visibleCategories(player), adminMode));

        if (Permissions.has(player, Permissions.GUI_SETTINGS)) {
            set(page, reservedSlots, slot("main.settings", 53), settingsButton());
        }
    }

    private @NotNull ItemElement<MenuState> closeButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "close", "close", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            player.closeInventory();
        });
    }

    private @NotNull ItemElement<MenuState> infoButton(int visibleCategories, boolean adminMode) {
        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats stats = status.stats();
        List<Component> lore = new ArrayList<>();
        lore.add(GuiItems.idDetail("Heads", stats.heads()));
        lore.add(GuiItems.idDetail("Categories", stats.categories()));
        lore.add(GuiItems.idDetail("Tags", stats.tags()));
        lore.add(GuiItems.idDetail("Collections", stats.collections()));

        if (adminMode) {
            lore.add(Component.empty());
            lore.add(GuiItems.metaDetail("State", status.state()));
            lore.add(GuiItems.metaDetail("Source", status.source()));
            lore.add(GuiItems.idDetail("Visible categories", visibleCategories));
            lore.add(GuiItems.idDetail("Hidden heads", plugin.headRegistry().hiddenHeads().size()));
        }

        return GuiHeadIcons.<MenuState>button(plugin, "main_info", "info", GuiItems.mini(plugin.guiConfig().icon("info").name()), lore, ignored -> {});
    }

    private @NotNull ItemElement<MenuState> searchButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "main_search", "search", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SEARCH) || !Permissions.has(player, Permissions.SEARCH)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            player.closeInventory();
            plugin.prompts().request(player, Component.text("Enter a search query.", NamedTextColor.GOLD), query -> {
                sendSearchQueryInfo(player, query);
                plugin.guis().openSearch(player, new SearchRequest(query, Set.of(), Set.of(), Set.of(), Set.of(), HeadSort.RELEVANCE, SortDirection.DESCENDING, 1, 28, false));
            }, () -> sendSearchCancelled(player));
        });
    }

    private @NotNull ItemElement<MenuState> playerHeadsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "player_heads", plugin.guiConfig().icon("player-heads"), context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            plugin.guis().openPlayerHeads(player);
        }, context -> mainButtonName("player-heads"), context -> mainButtonLore(context, "player-heads", plugin.headRegistry().playerHeads().knownPlayers().size()));
    }

    private @NotNull ItemElement<MenuState> favoritesButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "favorites", "favorites", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            plugin.guis().openFavorites(player);
        });
    }

    private @NotNull ItemElement<MenuState> browseButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "browse", plugin.guiConfig().icon("browse"), context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_BROWSE_MENU)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.guis().openBrowse(player);
        }, context -> mainButtonName("browse"), context -> mainButtonLore(context, "browse", visibleCategories(context)));
    }

    private @NotNull ItemElement<MenuState> settingsButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "settings", "settings", context -> {
            Player player = player(context);
            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SETTINGS)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            context.openPage(SettingsPageFactory.KEY);
        });
    }

    private @NotNull Component mainButtonName(@NotNull String iconKey) {
        return GuiItems.mini(plugin.guiConfig().icon(iconKey).name());
    }

    private @NotNull List<Component> mainButtonLore(@NotNull GuiContext<MenuState> context, @NotNull String iconKey, int heads) {
        List<Component> lore = new ArrayList<>(GuiItems.miniLore(plugin.guiConfig().icon(iconKey).lore()));
        if (adminMode(context)) {
            lore.add(Component.empty());
            lore.add(GuiItems.idDetail("Heads", heads));
        }
        return List.copyOf(lore);
    }

    private boolean adminMode(@NotNull GuiContext<MenuState> context) {
        Player player = player(context);
        return player != null && plugin.adminModes().enabled(player);
    }

    private int visibleCategories(@NotNull GuiContext<MenuState> context) {
        Player player = player(context);
        if (player == null) {
            return 0;
        }

        return visibleCategories(player);
    }

    private int visibleCategories(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return (int) plugin.headRegistry().categories().stream().filter(category -> Permissions.canViewCategory(player, category.id())).count();
    }

    private int slot(@NotNull String key, int fallback) {
        return plugin.guiConfig().slot(key, fallback);
    }

    private static Player player(@NotNull GuiContext<MenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }

    private static void sendSearchQueryInfo(@NotNull Player player, @NotNull String query) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(query, "query");
        player.sendMessage(Component.text("Search Info", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Query: ", NamedTextColor.GRAY).append(Component.text(query, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Sort: ", NamedTextColor.GRAY).append(Component.text("relevance descending", NamedTextColor.AQUA)));
    }

    private static void sendSearchCancelled(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        player.sendMessage(Component.text("Search cancelled.", NamedTextColor.GRAY));
    }

    private static void set(@NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<MenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }
}
