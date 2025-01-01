package app.shashi.VigiLens.feature.recording.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MediaRecorderManager {
    private static final String TAG = "MediaRecorderHelper";

    
    public static MediaRecorder prepareMediaRecorder(Context context, CameraCharacteristics cameraCharacteristics, WindowManager windowManager, String prefsName) {
        
        SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

        String resolutionPreference = prefs.getString("resolution", "720p");
        MediaRecorder mediaRecorder = new MediaRecorder();

        String videoPath = getVideoFilePath(context);

        int videoWidth = 1280, videoHeight = 720;
        int bitRate = 2000000; 
        int frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;

        boolean useHevc = isHevcSupported();

        if ("high".equals(resolutionPreference)) {
            List<Size> validSizes = getSupportedVideoSizes(cameraCharacteristics);
            if (!validSizes.isEmpty()) {
                boolean recorderPrepared = false;
                int attemptCount = 0;

                
                while (!recorderPrepared && attemptCount < validSizes.size()) {
                    Size selectedSize = validSizes.get(attemptCount);
                    videoWidth = selectedSize.getWidth();
                    videoHeight = selectedSize.getHeight();
                    frameRate = getMaxSupportedFrameRate(cameraCharacteristics, videoWidth, videoHeight);
                    bitRate = calculateBitRate(videoWidth, videoHeight, frameRate, useHevc);

                    mediaRecorder.reset();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mediaRecorder.setOutputFile(videoPath);

                    mediaRecorder.setVideoEncodingBitRate(bitRate);
                    mediaRecorder.setVideoFrameRate(frameRate);
                    mediaRecorder.setVideoSize(videoWidth, videoHeight);

                    
                    if (useHevc) {
                        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
                    } else {
                        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                    }

                    
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mediaRecorder.setAudioEncodingBitRate(128000);
                    mediaRecorder.setAudioSamplingRate(48000);
                    mediaRecorder.setAudioChannels(2);

                    
                    int rotation = windowManager.getDefaultDisplay().getRotation();
                    Integer sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    if (sensorOrientation == null) {
                        sensorOrientation = 0;
                    }
                    int orientationHint = (sensorOrientation - getRotationDegrees(rotation) + 360) % 360;
                    mediaRecorder.setOrientationHint(orientationHint);

                    try {
                        mediaRecorder.prepare();
                        recorderPrepared = true;
                        Log.d(TAG, "MediaRecorder prepared successfully with resolution " + videoWidth + "x" + videoHeight);
                    } catch (IOException | IllegalStateException e) {
                        Log.e(TAG, "MediaRecorder prepare failed: " + e.getMessage());
                        attemptCount++;
                        if (attemptCount < validSizes.size()) {
                            Log.d(TAG, "Attempting to prepare MediaRecorder with lower resolution.");
                        } else {
                            Log.e(TAG, "All resolutions failed to prepare MediaRecorder.");
                            return null;
                        }
                    }
                }
            } else {
                Log.e(TAG, "No valid video sizes available for 'high' resolution.");
                return null;
            }
        } else {
            
            switch (resolutionPreference) {
                case "1080p":
                    videoWidth = 1920;
                    videoHeight = 1080;
                    bitRate = useHevc ? 10000000 : 16000000;  
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                case "720p":
                    videoWidth = 1280;
                    videoHeight = 720;
                    bitRate = useHevc ? 5000000 : 8000000;  
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                case "480p":
                    videoWidth = 720;
                    videoHeight = 480;
                    bitRate = useHevc ? 2500000 : 4000000;  
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                case "360p":
                    videoWidth = 480;
                    videoHeight = 360;
                    bitRate = useHevc ? 1250000 : 2000000;   
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                case "240p":
                    videoWidth = 352;
                    videoHeight = 240;
                    bitRate = useHevc ? 750000 : 1000000;   
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                case "144p":
                    videoWidth = 176;
                    videoHeight = 144;
                    bitRate = useHevc ? 400000 : 600000;   
                    frameRate = isFrameRateSupported(cameraCharacteristics, videoWidth, videoHeight, 60) ? 60 : 30;
                    break;
                default:
                    Log.w(TAG, "Unknown resolution preference: " + resolutionPreference + ". Using default settings.");
                    break;
            }

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(videoPath);

            mediaRecorder.setVideoEncodingBitRate(bitRate);
            mediaRecorder.setVideoFrameRate(frameRate);
            mediaRecorder.setVideoSize(videoWidth, videoHeight);

            
            if (useHevc) {
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.HEVC);
            } else {
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            }

            
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(48000);
            mediaRecorder.setAudioChannels(2);

            
            int rotation = windowManager.getDefaultDisplay().getRotation();
            Integer sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (sensorOrientation == null) {
                sensorOrientation = 0;
            }
            int orientationHint = (sensorOrientation - getRotationDegrees(rotation) + 360) % 360;
            mediaRecorder.setOrientationHint(orientationHint);

            try {
                mediaRecorder.prepare();
                Log.d(TAG, "MediaRecorder prepared successfully with resolution " + videoWidth + "x" + videoHeight);
            } catch (IOException e) {
                Log.e(TAG, "MediaRecorder prepare failed: " + e.getMessage());
                return null;
            }
        }

        return mediaRecorder;
    }

    
    private static boolean isHevcSupported() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase("video/hevc")) {
                    return true;
                }
            }
        }
        return false;
    }

    
    private static List<Size> getSupportedVideoSizes(CameraCharacteristics characteristics) {
        List<Size> validSizes = new ArrayList<>();
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            Size[] sizes = map.getOutputSizes(MediaRecorder.class);
            if (sizes != null && sizes.length > 0) {
                validSizes.addAll(Arrays.asList(sizes));
                
                validSizes.sort(new Comparator<Size>() {
                    @Override
                    public int compare(Size o1, Size o2) {
                        return Long.compare((long) o2.getWidth() * o2.getHeight(), (long) o1.getWidth() * o1.getHeight());
                    }
                });
            }
        }
        return validSizes;
    }

    
    private static int getRotationDegrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    
    private static String getVideoFilePath(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (dir == null) {
            dir = new File(context.getFilesDir(), "movies");
        }
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                Log.e(TAG, "Failed to create video directory.");
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, timeStamp + ".mp4").getAbsolutePath();
    }

    
    private static int getMaxSupportedFrameRate(CameraCharacteristics characteristics, int width, int height) {
        int maxFrameRate = 30; 
        Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (fpsRanges != null) {
            int highestFrameRate = 0;
            for (Range<Integer> range : fpsRanges) {
                if (range.getUpper() > highestFrameRate) {
                    highestFrameRate = range.getUpper();
                }
            }
            if (isFrameRateSupported(characteristics, width, height, highestFrameRate)) {
                maxFrameRate = highestFrameRate;
            }
            Log.d(TAG, "Max supported frame rate: " + maxFrameRate + "fps for resolution " + width + "x" + height);
        }
        return maxFrameRate;
    }

    
    private static boolean isFrameRateSupported(CameraCharacteristics characteristics, int resolutionWidth, int resolutionHeight, int desiredFrameRate) {
        Range<Integer>[] fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (fpsRanges != null) {
            for (Range<Integer> range : fpsRanges) {
                if (range.contains(desiredFrameRate)) {
                    Size[] sizes = Objects.requireNonNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(MediaRecorder.class);
                    if (sizes != null) {
                        for (Size size : sizes) {
                            if (size.getWidth() == resolutionWidth && size.getHeight() == resolutionHeight) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    
    private static int calculateBitRate(int width, int height, int frameRate, boolean useHevc) {
        
        float compressionRatio = useHevc ? 0.5f : 1.0f; 

        
        int bitRate = (int) (width * height * frameRate * compressionRatio * 0.1f);
        
        bitRate = Math.max(bitRate, 2000000);  
        bitRate = Math.min(bitRate, 50000000); 
        return bitRate;
    }
}
