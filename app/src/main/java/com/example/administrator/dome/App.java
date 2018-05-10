package com.example.administrator.dome;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author suhu
 * @data 2018/5/7 0007.
 * @description
 */

public class App extends Application{

    private static ThreadPoolExecutor poolExecutor;

    @Override
    public void onCreate() {
        super.onCreate();
        SpeechUtility.createUtility(getApplicationContext(), SpeechConstant.APPID+"=587ecd47");

        poolExecutor = new ThreadPoolExecutor(4,6,1, TimeUnit.SECONDS,new LinkedBlockingDeque<Runnable>(128));
    }

    /**
     * 获得线程池
     * @return ThreadPoolExecutor
     */
    public static ThreadPoolExecutor getThreadPool(){
        return poolExecutor;
    }

}
