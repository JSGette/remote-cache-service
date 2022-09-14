package com.gette;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.bytestream.ByteStreamGrpc.ByteStreamImplBase;
import com.google.bytestream.ByteStreamProto.QueryWriteStatusRequest;
import com.google.bytestream.ByteStreamProto.QueryWriteStatusResponse;
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

    /*@Override
    public void read(ReadRequest request,
    StreamObserver<ReadResponse> responseObserver) {
        
    }*/

    /*@Override
    public void queryWriteStatus(QueryWriteStatusRequest request,
    StreamObserver<QueryWriteStatusResponse> responseObserver) {
        Write status = ByteStreamWriteBuffer.getWriteStatus(request.getResourceName());
        responseObserver.onNext(QueryWriteStatusResponse.newBuilder()
                                       .setCommittedSize(status.getCommitedSize())
                                       .setComplete(status.isCompleted())
                                       .build());
        responseObserver.onCompleted();
    }*/

    @Override
    public StreamObserver<WriteRequest> write(
        StreamObserver<WriteResponse> responseObserver) {
        log.info("BYTESTREAM Write Received...");
        
        return new StreamObserver<WriteRequest>() {
            Write status;
            TreeMap<Integer, byte[]> byteBuffer;
            String resourceName;
            Digest digest;

            @Override
            public void onNext(WriteRequest request) {
                if (resourceName == null && !request.getResourceName().isEmpty()) {
                    resourceName = request.getResourceName();
                    status = ByteStreamWriteBuffer.getWriteStatus(resourceName);
                    byteBuffer = ByteStreamWriteBuffer.getWriteBuffer(resourceName);
                }
                byte[] data = request.getData().toByteArray();
                int offset = (int) request.getWriteOffset();

                byteBuffer.put(offset, data);
                status.setCompleted(request.getFinishWrite());
                status.setCommitedSize(byteBuffer.keySet().stream().mapToInt(Integer::intValue).sum());
                responseObserver.onNext(WriteResponse.newBuilder().setCommittedSize(status.getCommitedSize()).build());
                if (status.isCompleted()) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    for (byte[] piece: byteBuffer.values()) {
                        outputStream.writeBytes(piece);
                    }
                    String hash = DigestUtils.sha256Hex(outputStream.toByteArray());
                    digest = Digest.newBuilder()
                                          .setHash(hash)
                                          .setSizeBytes(outputStream.toByteArray().length).build();
                    if (!cas.hasDigest(digest)) {
                        try(OutputStream out = Files.newOutputStream(cas.getStoragePath().resolve(hash))) {
                            out.write(outputStream.toByteArray());
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                    onCompleted();
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
