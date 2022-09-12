package com.gette;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import build.bazel.remote.execution.v2.ActionCacheGrpc.ActionCacheImplBase;
import build.bazel.remote.execution.v2.GetActionResultRequest;
import build.bazel.remote.execution.v2.UpdateActionResultRequest;
import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.OutputFile;

import io.grpc.stub.StreamObserver;

import com.google.protobuf.ByteString;

import org.apache.commons.codec.digest.DigestUtils;

public class ActionCacheImpl extends ActionCacheImplBase {
    private static final Logger log = LoggerFactory.getLogger(ActionCacheImpl.class);
    private final CacheStorage cache;

    public ActionCacheImpl() throws IOException {
        super();
        cache = CacheStorage.acStorage();
    }

    @Override
    public void updateActionResult(UpdateActionResultRequest request, 
    StreamObserver<ActionResult> responseObserver) {
        Digest actionDigest = request.getActionDigest();
        Path actionResultPath = cache.resolveDigestPath(actionDigest);
        ActionResult result = request.getActionResult();
        log.info("UpdateActionResult received...");
        try (OutputStream out = Files.newOutputStream(actionResultPath)) {
            log.info("Updating ActionResult with HASH: " + actionDigest.getHash());
            result.writeTo(out);
            cache.putDigest(actionDigest, actionResultPath);
            log.info("Cache inlined blobs to CAS...");
            CacheStorage casStorage = CacheStorage.casStorage();
            List<OutputFile> inlinedOutputFiles = result.getOutputFilesList();
            if (inlinedOutputFiles.size() > 0) {
                Path casStoragePath = casStorage.getStoragePath();
                for (OutputFile outputFile : inlinedOutputFiles) {
                    Digest outputFileDigest = outputFile.getDigest();
                    if (!casStorage.hasDigest(outputFileDigest) && outputFile.getContents().size() > 0) {
                        Path outputFileCASPath = casStoragePath.resolve(outputFileDigest.getHash());
                        try(OutputStream inlinedOut = Files.newOutputStream(outputFileCASPath)) {
                            log.info("================================");
                            log.info(outputFileDigest.getHash());
                            log.info(outputFile.getPath());
                            log.info(String.valueOf(outputFile.getContents().size()));
                            log.info("================================");
                            outputFile.getContents().writeTo(inlinedOut);
                        }
                        casStorage.putDigest(outputFileDigest, outputFileCASPath);
                        log.info("Inlined blob cached...");
                    }
                }
            }
            ByteString stdout = result.getStdoutRaw();
            Digest stdoutDigest = result.getStdoutDigest();
            if (!stdout.isEmpty() && !casStorage.hasDigest(stdoutDigest)) {
                //TODO: Check if hash from Digest equals hash of the contents
                log.info("Cache stdout...");
                Path stdoutCASPath = casStorage.getStoragePath().resolve(stdoutDigest.getHash());
                try(OutputStream stdoutOut = Files.newOutputStream(stdoutCASPath)) {
                    stdout.writeTo(stdoutOut);
                }
                casStorage.putDigest(stdoutDigest, stdoutCASPath);
                log.info("Stdout cached...");
            }
            ByteString stderr = result.getStderrRaw();
            Digest stderrDigest = result.getStderrDigest();
            if (!stderr.isEmpty() && !casStorage.hasDigest(stderrDigest)) {
                log.info("Cache stderr...");
                Path stderrCASPath = casStorage.getStoragePath().resolve(stderrDigest.getHash());
                try(OutputStream stderrOut = Files.newOutputStream(stderrCASPath)) {
                    stdout.writeTo(stderrOut);
                }
                casStorage.putDigest(stdoutDigest, stderrCASPath);
                log.info("Stderr cached...");                
            }
            log.info("Update complete...");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void writeContentToFile(ByteString data, Path path) {
        try(OutputStream out = Files.newOutputStream(path)) {
            data.writeTo(out);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void getActionResult(GetActionResultRequest request, 
    StreamObserver<ActionResult> responseObserver) {
        ActionResult actionResult;
        Digest actionDigest = request.getActionDigest();
        log.info("GetActionResult received...");
        log.info("ActionResult Hash: " + actionDigest.getHash());
        if (cache.hasDigest(actionDigest)) {
            log.info("ActionResult Found...");
            try (InputStream in = Files.newInputStream(cache.resolveDigestPath(request.getActionDigest()))) {
                actionResult = ActionResult.parseFrom(ByteString.readFrom(in));
            } catch (IOException exception) {
                throw new RuntimeException();
            }

        } else {
            log.info("ActionResult Not Found...");
            actionResult = null;
        }
        
        responseObserver.onNext(actionResult);
        responseObserver.onCompleted();
    }
}
