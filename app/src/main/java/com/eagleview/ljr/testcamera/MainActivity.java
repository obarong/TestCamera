package com.eagleview.ljr.testcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static String TAG = "TestCamera";
    private View layout;
    private Camera mCamera;
    boolean Debug = true;

    Bundle bundle = null; // 声明一个Bundle对象，用来存储数据

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 显示界面
        setContentView(R.layout.activity_main);

        //layout = this.findViewById(R.id.buttonLayout);

        SurfaceView surfaceView = (SurfaceView) this
                .findViewById(R.id.surfaceView);
        surfaceView.getHolder()
                .setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().setFixedSize(1280, 720); //设置Surface分辨率
        //surfaceView.getHolder().setKeepScreenOn(true);// 屏幕常亮
        surfaceView.getHolder().addCallback(new SurfaceCallback());//为SurfaceView的句柄添加一个回调函数
    }


    private final class SurfaceCallback implements SurfaceHolder.Callback {

        // 拍照状态变化时调用该方法
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            //Log.d(TAG, "SurfaceHolder width:" + width + " height:" + height);
            Camera.Parameters parameters = null;
            parameters = mCamera.getParameters(); // 获取各项参数
            parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
            parameters.setPreviewSize(width, height); // 设置预览大小
            parameters.setPreviewFrameRate(5);  //设置每秒显示4帧
            parameters.setPictureSize(width, height); // 设置保存的图片尺寸
            parameters.setJpegQuality(100); // 设置照片质量
            parameters.setFocusMode(parameters.FOCUS_MODE_CONTINUOUS_PICTURE);    //设置自动连续对焦，API14
            parameters.setRotation(90); //设置相机旋转
            mCamera.setParameters(parameters);

//            List<Size> mSupportedPreviewSizes;
//            mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
//            if (Debug)
//                Log.d(TAG, "supported preview size : ");
//            for (int i = 0; i < mSupportedPreviewSizes.size(); i++) {
//                if (Debug) {
//                    Log.d(TAG, " : " + i + "  "
//                            + mSupportedPreviewSizes.get(i).width + "  "
//                            + mSupportedPreviewSizes.get(i).height);
//                }
//            }

            //parameters.setPreviewSize(1280, 720);
            //parameters.setPictureSize(1280, 720);
        }

        // 开始拍照时调用该方法
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera = Camera.open(); // 打开摄像头
                mCamera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
                mCamera.setDisplayOrientation(getPreviewDegree(MainActivity.this));
                mCamera.startPreview(); // 开始预览
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        // 停止拍照时调用该方法
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            closeCamera();
            holder.setKeepScreenOn(false);
        }
    }

    private void closeCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.cancelAutoFocus();
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_CAMERA: // 按下拍照按钮
//                if (mCamera != null && event.getRepeatCount() == 0) {
//                    // 拍照
//                    //注：调用takePicture()方法进行拍照是传入了一个PictureCallback对象——当程序获取了拍照所得的图片数据之后
//                    //，PictureCallback对象将会被回调，该对象可以负责对相片进行保存或传入网络
//                    mCamera.takePicture(null, null, new MyPictureCallback());
//                }
//        }
//        return super.onKeyDown(keyCode, event);
//    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }

    /**
     * 按钮被点击触发的事件
     *
     * @param v
     */
    public void btnOnclick(View v) {
        if (mCamera != null) {
            switch (v.getId()) {
                case R.id.takepicture:
                    // 拍照
                    mCamera.takePicture(null, null, new MyPictureCallback());
                    break;
            }
        }
    }


    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                bundle = new Bundle();
                bundle.putByteArray("bytes", data); //将图片字节数据保存在bundle当中，实现数据交换
                File file = saveToSDCard(data); // 保存图片到sd卡中
                Toast.makeText(getApplicationContext(), R.string.success,
                        Toast.LENGTH_SHORT).show();
                camera.startPreview(); // 拍完照后，重新开始预览

                //通知多媒体库扫描图片
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将拍下来的照片存放在SD卡中
     * @param data
     * @throws IOException
     */
    public static File saveToSDCard(byte[] data) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory()
                + "/_TestCamera/");
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"XXX"的目录
            fileFolder.mkdir();
        }
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.write(data); // 写入sd卡中
        outputStream.close(); // 关闭输出流

        return jpgFile;
    }
}
