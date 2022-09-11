package com.gette;

import java.util.List;

import build.bazel.remote.execution.v2.Digest;
import build.bazel.remote.execution.v2.FindMissingBlobsRequest;
import build.bazel.remote.execution.v2.FindMissingBlobsResponse;
import build.bazel.remote.execution.v2.ContentAddressableStorageGrpc.ContentAddressableStorageImplBase;

import io.grpc.stub.StreamObserver;

public class ContentAddressableStorageImpl extends ContentAddressableStorageImplBase{

    @Override
    public void findMissingBlobs(FindMissingBlobsRequest request, 
    StreamObserver<FindMissingBlobsResponse> responseObserver) {
       List<Digest> blobDigests = request.getBlobDigestsList();

       responseObserver.onNext(FindMissingBlobsResponse.newBuilder().build());
       responseObserver.onCompleted();
    }

}
