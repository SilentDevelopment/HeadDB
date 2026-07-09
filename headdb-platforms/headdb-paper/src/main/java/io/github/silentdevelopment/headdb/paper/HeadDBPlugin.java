package io.github.silentdevelopment.headdb.paper;

import io.github.silentdevelopment.headdb.HeadDBService;
import io.github.silentdevelopment.headdb.paper.command.Commands;
import io.github.silentdevelopment.headdb.paper.config.PluginConfig;
import io.github.silentdevelopment.headdb.paper.config.ConfigException;
import io.github.silentdevelopment.headdb.paper.config.ConfigLoader;
import io.github.silentdevelopment.headdb.paper.economy.EconomyConfig;
import io.github.silentdevelopment.headdb.paper.economy.EconomyService;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiConfig;
import io.github.silentdevelopment.headdb.paper.gui.category.CustomCategoryService;
import io.github.silentdevelopment.headdb.paper.gui.edit.HeadEditListener;
import io.github.silentdevelopment.headdb.paper.gui.favorites.FavoriteHeadService;
import io.github.silentdevelopment.headdb.paper.gui.config.GuiConfigLoader;
import io.github.silentdevelopment.headdb.paper.gui.GuiService;
import io.github.silentdevelopment.headdb.paper.gui.admin.AdminModeService;
import io.github.silentdevelopment.headdb.paper.item.CachingHeadItemFactory;
import io.github.silentdevelopment.headdb.paper.item.DefaultHeadItemFactory;
import io.github.silentdevelopment.headdb.paper.item.HeadItemFactory;
import io.github.silentdevelopment.headdb.paper.local.HeadRegistry;
import io.github.silentdevelopment.headdb.paper.local.custom.CustomHeadStore;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverrideStore;
import io.github.silentdevelopment.headdb.paper.local.player.BukkitPlayerHeadService;
import io.github.silentdevelopment.headdb.paper.local.player.DisabledPlayerHeadService;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadCache;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadService;
import io.github.silentdevelopment.headdb.paper.local.storage.SqliteStorageMigrator;
import io.github.silentdevelopment.headdb.paper.local.storage.StrataLocalStores;
import io.github.silentdevelopment.headdb.paper.local.taxonomy.CustomTaxonomyService;
import io.github.silentdevelopment.headdb.paper.local.storage.NoopLocalStores;
import io.github.silentdevelopment.headdb.paper.message.Messages;
import io.github.silentdevelopment.headdb.paper.metrics.HeadDBMetrics;
import io.github.silentdevelopment.headdb.paper.prompt.PromptInputService;
import io.github.silentdevelopment.headdb.paper.runtime.PlatformRequirements;
import io.github.silentdevelopment.headdb.paper.runtime.PluginRuntime;
import io.github.silentdevelopment.headdb.paper.runtime.RuntimeDiagnostics;
import io.github.silentdevelopment.headdb.paper.runtime.StartupChecks;
import io.github.silentdevelopment.headdb.paper.service.PaperHeadDBService;
import io.github.silentdevelopment.headdb.paper.updater.UpdateService;
import io.github.silentdevelopment.hermes.id.LocaleId;
import io.github.silentdevelopment.hermes.paper.core.Hermes;
import io.github.silentdevelopment.hermes.paper.messenger.PaperMessenger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Objects;

public final class HeadDBPlugin extends JavaPlugin {

    private PluginConfig config;
    private GuiConfig guiConfig;
    private PluginRuntime runtime;
    private Messages messages;
    private HeadItemFactory itemFactory;
    private GuiService guiService;
    private HeadRegistry headRegistry;
    private PromptInputService promptInputService;
    private final AdminModeService adminModes = new AdminModeService();
    private FavoriteHeadService favoriteHeadService;
    private CustomCategoryService customCategoryService;
    private CustomTaxonomyService customTagService;
    private CustomTaxonomyService customCollectionService;
    private EconomyService economyService;
    private UpdateService updateService;

    @Override
    public void onEnable() {
        if (!isPaper()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!PlatformRequirements.supported(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            reload();

            this.promptInputService = new PromptInputService(this);
            getServer().getPluginManager().registerEvents(new HeadEditListener(this), this);

            registerCommands();
            cleanupSuccessfulUpdateBackup();
            startUpdater();

        } catch (ConfigException exception) {
            getSLF4JLogger().error("HeadDB config could not be loaded.", exception);
            getServer().getPluginManager().disablePlugin(this);
        } catch (RuntimeException exception) {
            getSLF4JLogger().error("HeadDB runtime could not be started.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);

        PluginRuntime currentRuntime = runtime;

        if (currentRuntime != null) {
            currentRuntime.close();
        }

        if (promptInputService != null) {
            promptInputService.shutdown();
        }

        if (updateService != null) {
            updateService.close();
        }

        HeadDBMetrics.unregister(this);

        adminModes.clear();
        favoriteHeadService = null;
        customCategoryService = null;
        customTagService = null;
        customCollectionService = null;
        economyService = null;
        updateService = null;
        runtime = null;
        config = null;
        guiConfig = null;
        messages = null;
        guiService = null;
        itemFactory = null;
        headRegistry = null;
        promptInputService = null;

        unregisterServices();
    }

    @ApiStatus.Internal
    public synchronized void reload() throws ConfigException {
        PluginConfig loadedConfig = new ConfigLoader(getDataFolder().toPath()).load();
        GuiConfig loadedGuiConfig = new GuiConfigLoader(this).load();
        EconomyConfig loadedEconomyConfig = EconomyConfig.load(this);
        Messages loadedMessages = createMessages(loadedConfig);
        PluginRuntime createdRuntime = PluginRuntime.create(this, loadedConfig);
        Path localStoreDatabase = loadedConfig.localStoreDatabase(getDataFolder().toPath());

        SqliteStorageMigrator.migrate(localStoreDatabase);

        HeadRegistry createdHeadRegistry = createHeadRegistry(loadedConfig, createdRuntime);
        HeadItemFactory createdItemFactory = createItemFactory(loadedConfig);
        FavoriteHeadService createdFavoriteHeadService = new FavoriteHeadService(localStoreDatabase);
        CustomCategoryService createdCustomCategoryService = new CustomCategoryService(localStoreDatabase);
        CustomTaxonomyService createdCustomTagService = new CustomTaxonomyService(localStoreDatabase, "tag", "Local custom tag.");
        CustomTaxonomyService createdCustomCollectionService = new CustomTaxonomyService(localStoreDatabase, "collection", "Local custom collection.");
        EconomyService createdEconomyService = EconomyService.create(this, loadedEconomyConfig);
        GuiService createdGuiService = new GuiService(this, createdItemFactory);
        UpdateService createdUpdateService = new UpdateService(this, loadedConfig);

        RuntimeDiagnostics.logConfig(this, loadedConfig);

        if (loadedConfig.isDebug()) {
            StartupChecks.run(this, loadedConfig, createdRuntime);
        }

        createdRuntime.start();

        PluginRuntime previousRuntime = this.runtime;
        UpdateService previousUpdateService = this.updateService;

        this.config = loadedConfig;
        this.guiConfig = loadedGuiConfig;
        this.messages = loadedMessages;
        this.runtime = createdRuntime;
        this.headRegistry = createdHeadRegistry;
        this.itemFactory = createdItemFactory;
        this.guiService = createdGuiService;
        this.favoriteHeadService = createdFavoriteHeadService;
        this.customCategoryService = createdCustomCategoryService;
        this.customTagService = createdCustomTagService;
        this.customCollectionService = createdCustomCollectionService;
        this.economyService = createdEconomyService;
        this.updateService = createdUpdateService;

        clearItemCache();
        clearSearchCache();

        if (previousUpdateService != null) {
            previousUpdateService.close();
        }

        if (previousRuntime != null) {
            previousRuntime.close();
        }

        RuntimeDiagnostics.logRuntimeState(this, createdRuntime);

        registerServices();

        HeadDBMetrics.register(this);
    }


    public synchronized void startUpdater() {
        UpdateService currentUpdateService = updateService;

        if (currentUpdateService == null) {
            throw new IllegalStateException("HeadDB update service is not initialized");
        }

        currentUpdateService.start();
    }

    public synchronized void cleanupSuccessfulUpdateBackup() {
        UpdateService currentUpdateService = updateService;

        if (currentUpdateService == null) {
            throw new IllegalStateException("HeadDB update service is not initialized");
        }

        currentUpdateService.cleanupBackupAfterSuccessfulLoad();
    }

    public @NotNull HeadItemFactory itemFactory() {
        HeadItemFactory currentItemFactory = itemFactory;

        if (currentItemFactory == null) {
            throw new IllegalStateException("HeadDB item factory is not initialized");
        }

        return currentItemFactory;
    }

    public @NotNull GuiService guis() {
        GuiService currentGuiService = guiService;

        if (currentGuiService == null) {
            throw new IllegalStateException("HeadDB GUI service is not initialized");
        }

        return currentGuiService;
    }

    public @NotNull GuiConfig guiConfig() {
        GuiConfig currentGuiConfig = guiConfig;

        if (currentGuiConfig == null) {
            throw new IllegalStateException("HeadDB GUI config is not initialized");
        }

        return currentGuiConfig;
    }

    public synchronized void reloadGuiConfigOnly() throws ConfigException {
        this.guiConfig = new GuiConfigLoader(this).load();
    }

    public @NotNull PluginConfig config() {
        PluginConfig currentConfig = config;

        if (currentConfig == null) {
            throw new IllegalStateException("HeadDB config is not initialized");
        }

        return currentConfig;
    }

    public @NotNull PluginRuntime runtime() {
        PluginRuntime currentRuntime = runtime;

        if (currentRuntime == null) {
            throw new IllegalStateException("HeadDB runtime is not initialized");
        }

        return currentRuntime;
    }

    public @NotNull HeadRegistry headRegistry() {
        HeadRegistry currentHeadRegistry = headRegistry;

        if (currentHeadRegistry == null) {
            throw new IllegalStateException("HeadDB head registry is not initialized");
        }

        return currentHeadRegistry;
    }


    public @NotNull PromptInputService prompts() {
        PromptInputService currentPromptInputService = promptInputService;

        if (currentPromptInputService == null) {
            throw new IllegalStateException("HeadDB prompt input service is not initialized");
        }

        return currentPromptInputService;
    }

    public @NotNull AdminModeService adminModes() {
        return adminModes;
    }

    public @NotNull FavoriteHeadService favorites() {
        FavoriteHeadService currentFavoriteHeadService = favoriteHeadService;
        if (currentFavoriteHeadService == null) {
            throw new IllegalStateException("HeadDB favorite service is not initialized");
        }
        return currentFavoriteHeadService;
    }

    public @NotNull CustomCategoryService customCategories() {
        CustomCategoryService currentCustomCategoryService = customCategoryService;
        if (currentCustomCategoryService == null) {
            throw new IllegalStateException("HeadDB custom category service is not initialized");
        }
        return currentCustomCategoryService;
    }



    public @NotNull CustomTaxonomyService customTags() {
        CustomTaxonomyService currentCustomTagService = customTagService;
        if (currentCustomTagService == null) {
            throw new IllegalStateException("HeadDB custom tag service is not initialized");
        }
        return currentCustomTagService;
    }

    public @NotNull CustomTaxonomyService customCollections() {
        CustomTaxonomyService currentCustomCollectionService = customCollectionService;
        if (currentCustomCollectionService == null) {
            throw new IllegalStateException("HeadDB custom collection service is not initialized");
        }
        return currentCustomCollectionService;
    }

    public @NotNull EconomyService economy() {
        EconomyService currentEconomyService = economyService;
        if (currentEconomyService == null) {
            throw new IllegalStateException("HeadDB economy service is not initialized");
        }
        return currentEconomyService;
    }

    public @NotNull UpdateService updater() {
        UpdateService currentUpdateService = updateService;
        if (currentUpdateService == null) {
            throw new IllegalStateException("HeadDB update service is not initialized");
        }
        return currentUpdateService;
    }

    public @NotNull Messages messages() {
        Messages currentMessages = messages;

        if (currentMessages == null) {
            throw new IllegalStateException("HeadDB messages are not initialized");
        }

        return currentMessages;
    }

    public void clearItemCache() {
        HeadItemFactory currentItemFactory = itemFactory;

        if (!(currentItemFactory instanceof CachingHeadItemFactory clearable)) {
            return;
        }

        clearable.clear();
    }

    public int itemCacheSize() {
        HeadItemFactory currentItemFactory = itemFactory;

        if (!(currentItemFactory instanceof CachingHeadItemFactory cache)) {
            return 0;
        }

        return cache.size();
    }

    public void clearSearchCache() {
        GuiService currentGuiService = guiService;

        if (currentGuiService == null) {
            return;
        }

        currentGuiService.clearSearchCache();
    }

    private void registerServices() {
        getServer().getServicesManager().unregister(HeadRegistry.class, this);
        getServer().getServicesManager().unregister(HeadDBService.class, this);
        getServer().getServicesManager().register(HeadRegistry.class, headRegistry, this, ServicePriority.Normal);
        getServer().getServicesManager().register(HeadDBService.class, new PaperHeadDBService(this), this, ServicePriority.Normal);
    }

    private void unregisterServices() {
        getServer().getServicesManager().unregisterAll(this);
    }

    private @NotNull HeadRegistry createHeadRegistry(@NotNull PluginConfig config, @NotNull PluginRuntime runtime) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(runtime, "runtime");

        Path localStoreDatabase = config.localStoreDatabase(getDataFolder().toPath());
        StrataLocalStores localStores = StrataLocalStores.sqlite(localStoreDatabase, runnable -> getServer().getAsyncScheduler().runNow(this, task -> runnable.run()));
        RemoteHeadOverrideStore overrideStore = config.remoteOverridesEnabled() ? localStores.remoteOverrides() : NoopLocalStores.remoteOverrides();
        CustomHeadStore customHeadStore = config.customHeadsEnabled() ? localStores.customHeads() : NoopLocalStores.customHeads();
        PlayerHeadCache playerHeadCache = config.playerHeadsEnabled() ? localStores.playerHeadCache(config.playerHeadCacheTtl()) : NoopLocalStores.playerHeadCache();
        PlayerHeadService playerHeadService = config.playerHeadsEnabled()
                ? new BukkitPlayerHeadService(this, playerHeadCache, config.playerHeadCacheTtl(), config.playerHeadFailedCacheTtl(), config.playerHeadsAllowExternalLookup())
                : new DisabledPlayerHeadService();

        CustomTaxonomyService customTags = new CustomTaxonomyService(localStoreDatabase, "tag", "Local custom tag.");
        CustomTaxonomyService customCollections = new CustomTaxonomyService(localStoreDatabase, "collection", "Local custom collection.");
        return new HeadRegistry(runtime.database(), overrideStore, customHeadStore, playerHeadService, customTags, customCollections);
    }

    private @NotNull HeadItemFactory createItemFactory(@NotNull PluginConfig config) {
        Objects.requireNonNull(config, "config");

        HeadItemFactory baseFactory = new DefaultHeadItemFactory(this);
        if (!config.cacheItemEnabled()) {
            return baseFactory;
        }

        return new CachingHeadItemFactory(baseFactory, config.cacheItemMaxSize());
    }

    private @NotNull Messages createMessages(@NotNull PluginConfig config) {
        Objects.requireNonNull(config, "config");

        Path messagesDirectory = config.messagesDirectory(getDataFolder().toPath());

        PaperMessenger messenger = Hermes.init(this)
                .key("headdb")
                .defaultLocale(LocaleId.of(config.defaultLocale()))
                .consoleLocale(LocaleId.of(config.consoleLocale()))
                .localesDirectory(messagesDirectory)
                .syncDefaultsFromResources("messages")
                .recursive(false)
                .yaml()
                .init();

        Hermes.registerCommands(this, messenger);

        return new Messages(messenger);
    }

    private void registerCommands() {
        new Commands(this).register();
    }

    public boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig"); // Legacy Paper
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.ServerBuildInfo"); // Modern Paper
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

}
