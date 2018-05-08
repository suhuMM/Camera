package com.example.kedaxunfei;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

/**
 * @author suhu
 * @data 2018/5/7 0007.
 * @description
 */

public class App extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID+"=587ecd47");
    }
}
