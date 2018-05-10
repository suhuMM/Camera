package com.example.administrator.dome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author suhu
 * @data 2018/5/8 0008.
 * @description
 */

public class FaceDialog extends Dialog{
    private static DisplayMetrics displayMetrics;
    private FaceDialog dialog;
    private Context context;
    private String url;
    private String name;

    private ImageView face;
    private TextView textView;





    public FaceDialog(@NonNull Context context, int themeResId,String url,String name) {
        super(context, themeResId);
        this.context = context;
        this.url = url;
        this.name = name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.dialog_face);
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        window.setGravity(Gravity.CENTER);

        Rect rect = new Rect();
        View view = window.getDecorView();
        view.getWindowVisibleDisplayFrame(rect);
        params.width = getScreenWidth();
        params.dimAmount = 0.5f;
        //getWindow().setWindowAnimations(R.style.popupWindowAnimation);
        getWindow().setWindowAnimations(R.style.popupWindowScale);
        window.setAttributes(params);

        initView();
        setData();

        //dismissDialog();
        //countTimer();


    }

    private void dismissDialog() {
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                dismiss();
            }
        };
        timer.schedule(task ,3000);

    }


    private void countTimer(){
        new CountDownTimer(3000,3000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                if (dialog!=null&&dialog.isShowing()){
                    dialog.dismiss();
                }
            }
        }.start();


    }

    private void setData() {
        textView.setText(name);
        //Glide.with(context).load(url).into(face);
    }

    private void initView() {
        face = findViewById(R.id.face);
        textView = findViewById(R.id.name);
        dialog = this;

    }


    public int getScreenWidth(){
        if (displayMetrics==null){
            displayMetrics = context.getResources().getDisplayMetrics();
        }
        return displayMetrics.widthPixels;
    }

    @Override
    public void show() {
        countTimer();
        super.show();
    }
}
