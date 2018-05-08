package com.example.administrator.dome;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Toast;

import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;

/**
 * @author suhu
 * @data 2018/5/8 0008.
 * @description
 */

public class SweetDialog {
    private static NiftyDialogBuilder dialogBuilder;

    public static NiftyDialogBuilder dialog(Context context, String name, Bitmap bitmap){
        return builder(context)
                .withTitle("用户验证信息")
                .withTitleColor("#000000")
                .withDividerColor("#FFFFFF")
                .withMessage(name)
                .withMessageColor("#FFFFFFFF")
                .withDialogColor("#CCCCCC")
                .isCancelableOnTouchOutside(true)
                .withDuration(700)
                .withEffect(Effectstype.Slidetop);
    }

    private static NiftyDialogBuilder builder(Context context){
        if (dialogBuilder==null){
            dialogBuilder=NiftyDialogBuilder.getInstance(context);
        }
        return dialogBuilder;
    }


    public static NiftyDialogBuilder fun(Context context){
        dialogBuilder=NiftyDialogBuilder.getInstance(context);
        return dialogBuilder
                .withTitle("Modal Dialog")                                  //.withTitle(null)  no title
                .withTitleColor("#FFFFFF")                                  //def
                .withDividerColor("#11000000")                              //def
                .withMessage("This is a modal Dialog.")                     //.withMessage(null)  no Msg
                .withMessageColor("#FFFFFFFF")                              //def  | withMessageColor(int resid)
                .withDialogColor("#FFE74C3C")                               //def  | withDialogColor(int resid)                               //def
                .isCancelableOnTouchOutside(true)                           //def    | isCancelable(true)
                .withDuration(1000)                                          //def
                .withEffect(Effectstype.Slidetop)                                         //def Effectstype.Slidetop
                .withButton1Text("OK")                                      //def gone
                .withButton2Text("Cancel")                                  //def gone
                .setButton1Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), "i'm btn1", Toast.LENGTH_SHORT).show();
                    }
                })
                .setButton2Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(v.getContext(), "i'm btn2", Toast.LENGTH_SHORT).show();
                    }
                });

    }

}
