package io.github.silentdevelopment.headdb.paper.economy;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class EconomyService {

    private final HeadDBPlugin plugin;
    private EconomyConfig config;
    private final EconomyProvider provider;

    private EconomyService(@NotNull HeadDBPlugin plugin, @NotNull EconomyConfig config, @NotNull EconomyProvider provider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public static @NotNull EconomyService create(@NotNull HeadDBPlugin plugin, @NotNull EconomyConfig config) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");

        if (!config.enabled()) {
            return new EconomyService(plugin, config, new DisabledEconomyProvider());
        }

        if (!config.provider().equalsIgnoreCase("vault")) {
            plugin.getSLF4JLogger().warn("Unsupported economy provider '{}'. Economy integration is disabled.", config.provider());
            return new EconomyService(plugin, config, new DisabledEconomyProvider());
        }

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getSLF4JLogger().warn("Economy is enabled but Vault is not loaded.");
            return new EconomyService(plugin, config, new DisabledEconomyProvider());
        }

        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            plugin.getSLF4JLogger().warn("Economy is enabled but Vault did not provide an Economy service.");
            return new EconomyService(plugin, config, new DisabledEconomyProvider());
        }

        return new EconomyService(plugin, config, new VaultEconomyProvider(registration.getProvider()));
    }

    public boolean enabled() {
        return config.enabled() && provider.available();
    }

    public double price(@NotNull Head head) {
        return price(head, null);
    }

    public double price(@NotNull Head head, @Nullable String customCategoryId) {
        Objects.requireNonNull(head, "head");

        Double headPrice = config.headPrice(head.id());
        if (headPrice != null) {
            return headPrice;
        }

        if (customCategoryId != null && !customCategoryId.isBlank()) {
            Double customCategoryPrice = config.customCategoryPrice(customCategoryId);
            if (customCategoryPrice != null) {
                return customCategoryPrice;
            }
        }

        if (head.id().isPlayer() && config.playerHeadPrice() != null) {
            return config.playerHeadPrice();
        }

        Double categoryPrice = config.categoryPrice(head.category());
        if (categoryPrice != null) {
            return categoryPrice;
        }

        return config.anyHeadPrice();
    }

    public boolean charge(@NotNull Player player, @NotNull Head head, int amount) {
        return charge(player, head, amount, null);
    }

    public boolean charge(@NotNull Player player, @NotNull Head head, int amount, @Nullable String customCategoryId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(head, "head");

        if (amount < 1) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        double unitPrice = price(head, customCategoryId);
        if (!enabled() || unitPrice <= 0.0D) {
            return true;
        }

        double total = unitPrice * amount;
        String formattedPrice = format(total);
        EconomyWithdrawal withdrawal = provider.withdraw(player, total);

        if (withdrawal.success()) {
            player.sendMessage(plugin.messages().economyPurchased(player, head, formattedPrice));
            return true;
        }

        player.sendMessage(plugin.messages().economyInvalidFunds(player, head, formattedPrice));
        return false;
    }

    public void setHeadPrice(@NotNull io.github.silentdevelopment.headdb.model.HeadId id, double price) {
        Objects.requireNonNull(id, "id");
        setPrice("prices.heads." + id.display().trim().toLowerCase(Locale.ROOT), price);
    }

    public void setCustomCategoryPrice(@NotNull String categoryId, double price) {
        Objects.requireNonNull(categoryId, "categoryId");
        setPrice("prices.custom-categories." + categoryId.trim().toLowerCase(Locale.ROOT), price);
    }

    public @NotNull String format(double amount) {
        return provider.format(amount);
    }

    private void setPrice(@NotNull String path, double price) {
        if (price < 0.0D) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }

        Path file = plugin.getDataFolder().toPath().resolve("economy.yml");
        try {
            Path parent = file.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
            if (price <= 0.0D) {
                yaml.set(path, null);
            } else {
                yaml.set(path, price);
            }
            yaml.save(file.toFile());
            config = EconomyConfig.load(plugin);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to update economy.yml.", exception);
        }
    }
}