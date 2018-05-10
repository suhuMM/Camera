package com.example.administrator.dome;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * @author suhu
 * @data 2018/5/9 0009.
 * @description
 */

public class WeakRefHandler extends Handler{
    private WeakReference<Callback> reference;

    public WeakRefHandler(Callback callback){
        reference = new WeakReference<Callback>(callback);
    }

    public WeakRefHandler(Callback callback, Looper looper){
        super(looper);
        reference = new WeakReference<Callback>(callback);
    }

    @Override
    public void handleMessage(Message msg) {
        if (reference!=null&&reference.get()!=null){
            Callback callback =reference.get();
            callback.handleMessage(msg);
        }
    }
}
