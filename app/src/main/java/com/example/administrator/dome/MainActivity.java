package com.example.administrator.dome;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,TextToSpeech.OnInitListener{

    private static final String TAG = "MainActivity";
    /**
     *  定义对象
     */

    private SurfaceView mSurfaceview = null;  // SurfaceView对象：(视图组件)视频显示
    /**
     * SurfaceHolder对象：(抽象接口)SurfaceView支持类
     *
     */


    private SurfaceHolder mSurfaceHolder = null;
    /**
     * Camera对象，相机预览
     * */
    private Camera mCamera =null;


    private boolean bIfPreview ;
    private int mPreviewWidth,mPreviewHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        initSurfaceView();
        //initSpeech();
        ttsRead();
        //faceDialog();


    }



    private void initSurfaceView()
    {
        mSurfaceview = (SurfaceView) this.findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceview.getHolder(); // 绑定SurfaceView，取得SurfaceHolder对象
        mSurfaceHolder.addCallback(this); // SurfaceHolder加入回调接口
        mSurfaceHolder.setFixedSize(1920, 1160); // 预览大小設置
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);// 設置顯示器類型，setType必须设置




    }



    byte a[] = new byte[1024*8];

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(1);// 开启摄像头（2.3版本后支持多摄像头,需传入参数）
  //      mCamera.setPreviewCallback(new encoderVideo(0, 0, (ImageView) findViewById(R.id.image)));//①原生yuv420sp视频存储方式
        mCamera.addCallbackBuffer(new byte[((1920 * 1080) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8]);
        mCamera.setPreviewCallbackWithBuffer(new encoderVideo());
        mCamera.startPreview();
        try
        {
            Log.i(TAG, "SurfaceHolder.Callback：surface Created");
            mCamera.setPreviewDisplay(mSurfaceHolder);//set the surface to be used for live preview

        } catch (Exception ex)
        {
            if(null != mCamera)
            {
                mCamera.release();
                mCamera = null;
            }
            Log.i(TAG+"initCamera", ex.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        Log.i(TAG, "SurfaceHolder.Callback：Surface Changed");
        mPreviewHeight = height;
        mPreviewWidth = width;
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
// TODO Auto-generated method stub
        Log.i(TAG, "SurfaceHolder.Callback：Surface Destroyed");
        if(null != mCamera)
        {
            mCamera.setPreviewCallback(null); //！！这个必须在前，不然退出出错
            mCamera.stopPreview();
            bIfPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }




    private void initCamera()//surfaceChanged中调用
    {
        Log.i(TAG, "going into initCamera");
        if (bIfPreview)
        {
            mCamera.stopPreview();//stopCamera();
        }
        if(null != mCamera)
        {
            try
            {
                /* Camera Service settings*/
                Camera.Parameters parameters = mCamera.getParameters();
                // parameters.setFlashMode("off"); // 无闪光灯
                //parameters.setPreviewFrameRate(10);//设置每秒3帧
                parameters.setPictureFormat(PixelFormat.JPEG); //Sets the image format for picture 设定相片格式为JPEG，默认为NV21
                parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP); //Sets the image format for preview picture，默认为NV21
                /*【ImageFormat】JPEG/NV16(YCrCb format，used for Video)/NV21(YCrCb format，used for Image)/RGB_565/YUY2/YU12*/

                // 【调试】获取caera支持的PictrueSize，看看能否设置？？
                List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
                List<Integer> previewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
                Log.i(TAG+"initCamera", "cyy support parameters is ");
                Camera.Size psize = null;
                for (int i = 0; i < pictureSizes.size(); i++)
                {
                    psize = pictureSizes.get(i);
                    Log.i(TAG+"initCamera", "PictrueSize,width: " + psize.width + " height" + psize.height);
                }
                for (int i = 0; i < previewSizes.size(); i++)
                {
                    psize = previewSizes.get(i);
                    Log.i(TAG+"initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
                }
                Integer pf = null;
                for (int i = 0; i < previewFormats.size(); i++)
                {
                    pf = previewFormats.get(i);
                    Log.i(TAG+"initCamera", "previewformates:" + pf);
                }

                // 设置拍照和预览图片大小
                parameters.setPictureSize(1160, 1920); //指定拍照图片的大小
                parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 指定preview的大小
                //这两个属性 如果这两个属性设置的和真实手机的不一样时，就会报错

                // 横竖屏镜头自动调整
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
                {
                    parameters.set("orientation", "portrait"); //
                    parameters.set("rotation", 90); // 镜头角度转90度（默认摄像头是横拍）
                    mCamera.setDisplayOrientation(90); // 在2.2以上可以使用
                } else// 如果是横屏
                {
                    parameters.set("orientation", "landscape"); //
                    mCamera.setDisplayOrientation(0); // 在2.2以上可以使用
                }

                /* 视频流编码处理 */
                //添加对视频流处理函数


                // 设定配置参数并开启预览
                mCamera.setParameters(parameters); // 将Camera.Parameters设定予Camera
                mCamera.startPreview(); // 打开预览画面
                bIfPreview = true;

                // 【调试】设置后的图片大小和预览大小以及帧率
                Camera.Size csize = mCamera.getParameters().getPreviewSize();
                mPreviewHeight = csize.height; //
                mPreviewWidth = csize.width;
                Log.i(TAG+"initCamera", "after setting, previewSize:width: " + csize.width + " height: " + csize.height);
                csize = mCamera.getParameters().getPictureSize();
                Log.i(TAG+"initCamera", "after setting, pictruesize:width: " + csize.width + " height: " + csize.height);
                Log.i(TAG+"initCamera", "after setting, previewformate is " + mCamera.getParameters().getPreviewFormat());
                Log.i(TAG+"initCamera", "after setting, previewframetate is " + mCamera.getParameters().getPreviewFrameRate());
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }




    private class encoderVideo implements Camera.PreviewCallback {

        private ByteArrayOutputStream baos;
        private byte[] rawImage;
        private Bitmap bitmap;

        private ImageView imageView;

        public encoderVideo() {
        }

        public encoderVideo(int width, int height, ImageView viewById) {
            imageView = viewById;
        }





        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //传递进来的data,默认是YUV420SP的
            // TODO Auto-generated method stub
            //imageView.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
            Camera.Size size = camera.getParameters().getPreviewSize();
            Log.i(TAG,"---------------------------------data :w="+size.width+"        h="+size.height+data.toString());
            read("李三欢迎光临");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }



            //readKDXF("你好欢迎光临");
            mCamera.addCallbackBuffer(data);

            //camera.setOneShotPreviewCallback(null);
            //处理data
//            Camera.Size previewSize = camera.getParameters().getPreviewSize();//
// 获取尺寸,格式转换的时候要用到
//            BitmapFactory.Options newOpts = new BitmapFactory.Options();
//            newOpts.inJustDecodeBounds = true;
//            YuvImage yuvimage = new YuvImage(
//                    data,
//                    ImageFormat.NV21,
//                    previewSize.width,
//                    previewSize.height,
//                    null);
//            baos = new ByteArrayOutputStream();
//            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
//            rawImage = baos.toByteArray();
//            //将rawImage转换成bitmap
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inPreferredConfig = Bitmap.Config.RGB_565;
//            bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
//            imageView.setImageBitmap(bitmap);





//            try
//            {
//                Log.i(TAG, "going into onPreviewFrame");
//                //mYUV420sp = data;   // 获取原生的YUV420SP数据
//                YUVIMGLEN = data.length;
//
//                // 拷贝原生yuv420sp数据
//                mYuvBufferlock.acquire();
//                System.arraycopy(data, 0, mYUV420SPSendBuffer, 0, data.length);
//                //System.arraycopy(data, 0, mWrtieBuffer, 0, data.length);
//                mYuvBufferlock.release();
//
//                // 开启编码线程，如开启PEG编码方式线程
//                mSendThread1.start();
//
//            } catch (Exception e)
//            {
//                Log.v("System.out", e.toString());
//            }
//          }
        }
}





/***************************************************************TTs语音播放***********************************************************************/


    private TextToSpeech tts;
    private boolean isRead = false;
    private void ttsRead(){
        isRead = true;
        tts = new TextToSpeech(this,this);
    }

    @Override
    public void onInit(int status) {
        if (status==TextToSpeech.SUCCESS){
            Log.e(TAG,"success");
            int result = tts.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.CHINA);
                tts.isLanguageAvailable(Locale.CHINA);
                tts.setPitch(1.0f);
                //tts.setSpeechRate(1.2f);
            }



        }else {
            Log.e(TAG,"失败");
        }
    }

    @Override
    protected void onDestroy() {

        if (tts!=null){
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

    }


    private void read(String str){
        faceDialog();
        if (isRead&&!tts.isSpeaking()){
            tts.speak(str,TextToSpeech.QUEUE_FLUSH,null);
        }
    }


    /****************************************************************dialog***********************************************************************************/



    private FaceDialog faceDialog(){
        FaceDialog dialog = new FaceDialog(MainActivity.this,R.style.dialog,"http://pic5.58cdn.com.cn/zhuanzh/n_v1bkuyfvltjuifpjbky4aq.jpg","张三");
        dialog.show();
        linkedList.add(dialog);
        return dialog;
    }

    private List<FaceDialog> linkedList = new LinkedList<>();








    /******************************************************************科大讯飞的接口*****************************************************/

    private void readKDXF(String str){
        VoiceUtils.getInstance().initmTts(MainActivity.this,str);
    }


    private void popup(){

    }


}
