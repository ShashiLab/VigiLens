package app.shashi.VigiLens.feature.recording.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.main.MainActivity;
import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;

public class NotificationManager {

    private static final String CHANNEL_ID_SERVICE = "ManualRecordingServiceChannel";
    private static final String CHANNEL_ID_ERROR = "ManualRecordingErrorChannel";
    private static final String CHANNEL_ID_INFO = "ManualRecordingInfoChannel";
    private static final String CHANNEL_ID_ACTIONABLE = "ManualRecordingActionableChannel";

    private final Context context;
    private final android.app.NotificationManager notificationManager;

    public NotificationManager(Context context) {
        this.context = context;
        notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void createNotificationChannels() {
        
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Service Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
        );
        serviceChannel.setDescription("Ongoing service notifications, like recording status.");

        
        NotificationChannel errorChannel = new NotificationChannel(
                CHANNEL_ID_ERROR,
                "Error Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
        );
        errorChannel.setDescription("Notifications for errors or issues during recording.");
        errorChannel.enableLights(true);
        errorChannel.setLightColor(Color.RED);
        errorChannel.enableVibration(true);

        
        NotificationChannel infoChannel = new NotificationChannel(
                CHANNEL_ID_INFO,
                "Info Notifications",
                android.app.NotificationManager.IMPORTANCE_LOW
        );
        infoChannel.setDescription("General updates, like recording start or stop.");

        
        NotificationChannel actionableChannel = new NotificationChannel(
                CHANNEL_ID_ACTIONABLE,
                "Actionable Notifications",
                android.app.NotificationManager.IMPORTANCE_LOW
        );
        actionableChannel.setDescription("Notifications requiring user action, like stopping recording.");

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(serviceChannel);
            notificationManager.createNotificationChannel(errorChannel);
            notificationManager.createNotificationChannel(infoChannel);
            notificationManager.createNotificationChannel(actionableChannel);
        }
    }

    public Notification createNotification(String title, String text, PendingIntent pendingIntent, PendingIntent actionIntent) {
        return new NotificationCompat.Builder(context, CHANNEL_ID_ACTIONABLE)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stop)
                .setContentIntent(pendingIntent)
                .addAction(new NotificationCompat.Action(R.drawable.ic_stop, context.getString(R.string.notification_action_stop), actionIntent))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app_logo))
                .build();
    }

    public Notification getNotification() {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, VideoRecordingService.class);
        stopIntent.setAction(VideoRecordingService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return createNotification(
                context.getString(R.string.notification_title_recording),
                context.getString(R.string.notification_text_recording_in_progress),
                pendingIntent,
                stopPendingIntent
        );
    }

    public void showStopNotification(String reason) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_INFO)
                .setContentTitle(context.getString(R.string.notification_title_recording_stopped))
                .setContentText(reason)
                .setSmallIcon(R.drawable.ic_stop)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app_logo))
                .setAutoCancel(true)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(2, notification);
        }
    }

    public void updateNotificationTimer(String formattedTime) {
        
        Notification notification = createOngoingNotification(formattedTime);
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }

    private Notification createOngoingNotification(String timerText) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, VideoRecordingService.class);
        stopIntent.setAction(VideoRecordingService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID_ACTIONABLE)
                .setContentTitle(context.getString(R.string.notification_title_recording))
                .setContentText("Recording in progress: " + timerText)
                .setSmallIcon(R.drawable.ic_stop)
                .setContentIntent(pendingIntent)
                .addAction(new NotificationCompat.Action(R.drawable.ic_stop, context.getString(R.string.notification_action_stop), stopPendingIntent))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app_logo))
                .build();
    }

    public void updateNotificationForMonitoring(String reason) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        
        Intent stopIntent = new Intent(context, VideoRecordingService.class);
        stopIntent.setAction(VideoRecordingService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID_ACTIONABLE)
                .setContentTitle("Recording Stopped")
                .setContentText(reason + " - Monitoring for camera availability.")
                .setSmallIcon(R.drawable.ic_stop)
                .setContentIntent(pendingIntent)
                .addAction(new NotificationCompat.Action(R.drawable.ic_stop, context.getString(R.string.notification_action_stop), stopPendingIntent))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_app_logo))
                .build();

        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        }
    }
}
