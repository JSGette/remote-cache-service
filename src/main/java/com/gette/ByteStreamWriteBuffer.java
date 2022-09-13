package com.gette;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ByteStreamWriteBuffer {
    private static final ConcurrentHashMap<String, ByteArrayOutputStream> writeBlobs = new ConcurrentHashMap<>();

    public static ByteArrayOutputStream getWriteBuffer(String blob) {
        if (!writeBlobs.containsKey(blob)) {
            writeBlobs.putIfAbsent(blob, new ByteArrayOutputStream());
        }
        return writeBlobs.get(blob);
    }
}