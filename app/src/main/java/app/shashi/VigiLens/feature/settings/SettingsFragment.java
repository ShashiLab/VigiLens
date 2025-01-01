

package app.shashi.VigiLens.feature.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.security.DisablePasswordActivity;
import app.shashi.VigiLens.feature.security.ResetPasswordActivity;
import app.shashi.VigiLens.feature.security.SetPasswordActivity;

public class SettingsFragment extends Fragment {

    private TextView textCameraSelection, textResolutionSelection, textRecordingDurationSelection, textThemeSelection, textLanguageSelection, textPreviewSizeSelection;
    private SwitchCompat switchEnablePassword, switchBiometricAuthentication, switchEnablePreview;

    private SettingsViewModel settingsViewModel;

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> setPasswordLauncher;
    private ActivityResultLauncher<Intent> disablePasswordLauncher;

    
    private final String[] preferenceFiles = {"user_prefs", "user_prefs_front_record", "user_prefs_back_record", "user_prefs_screen_off"};

    private String selectedPreferenceFile;

    
    private View layoutCamera;
    private View previewSettings;
    private View layoutPreviewSizeSelection;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    
                    if (Settings.canDrawOverlays(requireContext())) {
                        
                        settingsViewModel.setPreviewEnabled(true);
                        Toast.makeText(requireContext(), getString(R.string.message_permission_granted), Toast.LENGTH_SHORT).show();
                    } else {
                        
                        settingsViewModel.setPreviewEnabled(false);
                        Toast.makeText(requireContext(), getString(R.string.message_permission_denied), Toast.LENGTH_SHORT).show();
                    }
                }
        );

        
        setPasswordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        
                        switchEnablePassword.setChecked(true);  
                        settingsViewModel.setPasswordEnabled(true);
                    } else {
                        
                        switchEnablePassword.setChecked(false);
                        settingsViewModel.setPasswordEnabled(false);
                    }
                }
        );

        
        disablePasswordLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        
                        switchEnablePassword.setChecked(false);  
                        settingsViewModel.setPasswordEnabled(false);
                    } else {
                        
                        switchEnablePassword.setChecked(true);
                        settingsViewModel.setPasswordEnabled(true);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        
        initializeViews(view);

        
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        
        selectedPreferenceFile = "user_prefs"; 
        settingsViewModel.setPreferenceFileName(selectedPreferenceFile);

        
        observeViewModel();

        
        setupListeners(view);

        
        updateUIForPreferenceFile();

        
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.settings_menu, menu);

                
                MenuItem menuItem = menu.findItem(R.id.menu_spinner);
                Spinner spinner = (Spinner) menuItem.getActionView();

                
                setupSpinner(spinner);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false; 
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED); 

        return view;
    }

    private void initializeViews(View view) {
        
        textCameraSelection = view.findViewById(R.id.textCameraSelection);
        textResolutionSelection = view.findViewById(R.id.textResolutionSelection);
        textRecordingDurationSelection = view.findViewById(R.id.textRecordingDurationSelection);
        switchEnablePassword = view.findViewById(R.id.switchEnablePassword);
        switchBiometricAuthentication = view.findViewById(R.id.switchBiometricAuthentication);
        textThemeSelection = view.findViewById(R.id.textThemeSelection);
        textLanguageSelection = view.findViewById(R.id.textLanguageSelection);
        textPreviewSizeSelection = view.findViewById(R.id.textPreviewSizeSelection);
        switchEnablePreview = view.findViewById(R.id.switchEnablePreview);

        
        layoutCamera = view.findViewById(R.id.layoutCamera);
        previewSettings = view.findViewById(R.id.previewSettings);
        layoutPreviewSizeSelection = view.findViewById(R.id.layoutPreviewSizeSelection);
    }

    private void setupSpinner(Spinner spinner) {
        List<String> preferenceFileNamesList = new ArrayList<>();
        HashMap<String, String> preferenceFileDisplayNames = new HashMap<>();

        
        preferenceFileDisplayNames.put("user_prefs", getString(R.string.prefs_main));
        preferenceFileDisplayNames.put("user_prefs_screen_off", getString(R.string.prefs_lock));
        preferenceFileDisplayNames.put("user_prefs_back_record", getString(R.string.prefs_back));
        preferenceFileDisplayNames.put("user_prefs_front_record", getString(R.string.prefs_front));

        
        for (String prefFile : preferenceFiles) {
            preferenceFileNamesList.add(preferenceFileDisplayNames.get(prefFile));
        }

        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, preferenceFileNamesList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                view.setGravity(Gravity.CENTER_VERTICAL); 
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        
        int defaultPosition = Arrays.asList(preferenceFiles).indexOf(selectedPreferenceFile);
        spinner.setSelection(defaultPosition);

        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPreferenceFile = preferenceFiles[position];
                settingsViewModel.setPreferenceFileName(selectedPreferenceFile);
                settingsViewModel.loadSettings(); 
                observeViewModel(); 
                updateUIForPreferenceFile();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                
            }
        });
    }


    private void observeViewModel() {
        
        settingsViewModel.getLanguage().observe(getViewLifecycleOwner(), language -> {
            String localizedLanguage = getLocalizedOption(language,
                    getResources().getStringArray(R.array.language_options),
                    getResources().getStringArray(R.array.language_options_fixed));
            textLanguageSelection.setText(localizedLanguage);
        });

        
        settingsViewModel.getTheme().observe(getViewLifecycleOwner(), theme -> {
            String localizedTheme = getLocalizedOption(theme,
                    getResources().getStringArray(R.array.theme_options),
                    getResources().getStringArray(R.array.theme_options_fixed));
            textThemeSelection.setText(localizedTheme);
        });

        
        settingsViewModel.getCameraSelection().observe(getViewLifecycleOwner(), camera -> {
            String localizedCamera = getLocalizedOption(camera,
                    getResources().getStringArray(R.array.camera_options),
                    getResources().getStringArray(R.array.camera_options_fixed));
            textCameraSelection.setText(localizedCamera);
        });

        
        settingsViewModel.getResolutionSelection().observe(getViewLifecycleOwner(), resolution -> {
            String localizedResolution = getLocalizedOption(resolution,
                    getResources().getStringArray(R.array.video_quality_options),
                    getResources().getStringArray(R.array.video_quality_options_fixed));
            textResolutionSelection.setText(localizedResolution);
        });

        
        settingsViewModel.getRecordingDurationSelection().observe(getViewLifecycleOwner(), duration -> {
            if ("unlimited".equals(duration)) {
                textRecordingDurationSelection.setText(getString(R.string.label_unlimited));  
            } else {
                textRecordingDurationSelection.setText(duration + " " + getString(R.string.label_minutes));  
            }
        });

        
        settingsViewModel.getPreviewSizeSelection().observe(getViewLifecycleOwner(), previewSize -> {
            String localizedPreviewSize = getLocalizedOption(previewSize,
                    getResources().getStringArray(R.array.preview_size_options),
                    getResources().getStringArray(R.array.preview_size_options_fixed));
            textPreviewSizeSelection.setText(localizedPreviewSize);
        });

        
        settingsViewModel.isPasswordEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            if (switchEnablePassword.isChecked() != isEnabled) {
                switchEnablePassword.setChecked(isEnabled);
            }
        });

        
        settingsViewModel.isBiometricEnabled().observe(getViewLifecycleOwner(), switchBiometricAuthentication::setChecked);

        
        settingsViewModel.isPreviewEnabled().observe(getViewLifecycleOwner(), switchEnablePreview::setChecked);
    }

    private void setupListeners(View view) {
        
        switchEnablePassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!switchEnablePassword.isChecked()) {
                    
                    Intent intent = new Intent(getActivity(), SetPasswordActivity.class);
                    setPasswordLauncher.launch(intent);  
                } else {
                    
                    Intent intent = new Intent(getActivity(), DisablePasswordActivity.class);
                    disablePasswordLauncher.launch(intent);  
                }
            }
            return true;  
        });

        
        switchBiometricAuthentication.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String savedPassword = getSavedPassword();
            if (savedPassword.isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.toast_set_app_lock_first), Toast.LENGTH_SHORT).show();
                switchBiometricAuthentication.setChecked(false);
            } else {
                settingsViewModel.setBiometricAuthentication(isChecked);
            }
        });

        
        view.findViewById(R.id.layoutCamera).setOnClickListener(v -> {
            if (v.isEnabled()) { 
                showCustomDialog(
                        getString(R.string.title_select_camera),
                        getResources().getStringArray(R.array.camera_options),
                        getResources().getStringArray(R.array.camera_options_fixed),
                        textCameraSelection);
            }
        });

        
        view.findViewById(R.id.layoutResolution).setOnClickListener(v -> showCustomDialog(
                getString(R.string.title_select_resolution),
                getResources().getStringArray(R.array.video_quality_options),
                getResources().getStringArray(R.array.video_quality_options_fixed),
                textResolutionSelection));

        
        view.findViewById(R.id.layoutRecordingDuration).setOnClickListener(v -> {
            showRecordingDurationDialog();  
        });

        
        view.findViewById(R.id.layoutThemeSelection).setOnClickListener(v -> showCustomDialog(
                getString(R.string.title_select_theme),
                getResources().getStringArray(R.array.theme_options),
                getResources().getStringArray(R.array.theme_options_fixed),
                textThemeSelection));

        
        view.findViewById(R.id.layoutLanguageSelection).setOnClickListener(v -> showCustomDialog(
                getString(R.string.title_select_language),
                getResources().getStringArray(R.array.language_options),
                getResources().getStringArray(R.array.language_options_fixed), 
                textLanguageSelection));

        
        view.findViewById(R.id.layoutResetPassword).setOnClickListener(v -> {
            if (!getSavedPassword().isEmpty()) {
                Intent intent = new Intent(getActivity(), ResetPasswordActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), getString(R.string.toast_set_app_lock_first), Toast.LENGTH_SHORT).show();
            }
        });

        
        switchEnablePreview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchEnablePreview.isEnabled()) { 
                if (isChecked) {
                    if (!Settings.canDrawOverlays(requireContext())) {
                        showOverlayPermissionExplanationDialog();
                    } else {
                        
                        settingsViewModel.setPreviewEnabled(true);
                    }
                } else {
                    
                    settingsViewModel.setPreviewEnabled(false);
                }
            }
        });

        
        view.findViewById(R.id.layoutPreviewSizeSelection).setOnClickListener(v -> showCustomDialog(
                getString(R.string.title_select_preview_size),
                getResources().getStringArray(R.array.preview_size_options),
                getResources().getStringArray(R.array.preview_size_options_fixed),
                textPreviewSizeSelection));
    }

    private String getSavedPassword() {
        
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(selectedPreferenceFile, Context.MODE_PRIVATE);
        return sharedPreferences.getString("app_password", "");
    }

    private void showRecordingDurationDialog() {
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_recording_duration_selector, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(dialogView);

        
        AlertDialog dialog = builder.create();

        
        TextInputEditText editTextDuration = dialogView.findViewById(R.id.editTextDuration);
        SeekBar seekBarDuration = dialogView.findViewById(R.id.seekBarDuration);
        MaterialSwitch switchUnlimited = dialogView.findViewById(R.id.switchUnlimited);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        
        btnConfirm.setEnabled(false);

        
        String initialDuration = settingsViewModel.getRecordingDurationSelection().getValue();

        
        if ("unlimited".equals(initialDuration)) {
            switchUnlimited.setChecked(true);
            editTextDuration.setEnabled(false);
            seekBarDuration.setEnabled(false);
        } else {
            switchUnlimited.setChecked(false);
            editTextDuration.setText(initialDuration);  
            seekBarDuration.setProgress(Integer.parseInt(initialDuration));  
        }

        
        Runnable checkChanges = () -> {
            if (switchUnlimited.isChecked()) {
                
                btnConfirm.setEnabled(!"unlimited".equals(initialDuration));
            } else {
                String currentValue = Objects.requireNonNull(editTextDuration.getText()).toString();
                
                btnConfirm.setEnabled(!currentValue.equals(initialDuration));
            }
        };

        
        switchUnlimited.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editTextDuration.setEnabled(false);
                seekBarDuration.setEnabled(false);
            } else {
                editTextDuration.setEnabled(true);
                seekBarDuration.setEnabled(true);
            }
            checkChanges.run();  
        });

        
        seekBarDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    editTextDuration.setText(String.valueOf(progress));  
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                checkChanges.run();  
            }
        });

        
        editTextDuration.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    try {
                        int newValue = Integer.parseInt(s.toString());
                        if (newValue <= seekBarDuration.getMax()) {  
                            seekBarDuration.setProgress(newValue);  
                        }
                    } catch (NumberFormatException e) {
                        editTextDuration.setError(getString(R.string.error_invalid_number));  
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkChanges.run();  
            }
        });

        
        btnConfirm.setOnClickListener(v -> {
            if (switchUnlimited.isChecked()) {
                settingsViewModel.setRecordingDurationSelection("unlimited");
            } else {
                String durationValue = Objects.requireNonNull(editTextDuration.getText()).toString();
                settingsViewModel.setRecordingDurationSelection(durationValue);  
            }
            Toast.makeText(requireContext(), getString(R.string.toast_preference_saved), Toast.LENGTH_SHORT).show();
            dialog.dismiss();  
        });

        
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();  
        });

        
        dialog.show();
    }

    private void showCustomDialog(String title, String[] localizedOptions, String[] englishOptions,
                                  final TextView targetTextView) {
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());

        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_radio_selector, null);
        builder.setView(dialogView);

        
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(title);

        
        LinearLayout dialogRadioGroupContainer = dialogView.findViewById(R.id.dialogRadioGroupContainer);

        
        String currentPreference = getCurrentPreference(targetTextView.getId());

        
        AlertDialog dialog = builder.create();

        
        final String[] selectedEnglishOption = {currentPreference};
        final String[] selectedLocalizedOption = {""};

        
        final RadioButton[] currentlySelectedRadioButton = {null};

        
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        
        btnConfirm.setEnabled(false);

        
        for (int i = 0; i < localizedOptions.length; i++) {
            String localizedOption = localizedOptions[i];
            String englishOption = englishOptions[i];

            
            LinearLayout radioButtonContainer = new LinearLayout(getContext());
            radioButtonContainer.setOrientation(LinearLayout.HORIZONTAL);
            radioButtonContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            radioButtonContainer.setPadding(0, 16, 0, 16); 

            
            TextView radioButtonLabel = new TextView(getContext());
            radioButtonLabel.setText(localizedOption);
            radioButtonLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface));
            radioButtonLabel.setTextSize(18); 
            radioButtonLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f)); 

            
            RadioButton radioButton = new RadioButton(getContext(),
                    null,
                    com.google.android.material.R.attr.radioButtonStyle);
            radioButton.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_primary)));
            radioButton.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            
            radioButton.setContentDescription(localizedOption);

            
            radioButtonContainer.addView(radioButtonLabel);
            radioButtonContainer.addView(radioButton);

            
            dialogRadioGroupContainer.addView(radioButtonContainer);

            
            if (englishOption.equals(currentPreference)) {
                radioButton.setChecked(true);
                currentlySelectedRadioButton[0] = radioButton;  
                selectedLocalizedOption[0] = localizedOption;   
                btnConfirm.setEnabled(false);  
            }

            
            radioButton.setOnClickListener(v -> {
                if (currentlySelectedRadioButton[0] != null) {
                    currentlySelectedRadioButton[0].setChecked(false);  
                }

                currentlySelectedRadioButton[0] = radioButton;  
                selectedEnglishOption[0] = englishOption;
                selectedLocalizedOption[0] = localizedOption;
                radioButton.setChecked(true);  

                
                btnConfirm.setEnabled(!selectedEnglishOption[0].equals(currentPreference));
            });

            
            radioButtonLabel.setOnClickListener(v -> radioButton.performClick());
        }

        
        btnConfirm.setOnClickListener(v -> {
            
            targetTextView.setText(selectedLocalizedOption[0]);  
            savePreference(targetTextView.getId(), selectedEnglishOption[0]);  
            Toast.makeText(requireContext(), getString(R.string.toast_preference_saved), Toast.LENGTH_SHORT).show();
            dialog.dismiss();  
        });

        
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();  
        });

        
        dialog.show();
    }

    private String getCurrentPreference(int textViewId) {
        if (textViewId == R.id.textCameraSelection) {
            return settingsViewModel.getCameraSelection().getValue();
        } else if (textViewId == R.id.textResolutionSelection) {
            return settingsViewModel.getResolutionSelection().getValue();
        } else if (textViewId == R.id.textRecordingDurationSelection) {
            return settingsViewModel.getRecordingDurationSelection().getValue();
        } else if (textViewId == R.id.textThemeSelection) {
            return settingsViewModel.getTheme().getValue();
        } else if (textViewId == R.id.textLanguageSelection) {
            return settingsViewModel.getLanguage().getValue();
        } else if (textViewId == R.id.textPreviewSizeSelection) {
            return settingsViewModel.getPreviewSizeSelection().getValue();
        }

        return "";
    }

    private void savePreference(int textViewId, String value) {
        if (textViewId == R.id.textCameraSelection) {
            settingsViewModel.setCameraSelection(value);
        } else if (textViewId == R.id.textResolutionSelection) {
            settingsViewModel.setResolutionSelection(value);
        } else if (textViewId == R.id.textRecordingDurationSelection) {
            settingsViewModel.setRecordingDurationSelection(value);
        } else if (textViewId == R.id.textThemeSelection) {
            settingsViewModel.setTheme(value);
        } else if (textViewId == R.id.textLanguageSelection) {
            settingsViewModel.setLanguage(value);
        } else if (textViewId == R.id.textPreviewSizeSelection) {
            settingsViewModel.setPreviewSizeSelection(value);
        }
    }

    private String getLocalizedOption(String englishOption, String[] localizedOptions, String[] englishOptions) {
        for (int i = 0; i < englishOptions.length; i++) {
            if (englishOptions[i].equals(englishOption)) {
                return localizedOptions[i];  
            }
        }
        return englishOption;  
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
                    settingsViewModel.setPreviewEnabled(false);
                })
                .setCancelable(false)
                .show();
    }

    private void updateUIForPreferenceFile() {
        if ("user_prefs_screen_off".equals(selectedPreferenceFile)) {
            
            switchEnablePreview.setEnabled(false);
            switchEnablePreview.setChecked(false);

            layoutPreviewSizeSelection.setEnabled(false);

            previewSettings.setEnabled(false);
            previewSettings.setAlpha(0.5f);
            
            settingsViewModel.setPreviewEnabled(false);
        } else {
            
            switchEnablePreview.setEnabled(true);

            layoutPreviewSizeSelection.setEnabled(true);

            previewSettings.setEnabled(true);
            previewSettings.setAlpha(1f);

        }

        if ("user_prefs_back_record".equals(selectedPreferenceFile)) {
            
            if (layoutCamera != null) {
                layoutCamera.setEnabled(false);
                layoutCamera.setClickable(false);
                layoutCamera.setAlpha(0.5f); 
            }
            
            settingsViewModel.setCameraSelection("back_camera");
            
            String localizedCamera = getLocalizedOption("back_camera",
                    getResources().getStringArray(R.array.camera_options),
                    getResources().getStringArray(R.array.camera_options_fixed));
            textCameraSelection.setText(localizedCamera);
        } else if ("user_prefs_front_record".equals(selectedPreferenceFile)) {
            
            if (layoutCamera != null) {
                layoutCamera.setEnabled(false);
                layoutCamera.setClickable(false);
                layoutCamera.setAlpha(0.5f); 
            }
            
            settingsViewModel.setCameraSelection("front_camera");
            
            String localizedCamera = getLocalizedOption("front_camera",
                    getResources().getStringArray(R.array.camera_options),
                    getResources().getStringArray(R.array.camera_options_fixed));
            textCameraSelection.setText(localizedCamera);
        } else {
            
            if (layoutCamera != null) {
                layoutCamera.setEnabled(true);
                layoutCamera.setClickable(true);
                layoutCamera.setAlpha(1.0f); 
            }
        }
    }
}
