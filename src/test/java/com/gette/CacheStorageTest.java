package com.gette;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assertions;

import com.gette.CacheStorage;

import build.bazel.remote.execution.v2.Digest;

import org.apache.commons.codec.digest.DigestUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CacheStorageTest {
    HashMap<String, String> tmpFiles = new HashMap<>();
    ArrayList<Digest> testDigests = new ArrayList<>();
    static final Logger log = Logger.getLogger(CacheStorageTest.class.getName());
    CacheStorage cacheStorage;

    @BeforeAll
    public void setupCache(@TempDir Path tempDir) throws FileNotFoundException, IOException {
        tmpFiles.put("4f9c3633e8859bbe74114c4f82aa23ada90dc9a7b59643fd36451239ee1163ea", "this is test");
        tmpFiles.put("9e9bf89c0b50c80417b6720e2525f70492ff442ea19d2cc5858fd8b575725829", "this is also test");
        tmpFiles.put("e687c197cf99724bcbcc6f25d57f6165193bf8a34d1d922a33bc5b882734a388", "and this is also test");
        
        /*
         * Creating set of test files with random content
         * and calculate their SHA-256
         */
        for (Map.Entry<String, String> entry : tmpFiles.entrySet()) {
            String path = tempDir + "/" + entry.getKey();
            String content = entry.getValue();
            PrintWriter writer = new PrintWriter(new FileWriter(Paths.get(path).toFile()));
            writer.print(content);
            writer.close();
            log.info("FILE: " + path);
            log.info("HASH: " + DigestUtils.sha256Hex(new FileInputStream(path)));
            testDigests.add(Digest.newBuilder()
            .setHash(DigestUtils.sha256Hex(new FileInputStream(path)))
            .setSizeBytes(Paths.get(path).toFile().length())
            .build());
            /*
            Path pathToTmpFile = tempDir.resolve(tmpFile);
            Files.createFile(pathToTmpFile);
            byte[] bytes = new byte[8];
            new Random().nextBytes(bytes);
            String randomString = new String(bytes);
            log.info("STRING: " + randomString);
            PrintWriter writer = new PrintWriter(new FileWriter(pathToTmpFile.toFile()));
            writer.println(randomString);
            writer.close();
            String checksumSha256 = DigestUtils.sha256Hex(new FileInputStream(pathToTmpFile.toString()));
            Digest digest = Digest.newBuilder()
            .setHash(checksumSha256)
            .setSizeBytes(pathToTmpFile.toFile().length())
            .build();
            digestsInCache.add(digest);
            log.info("HASH: " + digest.getHash() + " SIZE: " + digest.getSerializedSize());
             */
        }

        cacheStorage = new CacheStorage(tempDir.toString());
    }

    @Test
    public void findDigest_test() throws IOException{
        Assertions.assertTrue(cacheStorage.findDigest(testDigests.get(0)));

        Digest nonExistingDigest = Digest.newBuilder()
        .setHash("2589f1fa898bfaf10b8b631ad3bf29d8d9c7ef113237718f72e88fad4a5bc2d8")
        .setSizeBytes(23)
        .build();
        
       Assertions.assertFalse(cacheStorage.findDigest(nonExistingDigest));
    }
    
}
