package io.github.silentdevelopment.headdb.core.database.refresh;

import io.github.silentdevelopment.headdb.core.compression.ZstdArtifactDecoder;
import io.github.silentdevelopment.headdb.core.database.DatabaseSnapshot;
import io.github.silentdevelopment.headdb.core.database.DefaultHeadDatabase;
import io.github.silentdevelopment.headdb.core.database.cache.CachedArtifact;
import io.github.silentdevelopment.headdb.core.database.cache.CachedDatabaseArtifacts;
import io.github.silentdevelopment.headdb.core.database.cache.DatabaseArtifactCache;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonCatalogPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationPartParser;
import io.github.silentdevelopment.headdb.core.database.parse.GsonRevocationsIndexParser;
import io.github.silentdevelopment.headdb.core.database.parse.ParsedCatalogIndex;
import io.github.silentdevelopment.headdb.core.database.parse.ParsedRevocationsIndex;
import io.github.silentdevelopment.headdb.core.database.revocation.HeadRevocation;
import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.core.hash.Sha256Verifier;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelection;
import io.github.silentdevelopment.headdb.core.remote.ArtifactSelector;
import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.core.remote.RemoteContentTypes;
import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import io.github.silentdevelopment.headdb.core.remote.RemoteResourceId;
import io.github.silentdevelopment.headdb.core.remote.http.RemoteHttpClient;
import io.github.silentdevelopment.headdb.core.remote.parse.RemoteManifestParser;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.model.Head;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public final class DatabaseRefreshService {

    private final DefaultHeadDatabase database;
    private final URI manifestUri;
    private final RemoteHttpClient httpClient;
    private final RemoteManifestParser manifestParser;
    private final ArtifactSelector artifactSelector;
    private final Sha256Verifier sha256Verifier;
    private final ZstdArtifactDecoder zstdDecoder;
    private final GsonCatalogIndexParser catalogIndexParser;
    private final GsonCatalogPartParser catalogPartParser;
    private final GsonRevocationsIndexParser revocationsIndexParser;
    private final GsonRevocationPartParser revocationPartParser;
    private final Clock clock;
    private final DatabaseArtifactCache artifactCache;
    private final AtomicBoolean running;
    private final AtomicReference<RemoteManifest> activeManifest;

    public DatabaseRefreshService(@NotNull DefaultHeadDatabase database, @NotNull URI manifestUri, @NotNull RemoteHttpClient httpClient, @NotNull RemoteManifestParser manifestParser, @NotNull ArtifactSelector artifactSelector, @NotNull Sha256Verifier sha256Verifier, @NotNull ZstdArtifactDecoder zstdDecoder, @NotNull GsonCatalogIndexParser catalogIndexParser, @NotNull GsonCatalogPartParser catalogPartParser, @NotNull GsonRevocationsIndexParser revocationsIndexParser, @NotNull GsonRevocationPartParser revocationPartParser, @NotNull Clock clock, @NotNull DatabaseArtifactCache artifactCache) {
        this.database = Objects.requireNonNull(database, "database");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.manifestParser = Objects.requireNonNull(manifestParser, "manifestParser");
        this.artifactSelector = Objects.requireNonNull(artifactSelector, "artifactSelector");
        this.sha256Verifier = Objects.requireNonNull(sha256Verifier, "sha256Verifier");
        this.zstdDecoder = Objects.requireNonNull(zstdDecoder, "zstdDecoder");
        this.catalogIndexParser = Objects.requireNonNull(catalogIndexParser, "catalogIndexParser");
        this.catalogPartParser = Objects.requireNonNull(catalogPartParser, "catalogPartParser");
        this.revocationsIndexParser = Objects.requireNonNull(revocationsIndexParser, "revocationsIndexParser");
        this.revocationPartParser = Objects.requireNonNull(revocationPartParser, "revocationPartParser");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.artifactCache = Objects.requireNonNull(artifactCache, "artifactCache");
        this.running = new AtomicBoolean(false);
        this.activeManifest = new AtomicReference<>();
        if (!manifestUri.isAbsolute()) {
            throw new IllegalArgumentException("Manifest URI must be absolute.");
        }
    }

    public boolean loadCached() throws IOException {
        begin();
        try {
            Optional<CachedDatabaseArtifacts> cached = artifactCache.load();
            if (cached.isEmpty()) {
                return false;
            }

            database.markLoading(DatabaseSource.CACHE);

            try {
                DatabaseSnapshot snapshot = parseArtifacts(cached.get(), DatabaseSource.CACHE);
                database.replace(snapshot);
                activeManifest.set(cached.get().manifest());
                return true;
            } catch (IOException | RuntimeException exception) {
                database.markFailed(refreshError(exception));
                throw exception;
            }
        } finally {
            running.set(false);
        }
    }

    public @NotNull DatabaseSnapshot refresh() throws IOException, InterruptedException {
        begin();
        try {
            database.markLoading(DatabaseSource.REMOTE);

            try {
                CachedDatabaseArtifacts artifacts = loadRemoteArtifacts();
                DatabaseSnapshot snapshot = parseArtifacts(artifacts, DatabaseSource.REMOTE);
                artifactCache.save(artifacts);
                database.replace(snapshot);
                activeManifest.set(artifacts.manifest());
                return snapshot;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                database.markFailed("Database refresh was interrupted.");
                throw exception;
            } catch (IOException | RuntimeException exception) {
                database.markFailed(refreshError(exception));
                throw exception;
            }
        } finally {
            running.set(false);
        }
    }

    public @NotNull DatabaseRefreshResult refreshIfChanged() throws IOException, InterruptedException {
        begin();

        try {
            RemoteManifestDocument document;

            try {
                document = loadRemoteManifest();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                if (activeManifest.get() == null) {
                    database.markFailed("Database refresh was interrupted.");
                }

                throw exception;
            } catch (IOException | RuntimeException exception) {
                if (activeManifest.get() == null) {
                    database.markFailed(refreshError(exception));
                }

                throw exception;
            }

            RemoteManifest currentManifest = activeManifest.get();
            if (currentManifest != null && sameDatabaseResources(currentManifest, document.manifest())) {
                activeManifest.set(document.manifest());
                return DatabaseRefreshResult.UNCHANGED;
            }

            database.markLoading(DatabaseSource.REMOTE);

            try {
                CachedDatabaseArtifacts artifacts = loadRemoteArtifacts(document);
                DatabaseSnapshot snapshot = parseArtifacts(artifacts, DatabaseSource.REMOTE);
                artifactCache.save(artifacts);
                database.replace(snapshot);
                activeManifest.set(artifacts.manifest());
                return DatabaseRefreshResult.UPDATED;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                database.markFailed("Database refresh was interrupted.");
                throw exception;
            } catch (IOException | RuntimeException exception) {
                database.markFailed(refreshError(exception));
                throw exception;
            }
        } finally {
            running.set(false);
        }
    }

    public @NotNull DatabaseSnapshot verifyRemote() throws IOException, InterruptedException {
        begin();

        try {
            CachedDatabaseArtifacts artifacts = loadRemoteArtifacts();
            return parseArtifacts(artifacts, DatabaseSource.REMOTE);
        } finally {
            running.set(false);
        }
    }

    private void begin() {
        if (running.compareAndSet(false, true)) {
            return;
        }
        throw new IllegalStateException("Database load or refresh is already running.");
    }

    private @NotNull RemoteManifestDocument loadRemoteManifest() throws IOException, InterruptedException {
        String manifestJson = httpClient.getText(manifestUri);
        RemoteManifest manifest = manifestParser.parse(manifestJson);
        return new RemoteManifestDocument(manifestJson, manifest);
    }

    private @NotNull CachedDatabaseArtifacts loadRemoteArtifacts() throws IOException, InterruptedException {
        return loadRemoteArtifacts(loadRemoteManifest());
    }

    private @NotNull CachedDatabaseArtifacts loadRemoteArtifacts(@NotNull RemoteManifestDocument document) throws IOException, InterruptedException {
        Objects.requireNonNull(document, "document");
        String manifestJson = document.json();
        RemoteManifest manifest = document.manifest();
        CachedArtifact catalogIndex = downloadSelected(artifactSelector.selectCatalogIndex(manifest), RemoteContentTypes.CATALOG_INDEX);
        ParsedCatalogIndex parsedCatalogIndex = catalogIndexParser.parse(decode(catalogIndex.selection().artifact(), catalogIndex.bytes()));
        List<CachedArtifact> catalogParts = downloadParts(manifest, RemoteResourceId.CATALOG, parsedCatalogIndex.artifacts(), RemoteContentTypes.CATALOG_PART);
        CachedArtifact revocationsIndex = downloadSelected(artifactSelector.selectRevocationsIndex(manifest), RemoteContentTypes.REVOCATIONS_INDEX);
        ParsedRevocationsIndex parsedRevocationsIndex = revocationsIndexParser.parse(decode(revocationsIndex.selection().artifact(), revocationsIndex.bytes()));
        List<CachedArtifact> revocationParts = downloadParts(manifest, RemoteResourceId.REVOCATIONS, parsedRevocationsIndex.artifacts(), RemoteContentTypes.REVOCATIONS_PART);
        return new CachedDatabaseArtifacts(manifestJson, manifest, catalogIndex, catalogParts, revocationsIndex, revocationParts);
    }

    private boolean sameDatabaseResources(@NotNull RemoteManifest current, @NotNull RemoteManifest remote) {
        return current.id().equals(remote.id())
                && current.catalogResource().index().equals(remote.catalogResource().index())
                && current.revocationsResource().index().equals(remote.revocationsResource().index());
    }

    private @NotNull DatabaseSnapshot parseArtifacts(@NotNull CachedDatabaseArtifacts artifacts, @NotNull DatabaseSource source) throws IOException {
        Objects.requireNonNull(artifacts, "artifacts");
        Objects.requireNonNull(source, "source");

        verifyArtifact(artifacts.revocationsIndex(), RemoteContentTypes.REVOCATIONS_INDEX);
        ParsedRevocationsIndex revocationsIndex = revocationsIndexParser.parse(decode(artifacts.revocationsIndex().selection().artifact(), artifacts.revocationsIndex().bytes()));

        if (revocationsIndex.artifacts().size() != artifacts.revocationParts().size()) {
            throw new IllegalStateException("Cached revocation part count does not match revocations index.");
        }

        List<HeadRevocation> revocations = new ArrayList<>();
        for (CachedArtifact part : artifacts.revocationParts()) {
            verifyArtifact(part, RemoteContentTypes.REVOCATIONS_PART);
            revocations.addAll(revocationPartParser.parse(decode(part.selection().artifact(), part.bytes())));
        }

        RevocationSet revocationSet = new RevocationSet(revocations);

        verifyArtifact(artifacts.catalogIndex(), RemoteContentTypes.CATALOG_INDEX);
        ParsedCatalogIndex catalogIndex = catalogIndexParser.parse(decode(artifacts.catalogIndex().selection().artifact(), artifacts.catalogIndex().bytes()));

        if (catalogIndex.artifacts().size() != artifacts.catalogParts().size()) {
            throw new IllegalStateException("Cached catalog part count does not match catalog index.");
        }

        List<Head> heads = new ArrayList<>();
        for (CachedArtifact part : artifacts.catalogParts()) {
            verifyArtifact(part, RemoteContentTypes.CATALOG_PART);
            for (Head head : catalogPartParser.parse(decode(part.selection().artifact(), part.bytes()))) {
                if (revocationSet.contains(head.id())) {
                    continue;
                }
                heads.add(head);
            }
        }

        return new DatabaseSnapshot(artifacts.manifest().id(), catalogIndex.id(), source, Instant.now(clock), heads, catalogIndex.categories(), catalogIndex.tags(), catalogIndex.collections(), revocationSet);
    }

    private @NotNull List<CachedArtifact> downloadParts(@NotNull RemoteManifest manifest, @NotNull String resourceId, @NotNull List<RemoteArtifact> artifacts, @NotNull String contentType) throws IOException, InterruptedException {
        List<CachedArtifact> parts = new ArrayList<>();
        for (RemoteArtifact artifact : artifacts) {
            parts.add(downloadSelected(artifactSelector.selectPart(manifest, resourceId, artifact), contentType));
        }
        return List.copyOf(parts);
    }

    private @NotNull CachedArtifact downloadSelected(@NotNull ArtifactSelection selection, @NotNull String contentType) throws IOException, InterruptedException {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(contentType, "contentType");
        requireContentType(selection.artifact(), contentType);
        byte[] bytes = httpClient.getBytes(selection.url());
        CachedArtifact cached = new CachedArtifact(selection, bytes);
        verifyArtifact(cached, contentType);
        return cached;
    }

    private void verifyArtifact(@NotNull CachedArtifact cached, @NotNull String contentType) {
        Objects.requireNonNull(cached, "cached");
        requireContentType(cached.selection().artifact(), contentType);
        RemoteArtifact artifact = cached.selection().artifact();
        byte[] bytes = cached.bytes();
        if (bytes.length != artifact.integrity().bytes()) {
            throw new IllegalStateException("Byte size verification failed for artifact: " + artifact.id());
        }
        if (sha256Verifier.verify(bytes, artifact.sha256())) {
            return;
        }
        throw new IllegalStateException("SHA-256 verification failed for artifact: " + artifact.id());
    }

    private @NotNull String decode(@NotNull RemoteArtifact artifact, byte @NotNull [] bytes) throws IOException {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(bytes, "bytes");
        if (artifact.compression().equals(RemoteArtifact.COMPRESSION_NONE)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (artifact.compression().equals(RemoteArtifact.COMPRESSION_ZSTD)) {
            return zstdDecoder.decodeString(bytes);
        }
        throw new IOException("Unsupported artifact compression: " + artifact.compression());
    }

    private void requireContentType(@NotNull RemoteArtifact artifact, @NotNull String expected) {
        if (artifact.contentType().equals(expected)) {
            return;
        }
        throw new IllegalArgumentException("Unexpected content type for artifact " + artifact.id() + ": expected " + expected + " but found " + artifact.contentType());
    }

    private @NotNull String refreshError(@NotNull Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private record RemoteManifestDocument(@NotNull String json, @NotNull RemoteManifest manifest) {

        private RemoteManifestDocument {
            Objects.requireNonNull(json, "json");
            Objects.requireNonNull(manifest, "manifest");
        }
    }
}
