package com.gette;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import build.bazel.remote.execution.v2.Digest;

import org.apache.commons.codec.digest.DigestUtils;

public final class CacheStorage {
    static final Logger log = Logger.getLogger(CacheStorage.class.getName());
    private ArrayList<Path> filesInCache = new ArrayList<>();
    private ArrayList<Digest> digestsInCache = new ArrayList<>();
    private Path cacheStoragePath;

    public CacheStorage(String cacheStoragePath) throws IOException {
        log.info("Initializing Cache Storage...");
        this.cacheStoragePath = Paths.get(cacheStoragePath);
        log.info("Storage Path: " + cacheStoragePath);
        walkThroughFileTree();
    }

    public boolean findDigest(Digest digest) throws IOException {
        log.info("Looking for Digest with HASH: " + digest.getHash() + " and SIZE: " + digest.getSizeBytes());
        if (digestsInCache.contains(digest)) {
            log.info("Found Digest...");
            return true;
        }
        log.info("Digest Not Found...");
        return false;
    }
    
    private void walkThroughFileTree() throws IOException {
        log.info("Walking through the cache directory...");
        filesInCache.addAll(Files.find(cacheStoragePath, Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile()).collect(Collectors.toList()));
        log.info("Adding existing files to index...");
        log.info("Files in cache: " + filesInCache.size());
        if (filesInCache.size() > 0) {
            log.info("Calculating digests...");
            for (Path filePath: filesInCache) {
                digestsInCache.add(Digest.newBuilder()
                .setHash(DigestUtils.sha256Hex(new FileInputStream(filePath.toString())))
                .setSizeBytes(filePath.toFile().length())
                .build());
            }
        }
        log.info("Done calculating digests...");
        log.info("Digests in cache: " + digestsInCache.size());
    }
}
