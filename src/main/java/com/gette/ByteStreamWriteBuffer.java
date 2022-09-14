package com.gette;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class ByteStreamWriteBuffer {
    private static final ConcurrentHashMap<String, Write> writeStatus = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, TreeMap<Integer, byte[]>> writeBlobs = new ConcurrentHashMap<>();

    public static TreeMap<Integer, byte[]> getWriteBuffer(String resource) {
        return writeBlobs.computeIfAbsent(resource, b -> new TreeMap<>());
    }

    public static Write getWriteStatus(String resource) {
        return writeStatus.computeIfAbsent(resource, b -> new Write(resource));
    }

    public static void removeWriteBuffer(String resource) {
        writeBlobs.remove(resource);
    }
}