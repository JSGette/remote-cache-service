package com.gette;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.FindMissingBlobsRequest;
import build.bazel.remote.execution.v2.FindMissingBlobsResponse;
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

}
