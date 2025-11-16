package com.github.thesilentpro.headdb.core;

import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thesilentpro.grim.listener.PageListeners;
import com.github.thesilentpro.grim.page.registry.PageRegistry;
import com.github.thesilentpro.headdb.api.HeadAPI;
import com.github.thesilentpro.headdb.api.HeadDatabase;
import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.core.command.HDBMainCommand;
import com.github.thesilentpro.headdb.core.command.HDBSubCommandManager;
import com.github.thesilentpro.headdb.core.config.Config;
import com.github.thesilentpro.headdb.core.config.ConfigManager;
import com.github.thesilentpro.headdb.core.config.SoundConfig;
import com.github.thesilentpro.headdb.core.economy.EconomyProvider;
import com.github.thesilentpro.headdb.core.economy.VaultEconomyProvider;
import com.github.thesilentpro.headdb.core.factory.ItemFactoryRegistry;
import com.github.thesilentpro.headdb.core.menu.MenuManager;
import com.github.thesilentpro.headdb.core.storage.PlayerStorage;
import com.github.thesilentpro.headdb.core.util.Compatibility;
import com.github.thesilentpro.headdb.core.util.HDBLocalization;
import com.github.thesilentpro.headdb.core.util.Utils;
import com.github.thesilentpro.headdb.implementation.BaseHeadAPI;
import com.github.thesilentpro.headdb.implementation.BaseHeadDatabase;
import com.github.thesilentpro.inputs.paper.PaperInputListener;

public class HeadDB extends JavaPlugin {

    // Avoid using class for this logger: it will use the fully qualified name with the default logger cfg.
    private static final Logger LOGGER = LoggerFactory.getLogger("HeadDB");

    private ConfigManager configManager;
    private HeadDatabase headDatabase;
    private HeadAPI headApi;
    private HDBSubCommandManager subCommandManager;
    private MenuManager menuManager;
    private PlayerStorage playerStorage;
    private HDBLocalization localization;
    private EconomyProvider economyProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ItemFactoryRegistry.init(this);

        this.configManager = new ConfigManager(this);
        this.configManager.loadAll(this);
        this.localization = new HDBLocalization(this);
        this.localization.init();

        Config config = this.configManager.getConfig();
        String econProvider = config.getEconomyProvider();
        if (econProvider != null) {
            if (econProvider.equalsIgnoreCase("NONE") || econProvider.isEmpty()) {
                LOGGER.debug("Economy is disabled.");
            } else if (config.getEconomyProvider().equalsIgnoreCase("VAULT")) {
                this.economyProvider = new VaultEconomyProvider();
                this.economyProvider.init();
                LOGGER.debug("Economy Provider: Vault");
            } else {
                LOGGER.warn("Unknown economy provider in the config.yml!");
            }
        }

        // Init database
        int databaseThreads = config.getDatabaseThreads();
        this.headDatabase = new BaseHeadDatabase(Utils.executorService(databaseThreads, "Head Database Worker"), config.resolveEnabledIndexes());
        this.headDatabase.update().thenAcceptAsync(heads -> DATABASE_UPDATE_ACTION.accept(config, heads), Compatibility.getMainThreadExecutor(this));
        this.headApi = new BaseHeadAPI(config.getApiThreads(), headDatabase);
        this.menuManager = new MenuManager(this);
        this.headDatabase.onReady().thenRunAsync(() -> this.menuManager.registerDefaults(this));
        this.playerStorage = new PlayerStorage();
        this.playerStorage.load();
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> this.playerStorage.save(), config.getPlayerStorageSaveInterval() * 20L, config.getPlayerStorageSaveInterval() * 20L);

        getServer().getServicesManager().register(HeadAPI.class, headApi, this, ServicePriority.Normal);

        // Command handling
        this.subCommandManager = new HDBSubCommandManager(this);
        this.subCommandManager.registerDefaults();

        PluginCommand command = getCommand("headdb");
        if (command == null) {
            throw new RuntimeException("Missing HeadDB command in plugin.yml"); // Should never reach this
        }

        HDBMainCommand mainCommand = new HDBMainCommand(this);
        command.setExecutor(mainCommand);
        command.setTabCompleter(mainCommand);

        new PageListeners().register(this);
        new PaperInputListener().register(this);

        // Start updater task
        if (config.isUpdaterEnabled()) {
            this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> this.headDatabase.update().thenAccept(heads -> DATABASE_UPDATE_ACTION.accept(config, heads)), 86400L * 20L, 86400L * 20L);
        }

        if (!Compatibility.IS_PAPER) {
            String bukkitName = Bukkit.getName();
            String bukkitVersion = Bukkit.getBukkitVersion().substring(0, Bukkit.getBukkitVersion().indexOf("-"));
            LOGGER.warn("""
                    \s
                    \s
                        You're running a non-Paper server implementation (detected: {}).
                        For best performance and full compatibility with this plugin, it is highly recommended switching to Paper.
                        > Download it here: https://papermc.io/downloads
                    \s
                    """, bukkitName + " " + bukkitVersion);
        }

        new Metrics(this, 9152);
        LOGGER.info("Done! Database is {}", !this.headDatabase.isReady() ? "loading..." : "ready");
    }

    @Override
    public void onDisable() {
        PageRegistry.INSTANCE.getPages().keySet().forEach(InventoryView::close);
        if (this.playerStorage != null) {
            this.playerStorage.save();
        }
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public HDBLocalization getLocalization() {
        return localization;
    }

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public HDBSubCommandManager getSubCommandManager() {
        return subCommandManager;
    }

    public SoundConfig getSoundConfig() {
        return this.configManager.getSoundConfig();
    }

    public Config getCfg() {
        return this.configManager.getConfig();
    }

    public com.github.thesilentpro.headdb.core.config.MenuConfig getMenuConfig() {
        return this.configManager.getMenuConfig();
    }

    public HeadAPI getHeadApi() {
        return headApi;
    }

    private static final BiConsumer<Config, List<Head>> DATABASE_UPDATE_ACTION = (config, heads) -> {
        int total = heads.size();
        LOGGER.info("Loaded {} heads!", total);

        if (config.isPreloadHeads() && total > 0) {
            int[] milestones = {25, 50, 75, 100};
            int nextMilestoneIndex = 0;

            for (int i = 0; i < total; i++) {
                heads.get(i).getItem(); // Loads the ItemStack in memory

                int percent = (int) (((i + 1) / (double) total) * 100);
                if (nextMilestoneIndex < milestones.length && percent >= milestones[nextMilestoneIndex]) {
                    LOGGER.info("Preloading heads... {}%", milestones[nextMilestoneIndex]);
                    nextMilestoneIndex++;
                }
            }
        }
    };

}