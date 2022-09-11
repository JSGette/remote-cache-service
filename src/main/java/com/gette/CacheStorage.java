package com.gette;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

import build.bazel.remote.execution.v2.Digest;

public final class CacheStorage {
    private static final Logger log = Logger.getLogger(CacheStorage.class.getName());
    private final Map<Digest, Path> cachedDigests;
    private Path cacheStoragePath;

    public CacheStorage(String cacheStoragePath) throws IOException {
        log.info("Initializing Cache Storage...");
        this.cacheStoragePath = Paths.get(cacheStoragePath);
        log.info("Storage Path: " + cacheStoragePath);
        log.info("Looking for already exising blobs...");
        cachedDigests = walkFileTree();
        log.info("Found " + cachedDigests.size() + " blobs...");
    }

    public List<Digest> findDigests(List<Digest> digests) {
        try (Stream<Digest> stream = digests.stream()) {
            return stream.filter(digest -> hasDigest(digest))
            .collect(Collectors.toList());
        }
    }

    public int getCachedDigestsCount() {
        return cachedDigests.size();
    }

    public boolean hasDigest(Digest digest){
        log.info("Looking for Digest with HASH: " + digest.getHash() + " and SIZE: " + digest.getSizeBytes());
        if (cachedDigests.keySet().contains(digest)) {
            log.info("Found Digest...");
            return true;
        }
        log.info("Digest Not Found...");
        return false;
    }

    private Map<Digest, Path> walkFileTree() throws IOException {
        try (Stream<Path> stream = Files.find(cacheStoragePath, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            return stream.collect(Collectors.toMap(
                file -> {
                    try (InputStream is = Files.newInputStream(file)) {
                        return Digest.newBuilder()
                        .setHash(DigestUtils.sha256Hex(is))
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
}
