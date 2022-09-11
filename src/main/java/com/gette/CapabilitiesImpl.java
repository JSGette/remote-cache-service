package com.gette;


import build.bazel.remote.execution.v2.ActionCacheUpdateCapabilities;
import build.bazel.remote.execution.v2.CacheCapabilities;
import build.bazel.remote.execution.v2.DigestFunction;
import build.bazel.remote.execution.v2.ExecutionCapabilities;
import build.bazel.remote.execution.v2.GetCapabilitiesRequest;
import build.bazel.remote.execution.v2.PriorityCapabilities;
import build.bazel.remote.execution.v2.ServerCapabilities;
import build.bazel.remote.execution.v2.SymlinkAbsolutePathStrategy;
import build.bazel.remote.execution.v2.CapabilitiesGrpc.CapabilitiesImplBase;
import build.bazel.remote.execution.v2.PriorityCapabilities.PriorityRange;
import io.grpc.stub.StreamObserver;

public class CapabilitiesImpl extends CapabilitiesImplBase{

    @Override
    public void getCapabilities(GetCapabilitiesRequest request, 
    StreamObserver<ServerCapabilities> responseObserver) {
        System.out.println("We are here");
        ServerCapabilities serverCapabilities = ServerCapabilities.newBuilder()
        .setCacheCapabilities(getCacheCapabilities())
        .setExecutionCapabilities(getExecutionCapabilities())
        .build();
        responseObserver.onNext(serverCapabilities);
        responseObserver.onCompleted();
    }

    protected CacheCapabilities getCacheCapabilities() {
        return CacheCapabilities.newBuilder()
        .addDigestFunctions(DigestFunction.Value.SHA256)
        .setActionCacheUpdateCapabilities(getActionCacheUpdateCapabilities())
        //Disabling symlinks to simplify
        .setSymlinkAbsolutePathStrategy(SymlinkAbsolutePathStrategy.Value.DISALLOWED)
        .build();
    }

    //Explicitly disabling Remote Execution
    protected ExecutionCapabilities getExecutionCapabilities() {
        return ExecutionCapabilities.newBuilder()
        .setExecEnabled(false)
        .build();
    }

    //Disabling AC Update to keep it as simple as possible
    protected ActionCacheUpdateCapabilities getActionCacheUpdateCapabilities() {
        return ActionCacheUpdateCapabilities.newBuilder()
            .setUpdateEnabled(false)
            .build();
    }
}