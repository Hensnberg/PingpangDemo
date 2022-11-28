package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import com.aliyun.player.IPlayer;
import com.aliyun.player.nativeclass.PlayerConfig;
import com.aliyun.player.source.UrlSource;
import com.aliyun.vodplayerview.constants.PlayParameter;
import com.aliyun.vodplayerview.widget.AliyunVodPlayerView;
import com.iflytek.cloud.*;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.speech.setting.IatSettings;
import com.iflytek.speech.setting.TtsSettings;
import com.iflytek.speech.util.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

//import butterknife.BindView;
//import butterknife.ButterKnife;

public class IatDemo extends Activity implements OnClickListener {

    private static final int PROT = 6565;
    private static final String HOST = "10.10.20.124";

    private static String TAG = "IatDemo";

//    @BindView(R.id.btn_play)
    Button btnPlay;
//    @BindView(R.id.linear_layout)
    LinearLayout mLinearLayout;

    private AliyunVodPlayerView mAliyunVodPlayerView;
    private View mView;
    private FrameLayout.LayoutParams layoutParams;

    private int videoPlayHeight;



    // 语音听写对象
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 听写结果内容
    private EditText mResultText;
    private EditText mResultText_answer;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<>();
    private SharedPreferences mSharedPreferences;
    private Toast mToast;
    private String mEngineType = "cloud";

    // 语音合成对象
    private SpeechSynthesizer mTts;
    private SharedPreferences mSharedPreferences_ts;

    public static String voicerCloud = "xiaoyan";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.iatdemo);
        initLayout();


        initAliyunPlayerView();
        //playVideo();//播放视频

        initEvent();





        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(this, mInitListener);
        mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        mResultText = ((EditText) findViewById(R.id.iat_text));
        mResultText_answer = ((EditText) findViewById(R.id.iat_text_answer));


        //语音合成
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);

        mSharedPreferences_ts = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
    }

    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.iat_recognize).setOnClickListener(this);
        findViewById(R.id.iat_stop).setOnClickListener(this);
        findViewById(R.id.iat_cancel).setOnClickListener(this);

        mEngineType = SpeechConstant.TYPE_CLOUD;
    }

    int ret = 0;// 函数调用返回值

    @Override
    public void onClick(View view) {
        if (null == mIat) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
            return;
        }

        switch (view.getId()) {
            // 开始听写
            // 如何判断一次听写结束：OnResult isLast=true 或者 onError
            case R.id.iat_recognize:
                mResultText.setText(null);// 清空显示内容
                mIatResults.clear();

                mResultText_answer.setText(null);// 清空结果显示内容
                // 设置参数
                setParam();
                mIatDialog.setListener(mRecognizerDialogListener);
                mIatDialog.show();
                showTip(getString(R.string.text_begin));
                break;
            // 停止听写
            case R.id.iat_stop:
                mIat.stopListening();
                showTip("停止听写");
                break;
            // 取消听写
            case R.id.iat_cancel:
                mIat.cancel();
                showTip("取消听写");
                break;
            default:
                break;
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };


    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, "recognizer result：" + results.getResultString());

            String text = JsonParser.parseIatResult(results.getResultString());
            mResultText.append(text);
            mResultText.setSelection(mResultText.length());
            if(isLast){
                return;
            }
            System.out.println(text);
            //text="比赛中发完小三角得球后续怎么处理 ";
            String res = new MainActivity().startClient(HOST, PROT, text);
            if(res.contains("https")){
                playVideo(res);
            }else {
                mResultText_answer.append(res);
                mResultText_answer.setSelection(mResultText.length());

                // 设置参数
                setTTSParam();
                Log.d(TAG, "准备点击： " + System.currentTimeMillis());
                int code = mTts.startSpeaking(res, mTtsListener);

                if (code != ErrorCode.SUCCESS) {
                    showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                }
            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };


    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
                mToast.show();
            }
        });
    }

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
        // 设置引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        //mIat.setParameter(MscKeys.REQUEST_AUDIO_URL,"true");

        //	this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
        // 在线听写支持多种小语种，若想了解请下载在线听写能力，参看其speechDemo
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

    }

    private void setTTSParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置使用云端引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicerCloud);
        //mTts.setParameter(SpeechConstant.TTS_DATA_NOTIFY,"1");//支持实时音频流抛出，仅在synthesizeToUri条件下支持
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
        //	mTts.setParameter(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC+"");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH,
                getExternalFilesDir("msc").getAbsolutePath() + "/tts.pcm");


    }

    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet"));
        tempBuffer.append(";");
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/sms_16k.jet"));
        //识别8k资源-使用8k的时候请解开注释
        return tempBuffer.toString();
    }


    @Override
    protected void onDestroy() {
        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();

        }
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        super.onDestroy();
    }

    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");

            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            Log.d(IatDemo.TAG, "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int i, int i1, int i2, String s) {

        }

        @Override
        public void onSpeakProgress(int i, int i1, int i2) {

        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else {
                showTip(error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "session id =" + sid);
            }

        }
    };

    private void initEvent() {


        //以下为几个常用监听回调
        mAliyunVodPlayerView.setOnPlayStateBtnClickListener(new AliyunVodPlayerView.OnPlayStateBtnClickListener() {
            @Override
            public void onPlayBtnClick(int playerState) {
                if (playerState == IPlayer.started) {
                    Log.i(TAG, "onPlayBtnClick: 暂停");
                } else if (playerState == IPlayer.paused) {
                    Log.i(TAG, "onPlayBtnClick: 播放");
                } else {
                    Log.i(TAG, "onPlayBtnClick: else");
                }
            }
        });

        mAliyunVodPlayerView.setOnCompletionListener(new IPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                Log.i(TAG, "onCompletion:  视频正常播放完成");
            }
        });
        mAliyunVodPlayerView.setOnFirstFrameStartListener(new IPlayer.OnRenderingStartListener() {
            @Override
            public void onRenderingStart() {
                Log.i(TAG, "onRenderingStart: 视频第一帧开始");
            }
        });


        //网络连接监听
        mAliyunVodPlayerView.setNetConnectedListener(new AliyunVodPlayerView.NetConnectedListener() {
            @Override
            public void onReNetConnected(boolean isReconnect) {

            }

            @Override
            public void onNetUnConnected() {

            }
        });


    }

    private void playVideo(String url) {


        UrlSource urlSource = new UrlSource();
        urlSource.setCoverPath("http://image.test.laodao.so/image/course/course/5cf047c0-ba7d-11e9-b949-23e7199f12a0.jpg");
        urlSource.setTitle("小窗口视频测试");
        urlSource.setUri(url);

        PlayerConfig playerConfig = mAliyunVodPlayerView.getPlayerConfig();
        //默认是5000
        int maxDelayTime = 5000;
        if (PlayParameter.PLAY_PARAM_URL.startsWith("artp")) {
            //如果url的开头是artp，将直播延迟设置成100，
            maxDelayTime = 100;
        }
        playerConfig.mMaxDelayTime = maxDelayTime;
        mAliyunVodPlayerView.setPlayerConfig(playerConfig);
        mAliyunVodPlayerView.setLocalSource(urlSource);


//        PlayerConfig playerConfig = mAliyunVodPlayerView.getPlayerConfig();
//        //默认是5000
//        int maxDelayTime = 5000;
//        if (PlayParameter.PLAY_PARAM_URL.startsWith("artp")) {
//            //如果url的开头是artp，将直播延迟设置成100，
//            maxDelayTime = 100;
//        }
//        playerConfig.mMaxDelayTime = maxDelayTime;
//        mAliyunVodPlayerView.setPlayerConfig(playerConfig);
//        mAliyunVodPlayerView.setLocalSource(urlSource);


//        VidAuth vidAuth = new VidAuth();
//        vidAuth.setVid(PlayParameter.PLAY_PARAM_VID);
//        vidAuth.setRegion(PlayParameter.PLAY_PARAM_REGION);
//        String palyauth=getVideoPlayAuth(PlayParameter.PLAY_PARAM_VID);
//        vidAuth.setPlayAuth(palyauth);
//        //vidAuth.setTitle(PlayParameter.PLAY_PARAM_TYPE_AUTH_TITLE);
//        //vidAuth.setCoverPath(PlayParameter.PLAY_PARAM_TYPE_AUTH_COVER_PATH);
//
//        mAliyunVodPlayerView.setAuthInfo(vidAuth);
    }

//    private String getVideoPlayAuth(String vid){
//        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai",
//                "LTAI5tMGNRYqdKw8RKKLQuBX",
//                "jkIaHmcoEOdc9eueWFiEq5uUjbvjOi");
//        /** use STS Token
//         DefaultProfile profile = DefaultProfile.getProfile(
//         "<your-region-id>",           // The region ID
//         "<your-access-key-id>",       // The AccessKey ID of the RAM account
//         "<your-access-key-secret>",   // The AccessKey Secret of the RAM account
//         "<your-sts-token>");          // STS Token
//         **/
//
//        IAcsClient client = new DefaultAcsClient(profile);
//
//
//        GetVideoPlayAuthRequest request = new GetVideoPlayAuthRequest();
//        request.setVideoId(vid);
//        request.setAuthInfoTimeout((long)3000);
//
//        try {
//            GetVideoPlayAuthResponse response = client.getAcsResponse(request);
//            String playAuth = response.getPlayAuth();
//            System.out.println(new Gson().toJson(response));
//            return playAuth;
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            System.out.println("ErrCode:" + e.getErrCode());
//            System.out.println("ErrMsg:" + e.getErrMsg());
//            System.out.println("RequestId:" + e.getRequestId());
//        }
//        return null;
//    }

    private void initAliyunPlayerView() {


        String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test_save_cache";
        File file = new File(sdDir);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }


        mAliyunVodPlayerView = (AliyunVodPlayerView) findViewById(R.id.video_view);
        //mView = new View(this);

        //保持屏幕敞亮
        mAliyunVodPlayerView.setKeepScreenOn(true);
        //mAliyunVodPlayerView.setKeepScreenOn(true);
        mAliyunVodPlayerView.setPlayingCache(true, sdDir, 60 * 60 /*时长, s */, 300 /*大小，MB*/);
        mAliyunVodPlayerView.setTheme(AliyunVodPlayerView.Theme.Green);
        mAliyunVodPlayerView.setCirclePlay(false);//是否循环播放
        mAliyunVodPlayerView.setAutoPlay(true);//是否自动播放
        mAliyunVodPlayerView.setTitleBarCanShow(false);
        //mAliyunVodPlayerView.setRenderRotate(IPlayer.RotateMode.ROTATE_90);


        layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, videoPlayHeight);
        //mLinearLayout.addView(mAliyunVodPlayerView, 0, layoutParams);


    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(true);
            mAliyunVodPlayerView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAliyunVodPlayerView != null) {
            mAliyunVodPlayerView.setAutoPlay(false);
            mAliyunVodPlayerView.onStop();
        }
    }
}
