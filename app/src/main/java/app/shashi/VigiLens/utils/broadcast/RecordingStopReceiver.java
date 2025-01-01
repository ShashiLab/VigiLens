package app.shashi.VigiLens.utils.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import app.shashi.VigiLens.feature.main.MainActivity;

public class RecordingStopReceiver extends BroadcastReceiver {

    private static final String TAG = "RecordingStopReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("app.shashi.VigiLens.RECORDING_STOPPED".equals(intent.getAction())) {
            Log.d(TAG, "Received stop recording broadcast");
            
            Intent stopIntent = new Intent(context, MainActivity.class);
            stopIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(stopIntent);
        }
    }
}
