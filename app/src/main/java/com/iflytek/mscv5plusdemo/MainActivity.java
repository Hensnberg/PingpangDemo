package com.iflytek.mscv5plusdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.examples.convertContext.ConvertReply;
import io.grpc.examples.convertContext.ConvertRequest;
import io.grpc.examples.convertContext.UserServiceGrpc;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {


    private static final int PROT = 6565;
    private static final String HOST = "10.10.20.124";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        initView();
        requestPermissions();
        //startClient(HOST, PROT, SENDTEXT);
    }

    private void initView() {
        TextView tipTv = (TextView) findViewById(R.id.tip);
        String buf = getString(R.string.example_explain);
        tipTv.setText(buf);
        // 语音转写
        findViewById(R.id.iatBtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IatDemo.class));
        });
        // 语音合成
        findViewById(R.id.ttsBtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TtsDemo.class));
        });
        // 唤醒
        findViewById(R.id.ivwBtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IvwActivity.class));
        });
    }

    private Toast mToast;

    private void showTip(final String str) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void requestPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECORD_AUDIO
                }, 0x0010);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    ////
    private void startServer(int port){
        try {
            Server server = NettyServerBuilder.forPort(port)
                    .addService(new UserImpl())
                    .build()
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
            //Log.d(TAG, e.getMessage());
        }
    }

    public String startClient(String host, int port, String name){
        String s = new GrpcTask(host, port, name).doInBackground();
        return s;
    }

    private class UserImpl extends UserServiceGrpc.UserServiceImplBase {
        public void getConvert(ConvertRequest request, StreamObserver<ConvertReply> responseObserver) {
            responseObserver.onNext(getConvert(request));
            responseObserver.onCompleted();
        }

        private ConvertReply getConvert(ConvertRequest request) {
            return ConvertReply.newBuilder()
                    .setAnswer(request.getSendtext())
                    .build();
        }
    }

    private class GrpcTask extends AsyncTask<Void, Void, String> {
        private String mHost;
        private String msendText;
        private int mPort;
        private ManagedChannel mChannel;

        public GrpcTask(String host, int port, String sendText) {
            this.mHost = host;
            this.msendText = sendText;
            this.mPort = port;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                mChannel = ManagedChannelBuilder.forAddress(mHost, mPort)
                        .usePlaintext()
                        .build();
                UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(mChannel);
                ConvertRequest message = ConvertRequest.newBuilder().setSendtext(msendText).build();
                ConvertReply reply = stub.getConvert(message);
                return reply.getAnswer();
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return "Failed... : "  + sw;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            //Log.d(TAG, result);
        }
    }

}
