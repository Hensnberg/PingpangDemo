package com.iflytek.mscv5plusdemo.grpc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.examples.convertContext.ConvertReply;
import io.grpc.examples.convertContext.ConvertRequest;
import io.grpc.examples.convertContext.UserServiceGrpc;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.Executors;

import static com.iflytek.mscv5plusdemo.grpc.GrpcChannel.getChannel;

public class ConvertServer {
    private UserServiceGrpc.UserServiceFutureStub futureStub;
    public ConvertServer(ManagedChannel channel){
        futureStub = UserServiceGrpc.newFutureStub(channel);
    }
    public void convert(String sendtext, final myCallBack callBack){
        if ("".equals(sendtext)||sendtext == null)
            throw new NullPointerException("sendtext no null！");
        final ConvertRequest request = ConvertRequest
                .newBuilder()
                .setSendtext(sendtext)
                .build();
        ListenableFuture<ConvertReply> future = futureStub.getConvert(request);
        //构建非阻塞调用，阻塞方式用BlockingStub
        Futures.addCallback(future, new FutureCallback<ConvertReply>() {
            @Override
            public void onSuccess(@NullableDecl ConvertReply result) {
                callBack.onSuccess(result.getAnswer());
            }
            @Override
            public void onFailure(Throwable t) {
                callBack.onFailure("网络异常!");
            }
        }, Executors.newCachedThreadPool());
    }

    public static void main(String[] args) {
        ConvertServer convertServer = new ConvertServer(getChannel());
    }
}
