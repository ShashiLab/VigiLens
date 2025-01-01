package app.shashi.VigiLens.feature.recording.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.core.permission.PermissionActivity;
import app.shashi.VigiLens.feature.recording.service.VideoRecordingService;
import app.shashi.VigiLens.utils.feedback.GitHubRatingManager;

public class RecordingFragment extends Fragment {

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    private boolean isRecording = false;
    private SharedPreferences sharedPreferences;
    private SharedPreferences globalSharedPreferences; 

    
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime;
    private TextView timeDisplay;

    private static final String PREFS_NAME = "user_prefs";
    private static final String GLOBAL_PREFS_NAME = "app_prefs";  
    private static final String PREF_START_TIME = "startTime";
    private static final String PREF_IS_RECORDING = "isRecording";

    private Vibrator vibrator;
    private ImageButton recordButton;
    private MaterialButton switchToFront;
    private MaterialButton switchToBack;
    private MaterialSwitch showPreviewSwitch;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);

        setHasOptionsMenu(true);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        globalSharedPreferences = requireActivity().getSharedPreferences(GLOBAL_PREFS_NAME, Context.MODE_PRIVATE); 

        recordButton = view.findViewById(R.id.start_button);
        switchToFront = view.findViewById(R.id.switch_to_front);
        switchToBack = view.findViewById(R.id.switch_to_back);
        timeDisplay = view.findViewById(R.id.time_display);

        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.canDrawOverlays(getContext())) {
                        
                    } else {
                        
                        showPreviewSwitch.setChecked(false);
                        Toast.makeText(getContext(), getString(R.string.toast_permissions_required), Toast.LENGTH_SHORT).show();
                    }
                });

        showPreviewSwitch = view.findViewById(R.id.show_preview_switch);

        
        boolean showPreview = sharedPreferences.getBoolean("show_preview", false);
        showPreviewSwitch.setChecked(showPreview);

        showPreviewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("show_preview", isChecked);
            editor.apply();

            if (isChecked) {
                if (!Settings.canDrawOverlays(getContext())) {
                    
                    showOverlayPermissionExplanationDialog();
                }
            }
        });

        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        
        timerHandler = new Handler();

        recordButton.setOnClickListener(v -> startOrStopRecording());

        switchToFront.setOnClickListener(v -> {
            playClickSound();  
            switchToCamera(v);
        });

        switchToBack.setOnClickListener(v -> {
            playClickSound();  
            switchToCamera(v);
        });

        
        preferenceChangeListener = (prefs, key) -> {
            if (PREF_IS_RECORDING.equals(key)) {
                boolean isRecordingNow = prefs.getBoolean(PREF_IS_RECORDING, false);
                if (isRecordingNow != isRecording) {
                    isRecording = isRecordingNow;
                    if (isRecording) {
                        startTime = prefs.getLong(PREF_START_TIME, System.currentTimeMillis());
                        startTimer();
                        updateUIRecordingStarted();
                    } else {
                        stopTimer();
                        updateUIRecordingStopped();
                    }
                }
            }
        };
        globalSharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener); 

        restoreRecordingState();
        updateCameraButtonState(); 

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        
        inflater.inflate(R.menu.record_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        int id = item.getItemId();

        if (id == R.id.action_rate_app) {

            GitHubRatingManager rateApp = new GitHubRatingManager(getActivity());
            rateApp.showStarDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showOverlayPermissionExplanationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.title_overlay_permission))
                .setMessage(getString(R.string.message_overlay_permission))
                .setPositiveButton(getString(R.string.btn_grant_permission), (dialog, which) -> {
                    
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    overlayPermissionLauncher.launch(intent);
                })
                .setNegativeButton(getString(R.string.btn_cancel), (dialog, which) -> {
                    showPreviewSwitch.setChecked(false);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreRecordingState();
        updateCameraButtonState();

        
        if (!Settings.canDrawOverlays(getContext())) {
            
            showPreviewSwitch.setChecked(false);
        } else {
            
            boolean showPreview = sharedPreferences.getBoolean("show_preview", false);
            showPreviewSwitch.setChecked(showPreview);
        }
    }

    private void startOrStopRecording() {
        playClickSound();
        if (isRecording) {
            requireActivity().stopService(new Intent(getActivity(), VideoRecordingService.class));
            
        } else {
            if (arePermissionsGranted()) {
                requireActivity().startService(new Intent(getActivity(), VideoRecordingService.class));
                
            } else {
                Toast.makeText(getActivity(), R.string.toast_permissions_required, Toast.LENGTH_SHORT).show();
                redirectToPermissionActivity();
            }
        }

        
        recordButton.setEnabled(false);
        new Handler().postDelayed(() -> recordButton.setEnabled(true), 500);
    }

    private void startTimer() {
        if (timerRunnable == null) {
            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    updateTimerDisplay();
                    timerHandler.postDelayed(this, 100);
                }
            };
        }
        timerHandler.post(timerRunnable);

        if (getActivity() != null) {
            timeDisplay.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.time_display_bg_active, getActivity().getTheme()));
        }
        timeDisplay.setTextColor(getResources().getColor(R.color.md_theme_primary));
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        timeDisplay.setText("00:00");

        if (getActivity() != null) {
            timeDisplay.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.time_display_bg, getActivity().getTheme()));
        }
        timeDisplay.setTextColor(getResources().getColor(R.color.md_theme_onBackground));
    }

    private void updateTimerDisplay() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int seconds = (int) (elapsedMillis / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours == 0) {
            timeDisplay.setText(String.format("%02d:%02d", minutes, seconds));
        } else {
            timeDisplay.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        }
    }

    
    private void restoreRecordingState() {
        isRecording = globalSharedPreferences.getBoolean(PREF_IS_RECORDING, false);

        if (isRecording) {
            startTime = globalSharedPreferences.getLong(PREF_START_TIME, System.currentTimeMillis());
            startTimer();
            updateUIRecordingStarted();
        } else {
            stopTimer();
            updateUIRecordingStopped();
        }
    }

    private void updateUIRecordingStarted() {
        recordButton.setImageResource(R.drawable.stop_recording_button);
        switchToFront.setEnabled(false);
        switchToBack.setEnabled(false);
    }

    private void updateUIRecordingStopped() {
        recordButton.setImageResource(R.drawable.start_recording_button);
        switchToFront.setEnabled(true);
        switchToBack.setEnabled(true);
    }

    private boolean arePermissionsGranted() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void redirectToPermissionActivity() {
        Intent intent = new Intent(requireActivity(), PermissionActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void switchToCamera(View view) {
        if (isRecording) {
            Toast.makeText(getActivity(), getString(R.string.toast_cannot_switch_camera_while_recording), Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        int viewId = view.getId();
        String selectedCamera = "";

        if (viewId == R.id.switch_to_front) {
            editor.putString("camera", "front_camera");
            selectedCamera = "front_camera";
        } else if (viewId == R.id.switch_to_back) {
            editor.putString("camera", "back_camera");
            selectedCamera = "back_camera";
        }
        editor.apply();

        playClickSound();
        updateCameraButtonState();
    }

    private void updateCameraButtonState() {
        String selectedCamera = sharedPreferences.getString("camera", "back_camera");

        if ("front_camera".equals(selectedCamera)) {
            
            switchToFront.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_secondaryContainer, requireActivity().getTheme()));
            switchToBack.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), android.R.color.transparent, requireActivity().getTheme()));

            
            switchToFront.setIconTint(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_onSecondaryContainer, requireActivity().getTheme()));
            switchToBack.setIconTint(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_onBackground, requireActivity().getTheme()));
        } else {
            
            switchToBack.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_secondaryContainer, requireActivity().getTheme()));
            switchToFront.setBackgroundTintList(ResourcesCompat.getColorStateList(getResources(), android.R.color.transparent, requireActivity().getTheme()));

            
            switchToBack.setIconTint(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_onSecondaryContainer, requireActivity().getTheme()));
            switchToFront.setIconTint(ResourcesCompat.getColorStateList(getResources(), R.color.md_theme_onBackground, requireActivity().getTheme()));
        }
    }

    private void playClickSound() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(100);  
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (preferenceChangeListener != null) {
            globalSharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener); 
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}
