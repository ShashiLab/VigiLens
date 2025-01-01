package app.shashi.VigiLens.feature.settings.quickaccess;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.shortcut.back.BackRecordActivity;
import app.shashi.VigiLens.feature.shortcut.front.FrontRecordActivity;
import app.shashi.VigiLens.feature.shortcut.lockscreen.LockAndRecordActivity;
import app.shashi.VigiLens.feature.shortcut.quick.QuickRecordActivity;

public class ShortcutsSettingsActivity extends AppCompatActivity {

    
    private MaterialSwitch switchLockAndRecordShortcut, switchQuickRecordShortcut,
            switchFrontRecordShortcut, switchBackRecordShortcut;

    
    private LinearLayout layoutLockAndRecordShortcut, layoutQuickRecordShortcut,
            layoutFrontRecordShortcut, layoutBackRecordShortcut;

    
    private ShortcutsSettingsViewModel settingsViewModel;

    
    private static final String SHORTCUT_ID_LOCK_AND_RECORD = "lock_and_record_shortcut";
    private static final String SHORTCUT_ID_QUICK_RECORD = "quick_record_shortcut";
    private static final String SHORTCUT_ID_FRONT_RECORD = "front_record_shortcut";
    private static final String SHORTCUT_ID_BACK_RECORD = "back_record_shortcut";

    
    private boolean isSwitchProgrammaticallyChanging = false;

    
    private ShortcutPinnedReceiver shortcutPinnedReceiver;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_shortcuts_settings);

        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);  
            getSupportActionBar().setTitle(getString(R.string.title_shortcuts));
        }

        
        switchLockAndRecordShortcut = findViewById(R.id.switchLockAndRecordShortcut);
        switchQuickRecordShortcut = findViewById(R.id.switchQuickRecordShortcut);
        switchFrontRecordShortcut = findViewById(R.id.switchFrontRecordShortcut);
        switchBackRecordShortcut = findViewById(R.id.switchBackRecordShortcut);

        
        layoutLockAndRecordShortcut = findViewById(R.id.layoutLockAndRecordShortcut);
        layoutQuickRecordShortcut = findViewById(R.id.layoutQuickRecordShortcut);
        layoutFrontRecordShortcut = findViewById(R.id.layoutFrontRecordShortcut);
        layoutBackRecordShortcut = findViewById(R.id.layoutBackRecordShortcut);

        
        settingsViewModel = new ViewModelProvider(this).get(ShortcutsSettingsViewModel.class);

        
        observeViewModel();

        
        setupListeners();

        
        updateSwitchStates();

        
        shortcutPinnedReceiver = new ShortcutPinnedReceiver();
        IntentFilter filter = new IntentFilter("app.shashi.VigiLens.SHORTCUT_PINNED");
        registerReceiver(shortcutPinnedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (shortcutPinnedReceiver != null) {
            unregisterReceiver(shortcutPinnedReceiver);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        if (item.getItemId() == android.R.id.home) {
            finish();  
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    
    private void observeViewModel() {
        
        settingsViewModel.isLockAndRecordEnabled().observe(this, isEnabled -> {
            if (switchLockAndRecordShortcut.isChecked() != isEnabled) {
                setSwitchCheckedProgrammatically(switchLockAndRecordShortcut, isEnabled);
            }
        });

        
        settingsViewModel.isQuickRecordEnabled().observe(this, isEnabled -> {
            if (switchQuickRecordShortcut.isChecked() != isEnabled) {
                setSwitchCheckedProgrammatically(switchQuickRecordShortcut, isEnabled);
            }
        });

        
        settingsViewModel.isFrontRecordEnabled().observe(this, isEnabled -> {
            if (switchFrontRecordShortcut.isChecked() != isEnabled) {
                setSwitchCheckedProgrammatically(switchFrontRecordShortcut, isEnabled);
            }
        });

        
        settingsViewModel.isBackRecordEnabled().observe(this, isEnabled -> {
            if (switchBackRecordShortcut.isChecked() != isEnabled) {
                setSwitchCheckedProgrammatically(switchBackRecordShortcut, isEnabled);
            }
        });
    }

    
    private void setupListeners() {
        
        switchLockAndRecordShortcut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammaticallyChanging) {
                return;
            }
            if (isChecked) {
                addLockAndRecordShortcut();  
            } else {
                showManualRemoveDialog();
                setSwitchCheckedProgrammatically(switchLockAndRecordShortcut, true);
            }
        });

        
        switchQuickRecordShortcut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammaticallyChanging) {
                return;
            }
            if (isChecked) {
                addQuickRecordShortcut();
            } else {
                showManualRemoveDialog();
                setSwitchCheckedProgrammatically(switchQuickRecordShortcut, true);
            }
        });

        
        switchFrontRecordShortcut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammaticallyChanging) {
                return;
            }
            if (isChecked) {
                addFrontRecordShortcut();
            } else {
                showManualRemoveDialog();
                setSwitchCheckedProgrammatically(switchFrontRecordShortcut, true);
            }
        });

        
        switchBackRecordShortcut.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isSwitchProgrammaticallyChanging) {
                return;
            }
            if (isChecked) {
                addBackRecordShortcut();
            } else {
                showManualRemoveDialog();
                setSwitchCheckedProgrammatically(switchBackRecordShortcut, true);
            }
        });

        
        layoutLockAndRecordShortcut.setOnClickListener(v -> switchLockAndRecordShortcut.performClick());
        layoutQuickRecordShortcut.setOnClickListener(v -> switchQuickRecordShortcut.performClick());
        layoutFrontRecordShortcut.setOnClickListener(v -> switchFrontRecordShortcut.performClick());
        layoutBackRecordShortcut.setOnClickListener(v -> switchBackRecordShortcut.performClick());
    }

    
    private void updateSwitchStates() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            
            switchLockAndRecordShortcut.setEnabled(false);
            switchQuickRecordShortcut.setEnabled(false);
            switchFrontRecordShortcut.setEnabled(false);
            switchBackRecordShortcut.setEnabled(false);
            Toast.makeText(this, getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show();
            return;
        }

        
        boolean lockAndRecordExists = isShortcutPinned(SHORTCUT_ID_LOCK_AND_RECORD);
        settingsViewModel.setLockAndRecordEnabled(lockAndRecordExists);

        
        boolean quickRecordExists = isShortcutPinned(SHORTCUT_ID_QUICK_RECORD);
        settingsViewModel.setQuickRecordEnabled(quickRecordExists);

        
        boolean frontRecordExists = isShortcutPinned(SHORTCUT_ID_FRONT_RECORD);
        settingsViewModel.setFrontRecordEnabled(frontRecordExists);

        
        boolean backRecordExists = isShortcutPinned(SHORTCUT_ID_BACK_RECORD);
        settingsViewModel.setBackRecordEnabled(backRecordExists);
    }

    
    private boolean isShortcutPinned(String shortcutId) {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager != null) {
            for (ShortcutInfo shortcut : shortcutManager.getPinnedShortcuts()) {
                if (shortcutId.equals(shortcut.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    
    private void addLockAndRecordShortcut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            
            if (isShortcutPinned(SHORTCUT_ID_LOCK_AND_RECORD)) {
                settingsViewModel.setLockAndRecordEnabled(true);
                return;
            }

            Intent shortcutIntent = new Intent(this, LockAndRecordActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("prefs_name", "user_prefs_lock_and_record");
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID_LOCK_AND_RECORD)
                    .setShortLabel(getString(R.string.label_screen_off))
                    .setLongLabel(getString(R.string.label_screen_off))
                    .setIcon(Icon.createWithResource(this, R.drawable.ic_screenoff))  
                    .setIntent(shortcutIntent)
                    .build();

            
            Intent callbackIntent = new Intent("app.shashi.VigiLens.SHORTCUT_PINNED")
                    .putExtra("shortcut_id", SHORTCUT_ID_LOCK_AND_RECORD);
            PendingIntent successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            
            boolean requestResult = shortcutManager.requestPinShortcut(shortcut, successCallback.getIntentSender());

            if (!requestResult) {
                
                Toast.makeText(this, getString(R.string.toast_shortcut_add_failed), Toast.LENGTH_SHORT).show();
                setSwitchCheckedProgrammatically(switchLockAndRecordShortcut, false);
            }
            
        } else {
            Toast.makeText(this, getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show();
            
            setSwitchCheckedProgrammatically(switchLockAndRecordShortcut, false);
        }
    }

    
    private void addQuickRecordShortcut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            
            if (isShortcutPinned(SHORTCUT_ID_QUICK_RECORD)) {
                settingsViewModel.setQuickRecordEnabled(true);
                return;
            }

            Intent shortcutIntent = new Intent(this, QuickRecordActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("prefs_name", "user_prefs_quick_record");
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID_QUICK_RECORD)
                    .setShortLabel(getString(R.string.label_shortcut_quick_record))
                    .setLongLabel(getString(R.string.label_shortcut_quick_record))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_quick_record))
                    .setIntent(shortcutIntent)
                    .build();

            
            Intent callbackIntent = new Intent("app.shashi.VigiLens.SHORTCUT_PINNED")
                    .putExtra("shortcut_id", SHORTCUT_ID_QUICK_RECORD);
            PendingIntent successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            
            boolean requestResult = shortcutManager.requestPinShortcut(shortcut, successCallback.getIntentSender());

            if (!requestResult) {
                
                Toast.makeText(this, getString(R.string.toast_shortcut_add_failed), Toast.LENGTH_SHORT).show();
                setSwitchCheckedProgrammatically(switchQuickRecordShortcut, false);
            }
            
        } else {
            Toast.makeText(this, getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show();
            
            setSwitchCheckedProgrammatically(switchQuickRecordShortcut, false);
        }
    }

    
    private void addFrontRecordShortcut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            
            if (isShortcutPinned(SHORTCUT_ID_FRONT_RECORD)) {
                settingsViewModel.setFrontRecordEnabled(true);
                return;
            }

            Intent shortcutIntent = new Intent(this, FrontRecordActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("prefs_name", "user_prefs_front_record");
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID_FRONT_RECORD)
                    .setShortLabel(getString(R.string.label_front_record))
                    .setLongLabel(getString(R.string.label_front_record))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_front_record))
                    .setIntent(shortcutIntent)
                    .build();

            
            Intent callbackIntent = new Intent("app.shashi.VigiLens.SHORTCUT_PINNED")
                    .putExtra("shortcut_id", SHORTCUT_ID_FRONT_RECORD);
            PendingIntent successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            
            boolean requestResult = shortcutManager.requestPinShortcut(shortcut, successCallback.getIntentSender());

            if (!requestResult) {
                
                Toast.makeText(this, getString(R.string.toast_shortcut_add_failed), Toast.LENGTH_SHORT).show();
                setSwitchCheckedProgrammatically(switchFrontRecordShortcut, false);
            }
            
        } else {
            Toast.makeText(this, getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show();
            
            setSwitchCheckedProgrammatically(switchFrontRecordShortcut, false);
        }
    }

    
    private void addBackRecordShortcut() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
            
            if (isShortcutPinned(SHORTCUT_ID_BACK_RECORD)) {
                settingsViewModel.setBackRecordEnabled(true);
                return;
            }

            Intent shortcutIntent = new Intent(this, BackRecordActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("prefs_name", "user_prefs_back_record");
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID_BACK_RECORD)
                    .setShortLabel(getString(R.string.label_back_record))
                    .setLongLabel(getString(R.string.label_back_record))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_back_record))
                    .setIntent(shortcutIntent)
                    .build();

            
            Intent callbackIntent = new Intent("app.shashi.VigiLens.SHORTCUT_PINNED")
                    .putExtra("shortcut_id", SHORTCUT_ID_BACK_RECORD);
            PendingIntent successCallback = PendingIntent.getBroadcast(
                    this,
                    0,
                    callbackIntent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            
            boolean requestResult = shortcutManager.requestPinShortcut(shortcut, successCallback.getIntentSender());

            if (!requestResult) {
                
                Toast.makeText(this, getString(R.string.toast_shortcut_add_failed), Toast.LENGTH_SHORT).show();
                setSwitchCheckedProgrammatically(switchBackRecordShortcut, false);
            }
            
        } else {
            Toast.makeText(this, getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show();
            
            setSwitchCheckedProgrammatically(switchBackRecordShortcut, false);
        }
    }


    private void showManualRemoveDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_remove_shortcut))
                .setMessage(getString(R.string.message_remove_shortcut))
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    
                    dialog.dismiss();
                })
                .show();
    }

    
    private void setSwitchCheckedProgrammatically(MaterialSwitch switchView, boolean isChecked) {
        isSwitchProgrammaticallyChanging = true;
        switchView.setChecked(isChecked);
        isSwitchProgrammaticallyChanging = false;
    }

    
    public class ShortcutPinnedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "app.shashi.VigiLens.SHORTCUT_PINNED".equals(intent.getAction())) {
                String shortcutId = intent.getStringExtra("shortcut_id");
                if (shortcutId != null) {
                    switch (shortcutId) {
                        case SHORTCUT_ID_LOCK_AND_RECORD:
                            settingsViewModel.setLockAndRecordEnabled(true);
                            Toast.makeText(context, getString(R.string.toast_shortcut_added), Toast.LENGTH_SHORT).show();
                            break;
                        case SHORTCUT_ID_QUICK_RECORD:
                            settingsViewModel.setQuickRecordEnabled(true);
                            Toast.makeText(context, getString(R.string.toast_shortcut_added), Toast.LENGTH_SHORT).show();
                            break;
                        case SHORTCUT_ID_FRONT_RECORD:
                            settingsViewModel.setFrontRecordEnabled(true);
                            Toast.makeText(context, getString(R.string.toast_shortcut_added), Toast.LENGTH_SHORT).show();
                            break;
                        case SHORTCUT_ID_BACK_RECORD:
                            settingsViewModel.setBackRecordEnabled(true);
                            Toast.makeText(context, getString(R.string.toast_shortcut_added), Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            
                            Toast.makeText(context, getString(R.string.toast_shortcut_added), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        updateSwitchStates();
    }
}
