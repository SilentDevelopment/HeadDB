package com.github.thesilentpro.headdb.implementation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.thesilentpro.headdb.api.HeadDatabase;
import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.implementation.model.HeadMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BaseHeadDatabase implements HeadDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseHeadDatabase.class);

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Head.class, new HeadMapper()).create();
    private final Executor executor;

    // Non-indexed
    private volatile List<Head> heads = null;

    // Indexed
    private volatile Map<Integer, Head> byId = null;
    private volatile Map<String, Head> byTexture = null;
    private volatile Map<String, List<Head>> byCategory = null;
    private volatile Map<String, List<Head>> byTag = null;
    private final Index[] indexes;

    // track the latest load
    private volatile CompletableFuture<List<Head>> lastUpdateFuture;

    public BaseHeadDatabase(@Nullable Executor executor, @Nullable Index... indexes) {
        this.executor = executor != null ? executor : Executors.newSingleThreadExecutor(r -> new Thread(r, "Head Database Worker"));
        this.indexes = indexes;
    }

    public BaseHeadDatabase(@Nullable Executor executor) {
        this(executor, (Index[]) null);
    }

    public BaseHeadDatabase(@Nullable Index... indexes) {
        this(null, indexes);
    }

    public BaseHeadDatabase() {
        this(null, (Index[]) null);
    }

    @Override
    public CompletableFuture<List<Head>> update() {
        if (lastUpdateFuture != null && !lastUpdateFuture.isDone()) {
            return lastUpdateFuture;
        }

        lastUpdateFuture = CompletableFuture.supplyAsync(() -> {
            LOGGER.debug("Fetching heads...");
            long start = System.currentTimeMillis();

            try {
                URL url = URI.create("https://raw.githubusercontent.com/TheSilentPro/heads/refs/heads/main/heads.json").toURL();
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.setRequestProperty("Accept", "application/json");
                request.setRequestProperty("Accept-Encoding", "gzip");

                long connectStart = System.currentTimeMillis();
                request.connect();
                long connectTime = System.currentTimeMillis() - connectStart;
                LOGGER.debug("Connected in {}ms (Response code: {})", connectTime, request.getResponseCode());

                if (request.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    LOGGER.error("Failed to fetch data. HTTP Response Code: {}", request.getResponseCode());
                    return Collections.emptyList();
                }

                InputStream raw = request.getInputStream();
                InputStream in = request.getContentEncoding().equalsIgnoreCase("gzip") ? new GZIPInputStream(raw) : raw;

                long readStart = System.currentTimeMillis();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), 8192);
                StringBuilder rawData = new StringBuilder();
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    rawData.append(line);
                    lineCount++;
                }
                long readTime = System.currentTimeMillis() - readStart;
                LOGGER.debug("Finished reading {} lines in {}ms", lineCount, readTime);

                long parseStart = System.currentTimeMillis();
                try {
                    this.heads = GSON.fromJson(rawData.toString(), HeadMapper.HEADS_LIST_TYPE);
                    LOGGER.debug("Parsed {} heads", heads.size());
                } catch (Exception ex) {
                    LOGGER.error("Failed to parse fetched JSON!", ex);
                    return Collections.emptyList();
                }
                long parseTime = System.currentTimeMillis() - parseStart;

                // Indexing
                if (indexes != null) {
                    LOGGER.debug("Indexing heads...");
                    long indexStart = System.currentTimeMillis();

                    if (hasIndex(Index.ID)) {
                        this.byId = this.heads.stream().collect(Collectors.toMap(Head::getId, h -> h));
                        LOGGER.debug("Index by ID completed");
                    }

                    if (hasIndex(Index.TEXTURE)) {
                        this.byTexture = this.heads.stream().collect(Collectors.toMap(Head::getTexture, h -> h));
                        LOGGER.debug("Index by Texture completed");
                    }

                    if (hasIndex(Index.CATEGORY)) {
                        Map<String, List<Head>> rawCat = this.heads.stream().collect(Collectors.groupingBy(Head::getCategory));
                        this.byCategory = rawCat.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> Collections.unmodifiableList(e.getValue())
                                ));
                        LOGGER.debug("Index by Category completed");
                    }

                    if (hasIndex(Index.TAG)) {
                        Map<String, List<Head>> tagBuilder = new HashMap<>();
                        for (Head head : this.heads) {
                            for (String tag : head.getTags()) {
                                tagBuilder
                                        .computeIfAbsent(tag, k -> new ArrayList<>())
                                        .add(head);
                            }
                        }
                        this.byTag = tagBuilder.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> Collections.unmodifiableList(e.getValue())
                                ));
                        LOGGER.debug("Index by Tag completed");
                    }

                    long indexTime = System.currentTimeMillis() - indexStart;
                    LOGGER.debug("Indexing completed in {}ms", indexTime);
                }

                long elapsed = System.currentTimeMillis() - start;
                LOGGER.debug("Update took {} seconds ({}ms total, read={}, parse={}, index={})",
                        TimeUnit.MILLISECONDS.toSeconds(elapsed),
                        elapsed,
                        readTime,
                        parseTime,
                        (System.currentTimeMillis() - start) - readTime - parseTime);

                return Collections.unmodifiableList(this.heads);

            } catch (MalformedURLException ex) {
                LOGGER.error("Malformed URL!", ex);
                throw new CompletionException("Malformed URL", ex);
            } catch (IOException ex) {
                LOGGER.error("Failed to update heads!", ex);
                throw new CompletionException("Failed to update heads", ex);
            }
        }, executor);

        return lastUpdateFuture;
    }


    // ... rest of class unchanged ...

    /**
     * Blocks until the most recent update() completes (success or failure),
     * then returns true if it succeeded, or false if it failed.
     */
    @Override
    public boolean awaitReady() {
        try {
            lastUpdateFuture.join();
            return true;
        } catch (CompletionException ignored) {
            return false;
        }
    }

    /**
     * Non-blocking check: has the most recent update() finished (successfully or not)?
     */
    @Override
    public boolean isReady() {
        return lastUpdateFuture != null
                && lastUpdateFuture.isDone()
                && !lastUpdateFuture.isCompletedExceptionally()
                && !lastUpdateFuture.isCancelled();
    }

    @Override
    public CompletableFuture<List<Head>> onReady() {
        return Objects.requireNonNullElseGet(lastUpdateFuture, CompletableFuture::new);
    }

    @Override
    @Nullable
    public List<Head> getHeads() {
        List<Head> currentHeads = this.heads;
        if (currentHeads == null) {
            return null;
        }
        return Collections.unmodifiableList(currentHeads);
    }

    @Override
    @NotNull
    public List<Head> getByCategory(String category) {
        if (byCategory != null) {
            return byCategory.getOrDefault(category, Collections.emptyList());
        }
        
        List<Head> currentHeads = heads;
        if (currentHeads == null || currentHeads.isEmpty()) {
            return Collections.emptyList();
        }

        List<Head> result = new ArrayList<>();
        for (Head head : currentHeads) {
            if (category.equals(head.getCategory())) {
                result.add(head);
            }
        }
        return result;
    }

    @Override
    @NotNull
    public List<Head> getByTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }

        if (byTag != null) {
            Set<Head> resultSet = new LinkedHashSet<>();
            for (String t : tags) {
                if (t != null) {
                    resultSet.addAll(byTag.getOrDefault(t, Collections.emptyList()));
                }
            }
            return new ArrayList<>(resultSet);
        }

        List<Head> currentHeads = heads;
        if (currentHeads == null || currentHeads.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> tagSet = new HashSet<>(tags.length);
        for (String tag : tags) {
            if (tag != null) {
                tagSet.add(tag);
            }
        }
        
        List<Head> result = new ArrayList<>();
        for (Head head : currentHeads) {
            for (String hTag : head.getTags()) {
                if (tagSet.contains(hTag)) {
                    result.add(head);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    @Nullable
    public Head getById(int id) {
        if (byId != null) {
            return byId.get(id);
        }
        
        List<Head> currentHeads = heads;
        if (currentHeads == null) {
            return null;
        }
        
        for (Head head : currentHeads) {
            if (head.getId() == id) {
                return head;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Head getByTexture(String texture) {
        if (byTexture != null) {
            return byTexture.get(texture);
        }
        
        List<Head> currentHeads = heads;
        if (currentHeads == null) {
            return null;
        }
        
        for (Head head : currentHeads) {
            if (texture.equals(head.getTexture())) {
                return head;
            }
        }
        return null;
    }

    private boolean hasIndex(Index index) {
        if (indexes == null) {
            return false;
        }
        for (Index i : indexes) {
            if (i == index) {
                return true;
            }
        }
        return false;
    }
}