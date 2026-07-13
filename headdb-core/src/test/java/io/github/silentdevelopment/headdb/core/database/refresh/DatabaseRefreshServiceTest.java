package io.github.silentdevelopment.headdb.core.database.refresh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
import io.github.silentdevelopment.headdb.core.database.DefaultHeadDatabase;
import io.github.silentdevelopment.headdb.core.database.cache.DatabaseArtifactCache;
import io.github.silentdevelopment.headdb.core.database.cache.InMemoryDatabaseArtifactCache;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.hash.Sha256Verifier;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.http.RemoteHttpClient;
import io.github.silentdevelopment.headdb.core.remote.parse.GsonRemoteManifestParser;
import io.github.silentdevelopment.headdb.core.test.TestDistribution;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.database.DatabaseState;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.model.HeadId;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class DatabaseRefreshServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private final Sha256Verifier sha256Verifier = new Sha256Verifier();

    @Test
    void refreshesDatabaseFromRemoteArtifacts() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);
        DatabaseSnapshot snapshot = service.refresh();
        assertEquals(DatabaseState.LOADED, database.status().state());
        assertEquals(DatabaseSource.REMOTE, database.status().source());
        assertEquals(NOW, database.status().loadedAt());
        assertEquals("heads", database.status().manifestId());
        assertEquals("catalog", database.status().artifactId());
        assertEquals(new DatabaseStats(2, 2, 2, 1, 1), database.stats());
        assertEquals(snapshot.stats(), database.stats());
        assertTrue(database.findById(HeadId.remote(1)).isPresent());
        assertTrue(database.findById(HeadId.remote(2)).isEmpty());
        assertTrue(database.findById(HeadId.remote(3)).isPresent());
    }

    @Test
    void marksDatabaseFailedWhenManifestDownloadFails() {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        httpClient.failText(TestDistribution.MANIFEST_URI, new IOException("manifest unavailable"));
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);
        assertThrows(IOException.class, service::refresh);
        assertEquals(DatabaseState.FAILED, database.status().state());
        assertEquals("manifest unavailable", database.status().lastError());
    }

    @Test
    void marksDatabaseFailedWhenCatalogPartShaDoesNotMatch() {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        httpClient.bytes(TestDistribution.CATALOG_PART_URI, "corrupt".getBytes(StandardCharsets.UTF_8));
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);
        assertThrows(IllegalStateException.class, service::refresh);
        assertEquals(DatabaseState.FAILED, database.status().state());
        assertEquals("Byte size verification failed for artifact: catalog-part-0000", database.status().lastError());
    }

    @Test
    void keepsPreviousSnapshotWhenRefreshFailsAfterSuccessfulLoad() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);
        service.refresh();
        httpClient.bytes(TestDistribution.CATALOG_INDEX_URI, "corrupt".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, service::refresh);
        assertEquals(DatabaseState.LOADED, database.status().state());
        assertTrue(database.findById(HeadId.remote(1)).isPresent());
    }

    @Test
    void rejectsRelativeManifestUri() {
        assertThrows(IllegalArgumentException.class, () -> service(new DefaultHeadDatabase(), URI.create("manifest.json"), new FakeRemoteHttpClient(), new InMemoryDatabaseArtifactCache()));
    }

    @Test
    void savesArtifactsAfterSuccessfulRefresh() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        InMemoryDatabaseArtifactCache artifactCache = new InMemoryDatabaseArtifactCache();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient, artifactCache);
        service.refresh();
        assertTrue(artifactCache.load().isPresent());
        assertEquals("heads", artifactCache.load().orElseThrow().manifest().id());
        assertEquals("catalog-index", artifactCache.load().orElseThrow().catalogIndex().selection().artifact().id());
        assertEquals("revocations-index", artifactCache.load().orElseThrow().revocationsIndex().selection().artifact().id());
    }

    @Test
    void skipsArtifactDownloadsWhenManifestResourcesAreUnchanged() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);

        service.refresh();
        int artifactRequests = httpClient.byteRequestCount();
        httpClient.text(TestDistribution.MANIFEST_URI, distribution.manifestJson.replace("\"revision\": 1", "\"revision\": 2"));

        DatabaseRefreshResult result = service.refreshIfChanged();

        assertEquals(DatabaseRefreshResult.UNCHANGED, result);
        assertEquals(artifactRequests, httpClient.byteRequestCount());
        assertEquals(2, httpClient.textRequestCount());
        assertEquals(DatabaseState.LOADED, database.status().state());
    }

    @Test
    void downloadsArtifactsWhenManifestResourcesChange() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution initial = TestDistribution.create();
        register(httpClient, initial);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);

        service.refresh();
        int artifactRequests = httpClient.byteRequestCount();

        TestDistribution updated = TestDistribution.createMultiPart();
        register(httpClient, updated);
        DatabaseRefreshResult result = service.refreshIfChanged();

        assertEquals(DatabaseRefreshResult.UPDATED, result);
        assertTrue(httpClient.byteRequestCount() > artifactRequests);
        assertEquals(DatabaseState.LOADED, database.status().state());
    }

    @Test
    void loadsCachedArtifacts() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        InMemoryDatabaseArtifactCache artifactCache = new InMemoryDatabaseArtifactCache();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase remoteDatabase = new DefaultHeadDatabase();
        DatabaseRefreshService remoteService = service(remoteDatabase, httpClient, artifactCache);
        remoteService.refresh();
        DefaultHeadDatabase cachedDatabase = new DefaultHeadDatabase();
        DatabaseRefreshService cachedService = service(cachedDatabase, new FakeRemoteHttpClient(), artifactCache);
        assertTrue(cachedService.loadCached());
        assertEquals(DatabaseState.LOADED, cachedDatabase.status().state());
        assertEquals(DatabaseSource.CACHE, cachedDatabase.status().source());
        assertEquals(new DatabaseStats(2, 2, 2, 1, 1), cachedDatabase.stats());
        assertTrue(cachedDatabase.findById(HeadId.remote(1)).isPresent());
        assertTrue(cachedDatabase.findById(HeadId.remote(2)).isEmpty());
        assertTrue(cachedDatabase.findById(HeadId.remote(3)).isPresent());
    }


    @Test
    void verifyRemoteDoesNotReplaceActiveDatabase() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);

        DatabaseSnapshot snapshot = service.verifyRemote();

        assertEquals(new DatabaseStats(2, 2, 2, 1, 1), snapshot.stats());
        assertEquals(DatabaseState.NOT_LOADED, database.status().state());
        assertEquals(DatabaseSource.NONE, database.status().source());
        assertTrue(database.findById(HeadId.remote(1)).isEmpty());
    }

    @Test
    void verifyRemoteDoesNotSaveCache() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        InMemoryDatabaseArtifactCache artifactCache = new InMemoryDatabaseArtifactCache();
        TestDistribution distribution = TestDistribution.create();
        register(httpClient, distribution);
        DatabaseRefreshService service = service(new DefaultHeadDatabase(), httpClient, artifactCache);

        service.verifyRemote();

        assertTrue(artifactCache.load().isEmpty());
    }

    @Test
    void refreshesMultiPartRemoteArtifacts() throws Exception {
        FakeRemoteHttpClient httpClient = new FakeRemoteHttpClient();
        TestDistribution distribution = TestDistribution.createMultiPart();
        register(httpClient, distribution);
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);

        service.refresh();

        assertEquals(new DatabaseStats(2, 2, 2, 1, 1), database.stats());
        assertTrue(database.findById(HeadId.remote(1)).isPresent());
        assertTrue(database.findById(HeadId.remote(2)).isEmpty());
        assertTrue(database.findById(HeadId.remote(3)).isPresent());
    }

    @Test
    void rejectsConcurrentVerifyDuringRefresh() throws Exception {
        BlockingRemoteHttpClient httpClient = new BlockingRemoteHttpClient();
        DatabaseRefreshService service = service(new DefaultHeadDatabase(), httpClient);

        Thread thread = new Thread(() -> {
            try {
                service.refresh();
            } catch (IOException | InterruptedException ignored) {
            }
        });

        thread.start();
        httpClient.awaitStarted();

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::verifyRemote);
        assertEquals("Database load or refresh is already running.", exception.getMessage());

        httpClient.release();
        thread.join();
    }

    @Test
    void rejectsConcurrentRefresh() throws Exception {
        BlockingRemoteHttpClient httpClient = new BlockingRemoteHttpClient();
        DefaultHeadDatabase database = new DefaultHeadDatabase();
        DatabaseRefreshService service = service(database, httpClient);

        Thread thread = new Thread(() -> {
            try {
                service.refresh();
            } catch (IOException | InterruptedException ignored) {
            }
        });

        thread.start();
        httpClient.awaitStarted();

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::refresh);
        assertEquals("Database load or refresh is already running.", exception.getMessage());

        httpClient.release();
        thread.join();
    }

    private DatabaseRefreshService service(DefaultHeadDatabase database, RemoteHttpClient httpClient) {
        return service(database, TestDistribution.MANIFEST_URI, httpClient, new InMemoryDatabaseArtifactCache());
    }

    private DatabaseRefreshService service(DefaultHeadDatabase database, RemoteHttpClient httpClient, DatabaseArtifactCache artifactCache) {
        return service(database, TestDistribution.MANIFEST_URI, httpClient, artifactCache);
    }

    private DatabaseRefreshService service(DefaultHeadDatabase database, URI manifestUri, RemoteHttpClient httpClient, DatabaseArtifactCache artifactCache) {
        return new DatabaseRefreshService(database, manifestUri, httpClient, new GsonRemoteManifestParser(), new ArtifactSelector("primary"), sha256Verifier, new ZstdArtifactDecoder(), new GsonCatalogIndexParser(), new GsonCatalogPartParser(), new GsonRevocationsIndexParser(), new GsonRevocationPartParser(), Clock.fixed(NOW, ZoneOffset.UTC), artifactCache);
    }

    private static void register(FakeRemoteHttpClient httpClient, TestDistribution distribution) {
        httpClient.text(TestDistribution.MANIFEST_URI, distribution.manifestJson);
        for (Map.Entry<URI, byte[]> entry : distribution.artifacts.entrySet()) {
            httpClient.bytes(entry.getKey(), entry.getValue());
        }
    }

    private static final class FakeRemoteHttpClient implements RemoteHttpClient {

        private final Map<URI, String> textResponses = new HashMap<>();
        private final Map<URI, byte[]> byteResponses = new HashMap<>();
        private final Map<URI, IOException> textFailures = new HashMap<>();
        private int textRequests;
        private int byteRequests;

        private void text(URI uri, String text) {
            textResponses.put(uri, text);
        }

        private void bytes(URI uri, byte[] bytes) {
            byteResponses.put(uri, bytes);
        }

        private void failText(URI uri, IOException exception) {
            textFailures.put(uri, exception);
        }

        private int textRequestCount() {
            return textRequests;
        }

        private int byteRequestCount() {
            return byteRequests;
        }

        @Override
        public @NotNull String getText(@NotNull URI uri) throws IOException {
            textRequests++;

            if (textFailures.containsKey(uri)) {
                throw textFailures.get(uri);
            }
            if (!textResponses.containsKey(uri)) {
                throw new IOException("No fake text response for " + uri);
            }
            return textResponses.get(uri);
        }

        @Override
        public byte @NotNull [] getBytes(@NotNull URI uri) throws IOException {
            byteRequests++;

            if (!byteResponses.containsKey(uri)) {
                throw new IOException("No fake byte response for " + uri);
            }
            return byteResponses.get(uri);
        }
    }

    private static final class BlockingRemoteHttpClient implements RemoteHttpClient {

        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private void awaitStarted() throws InterruptedException {
            started.await();
        }

        private void release() {
            release.countDown();
        }

        @Override
        public @NotNull String getText(@NotNull URI uri) throws IOException {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting.", exception);
            }
            throw new IOException("Stopped after concurrency assertion.");
        }

        @Override
        public byte @NotNull [] getBytes(@NotNull URI uri) throws IOException {
            throw new IOException("No bytes expected.");
        }
    }

}
