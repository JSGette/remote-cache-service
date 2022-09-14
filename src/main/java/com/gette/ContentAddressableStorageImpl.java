package com.gette;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;

import com.google.rpc.Status;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.Directory;
import build.bazel.remote.execution.v2.FileNode;
import build.bazel.remote.execution.v2.FindMissingBlobsRequest;
import build.bazel.remote.execution.v2.FindMissingBlobsResponse;
import build.bazel.remote.execution.v2.GetTreeRequest;
import build.bazel.remote.execution.v2.GetTreeResponse;
import build.bazel.remote.execution.v2.BatchUpdateBlobsRequest;
import build.bazel.remote.execution.v2.BatchUpdateBlobsResponse;
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc.ContentAddressableStorageImplBase;

import io.grpc.stub.StreamObserver;

public class ContentAddressableStorageImpl extends ContentAddressableStorageImplBase {
    private static final Logger log = Logger.getLogger(ContentAddressableStorageImpl.class.getName());
    private final CacheStorage cache = CacheStorage.casStorage();

    @Override
    public void findMissingBlobs(FindMissingBlobsRequest request,
                                 StreamObserver<FindMissingBlobsResponse> responseObserver) {
        List<Digest> digests = request.getBlobDigestsList();
        log.info("FindMissingBlobs received...");
        List<Digest> foundDigests = cache.findDigests(digests);
        log.info("Found " + foundDigests.size() + " blobs");
        List<Digest> missingDigests = new ArrayList<>();
        missingDigests.addAll(digests);
        missingDigests.removeAll(foundDigests);
        responseObserver.onNext(FindMissingBlobsResponse.newBuilder()
                .addAllMissingBlobDigests(missingDigests)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void batchUpdateBlobs(BatchUpdateBlobsRequest request,
                                 StreamObserver<BatchUpdateBlobsResponse> responseObserver) {
        log.info("BatchUpdateBlobs received...");
        List<BatchUpdateBlobsRequest.Request> uploadRequests = request.getRequestsList();
        List<BatchUpdateBlobsResponse.Response> responses = new ArrayList<>();

        for (BatchUpdateBlobsRequest.Request uploadRequest : uploadRequests) {
            Digest digest = uploadRequest.getDigest();
            ByteString data = uploadRequest.getData();
            try (OutputStream out = Files.newOutputStream(cache.resolveDigestPath(digest))) {
                log.info("Received blob " + digest.getHash());
                data.writeTo(out);
                BatchUpdateBlobsResponse.Response response = BatchUpdateBlobsResponse.Response.newBuilder()
                        .setDigest(digest)
                        .setStatus(Status.newBuilder()
                                .setCode(0)
                                .build())
                        .build();
                responses.add(response);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        responseObserver.onNext(BatchUpdateBlobsResponse.newBuilder().addAllResponses(responses).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTree(GetTreeRequest request,
                        StreamObserver<GetTreeResponse> responseObserver) {
        log.info("GetTree received...");
        List<FileNode> files;
        Digest root = request.getRootDigest();
        Path rootInCas = cache.getStoragePath().resolve(root.getHash());
        if (Files.exists(rootInCas)) {
            GetTreeResponse response = GetTreeResponse.newBuilder()
                    .addAllDirectories(Arrays.asList())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Referred Blob does not exist in CAS....").asRuntimeException());
        }
    }

}
