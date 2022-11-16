package com.iflytek.mscv5plusdemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.iflytek.cloud.*;
import com.iflytek.speech.setting.TtsSettings;

public class TtsDemo extends Activity implements OnClickListener {
    private static String TAG = TtsDemo.class.getSimpleName();
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认云端发音人
    public static String voicerCloud = "xiaoyan";

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private Toast mToast;
    private SharedPreferences mSharedPreferences;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.ttsdemo);
        initLayout();

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);

        mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
    }

    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.tts_play).setOnClickListener(this);

        findViewById(R.id.tts_cancel).setOnClickListener(this);
        findViewById(R.id.tts_pause).setOnClickListener(this);
        findViewById(R.id.tts_resume).setOnClickListener(this);
        mEngineType = SpeechConstant.TYPE_CLOUD;
    }

    @Override
    public void onClick(View view) {
        if (null == mTts) {
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
            return;
        }

        switch (view.getId()) {
            // 开始合成
            // 收到onCompleted 回调时，合成结束、生成合成音频
            // 合成的音频格式：只支持pcm格式
            case R.id.tts_play:
                String text = ((EditText) findViewById(R.id.tts_text)).getText().toString();
                // 设置参数
                setParam();
                Log.d(TAG, "准备点击： " + System.currentTimeMillis());
                int code = mTts.startSpeaking(text, mTtsListener);

                if (code != ErrorCode.SUCCESS) {
                    showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
                }
                break;
            // 取消合成
            case R.id.tts_cancel:
                mTts.stopSpeaking();
                break;
            // 暂停播放
            case R.id.tts_pause:
                mTts.pauseSpeaking();
                break;
            // 继续播放
            case R.id.tts_resume:
                mTts.resumeSpeaking();
                break;
        }
    }




    /**
     * 初始化监听。
     */
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
            Log.d(TtsDemo.TAG, "开始播放：" + System.currentTimeMillis());
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

            //实时音频流输出参考
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
				byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
				Log.e("MscSpeechLog", "buf is =" + buf);
			}*/
        }
    };

    private void showTip(final String str) {
        runOnUiThread(() -> {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    /**
     * 参数设置
     */
    private void setParam() {
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

    @Override
    protected void onDestroy() {
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
        super.onDestroy();
    }
}
