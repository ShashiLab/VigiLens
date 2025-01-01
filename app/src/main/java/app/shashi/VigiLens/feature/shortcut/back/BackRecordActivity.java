
package app.shashi.VigiLens.feature.shortcut.back;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;

public class BackRecordActivity extends AppCompatActivity {

    private static final String TAG = "BackRecordActivity";
    private static final String PREFS_NAME = "user_prefs_back_record";
    private static final String GLOBAL_PREFS_NAME = "app_prefs";  
    private static final String PREF_IS_RECORDING = "isRecording";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate called");

        
        SharedPreferences globalSharedPreferences = getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);


        
        boolean isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        if (!isRecording) {
            
            startRecording();
        } else {
            
            stopRecording();
        }

        
        finish();
        Log.d(TAG, "BackRecordActivity finished");
    }

    
    private void startRecording() {
        Log.d(TAG, "Starting VideoRecordingService with back camera");

        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("camera", "back_camera");
        editor.apply();

        
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction(Intent.ACTION_MAIN);
        serviceIntent.putExtra("prefs_name", PREFS_NAME);

        try {
            
            startForegroundService(serviceIntent);
            Log.d(TAG, "VideoRecordingService started successfully with back camera");

            
            

        } catch (Exception e) {
            Log.e(TAG, "Failed to start VideoRecordingService with back camera", e);
            
            
        }
    }

    
    private void stopRecording() {
        Log.d(TAG, "Stopping VideoRecordingService");

        
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        try {
            stopService(serviceIntent);
            Log.d(TAG, "VideoRecordingService stopped successfully");

            
            

        } catch (Exception e) {
            finish();
            Log.e(TAG, "Failed to stop VideoRecordingService", e);
            
            
        }
    }

    
}
