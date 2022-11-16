package com.iflytek.mscv5plusdemo.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcChannel {
    public static final String SERVER_IP = "192.168.137.1";
    private static final int PORT = 8282;
    public static ManagedChannel getChannel(){
        return ManagedChannelBuilder.forAddress(SERVER_IP,PORT)
                .usePlaintext().build();
    }
}
