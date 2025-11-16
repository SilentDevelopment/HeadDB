package com.github.thesilentpro.headdb.core.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thesilentpro.grim.button.Button;
import com.github.thesilentpro.grim.button.SimpleButton;
import com.github.thesilentpro.grim.page.Page;
import com.github.thesilentpro.grim.page.SimplePage;
import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.menu.gui.CustomCategoriesGUI;
import com.github.thesilentpro.headdb.core.menu.gui.FavoritesHeadsGUI;
import com.github.thesilentpro.headdb.core.menu.gui.HeadsGUI;
import com.github.thesilentpro.headdb.core.menu.gui.LocalHeadsGUI;
import com.github.thesilentpro.headdb.core.storage.PlayerData;
import com.github.thesilentpro.headdb.core.util.Compatibility;
import com.github.thesilentpro.inputs.paper.PaperInput;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MainMenu extends SimplePage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainMenu.class);
    private int[] categorySlots;

    public MainMenu(HeadDB plugin) {
        super(
            plugin.getMenuConfig().parseComponent(
                plugin.getMenuConfig().getTitle("main", 
                    plugin.getLocalization().getConsoleMessage("menu.main.name")
                        .orElse(Component.text("HeadDB").color(NamedTextColor.RED)).toString()
                )
            ),
            plugin.getMenuConfig().getSize("main", 6)
        );
        preventInteraction();
        
        // Load category slots from config
        List<Integer> slotsList = plugin.getMenuConfig().getSlots("main", "categorySlots", 
            Arrays.asList(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33));
        this.categorySlots = slotsList.stream().mapToInt(Integer::intValue).toArray();

        plugin.getHeadApi().onReady().thenAccept(heads -> {
            LOGGER.debug("RENDER THREAD = {}", Thread.currentThread().getName());
            renderCategoryButtons(plugin, heads);
            renderLocalButton(plugin);
            renderFavoritesButton(plugin);
            renderCustomCategoriesButton(plugin);
            renderSearchButton(plugin);
            renderInfoButton(plugin);
            fillBorder(plugin, this);
            reRender();
        });
    }

    private void renderCategoryButtons(HeadDB plugin, List<Head> heads) {
        // Get preferred order from config
        YamlConfiguration config = plugin.getMenuConfig().get("main");
        List<String> preferredOrder;
        if (config != null && config.contains("preferredCategoryOrder")) {
            preferredOrder = config.getStringList("preferredCategoryOrder");
        } else {
            preferredOrder = List.of("Alphabet", "Animals", "Blocks", "Decoration", "Food & Drinks", "Humanoid", "Humans", "Miscellaneous", "Monsters", "Plants");
        }

        Map<String, Head> headByCategory = new HashMap<>();
        for (Head head : heads) {
            headByCategory.putIfAbsent(head.getCategory(), head);
        }

        List<String> orderedCategories = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        preferredOrder.stream().filter(headByCategory::containsKey).forEach(cat -> {
            orderedCategories.add(cat);
            seen.add(cat);
        });
        headByCategory.keySet().stream().filter(seen::add).forEach(orderedCategories::add);

        for (int i = 0; i < Math.min(categorySlots.length, orderedCategories.size()); i++) {
            String category = orderedCategories.get(i);
            
            // Check if category button is enabled in config
            String categoryPath = "categoryButtons." + category;
            if (!plugin.getMenuConfig().isButtonEnabled("main", categoryPath)) {
                continue;
            }
            
            // Get slot from config or use default from categorySlots
            int slot = plugin.getMenuConfig().getButtonSlot("main", categoryPath, categorySlots[i]);
            
            // Get texture from config or use head from database
            String texture = plugin.getMenuConfig().getButtonTexture("main", categoryPath);
            Head head = headByCategory.get(category);
            
            // Get name from config or use localization
            Component name = plugin.getMenuConfig().getButtonNameComponent("main", categoryPath,
                plugin.getLocalization().getConsoleMessage("menu.main.category." + category.toLowerCase(Locale.ROOT) + ".name")
                    .orElse(Component.text(category).color(NamedTextColor.GOLD)));
            
            // Get lore from config
            List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", categoryPath);
            Component[] lore = loreList.isEmpty() ? new Component[]{Component.text("")} : loreList.toArray(new Component[0]);
            
            ItemStack item;
            if (!texture.isEmpty()) {
                // Use custom texture from config
                Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", categoryPath, Material.PLAYER_HEAD);
                item = plugin.getHeadApi().findByTexture(texture).join()
                    .map(h -> Compatibility.setItemDetails(h.getItem(), name, lore))
                    .orElse(Compatibility.newItem(fallbackMaterial, name, lore));
            } else if (head != null) {
                // Use head from database
                item = Compatibility.setItemDetails(head.getItem(), name, lore);
            } else {
                continue;
            }

            setButton(slot, new SimpleButton(item, ctx -> {
                Player player = (Player) ctx.event().getWhoClicked();
                if (!player.hasPermission("headdb.category." + category)) {
                    plugin.getLocalization().sendMessage(player, "noPermission");
                    Compatibility.playSound(player, plugin.getSoundConfig().get("noPermission"));
                    return;
                }

                HeadsGUI gui = plugin.getMenuManager().get(category.replace(" ", "_").replace("&", "_"));
                int page = plugin.getCfg().isTrackPage() ? gui.getGuiRegistry().getCurrentPage(player.getUniqueId(), gui.getKey()).orElse(0) : 0;
                gui.open(player, page);
                Compatibility.playSound(player, plugin.getSoundConfig().get("menu.open"));
            }));
        }
    }

    private void renderLocalButton(HeadDB plugin) {
        if (!plugin.getMenuConfig().isButtonEnabled("main", "buttons.local")) {
            return;
        }
        
        int slot = plugin.getMenuConfig().getButtonSlot("main", "buttons.local", 41);
        String texture = plugin.getMenuConfig().getButtonTexture("main", "buttons.local");
        if (texture.isEmpty()) {
            texture = "7f6bf958abd78295eed6ffc293b1aa59526e80f54976829ea068337c2f5e8";
        }
        
        Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", "buttons.local", Material.COMPASS);
        Component name = plugin.getMenuConfig().getButtonNameComponent("main", "buttons.local", 
            getMsg(plugin, "menu.main.local.name", "Local Heads", NamedTextColor.AQUA));
        List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", "buttons.local");
        Component[] lore = loreList.isEmpty() ? new Component[]{Component.text("")} : loreList.toArray(new Component[0]);
        
        ItemStack item = plugin.getHeadApi()
                .findByTexture(texture)
                .join()
                .map(head -> Compatibility.setItemDetails(head.getItem(), name, lore))
                .orElse(Compatibility.newItem(fallbackMaterial, name, lore));

        setButton(slot, new SimpleButton(item, ctx -> {
            Player player = (Player) ctx.event().getWhoClicked();
            if (!player.hasPermission("headdb.category.local")) {
                plugin.getLocalization().sendMessage(player, "noPermission");
                Compatibility.playSound(player, plugin.getSoundConfig().get("noPermission"));
                return;
            }

            List<ItemStack> localHeads = plugin.getHeadApi().computeLocalHeads();
            if (localHeads.isEmpty()) {
                plugin.getLocalization().sendMessage(player, "localNone");
                Compatibility.playSound(player, plugin.getSoundConfig().get("menu.none"));
                return;
            }

            Component localTitle = plugin.getMenuConfig().parseComponent(
                plugin.getMenuConfig().getTitle("local",
                    getMsg(plugin, "menu.local.name", "HeadDB » Local", NamedTextColor.GOLD).toString()
                )
            );
            LocalHeadsGUI gui = new LocalHeadsGUI(plugin, "local_" + player.getUniqueId(), localTitle, localHeads);
            int page = plugin.getCfg().isTrackPage() ? gui.getGuiRegistry().getCurrentPage(player.getUniqueId(), gui.getKey()).orElse(0) : 0;
            gui.open(player, page);
            Compatibility.playSound(player, plugin.getSoundConfig().get("menu.open"));
        }));
    }

    private void renderFavoritesButton(HeadDB plugin) {
        if (!plugin.getMenuConfig().isButtonEnabled("main", "buttons.favorites")) {
            return;
        }

        int slot = plugin.getMenuConfig().getButtonSlot("main", "buttons.favorites", 42);
        String texture = plugin.getMenuConfig().getButtonTexture("main", "buttons.favorites");
        if (texture.isEmpty()) {
            texture = "76fdd4b13d54f6c91dd5fa765ec93dd9458b19f8aa34eeb5c80f455b119f278";
        }

        Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", "buttons.favorites", Material.BOOK);
        Component name = plugin.getMenuConfig().getButtonNameComponent("main", "buttons.favorites",
            getMsg(plugin, "menu.main.favorites.name", "Favorites", NamedTextColor.YELLOW));
        List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", "buttons.favorites");
        Component[] lore = loreList.isEmpty() ? new Component[]{Component.text("")} : loreList.toArray(new Component[0]);

        ItemStack item = plugin.getHeadApi()
                .findByTexture(texture)
                .join()
                .map(head -> Compatibility.setItemDetails(head.getItem(), name, lore))
                .orElse(Compatibility.newItem(fallbackMaterial, name, lore));

        setButton(slot, new SimpleButton(item, ctx -> {
            Player player = (Player) ctx.event().getWhoClicked();
            if (!player.hasPermission("headdb.category.favorites")) {
                plugin.getLocalization().sendMessage(player, "noPermission");
                Compatibility.playSound(player, plugin.getSoundConfig().get("noPermission"));
                return;
            }

            UUID playerId = player.getUniqueId();
            PlayerData data = plugin.getPlayerStorage().getPlayer(playerId);
            List<CompletableFuture<Optional<Head>>> futures = data.getFavorites().stream()
                    .map(plugin.getHeadApi()::findById)
                    .toList();

            List<ItemStack> localItems = data.getLocalFavorites().stream()
                    .map(plugin.getHeadApi()::computeLocalHead)
                    .filter(Optional::isPresent).map(Optional::get).toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream().map(CompletableFuture::join)
                            .filter(Optional::isPresent).map(Optional::get).toList())
                    .thenAcceptAsync(favoriteHeads -> {
                        if (favoriteHeads.isEmpty() && localItems.isEmpty()) {
                            plugin.getLocalization().sendMessage(player, "favoritesNone");
                            Compatibility.playSound(player, plugin.getSoundConfig().get("menu.none"));
                            return;
                        }
                        Component favTitle = plugin.getMenuConfig().parseComponent(
                            plugin.getMenuConfig().getTitle("favorites",
                                getMsg(plugin, "menu.favorites.name", "HeadDB » Favorites", NamedTextColor.GOLD).toString()
                            )
                        );
                        FavoritesHeadsGUI gui = new FavoritesHeadsGUI(plugin, "favorites_" + playerId, favTitle, favoriteHeads, localItems);
                        int page = plugin.getCfg().isTrackPage() ? gui.getGuiRegistry().getCurrentPage(playerId, gui.getKey()).orElse(0) : 0;
                        gui.open(player, page);
                        Compatibility.playSound(player, plugin.getSoundConfig().get("menu.open"));
                    }, Compatibility.getMainThreadExecutor(plugin))
                    .exceptionally(ex -> {
                        LOGGER.error("Failed to compute favorite heads for: {}", playerId, ex);
                        return null;
                    });
        }));
    }

    private void renderCustomCategoriesButton(HeadDB plugin) {
        if (!plugin.getMenuConfig().isButtonEnabled("main", "buttons.customCategories")) {
            return;
        }

        int slot = plugin.getMenuConfig().getButtonSlot("main", "buttons.customCategories", 38);
        String texture = plugin.getMenuConfig().getButtonTexture("main", "buttons.customCategories");
        if (texture.isEmpty()) {
            texture = "e7bc251a6cb0d6d9f05c5711911a6ec24b209dbe64267901a4b03761debcf738";
        }

        Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", "buttons.customCategories", Material.NETHER_STAR);
        Component name = plugin.getMenuConfig().getButtonNameComponent("main", "buttons.customCategories",
            getMsg(plugin, "menu.main.customCategories.name", "More Categories", NamedTextColor.DARK_PURPLE));
        List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", "buttons.customCategories");
        Component[] lore = loreList.isEmpty() ? new Component[]{Component.text("")} : loreList.toArray(new Component[0]);

        ItemStack item = plugin.getHeadApi().findByTexture(texture).join()
                .map(head -> Compatibility.setItemDetails(head.getItem(), name, lore))
                .orElse(Compatibility.newItem(fallbackMaterial, name, lore));

        setButton(slot, new SimpleButton(item, ctx -> {
            Player player = (Player) ctx.event().getWhoClicked();
            if (!player.hasPermission("headdb.category.custom")) {
                plugin.getLocalization().sendMessage(player, "noPermission");
                Compatibility.playSound(player, plugin.getSoundConfig().get("noPermission"));
                return;
            }

            CustomCategoriesGUI gui = plugin.getMenuManager().getCustomCategoriesGui();
            if (gui.getPages().isEmpty()) {
                plugin.getLocalization().sendMessage(player, "customCategoriesNone");
                Compatibility.playSound(player, plugin.getSoundConfig().get("menu.none"));
                return;
            }

            int page = plugin.getCfg().isTrackPage() ?
                    gui.getGuiRegistry().getCurrentPage(player.getUniqueId(), gui.getKey()).orElse(0) : 0;
            gui.open(player, page);
            Compatibility.playSound(player, plugin.getSoundConfig().get("menu.open"));
        }));
    }

    private void renderSearchButton(HeadDB plugin) {
        if (!plugin.getMenuConfig().isButtonEnabled("main", "buttons.search")) {
            return;
        }

        int slot = plugin.getMenuConfig().getButtonSlot("main", "buttons.search", 39);
        String texture = plugin.getMenuConfig().getButtonTexture("main", "buttons.search");
        if (texture.isEmpty()) {
            texture = "9d9cc58ad25a1ab16d36bb5d6d493c8f5898c2bf302b64e325921c41c35867";
        }

        Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", "buttons.search", Material.COMPASS);
        Component name = plugin.getMenuConfig().getButtonNameComponent("main", "buttons.search",
            getMsg(plugin, "menu.main.search.name", "Search", NamedTextColor.GREEN));
        List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", "buttons.search");
        Component[] lore = loreList.isEmpty() ? new Component[]{Component.text("")} : loreList.toArray(new Component[0]);

        ItemStack item = plugin.getHeadApi().findByTexture(texture).join()
                .map(head -> Compatibility.setItemDetails(head.getItem(), name, lore))
                .orElse(Compatibility.newItem(fallbackMaterial, name, lore));

        setButton(slot, new SimpleButton(item, ctx -> {
            if (!ctx.event().getWhoClicked().hasPermission("headdb.command.search")) {
                plugin.getLocalization().sendMessage(ctx.event().getWhoClicked(), "noPermission");
                Compatibility.playSound(ctx.event().getWhoClicked(), plugin.getSoundConfig().get("noPermission"));
                return;
            }
            if (!Compatibility.IS_PAPER) {
                return;
            }

            Player player = (Player) ctx.event().getWhoClicked();
            player.closeInventory();
            plugin.getLocalization().sendMessage(player, "command.search.input");
            Compatibility.playSound(player, plugin.getSoundConfig().get("input.wait"));
            PaperInput.awaitString().then((input, event) -> {
                event.setCancelled(true);
                Compatibility.getMainThreadExecutor(plugin).execute(() -> player.performCommand("hdb search " + input));
            }).register(player.getUniqueId());
        }));
    }

    private void renderInfoButton(HeadDB plugin) {
        if (!plugin.getMenuConfig().isButtonEnabled("main", "buttons.info")) {
            return;
        }

        int slot = plugin.getMenuConfig().getButtonSlot("main", "buttons.info", 53);
        String texture = plugin.getMenuConfig().getButtonTexture("main", "buttons.info");
        if (texture.isEmpty()) {
            texture = "16439d2e306b225516aa9a6d007a7e75edd2d5015d113b42f44be62a517e574f";
        }
        
        Material fallbackMaterial = plugin.getMenuConfig().getButtonMaterial("main", "buttons.info", Material.BOOK);
        Component name = plugin.getMenuConfig().getButtonNameComponent("main", "buttons.info", 
            Component.text("Can't find the head you're looking for?").color(NamedTextColor.RED));
        List<Component> loreList = plugin.getMenuConfig().getButtonLoreComponents("main", "buttons.info");
        Component[] lore = loreList.toArray(new Component[0]);

        ItemStack item = plugin.getHeadApi()
                .findByTexture(texture)
                .join()
                .map(head -> Compatibility.setItemDetails(head.getItem(), name, lore))
                .orElse(Compatibility.newItem(fallbackMaterial, name, lore));

        setButton(slot, new SimpleButton(item, ctx -> Compatibility.sendMessage(ctx.event().getWhoClicked(), Component.text("Click to join: https://discord.gg/RJsVvVd").color(NamedTextColor.AQUA))));
    }

    private void fillBorder(HeadDB plugin, Page page) {
        if (!plugin.getMenuConfig().isBorderEnabled("main")) {
            return;
        }

        Material borderMaterial = plugin.getMenuConfig().getBorderMaterial("main", Material.BLACK_STAINED_GLASS_PANE);
        String borderName = plugin.getMenuConfig().getBorderName("main");
        Component name = borderName.isEmpty() ? Component.text("") : plugin.getMenuConfig().parseComponent(borderName);

        Button filler = new SimpleButton(Compatibility.newItem(borderMaterial, name));
        for (int i = 0; i < page.getSize(); i++) {
            if (page.getButton(i).isEmpty()) {
                page.setButton(i, filler);
            }
        }
    }

    private Component getMsg(HeadDB plugin, String key, String fallback, NamedTextColor color) {
        return plugin.getLocalization().getConsoleMessage(key).orElse(Component.text(fallback).color(color));
    }
}