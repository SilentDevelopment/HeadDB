package com.github.thesilentpro.headdb.core.config;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thesilentpro.headdb.core.HeadDB;

public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    private final Config config;
    private final SoundConfig soundConfig;
    private final MenuConfig menuConfig;

    public ConfigManager(HeadDB plugin) {
        this.config = new Config(plugin);
        this.soundConfig = new SoundConfig();
        this.menuConfig = new MenuConfig(plugin.getDataFolder());
    }

    public void loadAll(JavaPlugin plugin) {
        LOGGER.debug("Loading configurations...");
        long configStart = System.currentTimeMillis();
        config.load();
        LOGGER.debug("Loaded config.yml in {}ms", System.currentTimeMillis() - configStart);

        long soundsStart = System.currentTimeMillis();
        File soundFile = new File(plugin.getDataFolder(), "sounds.yml");
        if (!soundFile.exists()) {
            plugin.saveResource("sounds.yml", false);
        }

        soundConfig.load(YamlConfiguration.loadConfiguration(soundFile));
        LOGGER.debug("Loaded sounds.yml in {}ms", System.currentTimeMillis() - soundsStart);

        // Load menu configs
        long menusStart = System.currentTimeMillis();
        loadMenuConfigs(plugin);
        LOGGER.debug("Loaded menu configs in {}ms", System.currentTimeMillis() - menusStart);
    }

    private void loadMenuConfigs(JavaPlugin plugin) {
        String[] menuFiles = {"main", "category", "favorites", "local", "custom_categories", "purchase", "search"};
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        
        for (String menuFile : menuFiles) {
            File file = new File(menusFolder, menuFile + ".yml");
            if (!file.exists()) {
                plugin.saveResource("menus/" + menuFile + ".yml", false);
            }
            menuConfig.load(menuFile);
        }
    }

    public Config getConfig() {
        return config;
    }

    public SoundConfig getSoundConfig() {
        return soundConfig;
    }

    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

}
