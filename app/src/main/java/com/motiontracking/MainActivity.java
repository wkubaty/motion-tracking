package com.motiontracking;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.motiontracking.databinding.ActivityMainBinding;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private CameraDevice cameraDevice;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private Size imageDimension;

    private CameraCaptureSession cameraCaptureSession;

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    private CaptureRequest.Builder captureRequestBuilder;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void createCameraPreview() {
        try {
            Log.d(TAG, "createCameraPreview");
            SurfaceTexture texture  = binding.texture.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (null == cameraDevice) {
                        return;
                    }
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.d(TAG, "onConfigureFailed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_OFF);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.texture.setSurfaceTextureListener(textureListener);

    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startBackgroundThread();
        if (binding.texture.isAvailable()) {
            openCamera();
        } else {
            binding.texture.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        stopBackgroundThread();
        super.onPause();
    }

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameras = cameraManager.getCameraIdList();
            CameraCharacteristics map = cameraManager.getCameraCharacteristics(cameras[0]);
            StreamConfigurationMap streamConfigurationMap = map.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            cameraManager.openCamera(cameras[0], stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
}
