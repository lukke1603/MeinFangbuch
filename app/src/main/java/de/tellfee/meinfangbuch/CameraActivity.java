package de.tellfee.meinfangbuch;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity {
    private TextureView tv_preview;
    private ImageView iv_capture_image;

    private final String SAVE_PATH  = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))+"/MeinFangbuch";

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

    private ImageReader imageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private CaptureRequest.Builder streamCaptureRequestBuilder;
    private CameraCaptureSession streamCaptureSession;

    private CaptureRequest.Builder captureCaptureRequestBuilder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private SensorEventListener sensorListener;
    private SensorManager sensorManager;

    private int orientation;
    private Sensor sensor;


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

        tv_preview          = (TextureView) findViewById(R.id.tv_preview);
        iv_capture_image    = (ImageView) findViewById(R.id.iv_capture_image);

        tv_preview.setSurfaceTextureListener(surfaceTextureListener);
        iv_capture_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        Log.e("CAMERA_ACTIVITY", "onResume");
        startBackgroundThread();
        if (tv_preview.isAvailable()) {
            openCamera();
        } else {
            tv_preview.setSurfaceTextureListener(surfaceTextureListener);
        }

        SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        sensorListener  = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];

                if (x<5 && x>-5 && y > 5)
                    orientation = 90;
                else if (x<-5 && y<5 && y>-5)
                    orientation = 180;
                else if (x<5 && x>-5 && y<-5)
                    orientation = 270;
                else if (x>5 && y<5 && y>-5)
                    orientation = 0;
            }


            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensor  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void captureImage() {
        if(cameraDevice == null){
            Log.e("ERROR", "CameraDevice not initialized");
            return;
        }
        try {
//            final int rotation = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
//            final int rotation = getScreenOrientation();
            final int rotation = orientation;


            imageReader                     = ImageReader.newInstance(streamSize.getWidth(), streamSize.getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces    = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(new Surface(tv_preview.getSurfaceTexture()));


            captureCaptureRequestBuilder    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureCaptureRequestBuilder.addTarget(imageReader.getSurface());
            captureCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            captureCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 1);


            File filePath      = new File(SAVE_PATH);
            if(!filePath.exists()){
                filePath.mkdir();
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String fileName         = "DSC_"+format.format(new Date())+".jpg";
            final File file         = new File(SAVE_PATH+"/"+fileName);
            if(file.exists()){
                file.delete();
            }

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image         = null;
                    try {
                        image               = reader.acquireLatestImage();
                        ByteBuffer buffer   = image.getPlanes()[0].getBuffer();
                        byte[] bytes        = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        Matrix rotate       = new Matrix();
                        Log.e("ROTATION", ""+rotation+" -> "+ORIENTATIONS.get(rotation));
                        rotate.postRotate(rotation);
                        Bitmap bmp          = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        Bitmap bmpRotated   = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), rotate, false);
                        save(bmpRotated);
//                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        if(image != null){
                            image.close();
                        }
                    }


                }

                private void save(Bitmap bmp) throws IOException {
                    FileOutputStream fOut = null;
                    try {
                        fOut    = new FileOutputStream(file);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
                        fOut.flush();
                        fOut.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;

                    try {
                        output  = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if(output != null){
                            output.close();
                        }
                    }
                }
            };

            imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureCaptureRequestBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onPause() {
        Log.e("CAMERA_ACTIVITY", "onPause");
        //closeCamera();
        sensorManager.unregisterListener(sensorListener, sensor);
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

        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e("LOG", newConfig.orientation+"");
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


    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e("LOG", "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e("LOG", "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }



}
