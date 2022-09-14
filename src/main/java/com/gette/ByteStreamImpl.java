package com.gette;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.bytestream.ByteStreamGrpc.ByteStreamImplBase;
import com.google.bytestream.ByteStreamProto.ReadRequest;
import com.google.bytestream.ByteStreamProto.ReadResponse;
import com.google.bytestream.ByteStreamProto.WriteRequest;
import com.google.bytestream.ByteStreamProto.WriteResponse;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;

import org.apache.commons.codec.digest.DigestUtils;

import build.bazel.remote.execution.v2.Digest;

public class ByteStreamImpl extends ByteStreamImplBase {
    private static final Logger log = Logger.getLogger(ActionCacheImpl.class.getName());
    private final CacheStorage cas = CacheStorage.casStorage();

    @Override
    public void read(ReadRequest request,
    StreamObserver<ReadResponse> responseObserver) {
        log.info("BYTESTREAM Read Received...");
        String resourceName = request.getResourceName();
        try {
            //request format: blobs/{hash}/{size}
            Path pathToResource = cas.getStoragePath().resolve(resourceName.split("/")[1]);
            log.info("Resource HASH: " + resourceName.split("/")[1]);
            if (Files.exists(pathToResource)) {
                byte[] data = Files.readAllBytes(pathToResource);
                responseObserver.onNext(ReadResponse.newBuilder().setData(ByteString.copyFrom(data)).build());
                responseObserver.onCompleted();
            }
        } catch (IOException exception) {
            throw new RuntimeException();
        }
    }

    @Override
    public StreamObserver<WriteRequest> write(
        StreamObserver<WriteResponse> responseObserver) {
        log.info("BYTESTREAM Write Received...");
        return new StreamObserver<WriteRequest>(){
            String resourceName;
            //Set buffer size to 16 mb
            ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream(16*1024*1024);
        
            @Override
            public void onNext(WriteRequest request) {
                    if (resourceName == null && !request.getResourceName().isEmpty()) {
                        resourceName = request.getResourceName();
                    }
                    byte[] data = request.getData().toByteArray();
                    int offset = (int) request.getWriteOffset();
                    try {
                        writeBuffer.write(data);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                    if (request.getFinishWrite()) {
                        Digest digest = Digest.newBuilder()
                                              .setHash(DigestUtils.sha256Hex(writeBuffer.toByteArray()))
                                              .setSizeBytes(writeBuffer.size())
                                              .build();
                        if (!cas.hasDigest(digest)) {
                            try (OutputStream out = Files.newOutputStream(cas.resolveDigestPath(digest))) {
                                writeBuffer.writeTo(out);
                            } catch (IOException exception) {
                                throw new RuntimeException();
                            }
                        }
                        responseObserver.onNext(WriteResponse.newBuilder().setCommittedSize(writeBuffer.toByteArray().length).build());
                    }
            }
        
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        
            @Override
            public void onError(Throwable t) {
            }
        };
    }
}
