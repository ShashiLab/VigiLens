package app.shashi.VigiLens.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

public class DeviceUtils {
    public static String getDeviceInfo(Context context) {
        StringBuilder deviceInfo = new StringBuilder();

        deviceInfo.append("Model: ").append(Build.MODEL).append("\n");
        deviceInfo.append("Brand: ").append(Build.BRAND).append("\n");
        deviceInfo.append("Device: ").append(Build.DEVICE).append("\n");
        deviceInfo.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        deviceInfo.append("SDK Level: ").append(Build.VERSION.SDK_INT).append("\n");
        deviceInfo.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        deviceInfo.append("Product: ").append(Build.PRODUCT).append("\n");
        deviceInfo.append("Hardware: ").append(Build.HARDWARE).append("\n");
        deviceInfo.append("Build ID: ").append(Build.ID).append("\n");

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            deviceInfo.append("App Version: ")
                    .append(pInfo.versionName)
                    .append(" (")
                    .append(pInfo.versionCode)
                    .append(")\n");
        } catch (PackageManager.NameNotFoundException e) {
            deviceInfo.append("App Version: Unable to retrieve\n");
        }

        return deviceInfo.toString();
    }
}