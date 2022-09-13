package com.gette;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.bytestream.ByteStreamGrpc.ByteStreamImplBase;
import com.google.bytestream.ByteStreamProto.ReadRequest;
import com.google.bytestream.ByteStreamProto.ReadResponse;
import com.google.bytestream.ByteStreamProto.WriteRequest;
import com.google.bytestream.ByteStreamProto.WriteResponse;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;

import build.bazel.remote.execution.v2.Digest;

import org.apache.commons.codec.digest.DigestUtils;

public class ByteStreamImpl extends ByteStreamImplBase {
    private static final Logger log = Logger.getLogger(ActionCacheImpl.class.getName());
    private final CacheStorage cas = CacheStorage.casStorage();

    @Override
    public void read(ReadRequest request,
    StreamObserver<ReadResponse> responseObserver) {
        
    }

    @Override
    public StreamObserver<WriteRequest> write(
        StreamObserver<WriteResponse> responseObserver) {
        log.info("BYTESTREAM Write Received...");
        
        return new StreamObserver<WriteRequest>() {
            ByteArrayOutputStream writeBuffer = new ByteArrayOutputStream();

            @Override
            public void onNext(WriteRequest request) {
                
                ByteString data = request.getData();
                ByteStreamWriteBuffer.getWriteBuffer(request.getResourceName()).write(data.toByteArray(), Math.toIntExact(request.getWriteOffset()), data.toByteArray().length);
                if (request.getFinishWrite()){
                    String hash = DigestUtils.sha256Hex(writeBuffer.toByteArray());
                    Digest digest = Digest.newBuilder()
                                          .setHash(hash)
                                          .setSizeBytes(writeBuffer.toByteArray().length)
                                          .build();
                    if (!cas.hasDigest(digest)) {
                        try(OutputStream out = Files.newOutputStream(cas.getStoragePath().resolve(hash))) {
                            writeBuffer.writeTo(out);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
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
                responseObserver.onError(t);
            }
      };
    }
}
