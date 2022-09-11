package com.gette;


import java.util.logging.Logger;

import build.bazel.remote.execution.v2.ActionCacheUpdateCapabilities;
import build.bazel.remote.execution.v2.CacheCapabilities;
import build.bazel.remote.execution.v2.CapabilitiesGrpc.CapabilitiesImplBase;
import build.bazel.remote.execution.v2.DigestFunction;
import build.bazel.remote.execution.v2.ExecutionCapabilities;
import build.bazel.remote.execution.v2.GetCapabilitiesRequest;
import build.bazel.remote.execution.v2.ServerCapabilities;
import build.bazel.remote.execution.v2.SymlinkAbsolutePathStrategy;
import io.grpc.stub.StreamObserver;

import build.bazel.semver.SemVer;

public class CapabilitiesImpl extends CapabilitiesImplBase{
    private static final Logger log = Logger.getLogger(CapabilitiesImpl.class.getName());

    @Override
    public void getCapabilities(GetCapabilitiesRequest request, 
    StreamObserver<ServerCapabilities> responseObserver) {
        log.info("GetCapabilities received...");
        ServerCapabilities serverCapabilities = ServerCapabilities.newBuilder()
        .setCacheCapabilities(getCacheCapabilities())
        .setExecutionCapabilities(getExecutionCapabilities())
        .setLowApiVersion(SemVer.newBuilder()
            .setMajor(0)
            .setMinor(0)
            .setPatch(0)
            .build())
        .setHighApiVersion(SemVer.newBuilder()
            .setMajor(2)
            .setMinor(0)
            .setPatch(0)
            .build())
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

    //Excplitily disabling AC Update to keep it as simple as possible
    protected ActionCacheUpdateCapabilities getActionCacheUpdateCapabilities() {
        return ActionCacheUpdateCapabilities.newBuilder()
            .setUpdateEnabled(false)
            .build();
    }
}
