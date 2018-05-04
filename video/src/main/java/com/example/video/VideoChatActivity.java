package com.example.video;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.video.codec.RtspPacketDecode;
import com.example.video.codec.RtspPacketEncode;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by LONG on 2017/3/31.
 */

public class VideoChatActivity extends AppCompatActivity implements View.OnClickListener, Camera.PreviewCallback,  RtspPacketEncode.H264ToRtpLinsener {

    private static final String TAG = "VideoChatActivity";

    private InetAddress address;
    private DatagramSocket socket;
    private UdpSendTask netSendTask;
    //-----------------------------------------------------------


    ImageView headImage;

    // 程序中的按钮
    ImageButton record, buttonHangup, buttonAnswer;
    ImageView change;

    // 显示视频预览的SurfaceView
    SurfaceView sView, mView;
    // 记录是否正在进行录制
    private boolean isRecording = false;
    private Camera mCamera;
    private int cameraPosition = 1;//1代表前置摄像头，0代表后置摄像头
    private int displayOrientation = 90;//相机预览方向
    //视频分辨率
    int width = 320;
    int height = 240;
    byte[] h264;//接收H264
    //h264硬编码器
    AvcEncoder avcEncoder;
    //h264硬解码器
    AvcDecode avcDecode;
    RtspPacketDecode rtspPacketDecode;//rtsp解码器
    RtspPacketEncode rtspPacketEncode;//rtsp编码器

    //录制音频参数
    private int frequence = 8000; //录制频率，单位hz.这里的值注意了，写的不好，可能实例化AudioRecord对象的时候，会出错。我开始写成11025就不行。这取决于硬件设备
    private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去掉标题栏 ,必须放在setContentView之前
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        // 设置横屏显示
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        //getWindow().setFormat(PixelFormat.TRANSLUCENT);
        // 获取程序界面中的按钮
        record = (ImageButton) findViewById(R.id.record);
        change = (ImageView) findViewById(R.id.change);
//        buttonHangup =  findViewById(R.id.buttonHangup);
//        headImage =  findViewById(R.id.headImage);
//        buttonAnswer = findViewById(R.id.buttonAnswer);

        // 让切换相机按钮不可用。
        change.setEnabled(false);
        //change.setBackground(getResources().getDrawable(R.drawable.agx));
        // 为两个按钮的单击事件绑定监听器
        record.setOnClickListener(this);
        change.setOnClickListener(this);
        buttonHangup.setOnClickListener(this);
        buttonAnswer.setOnClickListener(this);
        // 获取程序界面中的大SurfaceView
        sView = (SurfaceView) this.findViewById(R.id.sView);
        // 设置分辨率
        sView.getHolder().setFixedSize(width, height);
        // 设置该组件让屏幕不会自动关闭
        sView.getHolder().setKeepScreenOn(true);
        // 获取程序界面中的小SurfaceView
        mView = (SurfaceView) this.findViewById(R.id.mView);
        // 设置分辨率
        mView.getHolder().setFixedSize(width, height);


//______________________
        netSendTask = new UdpSendTask();
        netSendTask.init();
        netSendTask.start();

    }

    @Override
    public void onClick(View source) {
        switch (source.getId()) {
            // 单击录制按钮
            case R.id.record:
                // new RecordTask().execute();
                mHandler.sendEmptyMessage(2);
                break;
            case R.id.change:
                //切换摄像头
                change();
                break;
         
        }
    }

    //初始化相机
    private void initCameara() {
        try {
            mCamera = Camera.open(cameraPosition);
            mCamera.setPreviewDisplay(mView.getHolder());
            //设置预览方向
            mCamera.setDisplayOrientation(displayOrientation);


            //获取相机配置参数
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedPreviewSizes = parameters
                    .getSupportedPreviewSizes();
            for (Camera.Size s : supportedPreviewSizes
                    ) {
                Log.v(TAG, s.width + "----" + s.height);
            }

            parameters.setFlashMode("off"); // 无闪光灯
            parameters.setPreviewFormat(ImageFormat.NV21); //Sets the image format for preview picture，默认为NV21,YV12
            parameters.setPreviewFrameRate(10);//设置帧率
            parameters.setPreviewSize(width, height);
            parameters.setPictureSize(width, height);

            mCamera.setParameters(parameters); // 将Camera.Parameters设定予Camera

            //设置预览回调
            mCamera.setPreviewCallback((Camera.PreviewCallback) this);
            mCamera.startPreview();

            //启动摄像头切换按钮
            change.setEnabled(true);
            //change.setBackground(getResources().getDrawable(R.drawable.ahv));

            //初始化视频编解码器
            avcEncoder = new AvcEncoder(width, height, 10, 125000);
            avcDecode = new AvcDecode(width, height, sView.getHolder().getSurface());
            rtspPacketDecode = new RtspPacketDecode(width, height);
            rtspPacketEncode = new RtspPacketEncode(this);
            h264 = new byte[avcEncoder.getYuvBuffer(width, height)];

        } catch (Exception e) {
            Log.i("jw", "camera error:" + Log.getStackTraceString(e));
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
        isRecording = false;
    }

    private void destroyCamera() {
        if (mCamera == null) {
            return;
        }
        //！！这个必须在前，不然退出出错
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        try {
            /*//这张方式可以将NV21格式数据转化成Bitmap
            int[] imgData = DataFormatUtils.NV21toARGB(data, size.width, size.height);
        	Bitmap bmp = Bitmap.createBitmap(imgData, size.width, size.height, Config.ARGB_8888);
        	imgV.setImageBitmap(BitmapUtils.addTimeToBitmap(bmp, System.currentTimeMillis()));*/

            //直接使用系统的YuvImage来进行转化图片，这里的支持ImageFormat.NV21和ImageFormat.YUY2,
            //但是YUY2的Camera是不支持的，所以这里会出现花屏现象
//            YuvImage image = new YuvImage(bytes, ImageFormat.NV21, size.width, size.height, null);
//            Bitmap bmp = null;
//            if (image != null) {
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
//
//
// }
            if (isRecording) {
                //摄像头数据转h264
                int ret = avcEncoder.offerEncoder(bytes, h264);
                if (ret > 0) {
                    //h264转rtp发送
                    rtspPacketEncode.h264ToRtp(h264, ret);
                    //netSendTask.pushBuf(h264, ret);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //切换前后摄像头
    public void change() {
        //切换前后摄像头
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);

            if (cameraPosition == 1) {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    displayOrientation = 90;
                    cameraPosition = 0;
                    break;
                }
            } else {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    displayOrientation = 90;
                    cameraPosition = 1;
                    break;
                }
            }
        }
        destroyCamera();
        initCameara();
    }



    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case -1:
                    Toast.makeText(VideoChatActivity.this,"对方已经取消",Toast.LENGTH_LONG).show();
                    break;
                case 0:
                    Toast.makeText(VideoChatActivity.this, "对方已拒绝",Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    Toast.makeText(VideoChatActivity.this, "对方已挂断",Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    //设置正在录制
                    isRecording = true;
                    //隐藏头像
                    headImage.setVisibility(View.GONE);
                    //设置背景图为空，否则会遮档视频
                    sView.setBackgroundResource(0);
                    mView.setBackgroundResource(0);
                    initCameara();
                    break;
                default:
            }
        }
    };

    @Override
    public void h264ToRtpResponse(byte[] out, int len) {

    }


    class UdpSendTask extends Thread {
        private ArrayList<ByteBuffer> mList;

        public void init() {
            try {
                socket = new DatagramSocket();
                address = InetAddress.getByName("192.168.10.84");
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            mList = new ArrayList<ByteBuffer>();

        }

        public void pushBuf(byte[] buf, int len) {
            ByteBuffer buffer = ByteBuffer.allocate(len);
            buffer.put(buf, 0, len);
            mList.add(buffer);
        }

        @Override
        public void run() {
            Log.d(TAG, "fall in udp send thread");
            while (true) {
                if (mList.size() <= 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (mList.size() > 0) {
                    ByteBuffer sendBuf = mList.get(0);
                    try {
                        Log.d(TAG, "send udp packet len:" + sendBuf.capacity());
                        DatagramPacket packet = new DatagramPacket(sendBuf.array(), sendBuf.capacity(), address, 5000);

                        socket.send(packet);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    mList.remove(0);
                }
            }
        }
    }
}