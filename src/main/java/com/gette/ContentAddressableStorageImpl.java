package com.gette;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.google.protobuf.ByteString;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.FindMissingBlobsRequest;
import build.bazel.remote.execution.v2.FindMissingBlobsResponse;
import build.bazel.remote.execution.v2.BatchUpdateBlobsRequest;
import build.bazel.remote.execution.v2.BatchUpdateBlobsResponse;
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc.ContentAddressableStorageImplBase;

import io.grpc.stub.StreamObserver;

public class ContentAddressableStorageImpl extends ContentAddressableStorageImplBase{
    private static final Logger log = Logger.getLogger(ContentAddressableStorageImpl.class.getName());
    private final CacheStorage cache;

    public ContentAddressableStorageImpl(String cacheDir) throws IOException {
        super();
        cache = new CacheStorage(cacheDir);
    }

    @Override
    public void findMissingBlobs(FindMissingBlobsRequest request,
    StreamObserver<FindMissingBlobsResponse> responseObserver) {
       List<Digest> digests = request.getBlobDigestsList();
       log.info("FindMissingBlobs received...");

       responseObserver.onNext(FindMissingBlobsResponse.newBuilder()
       .addAllMissingBlobDigests(cache.findDigests(digests))
       .build());
       responseObserver.onCompleted();
    }

    @Override
    public void batchUpdateBlobs(BatchUpdateBlobsRequest request,
    StreamObserver<BatchUpdateBlobsResponse> responseObserver) {
        List<BatchUpdateBlobsRequest.Request> uploadRequests = request.getRequestsList();

        for (BatchUpdateBlobsRequest.Request uploadRequest : uploadRequests) {
            Digest digest = uploadRequest.getDigest();
            ByteString data = uploadRequest.getData();
            log.info("Received blob " + digest.getHash());
        }

        responseObserver.onNext(BatchUpdateBlobsResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

}
