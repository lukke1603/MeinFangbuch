package de.tellfee.meinfangbuch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {
    private TextureView tv_preview;

    private Size previewSize;
    private Size previewSizeScaled;
    private double previewRatio;
    private Size streamSize;
    private double streamRatio;

    private SurfaceTexture surfaceTexture;
    private CameraDevice cameraDevice;
    private CameraManager manager;
    private String camID;
    private final int REQUEST_CAMERA_PERMISSION = 200;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private CaptureRequest.Builder streamCaptureRequestBuilder;
    private CameraCaptureSession streamCaptureSession;

    private CaptureRequest.Builder captureCaptureRequestBuilder;

    private TextureView.SurfaceTextureListener surfaceTextureListener   = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            previewSize     = new Size(width, height);
            previewRatio    = (double)width / (double)height;
            openCamera();
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeUIImmersive();

        setContentView(R.layout.activity_camera);

        tv_preview = (TextureView) findViewById(R.id.tv_preview);
        tv_preview.setSurfaceTextureListener(surfaceTextureListener);

        Log.e("CAMERA_ACTIVITY", "onResume");
        startBackgroundThread();
        if (tv_preview.isAvailable()) {
            openCamera();
        } else {
            tv_preview.setSurfaceTextureListener(surfaceTextureListener);
        }
    }


    @Override
    protected void onPause() {
        Log.e("CAMERA_ACTIVITY", "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    private void makeUIImmersive(){
        getSupportActionBar().hide();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }


    private void openCamera() {
        manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cams = manager.getCameraIdList();
            for (String cam : cams) {
                CameraCharacteristics c = manager.getCameraCharacteristics(cam);
                if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    camID                       = cam;
                    StreamConfigurationMap map  = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    streamSize                  = map.getOutputSizes(ImageFormat.JPEG)[0];
                    streamRatio                 = streamSize.getWidth() / streamSize.getHeight();
                }
            }

            if (camID != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                    return;
                }
                manager.openCamera(camID, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice    = camera;
                        createCameraPreview();
                    }

                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                }, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        surfaceTexture          = tv_preview.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(streamSize.getWidth(), streamSize.getHeight());
//        surfaceTexture.setDefaultBufferSize(1080, 1080);
        Surface surface         = new Surface(surfaceTexture);

        try {
            streamCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            streamCaptureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if(cameraDevice == null){
                        return;
                    }
                    
                    streamCaptureSession    = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        previewSizeScaled   = getPreviewSizeScaled();
//        Rect rect           = getPreviewRect();
//        surfaceTexture.setDefaultBufferSize(previewSizeScaled.getWidth(), previewSizeScaled.getHeight());

        streamCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        streamCaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, rect);
        try {
            streamCaptureSession.setRepeatingRequest(streamCaptureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Rect getPreviewRect(){
        Rect r = new Rect();
//        Size size       = previewSizeScaled;
        Size size       = new Size(1080, 1080);
        Log.e("PREVIEW-SIZE", ""+previewSize);
        Log.e("SIZE", ""+previewSizeScaled);

        Log.e("SCALED-RATIO", ""+(double)previewSizeScaled.getHeight() / (double)previewSizeScaled.getWidth());
        Log.e("RATIO", ""+(double)previewSize.getHeight() / (double)previewSize.getWidth());

        r.set(
                (streamSize.getWidth() - size.getHeight()) / 2,
                (streamSize.getHeight() - size.getWidth()) / 2,
                ((streamSize.getWidth() - size.getHeight()) / 2) + size.getHeight() - 1,
                ((streamSize.getHeight() - size.getWidth()) / 2) + size.getWidth() - 1
        );
        Log.e("RECT", ""+r);
        return r;
    }


    private Size getPreviewSizeScaled() {
        int width;
        double tmpRatio = (double)previewSize.getHeight() / (double)previewSize.getWidth();
        if(tmpRatio > streamRatio){
            width   = streamSize.getHeight();
        }else{
            width   = streamSize.getWidth();
        }
        int height      = (int)(width * (1 / previewRatio));
        Size newSize    = new Size(width, height);

        return newSize;


    }


    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
//        if (null != imageReader) {
//            imageReader.close();
//            imageReader = null;
//        }
    }


    protected void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }


    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler.getLooper().quit();
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(CameraActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }



}
