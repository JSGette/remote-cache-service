package com.gette;

import com.gette.CapabilitiesImpl;
import com.gette.RemoteCache;

import build.bazel.remote.execution.v2.CapabilitiesGrpc;
import build.bazel.remote.execution.v2.GetCapabilitiesRequest;
import build.bazel.remote.execution.v2.ServerCapabilities;
import build.bazel.remote.execution.v2.CapabilitiesGrpc.CapabilitiesStub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

import io.grpc.testing.GrpcCleanupRule;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;

import java.io.IOException;

/* Test is based on this example:
 * https://github.com/grpc/grpc-java/blob/master/examples/src/test/java/io/grpc/examples/helloworld/HelloWorldServerTest.java
 *
 * but slightly adapted in order to be used with Junit 5 instead of Junit 4
 */
public class RemoteCacheTest {
    GrpcCleanupRule grpcCleanup;

    @BeforeEach
    public void initGrpcServer() {
        grpcCleanup = new GrpcCleanupRule();
    }

    @Test
    void getCapabilitiesImpl_test() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(InProcessServerBuilder
        .forName(serverName).directExecutor().addService(new CapabilitiesImpl()).build().start());

        CapabilitiesGrpc.CapabilitiesBlockingStub blockingStub = CapabilitiesGrpc.newBlockingStub(
            // Create a client channel and register for automatic graceful shutdown.
            grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        ServerCapabilities reply = blockingStub.getCapabilities(GetCapabilitiesRequest.newBuilder().build());

        Assertions.assertEquals(1, reply.getCacheCapabilities().getDigestFunctionsCount());
    }
}
