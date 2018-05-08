package com.example.kedaxunfei;

import android.content.Context;
import android.os.Bundle;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

/**
 * @author suhu
 * @data 2018/5/7 0007.
 * @description
 */

public class VoiceUtils {

    private static VoiceUtils mVoice;
    public static VoiceUtils getInstance(){
        synchronized (VoiceUtils.class){
            if(null==mVoice){
                mVoice=new VoiceUtils();
            }
            return mVoice;
        }
    }

    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 默认发音人
    private String voicer = "xiaoyan";
    //初始化监听器
    private InitListener Listener;
    //播放监听器SynthesizerListener
    private SynthesizerListener mSynthesizerListener;

    public void initmTts(Context context, final String msg){

        if(null==Listener){
            Listener=new InitListener() {
                @Override
                public void onInit(int code) {

                    if (code != ErrorCode.SUCCESS) {

                    } else {
                        // 初始化成功，之后可以调用startSpeaking方法
                        // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                        // 正确的做法是将onCreate中的startSpeaking调用移至这里

                        mTts.startSpeaking(msg,mSynthesizerListener);
                    }
                }
            };
        }

        if(null==mSynthesizerListener){
            mSynthesizerListener= new SynthesizerListener() {
                @Override
                public void onSpeakBegin() {

                }

                @Override
                public void onBufferProgress(int i, int i1, int i2, String s) {

                }

                @Override
                public void onSpeakPaused() {

                }

                @Override
                public void onSpeakResumed() {

                }

                @Override
                public void onSpeakProgress(int i, int i1, int i2) {

                }

                @Override
                public void onCompleted(SpeechError speechError) {

                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {

                }
            };
        }

        if(null==mTts){
            mTts=SpeechSynthesizer.createSynthesizer(context, Listener);
            mTts.setParameter(SpeechConstant.VOICE_NAME,voicer);
            mTts.setParameter(SpeechConstant.SPEED,"50");
            mTts.setParameter(SpeechConstant.VOLUME,"80");
            mTts.setParameter(SpeechConstant.ENGINE_TYPE,SpeechConstant.TYPE_CLOUD);
        }else {
            mTts.startSpeaking(msg,mSynthesizerListener);
        }
    }

}
