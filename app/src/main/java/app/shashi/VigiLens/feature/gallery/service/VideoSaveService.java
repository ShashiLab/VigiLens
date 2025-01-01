package app.shashi.VigiLens.feature.gallery.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class VideoSaveService extends Service {

    private static final String CHANNEL_ID = "VideoSaveServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private static final String TAG = "VideoSaveService";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("video_path")) {
            String videoPath = intent.getStringExtra("video_path");
            File videoFile = new File(videoPath);

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notification_title_saving_video))
                    .setContentText(getString(R.string.notification_text_saving_video))
                    .setSmallIcon(R.drawable.ic_save)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app_logo))  
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)  
                    .setOnlyAlertOnce(true)
                    .setProgress(100, 0, false);

            startForeground(NOTIFICATION_ID, notificationBuilder.build());

            new Thread(() -> {
                boolean success = false;
                String errorMessage = null;

                long availableSpace = getAvailableSpace();
                if (availableSpace < videoFile.length()) {
                    errorMessage = getString(R.string.title_error_not_enough_space);
                    stopForeground(true);
                    stopSelf();
                    showCompletionNotification(false, getString(R.string.title_failed_to_save_video) + errorMessage);
                    return;
                }

                try {
                    success = saveVideo(videoFile);
                } catch (IOException e) {
                    Log.e(TAG, "Error saving video", e);
                    errorMessage = getFriendlyErrorMessage(e);
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error saving video", e);
                    errorMessage = "An unexpected error occurred";
                }

                stopForeground(true);
                stopSelf();
                showCompletionNotification(success, success ? getString(R.string.title_video_saved_to_gallery) : getString(R.string.title_failed_to_save_video) + errorMessage);
            }).start();

        } else {
            stopSelf();
        }

        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Video Save Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        serviceChannel.setDescription("Alerts about saving video files, such as completion or errors.");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }


    private boolean saveVideo(File videoFile) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

        ContentResolver resolver = getContentResolver();
        Uri videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        boolean success = false;
        if (videoUri != null) {
            try (FileInputStream inputStream = new FileInputStream(videoFile);
                 FileChannel inputChannel = inputStream.getChannel();
                 OutputStream outputStream = resolver.openOutputStream(videoUri);
                 WritableByteChannel outputChannel = Channels.newChannel(outputStream)) {

                long totalBytes = videoFile.length();
                long bytesCopied = 0;
                long chunkSize = 8 * 1024;  
                long startTime = System.currentTimeMillis();

                while (bytesCopied < totalBytes) {
                    long bytesTransferred = inputChannel.transferTo(bytesCopied, chunkSize, outputChannel);
                    bytesCopied += bytesTransferred;

                    int progress = (int) (bytesCopied * 100 / totalBytes);
                    long currentTime = System.currentTimeMillis();

                    if (currentTime - startTime > 1000) {  
                        updateNotification(progress);
                        startTime = currentTime;
                    }
                }

                success = true;
            }
        }

        return success;
    }


    private void updateNotification(int progress) {
        String progressText = String.format(getString(R.string.notification_saving_video_progress), progress);

        notificationBuilder.setProgress(100, progress, false)
                .setContentText(progressText);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }


    private void showCompletionNotification(boolean success, String message) {
        int largeIconResource = success ? R.drawable.ic_success_logo : R.drawable.ic_error_logo;

        NotificationCompat.Builder completionBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(success ? getString(R.string.notification_title_save_completed) : getString(R.string.notification_title_save_failed))
                .setContentText(message)
                .setSmallIcon(success ? R.drawable.ic_done : R.drawable.ic_error)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIconResource))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (!success) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }

        notificationManager.notify(NOTIFICATION_ID, completionBuilder.build());

        
        Intent broadcastIntent = new Intent("app.shashi.VigiLens.VIDEO_SAVE_COMPLETE");
        broadcastIntent.putExtra("success", success);
        broadcastIntent.setPackage(getPackageName());
        sendBroadcast(broadcastIntent);
    }


    private String getFriendlyErrorMessage(IOException e) {
        if (Objects.requireNonNull(e.getMessage()).contains("ENOSPC")) {
            return getString(R.string.title_error_not_enough_space);
        } else if (e.getMessage().contains("EACCES")) {
            return getString(R.string.notification_text_error_permission_denied);
        } else if (e.getMessage().contains("ENOTFOUND")) {
            return getString(R.string.notification_text_error_file_not_found);
        } else {
            return getString(R.string.notification_text_error_unknown);
        }
    }


    private long getAvailableSpace() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
    }
}
