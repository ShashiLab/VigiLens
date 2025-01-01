package app.shashi.VigiLens.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import app.shashi.VigiLens.R;

public class CommonUtils {

    
    public static void sendFeedbackEmail(Context context) {
        String appName = context.getString(R.string.app_name);
        String appVersion = getAppVersion(context);
        String deviceModel = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        String appId = context.getPackageName();

        String subject = "Feedback: " + appName;
        String body = "App Name: " + appName + "\n" +
                "App ID: " + appId + "\n" +
                "Version: " + appVersion + "\n" +
                "Device Model: " + deviceModel + "\n" +
                "Android Version: " + androidVersion + "\n\n" +
                context.getString(R.string.email_feedback_prompt) +
                "\n----------------------------------------\n\n";

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"AppsByShashi@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.setPackage("com.google.android.gm");

        if (emailIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(emailIntent);
        } else {
            Intent chooserIntent = Intent.createChooser(emailIntent, context.getString(R.string.title_app_feedback));
            if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooserIntent);
            } else {
                Toast.makeText(context, context.getString(R.string.toast_no_email_clients), Toast.LENGTH_SHORT).show();
            }
        }
    }

    
    public static String getAppVersion(Context context) {
        String version = "1.0"; 
        try {
            version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            
            Log.e("Utils", "Error getting app version: " + e.getMessage());
        }
        return version;
    }

}
