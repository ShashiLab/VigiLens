package app.shashi.VigiLens.feature.shortcut.lockscreen;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;
import app.shashi.VigiLens.utils.admin.DeviceAdminManager;

public class LockAndRecordActivity extends AppCompatActivity {

    private static final String TAG = "LockAndRecordActivity";
    private static final String PREFS_NAME = "user_prefs_screen_off";  
    private static final String GLOBAL_PREFS_NAME = "app_prefs";       
    private static final String PREF_IS_RECORDING = "isRecording";     
    private static final String STOP_ON_UNLOCK_KEY = "stop_recording_on_device_unlock";  

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        
        SharedPreferences globalSharedPreferences = getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE);
        boolean isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        if (isRecording) {
            
            stopCurrentRecording();
        }

        
        if (devicePolicyManager != null) {
            try {
                devicePolicyManager.lockNow();
                
                startRecordingService();
            } catch (SecurityException e) {
                
                Toast.makeText(this, getString(R.string.toast_device_admin_permission_required), Toast.LENGTH_LONG).show();

                
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                ComponentName componentName = new ComponentName(this, DeviceAdminManager.class);  
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.message_device_admin_explanation));
                startActivity(intent);
            }
        } else {
            
            Toast.makeText(this, getString(R.string.toast_device_admin_unavailable), Toast.LENGTH_LONG).show();
        }

        
        finish();
    }

    
    private void stopCurrentRecording() {
        Log.d(TAG, "Stopping current recording...");

        
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        stopService(serviceIntent);

        Log.d(TAG, "Current recording stopped.");
    }

    
    private void startRecordingService() {
        Log.d(TAG, "Starting new recording...");

        
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("show_preview", false);  
        editor.putBoolean(STOP_ON_UNLOCK_KEY, true);  
        editor.apply();

        
        Intent serviceIntent = new Intent(this, VideoRecordingService.class);
        serviceIntent.setAction(Intent.ACTION_MAIN);
        serviceIntent.putExtra("prefs_name", PREFS_NAME);

        try {
            
            startService(serviceIntent);  

            Log.d(TAG, "New recording started.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VideoRecordingService", e);
        }
    }
}
