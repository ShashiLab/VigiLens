package app.shashi.VigiLens.feature.settings;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import app.shashi.VigiLens.feature.main.MainActivity;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class SettingsViewModel extends AndroidViewModel {

    private SharedPreferences sharedPreferences;

    
    private String preferenceFileName;

    
    private final MutableLiveData<String> language = new MutableLiveData<>();
    private final MutableLiveData<String> theme = new MutableLiveData<>();
    private final MutableLiveData<Boolean> enablePassword = new MutableLiveData<>();
    private final MutableLiveData<Boolean> biometricAuthentication = new MutableLiveData<>();
    private final MutableLiveData<Boolean> enablePreview = new MutableLiveData<>();
    private final MutableLiveData<String> cameraSelection = new MutableLiveData<>();
    private final MutableLiveData<String> resolutionSelection = new MutableLiveData<>();
    private final MutableLiveData<String> recordingDurationSelection = new MutableLiveData<>();
    private final MutableLiveData<String> previewSizeSelection = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        
        preferenceFileName = "user_prefs";
        sharedPreferences = application.getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE);
        loadSettings();
    }

    public void setPreferenceFileName(String preferenceFileName) {
        this.preferenceFileName = preferenceFileName;
        sharedPreferences = getApplication().getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE);
    }

    public void loadSettings() {
        
        language.setValue(sharedPreferences.getString("language", "system_default"));
        theme.setValue(sharedPreferences.getString("theme", "dark"));
        enablePassword.setValue(!sharedPreferences.getString("app_password", "").isEmpty());
        biometricAuthentication.setValue(sharedPreferences.getBoolean("biometric_authentication_prompt", false));
        enablePreview.setValue(sharedPreferences.getBoolean("show_preview", false));
        cameraSelection.setValue(sharedPreferences.getString("camera", "back_camera"));
        resolutionSelection.setValue(sharedPreferences.getString("resolution", "720p"));
        recordingDurationSelection.setValue(sharedPreferences.getString("recordingDuration", "unlimited"));
        previewSizeSelection.setValue(sharedPreferences.getString("preview_size", "medium"));
    }

    
    public LiveData<String> getLanguage() {
        return language;
    }

    public LiveData<String> getTheme() {
        return theme;
    }

    public LiveData<Boolean> isPasswordEnabled() {
        return enablePassword;
    }

    public LiveData<Boolean> isBiometricEnabled() {
        return biometricAuthentication;
    }

    public LiveData<Boolean> isPreviewEnabled() {
        return enablePreview;
    }

    public LiveData<String> getCameraSelection() {
        return cameraSelection;
    }

    public LiveData<String> getResolutionSelection() {
        return resolutionSelection;
    }

    public LiveData<String> getRecordingDurationSelection() {
        return recordingDurationSelection;
    }

    public LiveData<String> getPreviewSizeSelection() {
        return previewSizeSelection;
    }

    
    public void setLanguage(String languageOption) {
        sharedPreferences.edit().putString("language", languageOption).apply();
        language.setValue(languageOption); 
        applyLanguage(languageOption); 
    }

    public void setTheme(String themeOption) {
        sharedPreferences.edit().putString("theme", themeOption).apply();
        theme.setValue(themeOption);
        applyTheme(themeOption);
    }

    public void setPasswordEnabled(boolean enabled) {
        enablePassword.setValue(enabled);
        if (enabled) {
            
            
            setBiometricAuthentication(false);
        } else {
            sharedPreferences.edit().remove("app_password").apply();
            
            setBiometricAuthentication(false);
        }
    }

    public void setBiometricAuthentication(boolean enabled) {
        sharedPreferences.edit().putBoolean("biometric_authentication_prompt", enabled).apply();
        biometricAuthentication.setValue(enabled);
    }

    public void setPreviewEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("show_preview", enabled).apply();
        enablePreview.setValue(enabled);
    }

    public void setCameraSelection(String camera) {
        sharedPreferences.edit().putString("camera", camera).apply();
        cameraSelection.setValue(camera);
    }

    public void setResolutionSelection(String resolution) {
        sharedPreferences.edit().putString("resolution", resolution).apply();
        resolutionSelection.setValue(resolution);
    }

    public void setRecordingDurationSelection(String duration) {
        sharedPreferences.edit().putString("recordingDuration", duration).apply();
        recordingDurationSelection.setValue(duration);
    }

    public void setPreviewSizeSelection(String previewSize) {
        sharedPreferences.edit().putString("preview_size", previewSize).apply();
        previewSizeSelection.setValue(previewSize);
    }

    private void applyLanguage(String languageOption) {
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("language", languageOption);
        editor.apply();

        
        Context context = LocaleManager.updateBaseContextLocale(getApplication());

        
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private void applyTheme(String themeOption) {
        switch (themeOption) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system_default":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
