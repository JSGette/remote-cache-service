package com.gette;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import build.bazel.remote.execution.v2.ActionCacheGrpc.ActionCacheImplBase;
import build.bazel.remote.execution.v2.GetActionResultRequest;
import build.bazel.remote.execution.v2.UpdateActionResultRequest;
import build.bazel.remote.execution.v2.ActionResult;
import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.OutputFile;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import com.google.protobuf.ByteString;

import org.apache.commons.codec.digest.DigestUtils;

public class ActionCacheImpl extends ActionCacheImplBase {
    private static final Logger log = Logger.getLogger(ActionCacheImpl.class.getName());
    private final CacheStorage cache;

    public ActionCacheImpl(CacheStorage cache) {
        this.cache = cache;
    }

    @Override
    public void updateActionResult(UpdateActionResultRequest request,
                                   StreamObserver<ActionResult> responseObserver) {
        log.info("UpdateActionResult received...");
        Digest actionDigest = request.getActionDigest();
        Path actionResultPath = cache.resolveDigestPath(actionDigest);
        ActionResult result = request.getActionResult();
        //if (validateActionResult(result)) {

        try (OutputStream out = Files.newOutputStream(actionResultPath)) {
            log.info("Updating ActionResult with HASH: " + actionDigest.getHash());
            if (result.getExitCode() == 0) {
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
                            log.info("Inlined Blob size not 0...");
                            Path outputFileCASPath = casStoragePath.resolve(outputFileDigest.getHash());
                            try (OutputStream inlinedOut = Files.newOutputStream(outputFileCASPath)) {
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
                    try (OutputStream stdoutOut = Files.newOutputStream(stdoutCASPath)) {
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
                    try (OutputStream stderrOut = Files.newOutputStream(stderrCASPath)) {
                        stdout.writeTo(stderrOut);
                    }
                    casStorage.putDigest(stdoutDigest, stderrCASPath);
                    log.info("Stderr cached...");
                }
                log.info("Update complete...");
            }
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        /*} else {
            log.warning("Referred Blob does not exist in CAS");
            //responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Referred Blob does not exist in CAS....").asRuntimeException());
            responseObserver.onNext(ActionResult.newBuilder().build());
            responseObserver.onCompleted();
        } */
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
            responseObserver.onNext(actionResult);
            responseObserver.onCompleted();
        } else {
            log.warning("ActionResult Not Found...");
            responseObserver.onError(Status.NOT_FOUND.withDescription("ActionResult Not Found in AC...").asRuntimeException());
        }
    }

    public boolean validateActionResult(ActionResult result) {
        /*
        Rudimentary check. 
        If AC is referring to a non-existing blob
        this will lead to
        `Invalid action cache entry` error on the cliend side
        */
        for (OutputFile f : result.getOutputFilesList()) {
            if (!CacheStorage.casStorage().hasDigest(f.getDigest())) {
                return false;
            }
        }
        return true;
    }
}
