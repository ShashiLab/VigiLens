package app.shashi.VigiLens.feature.widget.quick;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;

public class QuickRecordWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "QuickRecordWidgetProvider";
    private static final String PREFS_NAME = "user_prefs";
    private static final String GLOBAL_PREFS_NAME = "app_prefs";  
    private static final String PREF_IS_RECORDING = "isRecording";
    private static final String ACTION_TOGGLE_QUICK_RECORDING = "app.shashi.VigiLens.widget.TOGGLE_QUICK_RECORDING";

    
    private static final String ACTION_RECORDING_STARTED = "app.shashi.VigiLens.RECORDING_STARTED";
    private static final String ACTION_RECORDING_STOPPED = "app.shashi.VigiLens.RECORDING_STOPPED";

    
    private SharedPreferences globalSharedPreferences;

    
    private final BroadcastReceiver recordingStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_RECORDING_STARTED.equals(intent.getAction())) {
                updateWidgetRecordingState(context, true);
            } else if (ACTION_RECORDING_STOPPED.equals(intent.getAction())) {
                updateWidgetRecordingState(context, false);
            }
        }
    };

    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        globalSharedPreferences = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);

        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RECORDING_STARTED);
        filter.addAction(ACTION_RECORDING_STOPPED);
        context.getApplicationContext().registerReceiver(recordingStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        context.getApplicationContext().unregisterReceiver(recordingStateReceiver);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for QuickRecordWidget");

        
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TOGGLE_QUICK_RECORDING.equals(intent.getAction())) {
            Log.d(TAG, "Quick recording toggle requested");
            toggleRecording(context);
        }
    }

    
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences globalSharedPreferences = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_quick_record);
        if (isRecording) {
            views.setImageViewResource(R.id.widget_quick_record_icon, R.mipmap.ic_stop_recording); 
        } else {
            views.setImageViewResource(R.id.widget_quick_record_icon, R.mipmap.ic_quick_record); 
        }

        
        Intent intent = new Intent(context, QuickRecordWidgetProvider.class);
        intent.setAction(ACTION_TOGGLE_QUICK_RECORDING);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_quick_record_icon, pendingIntent);

        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    
    private void toggleRecording(Context context) {
        SharedPreferences globalSharedPreferences = context.getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        if (!isRecording) {
            startRecording(context);
        } else {
            stopRecording(context);
        }

        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, QuickRecordWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    
    private void startRecording(Context context) {
        Log.d(TAG, "Starting VideoRecordingService");

        
        Intent serviceIntent = new Intent(context, VideoRecordingService.class);
        serviceIntent.setAction(Intent.ACTION_MAIN);
        serviceIntent.putExtra("prefs_name", PREFS_NAME);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "VideoRecordingService started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start VideoRecordingService", e);
        }
    }

    
    private void stopRecording(Context context) {
        Log.d(TAG, "Stopping VideoRecordingService");

        Intent serviceIntent = new Intent(context, VideoRecordingService.class);
        try {
            context.stopService(serviceIntent);
            Log.d(TAG, "VideoRecordingService stopped successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop VideoRecordingService", e);
        }
    }

    
    private void updateWidgetRecordingState(Context context, boolean isRecording) {
        SharedPreferences.Editor editor = globalSharedPreferences.edit();
        editor.putBoolean(PREF_IS_RECORDING, isRecording);
        editor.apply();

        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, QuickRecordWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetComponent);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
