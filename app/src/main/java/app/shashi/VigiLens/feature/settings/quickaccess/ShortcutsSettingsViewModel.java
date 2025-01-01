package app.shashi.VigiLens.feature.settings.quickaccess;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ShortcutsSettingsViewModel extends AndroidViewModel {

    private static final String PREFS_LOCK_AND_RECORD = "user_prefs_lock_and_record";
    private static final String PREFS_QUICK_RECORD = "user_prefs";
    private static final String PREFS_FRONT_RECORD = "user_prefs_front_record";
    private static final String PREFS_BACK_RECORD = "user_prefs_back_record";

    private final SharedPreferences lockAndRecordPrefs;
    private final SharedPreferences quickRecordPrefs;
    private final SharedPreferences frontRecordPrefs;
    private final SharedPreferences backRecordPrefs;

    
    private final MutableLiveData<Boolean> lockAndRecordEnabled = new MutableLiveData<>();

    
    private final MutableLiveData<Boolean> quickRecordEnabled = new MutableLiveData<>();

    
    private final MutableLiveData<Boolean> frontRecordEnabled = new MutableLiveData<>();

    
    private final MutableLiveData<Boolean> backRecordEnabled = new MutableLiveData<>();

    public ShortcutsSettingsViewModel(@NonNull Application application) {
        super(application);
        lockAndRecordPrefs = application.getSharedPreferences(PREFS_LOCK_AND_RECORD, Context.MODE_PRIVATE);
        quickRecordPrefs = application.getSharedPreferences(PREFS_QUICK_RECORD, Context.MODE_PRIVATE);
        frontRecordPrefs = application.getSharedPreferences(PREFS_FRONT_RECORD, Context.MODE_PRIVATE);
        backRecordPrefs = application.getSharedPreferences(PREFS_BACK_RECORD, Context.MODE_PRIVATE);
        loadSettings();
    }

    private void loadSettings() {
        lockAndRecordEnabled.setValue(lockAndRecordPrefs.getBoolean("shortcut_enabled", false));
        quickRecordEnabled.setValue(quickRecordPrefs.getBoolean("shortcut_enabled", false));
        frontRecordEnabled.setValue(frontRecordPrefs.getBoolean("shortcut_enabled", false));
        backRecordEnabled.setValue(backRecordPrefs.getBoolean("shortcut_enabled", false));
    }

    
    public LiveData<Boolean> isLockAndRecordEnabled() {
        return lockAndRecordEnabled;
    }

    public LiveData<Boolean> isQuickRecordEnabled() {
        return quickRecordEnabled;
    }

    public LiveData<Boolean> isFrontRecordEnabled() {
        return frontRecordEnabled;
    }

    public LiveData<Boolean> isBackRecordEnabled() {
        return backRecordEnabled;
    }

    
    public void setLockAndRecordEnabled(boolean enabled) {
        lockAndRecordPrefs.edit().putBoolean("shortcut_enabled", enabled).apply();
        lockAndRecordEnabled.setValue(enabled);
    }

    public void setQuickRecordEnabled(boolean enabled) {
        quickRecordPrefs.edit().putBoolean("shortcut_enabled", enabled).apply();
        quickRecordEnabled.setValue(enabled);
    }

    public void setFrontRecordEnabled(boolean enabled) {
        frontRecordPrefs.edit().putBoolean("shortcut_enabled", enabled).apply();
        frontRecordEnabled.setValue(enabled);
    }

    public void setBackRecordEnabled(boolean enabled) {
        backRecordPrefs.edit().putBoolean("shortcut_enabled", enabled).apply();
        backRecordEnabled.setValue(enabled);
    }
}
