package com.gette;

import build.bazel.remote.execution.v2.Digest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CacheStorage {
    private static final Logger log = Logger.getLogger(CacheStorage.class.getName());
    private static final CacheStorage acStorage = new CacheStorage("/tmp/remote_cache/ac");
    private static final CacheStorage casStorage = new CacheStorage("/tmp/remote_cache/cas");
    private final Map<Digest, Path> cachedDigests;
    private Path cacheStoragePath;

    public CacheStorage(String cacheStoragePath) {
        try {
            log.info("Initializing Cache Storage...");
            this.cacheStoragePath = Paths.get(cacheStoragePath);
            log.info("Storage Path: " + cacheStoragePath);
            log.info("Looking for already exising blobs...");
            cachedDigests = walkFileTree(this.cacheStoragePath);
            log.info("Found " + cachedDigests.size() + " blobs...");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static CacheStorage acStorage() throws IOException {
        return acStorage;
    }

    public static CacheStorage casStorage() {
        return casStorage;
    }

    public Path getStoragePath() {
        return cacheStoragePath;
    }

    public List<Digest> findDigests(List<Digest> digests) {
        return digests.stream()
                .filter(digest -> hasDigest(digest))
                .collect(Collectors.toList());
    }

    public int getCachedDigestsCount() {
        return cachedDigests.size();
    }

    public boolean hasDigest(Digest digest) {
        log.fine("Looking for Digest with HASH: " + digest.getHash());
        if (cachedDigests.keySet().contains(digest)) {
            log.fine("Found Digest...");
            return true;
        }
        log.fine("Digest Not Found...");
        return false;
    }

    /*
     * GetActionResult sends size and hash of Action
     * but not the size of ActionResult leading
     * to NOT_FOUND every time.
     */
    public boolean hasHash(String hash) {
        if (cachedDigests.keySet().stream()
                .map(digest -> digest.getHash())
                .collect(Collectors.toList())
                .contains(hash)) {
            return true;
        } else {
            return false;
        }
    }

    public Map<Digest, Path> walkFileTree(Path rootPath) throws IOException {
        try (Stream<Path> stream = Files.find(rootPath, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            return stream.collect(Collectors.toMap(
                    file -> {
                        try {
                            return Digest.newBuilder()
                                    .setHash(file.getFileName().toString())
                                    .setSizeBytes(Files.size(file))
                                    .build();
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    },
                    file -> file,
                    (f1, f2) -> f2,
                    ConcurrentHashMap::new));
        }
    }

    public void putDigest(Digest digest, Path path) {
        log.fine("Adding HASH: " + digest.getHash() + " to PATH: " + path);
        cachedDigests.putIfAbsent(digest, path);
    }

    public Path resolveDigestPath(Digest digest) {
        return cacheStoragePath.resolve(digest.getHash());
    }
}
