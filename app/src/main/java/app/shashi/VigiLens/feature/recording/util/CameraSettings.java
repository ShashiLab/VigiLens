package app.shashi.VigiLens.feature.recording.util;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;

public class CameraSettings {

    public static void applyCaptureSettings(CaptureRequest.Builder captureRequestBuilder, CameraCharacteristics cameraCharacteristics, int frameRate) {
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);

        
        setAeTargetFpsRange(captureRequestBuilder, cameraCharacteristics, frameRate);

        
        boolean videoStabilizationSupported = isVideoStabilizationSupported(cameraCharacteristics);
        boolean opticalStabilizationSupported = isOpticalStabilizationSupported(cameraCharacteristics);

        if (videoStabilizationSupported) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
            captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
        } else if (opticalStabilizationSupported) {
            captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
        }

        if (isHdrSupported(cameraCharacteristics)) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);
        }

        
        captureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, getHighestQualityNoiseReductionMode(cameraCharacteristics));
        captureRequestBuilder.set(CaptureRequest.EDGE_MODE, getHighestQualityEdgeMode(cameraCharacteristics));

        if (isFaceDetectionSupported(cameraCharacteristics)) {
            captureRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);
        }

        
        
        
    }

    private static void setAeTargetFpsRange(CaptureRequest.Builder builder, CameraCharacteristics characteristics, int frameRate) {
        Range<Integer>[] availableFpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (availableFpsRanges != null) {
            Range<Integer> chosenFpsRange = null;
            for (Range<Integer> range : availableFpsRanges) {
                if (range.contains(frameRate)) {
                    if (chosenFpsRange == null || range.getUpper() < chosenFpsRange.getUpper()) {
                        chosenFpsRange = range;
                    }
                }
            }
            if (chosenFpsRange != null) {
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, chosenFpsRange);
            }
        }
    }

    private static boolean isHdrSupported(CameraCharacteristics cameraCharacteristics) {
        int[] availableSceneModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        if (availableSceneModes != null) {
            for (int mode : availableSceneModes) {
                if (mode == CaptureRequest.CONTROL_SCENE_MODE_HDR) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isVideoStabilizationSupported(CameraCharacteristics cameraCharacteristics) {
        int[] availableVideoStabilizationModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        if (availableVideoStabilizationModes != null) {
            for (int mode : availableVideoStabilizationModes) {
                if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOpticalStabilizationSupported(CameraCharacteristics cameraCharacteristics) {
        int[] modes = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (modes != null) {
            for (int mode : modes) {
                if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isFaceDetectionSupported(CameraCharacteristics cameraCharacteristics) {
        int[] availableFaceDetectModes = cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        if (availableFaceDetectModes != null) {
            for (int mode : availableFaceDetectModes) {
                if (mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE ||
                        mode == CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getHighestQualityNoiseReductionMode(CameraCharacteristics characteristics) {
        int[] availableModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
        if (availableModes != null) {
            if (contains(availableModes, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)) {
                return CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY;
            } else if (contains(availableModes, CaptureRequest.NOISE_REDUCTION_MODE_FAST)) {
                return CaptureRequest.NOISE_REDUCTION_MODE_FAST;
            }
        }
        return CaptureRequest.NOISE_REDUCTION_MODE_OFF;
    }

    private static int getHighestQualityEdgeMode(CameraCharacteristics characteristics) {
        int[] availableModes = characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);
        if (availableModes != null) {
            if (contains(availableModes, CaptureRequest.EDGE_MODE_HIGH_QUALITY)) {
                return CaptureRequest.EDGE_MODE_HIGH_QUALITY;
            } else if (contains(availableModes, CaptureRequest.EDGE_MODE_FAST)) {
                return CaptureRequest.EDGE_MODE_FAST;
            }
        }
        return CaptureRequest.EDGE_MODE_OFF;
    }

    private static boolean contains(int[] modes, int modeToCheck) {
        for (int mode : modes) {
            if (mode == modeToCheck) {
                return true;
            }
        }
        return false;
    }
}
