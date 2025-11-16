package com.github.thesilentpro.headdb.core.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MenuConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuConfig.class);
    
    private final Map<String, YamlConfiguration> configs = new HashMap<>();
    private final File menusFolder;

    public MenuConfig(File dataFolder) {
        this.menusFolder = new File(dataFolder, "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }
    }

    public void load(String menuName) {
        File file = new File(menusFolder, menuName + ".yml");
        if (file.exists()) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                configs.put(menuName, config);
                LOGGER.debug("Loaded menu config: {}", menuName);
            } catch (Exception e) {
                LOGGER.error("Failed to load menu config: {}", menuName, e);
            }
        }
    }

    public YamlConfiguration get(String menuName) {
        return configs.get(menuName);
    }

    public String getTitle(String menuName, String defaultTitle) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains("title")) {
            return config.getString("title", defaultTitle);
        }
        return defaultTitle;
    }

    public int getSize(String menuName, int defaultSize) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains("size")) {
            return config.getInt("size", defaultSize);
        }
        return defaultSize;
    }

    public List<Integer> getSlots(String menuName, String path, List<Integer> defaultSlots) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(path)) {
            return config.getIntegerList(path);
        }
        return defaultSlots;
    }

    public boolean isButtonEnabled(String menuName, String buttonPath) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null) {
            return config.getBoolean(buttonPath + ".enabled", true);
        }
        return true;
    }

    public int getButtonSlot(String menuName, String buttonPath, int defaultSlot) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(buttonPath + ".slot")) {
            return config.getInt(buttonPath + ".slot", defaultSlot);
        }
        return defaultSlot;
    }

    public String getButtonName(String menuName, String buttonPath, String defaultName) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(buttonPath + ".name")) {
            return config.getString(buttonPath + ".name", defaultName);
        }
        return defaultName;
    }

    public List<String> getButtonLore(String menuName, String buttonPath) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(buttonPath + ".lore")) {
            return config.getStringList(buttonPath + ".lore");
        }
        return new ArrayList<>();
    }

    public Material getButtonMaterial(String menuName, String buttonPath, Material defaultMaterial) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(buttonPath + ".material")) {
            try {
                return Material.valueOf(config.getString(buttonPath + ".material"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid material in {}.{}: {}", menuName, buttonPath, config.getString(buttonPath + ".material"));
            }
        }
        return defaultMaterial;
    }

    public String getButtonTexture(String menuName, String buttonPath) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null && config.contains(buttonPath + ".texture")) {
            return config.getString(buttonPath + ".texture", "");
        }
        return "";
    }

    public ConfigurationSection getSection(String menuName, String path) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null) {
            return config.getConfigurationSection(path);
        }
        return null;
    }

    public boolean isBorderEnabled(String menuName) {
        YamlConfiguration config = configs.get(menuName);
        if (config != null) {
            return config.getBoolean("border.enabled", true);
        }
        return true;
    }

    public Material getBorderMaterial(String menuName, Material defaultMaterial) {
        return getButtonMaterial(menuName, "border", defaultMaterial);
    }

    public String getBorderName(String menuName) {
        return getButtonName(menuName, "border", "");
    }

    public Component parseComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        try {
            return MiniMessage.miniMessage().deserialize(text);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse MiniMessage: {}", text, e);
            return Component.text(text);
        }
    }

    public List<Component> parseComponentList(List<String> texts) {
        return texts.stream()
                .map(this::parseComponent)
                .collect(Collectors.toList());
    }

    public Component getButtonNameComponent(String menuName, String buttonPath, Component defaultName) {
        String name = getButtonName(menuName, buttonPath, null);
        if (name != null) {
            return parseComponent(name);
        }
        return defaultName;
    }

    public List<Component> getButtonLoreComponents(String menuName, String buttonPath) {
        List<String> lore = getButtonLore(menuName, buttonPath);
        return parseComponentList(lore);
    }
}
