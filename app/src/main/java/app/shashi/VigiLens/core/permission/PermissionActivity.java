package app.shashi.VigiLens.core.permission;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.main.MainActivity;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class PermissionActivity extends AppCompatActivity {
    private static final String TAG = "PermissionActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private String[] permissions;

    private SwitchCompat cameraSwitch;
    private SwitchCompat audioSwitch;
    private SwitchCompat notificationSwitch;
    private Button continueButton;

    private CheckBox acceptTermsCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);

        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_app_permission));
        }

        LinearLayout sectionNotifications = findViewById(R.id.section_notifications);

        cameraSwitch = findViewById(R.id.switch_camera);
        audioSwitch = findViewById(R.id.switch_audio);
        notificationSwitch = findViewById(R.id.switch_notifications);
        continueButton = findViewById(R.id.button_continue);
        acceptTermsCheckBox = findViewById(R.id.checkbox_accept_terms);

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
            sectionNotifications.setVisibility(View.VISIBLE);
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
            sectionNotifications.setVisibility(View.GONE);
        }

        cameraSwitch.setOnCheckedChangeListener(this::onPermissionSwitchChanged);
        audioSwitch.setOnCheckedChangeListener(this::onPermissionSwitchChanged);
        notificationSwitch.setOnCheckedChangeListener(this::onPermissionSwitchChanged);

        continueButton.setOnClickListener(v -> finishWithResult());

        setCheckBoxTextWithLinks(); 

        
        acceptTermsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveTermsAcceptance(isChecked);
            setContinueButtonState();
        });

        updateUI();
        loadTermsAcceptance();
        setContinueButtonState();
    }

    private void loadTermsAcceptance() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean termsAccepted = sharedPreferences.getBoolean("terms_accepted", false);
        acceptTermsCheckBox.setChecked(termsAccepted);
    }

    @Override
    protected void attachBaseContext(Context base) {
        
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }


    private void saveTermsAcceptance(boolean accepted) {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("terms_accepted", accepted);
        editor.apply();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.permission_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_help) {
            showHelpDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelpDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_grant_permissions))
                .setMessage(getString(R.string.message_help_dialog))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    
                })
                .show();
    }


    private void setCheckBoxTextWithLinks() {
        String text = getString(R.string.terms_privacy_agreement);

        SpannableString spannableString = new SpannableString(text);

        
        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://vigilens.shashi.app/terms-of-use"));
                startActivity(browserIntent);
            }
        };

        
        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://vigilens.shashi.app/privacy-policy"));
                startActivity(browserIntent);
            }
        };

        
        int termsStart = text.indexOf("Terms of Use");
        int termsEnd = termsStart + "Terms of Use".length();
        int privacyStart = text.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        
        acceptTermsCheckBox.setText(spannableString);
        acceptTermsCheckBox.setMovementMethod(LinkMovementMethod.getInstance());

        
        acceptTermsCheckBox.setLinkTextColor(ContextCompat.getColor(this, R.color.md_theme_primary));
    }

    private void onPermissionSwitchChanged(CompoundButton buttonView, boolean isChecked) {
        String permission = null;
        int buttonId = buttonView.getId();

        if (buttonId == R.id.switch_camera) {
            permission = Manifest.permission.CAMERA;
        } else if (buttonId == R.id.switch_audio) {
            permission = Manifest.permission.RECORD_AUDIO;
        } else if (buttonId == R.id.switch_notifications) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                }
            }
        }

        if (permission != null) {
            if (isChecked) {
                requestPermission(permission);
            } else {

                
                if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                    
                    buttonView.setChecked(false);
                    Toast.makeText(this, getString(R.string.toast_permissions_required), Toast.LENGTH_SHORT).show();

                }
            }
        }

        setContinueButtonState();
    }

    private void requestPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            
            updateUI();
        }
    }

    private void updateUI() {
        cameraSwitch.setChecked(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        audioSwitch.setChecked(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            
            notificationSwitch.setChecked(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        } else {
            
            notificationSwitch.setChecked(true);
        }
    }

    private boolean arePermissionsGranted() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void setContinueButtonState() {
        boolean termsAccepted = acceptTermsCheckBox.isChecked();

        continueButton.setEnabled(termsAccepted);

        if (termsAccepted) {
            
            continueButton.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_primary, getTheme()));
            continueButton.setTextColor(ContextCompat.getColor(this, R.color.md_theme_onPrimary)); 
        } else {
            
            continueButton.setBackgroundTintList(getResources().getColorStateList(R.color.md_theme_surfaceContainerHigh, getTheme()));
            continueButton.setTextColor(ContextCompat.getColor(this, R.color.md_theme_outline)); 
        }
    }

    private void finishWithResult() {
        Intent intent = new Intent();
        intent.putExtra("permissions_granted", true);
        setResult(RESULT_OK, intent);

        
        if (isTaskRoot()) {
            
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainIntent);
        }

        
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean permissionsChanged = false;
            for (int i = 0; i < permissions.length; i++) {
                switch (permissions[i]) {
                    case Manifest.permission.CAMERA:
                        cameraSwitch.setChecked(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                        permissionsChanged = true;
                        break;
                    case Manifest.permission.RECORD_AUDIO:
                        audioSwitch.setChecked(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                        permissionsChanged = true;
                        break;
                    case Manifest.permission.POST_NOTIFICATIONS:
                        notificationSwitch.setChecked(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                        permissionsChanged = true;
                        break;
                }
            }
            if (permissionsChanged) {
                updateUI();
                setContinueButtonState();
                if (!arePermissionsGranted()) {
                    Log.e(TAG, "Some permissions are denied.");
                    
                    boolean showDeniedDialog = false;
                    for (int result : grantResults) {
                        if (result == PackageManager.PERMISSION_DENIED) {
                            showDeniedDialog = true;
                            break;
                        }
                    }
                    if (showDeniedDialog) {
                        showPermissionDeniedDialog();
                    }
                } else {
                    Log.d(TAG, "All permissions granted.");
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_permission_denied))
                .setMessage(getString(R.string.message_permission_denied))
                .setPositiveButton(getString(R.string.btn_setting), (dialog, which) -> openSettings())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        alertDialog.show();
    }


    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
