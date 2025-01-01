package app.shashi.VigiLens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.shashi.VigiLens.core.permission.PermissionActivity;
import app.shashi.VigiLens.feature.security.EnterPasswordActivity;
import app.shashi.VigiLens.feature.shortcut.back.BackRecordActivity;
import app.shashi.VigiLens.feature.shortcut.front.FrontRecordActivity;
import app.shashi.VigiLens.feature.shortcut.lockscreen.LockAndRecordActivity;
import app.shashi.VigiLens.feature.shortcut.quick.QuickRecordActivity;
import app.shashi.VigiLens.feature.widget.back.BackRecordWidgetProvider;
import app.shashi.VigiLens.feature.widget.front.FrontRecordWidgetProvider;
import app.shashi.VigiLens.feature.widget.lockscreen.LockAndRecordWidgetProvider;
import app.shashi.VigiLens.feature.widget.quick.QuickRecordWidgetProvider;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class VigiLens extends android.app.Application implements android.app.Application.ActivityLifecycleCallbacks {

    private static final String TAG = "MyApp";

    
    private AppOpenAd appOpenAd;
    private WeakReference<Activity> currentActivityRef;
    private volatile boolean isAdShowing = false;
    private volatile boolean isReturningFromBackground = false;
    private volatile boolean isFirstLaunch = true;

    
    private SharedPreferences sharedPreferences;

    
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    
    private final Handler mainHandler;

    public VigiLens() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 
            mainHandler = Handler.createAsync(Looper.getMainLooper());
        } else {
            mainHandler = new Handler(Looper.getMainLooper());
        }
    }

    
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    
    private final List<Class<?>> shortcutActivities = Arrays.asList(
            QuickRecordActivity.class,
            LockAndRecordActivity.class,
            FrontRecordActivity.class,
            BackRecordActivity.class,

            QuickRecordWidgetProvider.class,
            LockAndRecordWidgetProvider.class,
            FrontRecordWidgetProvider.class,
            BackRecordWidgetProvider.class
    );

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads initialized.");
        });

        
        executorService.execute(() -> {
            String theme = sharedPreferences.getString("theme", "dark");
            mainHandler.post(() -> applyTheme(theme));
        });

        
        registerActivityLifecycleCallbacks(this);
    }

    
    private void applyTheme(String theme) {
        switch (theme) {
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

    
    public void setAuthenticated(boolean isAuthenticated) {
        executorService.execute(() -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isAuthenticated", isAuthenticated);
            editor.apply();
        });
    }

    
    public boolean isAuthenticated() {
        return sharedPreferences.getBoolean("isAuthenticated", false);
    }

    
    private boolean areTermsAccepted() {
        return sharedPreferences.getBoolean("terms_accepted", false);
    }

    
    private void redirectToEnterPasswordActivity() {
        Intent intent = new Intent(this, EnterPasswordActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    
    private void redirectToPermissionActivity() {
        Intent intent = new Intent(this, PermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
        
    }

    
    public void loadAppOpenAd() {
        mainHandler.post(() -> {
            if (appOpenAd != null) {
                Log.d(TAG, "App Open Ad already loaded.");
                return;
            }

            AdRequest adRequest = new AdRequest.Builder().build();
            String adUnitId = getString(R.string.app_open_ad);

            AppOpenAd.load(this, adUnitId, adRequest, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            appOpenAd = ad;
                            Log.d(TAG, "App Open Ad loaded successfully");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            appOpenAd = null;
                            Log.d(TAG, "App Open Ad failed to load: " + loadAdError.getMessage());
                        }
                    });
        });
    }

    
    public void showAppOpenAdIfAvailable() {
        mainHandler.post(() -> {
            Activity currentActivity = currentActivityRef != null ? currentActivityRef.get() : null;

            if (appOpenAd != null && !isAdShowing && currentActivity != null) {
                isAdShowing = true;
                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        isAdShowing = false;
                        appOpenAd = null;
                        Log.d(TAG, "App Open Ad dismissed.");
                        loadAppOpenAd(); 
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        isAdShowing = false;
                        appOpenAd = null;
                        Log.d(TAG, "App Open Ad failed to show: " + adError.getMessage());
                        loadAppOpenAd(); 
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d(TAG, "App Open Ad showed.");
                    }
                });

                appOpenAd.show(currentActivity);
            } else {
                Log.d(TAG, "App Open Ad not ready or already showing");
                
                loadAppOpenAd();
            }
        });
    }

    
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);

        if (isReturningFromBackground && !isFirstLaunch) {
            
            if (!isShortcutActivity(activity)) {
                showAppOpenAdIfAvailable();
            }
        }
        isFirstLaunch = false;
        isReturningFromBackground = false; 
    }

    
    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Activity currentActivity = currentActivityRef != null ? currentActivityRef.get() : null;
        if (activity.equals(currentActivity)) {
            currentActivityRef.clear();
        }
    }

    
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            
            executorService.execute(() -> {
                String appPassword = sharedPreferences.getString("app_password", null);
                boolean isAuthenticated = isAuthenticated();

                mainHandler.post(() -> {
                    if (appPassword != null && !isAuthenticated) {
                        if (!isShortcutActivity(activity)) {
                            
                            redirectToEnterPasswordActivity();
                        }
                    } else if (!areTermsAccepted()) {
                        if (!isShortcutActivity(activity)) {
                            
                            redirectToPermissionActivity();
                        }
                    } else {
                        
                        if (!isShortcutActivity(activity)) {
                            loadAppOpenAd();
                        }
                    }
                });
            });
        }
    }

    
    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            
            setAuthenticated(false); 
            isReturningFromBackground = true;
        }
    }

    
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        executorService.shutdown();
    }

    
    private boolean isShortcutActivity(Activity activity) {
        for (Class<?> shortcutClass : shortcutActivities) {
            if (shortcutClass.isInstance(activity)) {
                return true;
            }
        }
        return false;
    }
}
