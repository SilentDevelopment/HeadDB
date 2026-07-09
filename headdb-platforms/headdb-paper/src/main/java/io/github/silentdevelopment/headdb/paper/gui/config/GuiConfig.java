package io.github.silentdevelopment.headdb.paper.gui.config;

import io.github.silentdevelopment.headdb.paper.config.ConfigException;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuiConfig {

    private static final List<String> ICON_KEYS = List.of(
            "back",
            "previous",
            "next",
            "info",
            "settings",
            "languages",
            "status",
            "debug",
            "verify",
            "refresh",
            "reload",
            "close",
            "browse",
            "browse-all",
            "search",
            "player-heads",
            "custom-heads",
            "hidden-heads",
            "favorites",
            "collections",
            "tags",
            "create-tag",
            "create-collection",
            "create-category",
            "taxonomy-id",
            "taxonomy-name",
            "taxonomy-description",
            "taxonomy-save",
            "taxonomy-filter-all",
            "taxonomy-filter-custom",
            "taxonomy-search",
            "create-head",
            "create-head-held",
            "create-head-fresh",
            "create-head-id",
            "create-head-name",
            "create-head-texture",
            "create-head-category",
            "create-head-tags",
            "create-head-collections",
            "create-head-save",
            "more-categories",
            "more-category",
            "category",
            "category-add",
            "category-remove",
            "category-name",
            "category-description",
            "category-permission",
            "category-save-draft",
            "category-publish",
            "category-delete",
            "category-price",
            "category-id",
            "category-material",
            "category-head-icon",
            "category-view-heads",
            "category-save",
            "material-select",
            "material-type-chat",
            "button-lore-line",
            "confirm-yes",
            "confirm-no",
            "show-all",
            "admin-mode-on",
            "admin-mode-off",
            "edit",
            "edit-name",
            "edit-lore",
            "edit-category",
            "edit-tags",
            "edit-collections",
            "edit-visibility-visible",
            "edit-visibility-hidden",
            "edit-delete",
            "edit-publish",
            "edit-price",
            "edit-reset",
            "lore-add",
            "lore-clear",
            "lore-line",
            "gui-edit-lore",
            "gui-edit-reset",
            "gui-edit-head-id",
            "gui-edit-type",
            "gui-edit-material",
            "gui-edit-name",
            "lore-reset",
            "reset-language",
            "no-permission",
            "empty",
            "sort-filter",
            "sort-cycle",
            "sort-direction",
            "filter-category",
            "filter-tags",
            "filter-collections",
            "filter-ids",
            "filter-selected",
            "filter-unselected",
            "clear-filters",
            "sort-option"
    );

    private final int version;
    private final GuiFillerConfig filler;
    private final Map<String, Integer> slots;
    private final Map<String, String> texts;
    private final Map<String, GuiIconConfig> icons;

    private GuiConfig(int version, @NotNull GuiFillerConfig filler, @NotNull Map<String, Integer> slots, @NotNull Map<String, String> texts, @NotNull Map<String, GuiIconConfig> icons) {
        this.version = version;
        this.filler = Objects.requireNonNull(filler, "filler");
        this.slots = Map.copyOf(Objects.requireNonNull(slots, "slots"));
        this.texts = Map.copyOf(Objects.requireNonNull(texts, "texts"));
        this.icons = Map.copyOf(Objects.requireNonNull(icons, "icons"));
    }

    public int version() {
        return version;
    }

    public @NotNull GuiFillerConfig filler() {
        return filler;
    }

    public int slot(@NotNull String key, int fallback) {
        Objects.requireNonNull(key, "key");
        return slots.getOrDefault(key, fallback);
    }


    public @NotNull String text(@NotNull String key, @NotNull String fallback) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fallback, "fallback");
        return texts.getOrDefault(key, fallback);
    }

    public @NotNull GuiIconConfig icon(@NotNull String key) {
        Objects.requireNonNull(key, "key");

        GuiIconConfig icon = icons.get(key);
        if (icon == null) {
            throw new ConfigException("Missing GUI icon config: " + key);
        }

        return icon;
    }

    public @NotNull GuiIconConfig iconOrDefault(@NotNull String key, @NotNull String fallbackKey) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fallbackKey, "fallbackKey");

        GuiIconConfig icon = icons.get(key);
        if (icon != null) {
            return icon;
        }

        GuiIconConfig fallback = icon(fallbackKey);
        return new GuiIconConfig(key, fallback.type(), fallback.headId(), fallback.material(), fallback.name(), fallback.lore());
    }

    public boolean hasIcon(@NotNull String key) {
        Objects.requireNonNull(key, "key");
        return icons.containsKey(key);
    }

    public void validate() {
        validateMaterial("filler.material", filler.material());

        for (Map.Entry<String, Integer> entry : slots.entrySet()) {
            if (entry.getValue() < 0 || entry.getValue() > 53) {
                throw new ConfigException("gui.yml slots." + entry.getKey() + " must be between 0 and 53");
            }
        }

        for (String key : ICON_KEYS) {
            if (!icons.containsKey(key)) {
                throw new ConfigException("gui.yml is missing required icon config: icons." + key);
            }

            validateIcon(icon(key));
        }
    }

    public static @NotNull List<String> iconKeys() {
        return ICON_KEYS;
    }

    public static @NotNull GuiConfig fromMap(@NotNull Map<String, Object> root) {
        Objects.requireNonNull(root, "root");

        int version = intValue(root, "version", 1);

        Map<String, Object> fillerMap = section(root, "filler");
        GuiFillerConfig filler = new GuiFillerConfig(booleanValue(fillerMap, "enabled", true), stringValue(fillerMap, "material", "BLACK_STAINED_GLASS_PANE"), stringValue(fillerMap, "name", " "), stringList(fillerMap.get("lore")));

        Map<String, Integer> slots = new LinkedHashMap<>();
        Map<String, Object> slotRoot = optionalSection(root, "slots");
        for (Map.Entry<String, Object> entry : slotRoot.entrySet()) {
            slots.put(entry.getKey(), intObjectValue("slots." + entry.getKey(), entry.getValue()));
        }

        Map<String, String> texts = new LinkedHashMap<>();
        Map<String, Object> textRoot = optionalSection(root, "texts");
        for (Map.Entry<String, Object> entry : textRoot.entrySet()) {
            texts.put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        Map<String, Object> iconRoot = section(root, "icons");
        Map<String, GuiIconConfig> icons = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : iconRoot.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?>)) {
                throw new ConfigException("gui.yml icons." + entry.getKey() + " must be an object.");
            }

            Map<String, Object> iconMap = section(iconRoot, entry.getKey());
            Map<String, Object> fallbackMap = entry.getKey().startsWith("category.") ? optionalSection(iconRoot, "category") : Map.of();
            String fallbackType = stringValue(fallbackMap, "type", "ITEM");
            String fallbackHeadId = stringValue(fallbackMap, "head-id", "");
            String fallbackMaterial = stringValue(fallbackMap, "material", "STONE");
            String fallbackName = stringValue(fallbackMap, "name", "<white>" + entry.getKey());
            List<String> fallbackLore = stringList(fallbackMap.get("lore"));
            icons.put(entry.getKey(), new GuiIconConfig(entry.getKey(), stringValue(iconMap, "type", fallbackType), stringValue(iconMap, "head-id", fallbackHeadId), stringValue(iconMap, "material", fallbackMaterial), stringValue(iconMap, "name", fallbackName), iconMap.containsKey("lore") ? stringList(iconMap.get("lore")) : fallbackLore));
        }

        GuiConfig config = new GuiConfig(version, filler, slots, texts, icons);
        config.validate();
        return config;
    }

    private static void validateIcon(@NotNull GuiIconConfig icon) {
        Objects.requireNonNull(icon, "icon");

        try {
            icon.iconType();
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("gui.yml icons." + icon.key() + ".type must be ITEM or HEAD", exception);
        }

        validateMaterial("icons." + icon.key() + ".material", icon.material());

        if (icon.name() == null) {
            throw new ConfigException("gui.yml icons." + icon.key() + ".name cannot be null");
        }

        if (icon.lore() == null) {
            throw new ConfigException("gui.yml icons." + icon.key() + ".lore cannot be null");
        }
    }

    private static void validateMaterial(@NotNull String key, @NotNull String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        if (GuiMaterials.item(value).isEmpty()) {
            throw new ConfigException("gui.yml " + key + " must be a valid modern item material");
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> section(@NotNull Map<String, Object> root, @NotNull String key) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(key, "key");

        Object value = root.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("gui.yml section '" + key + "' must be an object.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return result;
    }

    private static @NotNull Map<String, Object> optionalSection(@NotNull Map<String, Object> root, @NotNull String key) {
        Object value = root.get(key);
        if (value == null) {
            return Map.of();
        }

        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("gui.yml section '" + key + "' must be an object.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }

        return result;
    }

    private static boolean booleanValue(@NotNull Map<String, Object> map, @NotNull String key, boolean fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        if (value instanceof Boolean bool) {
            return bool;
        }

        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static int intValue(@NotNull Map<String, Object> map, @NotNull String key, int fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        return intObjectValue(key, value);
    }

    private static int intObjectValue(@NotNull String key, @NotNull Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ConfigException("gui.yml " + key + " must be an integer.", exception);
        }
    }

    private static @NotNull String stringValue(@NotNull Map<String, Object> map, @NotNull String key, @NotNull String fallback) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }

        return String.valueOf(value);
    }

    private static @NotNull List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry == null) {
                    continue;
                }

                result.add(String.valueOf(entry));
            }

            return List.copyOf(result);
        }

        String scalar = String.valueOf(value);
        if (scalar.isBlank()) {
            return List.of();
        }

        return List.of(scalar);
    }
}
