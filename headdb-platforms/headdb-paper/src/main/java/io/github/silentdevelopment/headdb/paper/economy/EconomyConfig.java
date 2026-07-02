package io.github.silentdevelopment.headdb.paper.economy;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record EconomyConfig(
        boolean enabled,
        @NotNull String provider,
        double anyHeadPrice,
        @NotNull Map<String, Double> categoryPrices,
        @NotNull Map<String, Double> customCategoryPrices,
        @NotNull Map<String, Double> headPrices,
        @Nullable Double playerHeadPrice
) {

    public EconomyConfig {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(categoryPrices, "categoryPrices");
        Objects.requireNonNull(customCategoryPrices, "customCategoryPrices");
        Objects.requireNonNull(headPrices, "headPrices");

        provider = provider.trim().toLowerCase(Locale.ROOT);
        if (provider.isBlank()) {
            provider = "vault";
        }

        anyHeadPrice = Math.max(0.0D, anyHeadPrice);
        categoryPrices = Map.copyOf(categoryPrices);
        customCategoryPrices = Map.copyOf(customCategoryPrices);
        headPrices = Map.copyOf(headPrices);
        if (playerHeadPrice != null && playerHeadPrice < 0.0D) {
            playerHeadPrice = null;
        }
    }

    public static @NotNull EconomyConfig load(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        Path file = plugin.getDataFolder().toPath().resolve("economy.yml");
        copyDefault(file);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        return new EconomyConfig(
                yaml.getBoolean("enabled", false),
                yaml.getString("provider", "vault"),
                yaml.getDouble("prices.any-head", 0.0D),
                prices(yaml.getConfigurationSection("prices.categories")),
                prices(yaml.getConfigurationSection("prices.custom-categories")),
                headPrices(yaml.getConfigurationSection("prices.heads")),
                yaml.isSet("prices.player-heads") ? yaml.getDouble("prices.player-heads", 0.0D) : null
        );
    }

    public @Nullable Double headPrice(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");

        Double direct = headPrices.get(key(id.display()));
        if (direct != null) {
            return direct;
        }

        return headPrices.get(key(id.toString()));
    }

    public @Nullable Double categoryPrice(@NotNull String category) {
        Objects.requireNonNull(category, "category");
        return categoryPrices.get(key(category));
    }

    public @Nullable Double customCategoryPrice(@NotNull String categoryId) {
        Objects.requireNonNull(categoryId, "categoryId");
        return customCategoryPrices.get(key(categoryId));
    }

    public static @NotNull String key(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull Map<String, Double> prices(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, Double> prices = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            double value = section.getDouble(key, 0.0D);
            if (value <= 0.0D) {
                continue;
            }

            prices.put(key(key), value);
        }

        return Map.copyOf(prices);
    }

    private static @NotNull Map<String, Double> headPrices(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, Double> prices = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            double value = section.getDouble(key, 0.0D);
            if (value <= 0.0D) {
                continue;
            }

            prices.put(key(key), value);
        }

        return Map.copyOf(prices);
    }

    private static void copyDefault(@NotNull Path file) {
        if (Files.exists(file)) {
            return;
        }

        try {
            Path parent = file.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, defaultConfig());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create economy.yml.", exception);
        }
    }

    private static @NotNull String defaultConfig() {
        return "enabled: false\n"
                + "provider: vault\n"
                + "\n"
                + "prices:\n"
                + "  # Fallback price for every head. Set to 0 to make heads free.\n"
                + "  any-head: 0.0\n"
                + "\n"
                + "  # Price for player heads. If omitted, any-head is used.\n"
                + "  player-heads: 0.0\n"
                + "\n"
                + "  # Remote/custom head category prices. These override any-head.\n"
                + "  categories: {}\n"
                + "\n"
                + "  # More Categories prices, keyed by custom category id. These override normal category prices.\n"
                + "  custom-categories: {}\n"
                + "\n"
                + "  # Per-head prices. Keys can use visible ids: 1, custom:example, player:<uuid>.\n"
                + "  # Per-head prices override category and fallback prices.\n"
                + "  heads: {}\n";
    }
}
