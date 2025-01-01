package app.shashi.VigiLens.feature.gallery.util;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.concurrent.TimeUnit;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.gallery.service.VideoSaveService;

public class VideoSaver {

    public interface SaveVideoCallback {
        void onSaveSuccess();

        void onSaveFailure(String errorMessage);
    }

    private static VideoSaver instance;
    private RewardedAd rewardedAd;
    private final Context appContext;
    private SaveVideoCallback callback;
    private static final String CHANNEL_ID = "VideoSaveServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private final Handler handler;
    private Runnable timeoutRunnable;
    private boolean isAdShownOrSkipped = false;

    private VideoSaver(Context context) {
        this.appContext = context.getApplicationContext();
        MobileAds.initialize(this.appContext, initializationStatus -> {
            
        });
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    public static VideoSaver getInstance(Context context) {
        if (instance == null) {
            instance = new VideoSaver(context);
        }
        return instance;
    }

    
    public void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        String adUnitId = appContext.getString(R.string.rewarded_ad_for_saving_video_to_gallery);

        RewardedAd.load(appContext, adUnitId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }
        });
    }

    
    public void saveVideoWithAd(Activity activity, String videoPath, SaveVideoCallback callback) {
        this.callback = callback;

        
        File videoFile = new File(videoPath);
        long videoSize = videoFile.length();
        long extraSpace = 50 * 1024 * 1024; 

        long availableSpace = getAvailableSpace();

        if (availableSpace < (videoSize + extraSpace)) {
            
            String errorMessage = activity.getString(R.string.title_error_not_enough_space);
            callback.onSaveFailure(errorMessage);
            showSaveResultDialog(activity, false, errorMessage);
            return;
        }

        if (rewardedAd != null) {
            
            isAdShownOrSkipped = false;
            rewardedAd.show(activity, rewardItem -> {
                if (!isAdShownOrSkipped) {
                    isAdShownOrSkipped = true;
                    cancelAdTimeout();
                    startVideoSaveService(videoPath);
                    
                    loadRewardedAd();
                }
            });

            
            timeoutRunnable = () -> {
                if (!isAdShownOrSkipped) {
                    isAdShownOrSkipped = true;
                    Toast.makeText(activity, activity.getString(R.string.toast_ad_not_available), Toast.LENGTH_SHORT).show();
                    startVideoSaveService(videoPath);
                    
                    loadRewardedAd();
                }
            };
            handler.postDelayed(timeoutRunnable, TimeUnit.SECONDS.toMillis(5)); 
        } else {
            
            Toast.makeText(activity, activity.getString(R.string.toast_ad_not_available), Toast.LENGTH_SHORT).show();
            startVideoSaveService(videoPath);
            
            loadRewardedAd();
        }

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerBroadcastReceiver(activity);
        }
    }

    
    private void cancelAdTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    
    private void startVideoSaveService(String videoPath) {
        Intent serviceIntent = new Intent(appContext, VideoSaveService.class);
        serviceIntent.putExtra("video_path", videoPath);
        ContextCompat.startForegroundService(appContext, serviceIntent);
    }

    
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void registerBroadcastReceiver(Activity activity) {
        
        
        BroadcastReceiver saveResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra("success", false);
                String errorMessage = intent.getStringExtra("error_message");

                if (success) {
                    callback.onSaveSuccess();
                    showSaveResultDialog(activity, true, null);
                } else {
                    callback.onSaveFailure(errorMessage);
                    showSaveResultDialog(activity, false, errorMessage);
                }

                
                try {
                    appContext.unregisterReceiver(this);
                } catch (IllegalArgumentException e) {
                    
                    Log.e("ReceiverUnregister", "Receiver was not registered: ", e);
                }

            }
        };

        IntentFilter filter = new IntentFilter("app.shashi.VigiLens.VIDEO_SAVE_COMPLETE");

        appContext.registerReceiver(saveResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    
    public void showSaveResultDialog(Activity activity, boolean isSuccess, String message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_save_result, null);
        builder.setView(dialogView);

        ImageView icon = dialogView.findViewById(R.id.dialog_icon);
        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView msg = dialogView.findViewById(R.id.dialog_message);
        ImageView closeIcon = dialogView.findViewById(R.id.dialog_close_icon);

        if (isSuccess) {
            icon.setImageResource(R.drawable.ic_success_logo);
            title.setText(activity.getString(R.string.title_video_saved));
            msg.setText(activity.getString(R.string.message_video_saved));
        } else {
            icon.setImageResource(R.drawable.ic_error_logo);
            title.setText(activity.getString(R.string.title_save_failed));
            msg.setText(message != null ? message : activity.getString(R.string.message_save_failed));
        }

        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();

        closeIcon.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Video Save Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        serviceChannel.setDescription("Alerts about saving video files, such as completion or errors.");

        NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    
    public void initializeSavingNotification() {
        NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setContentTitle(appContext.getString(R.string.notification_title_saving_video))
                .setContentText(appContext.getString(R.string.notification_text_saving_video))
                .setSmallIcon(R.drawable.ic_save)
                .setLargeIcon(BitmapFactory.decodeResource(appContext.getResources(), R.drawable.ic_app_logo))  
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)  
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    
    private long getAvailableSpace() {
        StatFs stat;
        stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return stat.getAvailableBytes();
    }
}
