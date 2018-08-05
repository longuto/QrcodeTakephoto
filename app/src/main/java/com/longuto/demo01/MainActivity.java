package com.longuto.demo01;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.permission.InvokeListener;
import com.jph.takephoto.permission.PermissionManager;
import com.jph.takephoto.permission.TakePhotoInvocationHandler;

import java.io.File;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.QRCodeDecoder;
import cn.bingoogolapple.qrcode.zxing.ZXingView;

public class MainActivity extends AppCompatActivity implements QRCodeView.Delegate, TakePhoto.TakeResultListener, InvokeListener{

    ZXingView mZXingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getTakePhoto().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        StateBarTranslucentUtils.setStateBarTranslucent(this);
        setContentView(R.layout.activity_main);
        mZXingView = findViewById(R.id.zxingview);
        initZxingView();
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPhotos();
            }
        });
    }

    private void openPhotos() {
        mTakeOrPickPhotoManager = new TakeOrPickPhotoManager(getTakePhoto());

        //设置为选择图片不裁剪
        mTakeOrPickPhotoManager.setCrop(false);
        //选择图片
        mTakeOrPickPhotoManager.takeOrPickPhoto(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getTakePhoto().onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //以下代码为处理Android6.0、7.0动态权限所需
        PermissionManager.TPermissionType type=PermissionManager.onRequestPermissionsResult(requestCode,permissions,grantResults);
        PermissionManager.handlePermissionsResult(this,type,invokeParam,this);
    }

    /**
     * 初始化zxing
     */
    private void initZxingView() {
        mZXingView.setDelegate(this);
        mZXingView.startSpot();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mZXingView.startCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mZXingView.stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mZXingView.onDestroy();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        vibrate();

        mZXingView.startSpot();
    }

    @Override
    public void onScanQRCodeOpenCameraError() {

    }

    /**
     * 震动手机
     */
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(200);
    }

    @Override
    public void takeSuccess(TResult result) {
        File file = new File(result.getImages().get(0).getCompressPath());
        String path = file.getAbsolutePath();

        //获取扫描图片结果的过程必须在异步线程中进行
        //在这里，因为获取图片的过程 是在异步线程中进行的，所以上面不必在开启新的异步线程了。
        String spotResult = QRCodeDecoder.syncDecodeQRCode(path);

        //需要返回到UI线程 刷新头像
        Message msg = mHandler.obtainMessage();
        msg.what = 0x1001;
        msg.obj = spotResult;
        mHandler.sendMessage(msg);
    }

    @Override
    public void takeFail(TResult result, String msg) {

    }

    @Override
    public void takeCancel() {

    }

    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type=PermissionManager.checkPermission(TContextWrap.of(this),invokeParam.getMethod());
        if(PermissionManager.TPermissionType.WAIT.equals(type)){
            this.invokeParam=invokeParam;
        }
        return type;
    }

    /**
     * 获取TakePhoto实例
     *
     * @return
     */
    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this)
                    .bind(new TakePhotoImpl(this, this));

        }
        return takePhoto;
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x1001:
                    String result = (String) msg.obj;
                    if (TextUtils.isEmpty(result)) {
                        Toast.makeText(MainActivity.this, "未发现二维码", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public TakeOrPickPhotoManager mTakeOrPickPhotoManager;;
    TakePhoto takePhoto;
    InvokeParam invokeParam;
}
