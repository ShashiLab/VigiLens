package app.shashi.VigiLens.feature.recording.service;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.recording.ui.FloatingPreviewView;
import app.shashi.VigiLens.feature.recording.util.CameraSettings;
import app.shashi.VigiLens.feature.recording.util.MediaRecorderManager;
import app.shashi.VigiLens.feature.recording.util.NotificationManager;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class VideoRecordingService extends Service implements FloatingPreviewView.TimerUpdateListener {

    private static final String TAG = "VideoRecordingService";
    private String prefsName = "user_prefs";  

    public static final String ACTION_STOP = "app.shashi.VigiLens.ACTION_STOP";
    private static final String GLOBAL_PREFS_NAME = "app_prefs";  
    private static final String PREF_START_TIME = "startTime";
    private static final String PREF_IS_RECORDING = "isRecording";

    private final Handler stopHandler = new Handler();
    private Runnable stopRunnable;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private MediaRecorder mediaRecorder;
    private CameraCharacteristics cameraCharacteristics;
    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;

    private boolean showPreview;
    private FloatingPreviewView floatingPreview;
    private String previewSizePref;

    
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long recordingStartTime;

    private BroadcastReceiver screenUnlockReceiver;
    private static final String STOP_ON_UNLOCK_KEY = "stop_recording_on_device_unlock";  

    
    private static final long MIN_STORAGE_THRESHOLD = 50 * 1024 * 1024; 
    private final Handler storageCheckHandler = new Handler();
    private final Runnable storageCheckRunnable = new Runnable() {
        @Override
        public void run() {
            long availableStorage = getAvailableStorage();
            if (availableStorage < MIN_STORAGE_THRESHOLD) {
                Log.d(TAG, "Low storage, stopping recording.");
                stopRecording(getString(R.string.notification_text_recording_stopped_low_storage), true);
            } else {
                storageCheckHandler.postDelayed(this, 10000); 
            }
        }
    };

    private NotificationManager notificationHelper;

    private CameraManager cameraManager;
    private CameraManager.AvailabilityCallback availabilityCallback;
    private String desiredCameraId;

    int frameRate = 30;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    
    @Override
    public void onCreate() {
        super.onCreate();
        registerUnlockReceiver();

        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        float screenAspectRatio = (float) screenWidth / screenHeight;

        
        notificationHelper = new NotificationManager(this);
        notificationHelper.createNotificationChannels();
        startForeground(1, notificationHelper.getNotification());

        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    }


    private void registerUnlockReceiver() {
        IntentFilter filter = new IntentFilter();
        
        filter.addAction(Intent.ACTION_USER_PRESENT);
        
        filter.addAction(Intent.ACTION_SCREEN_ON);

        screenUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                
                boolean stopOnUnlock = sharedPreferences.getBoolean(STOP_ON_UNLOCK_KEY, false);

                
                if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {

                    if (stopOnUnlock) {
                        
                        stopRecording("Recording stopped due to screen unlock or screen on.", true);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(STOP_ON_UNLOCK_KEY, false);  
                        editor.apply();
                    }
                }
            }
        };

        
        registerReceiver(screenUnlockReceiver, filter);
    }



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopRecording("Recording stopped by user.", true);
            return START_NOT_STICKY;
        }

        
        if (intent != null && intent.hasExtra("prefs_name")) {
            prefsName = intent.getStringExtra("prefs_name");
        }

        
        sharedPreferences = getSharedPreferences(prefsName, MODE_PRIVATE);

        
        showPreview = sharedPreferences.getBoolean("show_preview", false);
        previewSizePref = sharedPreferences.getString("preview_size", "medium");

        
        startCamera();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording("Recording stopped.", true);
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (floatingPreview != null) {
            floatingPreview.stop();
            floatingPreview = null;
        }
        unregisterReceiver(screenUnlockReceiver);
    }

    private void startCamera() {
        try {
            String cameraId = getCameraId();
            if (cameraId != null) {
                cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Camera permission not granted.");
                    return;
                }

                
                mediaRecorder = MediaRecorderManager.prepareMediaRecorder(VideoRecordingService.this, cameraCharacteristics, windowManager, prefsName);

                if (showPreview && floatingPreview == null) {
                    floatingPreview = new FloatingPreviewView(this, this);
                }

                cameraManager.openCamera(cameraId, stateCallback, null);
            } else {
                Log.e(TAG, "No camera found.");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception: " + e.getMessage());
        }
    }

    private String getCameraId() throws CameraAccessException {
        
        SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        String cameraPreference = prefs.getString("camera", "back_camera");

        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing != null) {
                if (cameraPreference.equals("front_camera") && lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    desiredCameraId = cameraId;
                    return cameraId;
                } else if (cameraPreference.equals("back_camera") && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    desiredCameraId = cameraId;
                    return cameraId;
                }
            }
        }
        return null;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            Log.d(TAG, "Camera opened.");

            if (showPreview && floatingPreview != null) {
                floatingPreview.show(previewSizePref, new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int width, int height) {
                        Surface surface = new Surface(surfaceTexture);
                        startRecordingWithPreview(surface);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                        
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                        
                        return true; 
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                        
                    }
                });
            } else {
                startRecording();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera disconnected.");
            stopRecording("Camera disconnected.", false);
            startMonitoringCameraAvailability();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            String errorMsg;
            switch (error) {
                case ERROR_CAMERA_DEVICE:
                    errorMsg = "Fatal (device)";
                    break;
                case ERROR_CAMERA_DISABLED:
                    errorMsg = "Device policy";
                    break;
                case ERROR_CAMERA_IN_USE:
                    errorMsg = "Camera in use";
                    break;
                case ERROR_CAMERA_SERVICE:
                    errorMsg = "Fatal (service)";
                    break;
                case ERROR_MAX_CAMERAS_IN_USE:
                    errorMsg = "Maximum cameras in use";
                    break;
                default:
                    errorMsg = "Unknown";
                    break;
            }
            Log.e(TAG, "Camera error (" + error + "): " + errorMsg);
            stopRecording("Camera error: " + errorMsg, false);
            startMonitoringCameraAvailability();
        }
    };

    private void startRecordingWithPreview(Surface previewSurface) {
        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorderManager.prepareMediaRecorder(VideoRecordingService.this, cameraCharacteristics, windowManager, prefsName);
            if (mediaRecorder == null) {
                Log.e(TAG, "Failed to prepare MediaRecorder");
                return;
            }
        }

        if (cameraDevice == null) {
            Log.e(TAG, "CameraDevice is null");
            return;
        }

        Surface recorderSurface = mediaRecorder.getSurface();

        try {
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(recorderSurface);
            captureRequestBuilder.addTarget(previewSurface);

            
            CameraSettings.applyCaptureSettings(captureRequestBuilder, cameraCharacteristics, frameRate);

            cameraDevice.createCaptureSession(Arrays.asList(recorderSurface, previewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        mediaRecorder.start();
                        Log.d(TAG, "Recording with preview started.");

                        
                        recordingStartTime = System.currentTimeMillis();
                        if (floatingPreview != null) {
                            floatingPreview.startTimer(recordingStartTime);
                        } else {
                            startTimer();
                        }

                        
                        saveRecordingState();

                        
                        scheduleStopRecording();

                        storageCheckHandler.post(storageCheckRunnable); 
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Capture session exception: " + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed.");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Start recording exception: " + e.getMessage());
        }
    }

    private void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = MediaRecorderManager.prepareMediaRecorder(VideoRecordingService.this, cameraCharacteristics, windowManager, prefsName);
            if (mediaRecorder == null) {
                Log.e(TAG, "Failed to prepare MediaRecorder");
                return;
            }
        }

        if (cameraDevice == null) {
            Log.e(TAG, "CameraDevice is null");
            return;
        }

        Surface recorderSurface = mediaRecorder.getSurface();
        try {
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(recorderSurface);

            
            CameraSettings.applyCaptureSettings(captureRequestBuilder, cameraCharacteristics, frameRate);

            cameraDevice.createCaptureSession(Collections.singletonList(recorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                        mediaRecorder.start();
                        Log.d(TAG, "Recording started.");

                        sendRecordingBroadcast("app.shashi.VigiLens.RECORDING_STARTED");

                        
                        recordingStartTime = System.currentTimeMillis();
                        startTimer();

                        
                        saveRecordingState();

                        
                        scheduleStopRecording();

                        storageCheckHandler.post(storageCheckRunnable); 
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Capture session exception: " + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed.");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Start recording exception: " + e.getMessage());
        }
    }

    private void startTimer() {
        if (timerHandler == null) {
            timerHandler = new Handler(Looper.getMainLooper());
        }
        if (timerRunnable == null) {
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    updateTimer();
                    timerHandler.postDelayed(this, 1000); 
                }
            };
        }
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler = null;
            timerRunnable = null;
        }
    }

    private void updateTimer() {
        long elapsedMillis = System.currentTimeMillis() - recordingStartTime;
        String formattedTime = formatElapsedTime(elapsedMillis);

        
        notificationHelper.updateNotificationTimer(formattedTime);
    }

    private String formatElapsedTime(long elapsedMillis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60;
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis);

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    private void scheduleStopRecording() {
        
        SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        String durationPreference = prefs.getString("recordingDuration", "unlimited");

        
        if (stopRunnable != null) {
            stopHandler.removeCallbacks(stopRunnable);
        }

        if (!"unlimited".equalsIgnoreCase(durationPreference)) {
            try {
                
                int durationMinutes = Integer.parseInt(durationPreference.trim());

                if (durationMinutes > 0) {
                    
                    long durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes);

                    
                    stopRunnable = () -> stopRecording("Recording stopped after duration limit.", true);
                    stopHandler.postDelayed(stopRunnable, durationMillis);

                    Log.d(TAG, "Scheduled stop recording in " + durationMinutes + " minutes.");
                } else {
                    Log.e(TAG, "Invalid duration minutes: " + durationMinutes);
                }
            } catch (NumberFormatException e) {
                
                Log.e(TAG, "Invalid duration preference: " + durationPreference, e);
                
            }
        } else {
            
            Log.d(TAG, "Recording duration set to unlimited; no stop scheduled.");
        }
    }

    private long getAvailableStorage() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private void stopRecording(String reason, boolean stopService) {
        try {
            if (captureSession != null) {
                captureSession.stopRepeating();
                captureSession.abortCaptures();
                captureSession.close();
                captureSession = null;
            }

            if (mediaRecorder != null) {
                try {
                    mediaRecorder.stop();
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException stopping MediaRecorder: " + e.getMessage());
                    
                }
                mediaRecorder.reset();
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception during stopRecording: " + e.getMessage());
        } finally {
            releaseCamera();
            releaseMediaRecorder();

            
            if (stopRunnable != null) {
                stopHandler.removeCallbacks(stopRunnable);
                stopRunnable = null;
            }

            storageCheckHandler.removeCallbacks(storageCheckRunnable); 
            clearRecordingState();
            Log.d(TAG, "Recording stopped.");
            sendRecordingBroadcast("app.shashi.VigiLens.RECORDING_STOPPED");
            
            stopTimer();

            
            if (floatingPreview != null) {
                floatingPreview.stop();
                floatingPreview = null;
            }

            if (stopService) {
                
                if (availabilityCallback != null) {
                    cameraManager.unregisterAvailabilityCallback(availabilityCallback);
                    availabilityCallback = null;
                }
                
                stopForeground(true);
                stopSelf();

                
                notificationHelper.showStopNotification(reason);
            } else {
                
                notificationHelper.updateNotificationForMonitoring(reason);
            }
        }
    }

    private void sendRecordingBroadcast(String action) {
        Intent intent = new Intent(action);
        sendBroadcast(intent);
        Log.d(TAG, "Broadcast sent: " + action);
    }

    private void releaseCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release(); 
            mediaRecorder = null;
        }
    }

    private void saveRecordingState() {
        SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_START_TIME, recordingStartTime);
        editor.putBoolean(PREF_IS_RECORDING, true);
        editor.apply();
    }

    
    private void clearRecordingState() {
        SharedPreferences sharedPreferences = getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PREF_START_TIME);
        editor.remove(PREF_IS_RECORDING);
        editor.apply();
    }



    private void startMonitoringCameraAvailability() {
        if (availabilityCallback == null) {
            availabilityCallback = new CameraManager.AvailabilityCallback() {
                @Override
                public void onCameraAvailable(@NonNull String cameraId) {
                    super.onCameraAvailable(cameraId);
                    if (cameraId.equals(desiredCameraId)) {
                        Log.d(TAG, "Camera is now available.");
                        cameraManager.unregisterAvailabilityCallback(availabilityCallback);
                        availabilityCallback = null;
                        startCamera();
                    }
                }

                @Override
                public void onCameraUnavailable(@NonNull String cameraId) {
                    super.onCameraUnavailable(cameraId);
                    Log.d(TAG, "Camera unavailable: " + cameraId);
                }
            };
        }
        cameraManager.registerAvailabilityCallback(availabilityCallback, new Handler(Looper.getMainLooper()));
    }

    
    @Override
    public void onTimerUpdate(String formattedTime) {
        
        notificationHelper.updateNotificationTimer(formattedTime);
    }

    @Override
    public void onStopButtonClicked() {
        
        stopRecording("Recording stopped by user.", true);
    }
}
