package app.shashi.VigiLens.utils.admin;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class DeviceAdminManager extends android.app.admin.DeviceAdminReceiver {
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
    }
}
