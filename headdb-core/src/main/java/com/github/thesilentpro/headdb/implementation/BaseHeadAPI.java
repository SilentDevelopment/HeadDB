package com.github.thesilentpro.headdb.implementation;


import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.github.thesilentpro.headdb.api.HeadAPI;
import com.github.thesilentpro.headdb.api.HeadDatabase;
import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.core.factory.ItemFactoryRegistry;
import com.github.thesilentpro.headdb.core.util.Utils;

/**
 * Default implementation of {@link HeadAPI} using BaseHeadDatabase.
 */
public class BaseHeadAPI implements HeadAPI {

    private final ExecutorService executor;
    private final HeadDatabase database;

    public BaseHeadAPI(int workerThreads, HeadDatabase headDatabase) {
        this.executor = Utils.executorService(workerThreads, "HeadAPI Worker");
        this.database = headDatabase;
        this.database.update();
    }

    @Override
    public void awaitReady() {
        database.awaitReady();
    }

    @Override
    public boolean isReady() {
        return database.isReady();
    }

    @Override
    public CompletableFuture<List<Head>> onReady() {
        return database.onReady();
    }

    @NotNull
    @Override
    public CompletableFuture<List<Head>> searchByName(@NotNull String name, boolean lenient) {
        return CompletableFuture.supplyAsync(() -> {
            List<Head> heads = database.getHeads();
            if (heads == null || heads.isEmpty()) {
                return Collections.emptyList();
            }
            return heads.stream()
                    .filter(h -> lenient ? Utils.matches(h.getName(), name) : h.getName().equalsIgnoreCase(name))
                    .collect(Collectors.toList());
        }, executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Optional<Head>> findByName(@NotNull String name, boolean lenient) {
        return CompletableFuture.supplyAsync(() -> {
            List<Head> heads = database.getHeads();
            if (heads == null || heads.isEmpty()) {
                return Optional.empty();
            }
            return heads.stream()
                    .filter(h -> lenient ? Utils.matches(h.getName(), name) : h.getName().equalsIgnoreCase(name))
                    .findAny();
        }, executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Optional<Head>> findById(int id) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(database.getById(id)), executor);
    }

    @NotNull
    @Override
    public CompletableFuture<Optional<Head>> findByTexture(@NotNull String texture) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(database.getByTexture(texture)), executor);
    }

    @NotNull
    @Override
    public CompletableFuture<List<Head>> findByCategory(@NotNull String category) {
        return CompletableFuture.supplyAsync(() -> database.getByCategory(category), executor);
    }

    @NotNull
    @Override
    public CompletableFuture<List<Head>> findByTags(@NotNull String... tags) {
        return CompletableFuture.supplyAsync(() -> database.getByTags(tags), executor);
    }

    @NotNull
    @Override
    public CompletableFuture<List<Head>> getHeads() {
        return CompletableFuture.supplyAsync(() -> database.getHeads() != null ? database.getHeads() : Collections.emptyList(), executor);
    }

    @NotNull
    @Override
    public List<String> findKnownCategories() {
        List<Head> heads = database.getHeads();
        if (heads == null || heads.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> categories = new LinkedHashSet<>();
        for (Head head : heads) {
            categories.add(head.getCategory());
        }
        return new ArrayList<>(categories);
    }

    @NotNull
    @Override
    public List<ItemStack> computeLocalHeads() {
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        List<ItemStack> heads = new ArrayList<>();
        for (OfflinePlayer player : players) {
            heads.add(ItemFactoryRegistry.get().asItem(player));
        }
        return heads;
    }

    @NotNull
    @Override
    public Optional<ItemStack> computeLocalHead(UUID uniqueId) {
        return Optional.ofNullable(ItemFactoryRegistry.get().asItem(Bukkit.getOfflinePlayer(uniqueId)));
    }

    @Override
    public @NotNull ExecutorService getExecutor() {
        return executor;
    }

}