package com.github.thesilentpro.headdb.core.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.config.CustomCategory;
import com.github.thesilentpro.headdb.core.menu.gui.CustomCategoriesGUI;
import com.github.thesilentpro.headdb.core.menu.gui.HeadsGUI;
import com.github.thesilentpro.headdb.core.util.Compatibility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MenuManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuManager.class);
    private final MainMenu mainMenu;
    private CustomCategoriesGUI customCategoriesGui;
    private final Map<String, HeadsGUI> guis = new HashMap<>();

    public MenuManager(HeadDB plugin) {
        this.mainMenu = new MainMenu(plugin);
        this.customCategoriesGui = null;
    }

    public void registerDefaults(HeadDB plugin) {
        for (String knownCategory : plugin.getHeadApi().findKnownCategories()) {
            plugin.getHeadApi().findByCategory(knownCategory).thenAcceptAsync(heads -> {
                try {
                    String key = knownCategory.replace(" ", "_").replace("&", "_");
                    
                    // Try to get title from categoryTitles first
                    String titleStr = plugin.getMenuConfig().getButtonName("category", "categoryTitles." + knownCategory, null);
                    Component title;
                    
                    if (titleStr != null) {
                        // Use specific category title
                        title = plugin.getMenuConfig().parseComponent(titleStr);
                    } else {
                        // Use template with {category} placeholder
                        titleStr = plugin.getMenuConfig().getTitle("category", "<red>HeadDB <gray>» <gold>{category}");
                        
                        if (titleStr.contains("{category}")) {
                            // Get localized category name component
                            Component categoryNameComponent = plugin.getMenuConfig().getButtonNameComponent("main", "categoryButtons." + knownCategory, 
                                plugin.getLocalization().getConsoleMessage("menu.main.category." + knownCategory.toLowerCase(Locale.ROOT) + ".name")
                                    .orElse(Component.text(knownCategory))
                            );
                            
                            // Parse title template and replace {category} with the component
                            Component titleTemplate = plugin.getMenuConfig().parseComponent(titleStr.replace("{category}", "CATEGORY_PLACEHOLDER"));
                            title = titleTemplate.replaceText(builder -> builder.matchLiteral("CATEGORY_PLACEHOLDER").replacement(categoryNameComponent));
                        } else {
                            title = plugin.getMenuConfig().parseComponent(titleStr);
                        }
                    }
                    register(key, new HeadsGUI(plugin, key, title, heads));
                } catch (Throwable ex) {
                    LOGGER.error("Failed to register known category: {}", knownCategory, ex);
                }
            }, Compatibility.getMainThreadExecutor(plugin));
        }

        // Load custom categories
        List<CustomCategory> customCategories = plugin.getCfg().resolveCustomCategories().stream()
                .peek(category -> {
                    if (!category.isEnabled()) {
                        LOGGER.debug("Skipping disabled custom category: {}", category.getIdentifier());
                    }
                })
                .filter(CustomCategory::isEnabled)
                .peek(category -> {
                    String titleStr = plugin.getMenuConfig().getTitle("category",
                        plugin.getLocalization().getConsoleMessage("menu.category." + category.getIdentifier())
                            .orElseGet(() -> MiniMessage.miniMessage().deserialize("<red>HeadDB <gray>» " + category.getName())).toString()
                    );
                    // Replace {category} placeholder with actual category name
                    titleStr = titleStr.replace("{category}", category.getName());
                    Component title = plugin.getMenuConfig().parseComponent(titleStr);
                    register(category.getIdentifier(), new HeadsGUI(plugin, "custom_" + category.getIdentifier(), title, category.getHeads()));
                })
                .collect(Collectors.toList());

        Component customCatTitle = plugin.getMenuConfig().parseComponent(
            plugin.getMenuConfig().getTitle("custom_categories",
                plugin.getLocalization().getConsoleMessage("menu.customCategories.name")
                    .orElseGet(() -> Component.text("HeadDB » More Categories").color(NamedTextColor.GOLD)).toString()
            )
        );
        customCategoriesGui = new CustomCategoriesGUI(plugin, "custom_categories", customCatTitle, customCategories);
    }

    public void register(String key, HeadsGUI menu) {
        this.guis.put(key, menu);
    }

    public HeadsGUI get(String key) {
        return this.guis.get(key);
    }

    public CustomCategoriesGUI getCustomCategoriesGui() {
        return customCategoriesGui;
    }

    public MainMenu getMainMenu() {
        return this.mainMenu;
    }

}
