package app.shashi.VigiLens.feature.recording.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import app.shashi.VigiLens.R;

public class FloatingPreviewView {

    private final Context context;
    private final WindowManager windowManager;
    private View floatingView;
    private MaterialButton previewStopButton;
    private TextView previewTimer;
    private WindowManager.LayoutParams params;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long recordingStartTime;
    private final TimerUpdateListener timerUpdateListener;
    private MaterialCardView floatingPreviewContainer;
    private MaterialCardView previewTimerContainer;
    private FrameLayout stopButtonContainer;

    public interface TimerUpdateListener {
        void onTimerUpdate(String formattedTime);
        void onStopButtonClicked();
    }

    public FloatingPreviewView(Context context, TimerUpdateListener listener) {
        this.context = context;
        this.timerUpdateListener = listener;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show(String previewSizePref, TextureView.SurfaceTextureListener surfaceTextureListener) {
        Context themedContext = new android.view.ContextThemeWrapper(context, R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        floatingView = inflater.inflate(R.layout.floating_preview, null);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Determine target size based on preference
        float targetSizePercentage;
        switch (previewSizePref) {
            case "extra_small":
                targetSizePercentage = 0.15f;
                break;
            case "small":
                targetSizePercentage = 0.25f;
                break;
            case "medium":
                targetSizePercentage = 0.5f;
                break;
            case "large":
                targetSizePercentage = 0.75f;
                break;
            case "extra_large":
                targetSizePercentage = 0.9f;
                break;
            default:
                targetSizePercentage = 0.5f;
                break;
        }

        boolean isPortrait = screenHeight > screenWidth;
        float aspectRatio = isPortrait ? 9f/16f : 16f/9f;

        int previewWidth, previewHeight;
        if (isPortrait) {
            previewWidth = (int) (screenWidth * targetSizePercentage);
            previewHeight = (int) (previewWidth / aspectRatio);

            if (previewHeight > screenHeight * targetSizePercentage) {
                previewHeight = (int) (screenHeight * targetSizePercentage);
                previewWidth = (int) (previewHeight * aspectRatio);
            }
        } else {
            previewHeight = (int) (screenHeight * targetSizePercentage);
            previewWidth = (int) (previewHeight * aspectRatio);

            if (previewWidth > screenWidth * targetSizePercentage) {
                previewWidth = (int) (screenWidth * targetSizePercentage);
                previewHeight = (int) (previewWidth / aspectRatio);
            }
        }

        if (previewSizePref.equals("medium")) {
            int minimumHeight = dpToPx(600);
            if (previewHeight < minimumHeight) {
                previewHeight = minimumHeight;
                previewWidth = (int) (previewHeight * aspectRatio);
            }
        }

        int layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        params = new WindowManager.LayoutParams(
                previewWidth,
                previewHeight,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = dpToPx(100);

        TextureView previewTextureView = floatingView.findViewById(R.id.floating_texture_view);
        previewTextureView.setSurfaceTextureListener(surfaceTextureListener);

        windowManager.addView(floatingView, params);

        floatingPreviewContainer = floatingView.findViewById(R.id.floating_preview_container);
        previewTimerContainer = floatingView.findViewById(R.id.preview_timer_container);
        previewStopButton = floatingView.findViewById(R.id.preview_stop_button);
        previewTimer = floatingView.findViewById(R.id.preview_timer);
        stopButtonContainer = floatingView.findViewById(R.id.stop_button_container);

        previewStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerUpdateListener != null) {
                    timerUpdateListener.onStopButtonClicked();
                }
            }
        });

        adjustUIElements(previewSizePref);

        makePreviewWindowDraggable();
    }
    
    private void adjustUIElements(String previewSizePref) {
        
        int buttonSizePx;
        int iconSizePx;
        float textSizeSp;
        int bottomMarginPx;
        float cardCornerRadiusPx;
        float timerCornerRadiusPx;

        
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        switch (previewSizePref) {
            case "extra_small":
                buttonSizePx = dpToPx(24f);
                iconSizePx = dpToPx(12f);
                textSizeSp = 8f;
                bottomMarginPx = dpToPx(4f);
                cardCornerRadiusPx = dpToPx(4f);
                timerCornerRadiusPx = dpToPx(4f);
                break;
            case "small":
                buttonSizePx = dpToPx(40f);
                iconSizePx = dpToPx(20f);
                textSizeSp = 12f;
                bottomMarginPx = dpToPx(8f);
                cardCornerRadiusPx = dpToPx(8f);
                timerCornerRadiusPx = dpToPx(6f);
                break;
            case "medium":
                buttonSizePx = dpToPx(56f);
                iconSizePx = dpToPx(28f);
                textSizeSp = 22f;
                bottomMarginPx = dpToPx(16f);
                cardCornerRadiusPx = dpToPx(12f);
                timerCornerRadiusPx = dpToPx(8f);
                break;
            case "large":
                buttonSizePx = dpToPx(64f);
                iconSizePx = dpToPx(32f);
                textSizeSp = 22f;
                bottomMarginPx = dpToPx(20f);
                cardCornerRadiusPx = dpToPx(14f);
                timerCornerRadiusPx = dpToPx(12f);
                break;
            case "extra_large":
                buttonSizePx = dpToPx(70f);
                iconSizePx = dpToPx(35f);
                textSizeSp = 26f;
                bottomMarginPx = dpToPx(24f);
                cardCornerRadiusPx = dpToPx(16f);
                timerCornerRadiusPx = dpToPx(14f);
                break;
            default:
                
                buttonSizePx = dpToPx(56f);
                iconSizePx = dpToPx(28f);
                textSizeSp = 22f;
                bottomMarginPx = dpToPx(20f);
                cardCornerRadiusPx = dpToPx(14f);
                timerCornerRadiusPx = dpToPx(12f);
                break;
        }

        
        ViewGroup.LayoutParams buttonParams = previewStopButton.getLayoutParams();
        buttonParams.width = buttonSizePx;
        buttonParams.height = buttonSizePx;
        previewStopButton.setLayoutParams(buttonParams);

        
        previewStopButton.setIconSize(iconSizePx);

        
        previewTimer.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, textSizeSp);

        
        previewTimerContainer.setRadius(timerCornerRadiusPx);

        
        floatingPreviewContainer.setRadius(cardCornerRadiusPx);

        
        FrameLayout.LayoutParams stopButtonLayoutParams = (FrameLayout.LayoutParams) stopButtonContainer.getLayoutParams();
        stopButtonLayoutParams.bottomMargin = bottomMarginPx;
        stopButtonContainer.setLayoutParams(stopButtonLayoutParams);
    }

    
    private int dpToPx(float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    
    private int spToPx(float sp) {
        return Math.round(sp * context.getResources().getDisplayMetrics().scaledDensity);
    }

    public void startTimer(long startTime) {
        recordingStartTime = startTime;
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimer();
                timerHandler.postDelayed(this, 1000); 
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimer() {
        long elapsedMillis = System.currentTimeMillis() - recordingStartTime;
        String formattedTime = formatElapsedTime(elapsedMillis);

        
        if (previewTimer != null) {
            previewTimer.setText(formattedTime);
        }
        if (timerUpdateListener != null) {
            timerUpdateListener.onTimerUpdate(formattedTime);
        }
    }

    private String formatElapsedTime(long elapsedMillis) {
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public void stop() {
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void makePreviewWindowDraggable() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private long touchStartTime;
            private static final int MAX_CLICK_DURATION = 200;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;
                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - touchStartTime;
                        if (clickDuration < MAX_CLICK_DURATION) {
                            
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }
}
