package com.gette;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;

import com.google.rpc.Status;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.FindMissingBlobsRequest;
import build.bazel.remote.execution.v2.FindMissingBlobsResponse;
import build.bazel.remote.execution.v2.BatchUpdateBlobsRequest;
import build.bazel.remote.execution.v2.BatchUpdateBlobsResponse;
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc.ContentAddressableStorageImplBase;

import io.grpc.stub.StreamObserver;

public class ContentAddressableStorageImpl extends ContentAddressableStorageImplBase{
    private static final Logger log = Logger.getLogger(ContentAddressableStorageImpl.class.getName());
    private final CacheStorage cache = CacheStorage.casStorage();

    @Override
    public void findMissingBlobs(FindMissingBlobsRequest request,
    StreamObserver<FindMissingBlobsResponse> responseObserver) {
       List<Digest> digests = request.getBlobDigestsList();
       log.info("FindMissingBlobs received...");
       List<Digest> foundDigests = cache.findDigests(digests);
       log.info("Found " + foundDigests.size() + " blobs");
       responseObserver.onNext(FindMissingBlobsResponse.newBuilder()
       .addAllMissingBlobDigests(foundDigests)
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

}
