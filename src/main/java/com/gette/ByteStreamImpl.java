package com.gette;

import com.google.bytestream.ByteStreamGrpc.ByteStreamImplBase;
import com.google.bytestream.ByteStreamProto.ReadRequest;
import com.google.bytestream.ByteStreamProto.ReadResponse;
import com.google.bytestream.ByteStreamProto.WriteRequest;
import com.google.bytestream.ByteStreamProto.WriteResponse;
import io.grpc.stub.StreamObserver;

public class ByteStreamImpl extends ByteStreamImplBase {

    @Override
    public void read(ReadRequest request,
    StreamObserver<ReadResponse> responseObserver) {

    }

    @Override
    public StreamObserver<WriteRequest> write(
        StreamObserver<WriteResponse> responseObserver) {
      return null;
    }
    
}
