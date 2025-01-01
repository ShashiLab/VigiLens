package app.shashi.VigiLens.feature.widget.lockscreen;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;
import app.shashi.VigiLens.feature.shortcut.lockscreen.LockAndRecordActivity;

public class LockAndRecordWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "LockAndRecordWidgetProvider";
    private static final String PREFS_NAME = "user_prefs_screen_off";
    private static final String GLOBAL_PREFS_NAME = "app_prefs";
    private static final String PREF_IS_RECORDING = "isRecording";
    private static final String STOP_ON_UNLOCK_KEY = "stop_recording_on_device_unlock";
    private static final String ACTION_LOCK_AND_RECORD = "app.shashi.VigiLens.widget.LOCK_AND_RECORD";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for LockAndRecordWidget");

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_LOCK_AND_RECORD.equals(intent.getAction())) {
            Log.d(TAG, "Lock and record action triggered");
            handleLockAndRecord(context);
        }
    }

    
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lock_and_record);

        
        Intent intent = new Intent(context, LockAndRecordWidgetProvider.class);
        intent.setAction(ACTION_LOCK_AND_RECORD);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_lock_and_record_icon, pendingIntent);

        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    
    private void handleLockAndRecord(Context context) {
        SharedPreferences globalSharedPreferences = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        if (isRecording) {
            
            stopCurrentRecording(context);
        }

        
        lockScreenAndStartRecording(context);
    }

    
    private void lockScreenAndStartRecording(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (devicePolicyManager != null) {
            try {
                
                devicePolicyManager.lockNow();

                
                SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("show_preview", false);  
                editor.putBoolean(STOP_ON_UNLOCK_KEY, true);  
                editor.apply();

                Intent serviceIntent = new Intent(context, VideoRecordingService.class);
                serviceIntent.setAction(Intent.ACTION_MAIN);
                serviceIntent.putExtra("prefs_name", PREFS_NAME);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }


                Log.d(TAG, "New recording started after locking the device.");
            } catch (SecurityException e) {
                Log.e(TAG, "Device admin permission not granted", e);
                Toast.makeText(context, context.getString(R.string.toast_device_admin_permission_required), Toast.LENGTH_LONG).show();

                
                Intent permissionIntent = new Intent(context, LockAndRecordActivity.class);
                permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(permissionIntent);
            }
        } else {
            Log.e(TAG, "DevicePolicyManager is null");
            Toast.makeText(context, context.getString(R.string.toast_device_admin_unavailable), Toast.LENGTH_LONG).show();
        }
    }

    
    private void stopCurrentRecording(Context context) {
        Log.d(TAG, "Stopping current recording...");

        
        Intent serviceIntent = new Intent(context, VideoRecordingService.class);
        context.stopService(serviceIntent);

        Log.d(TAG, "Current recording stopped.");
    }
}
