package app.shashi.VigiLens.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.databinding.ActivityReportBugBinding;
import app.shashi.VigiLens.utils.DeviceUtils;

public class ReportBugActivity extends AppCompatActivity {
    private ActivityReportBugBinding binding;
    private Uri screenshotUri = null;
    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleSelectedScreenshot(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportBugBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupInputFields();
        setupAttachmentButton();
        setupSubmitButton();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_report_bug);
        }
    }

    private void setupInputFields() {
        // Add text change listeners for real-time validation
        binding.bugDescription.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateDescription();
            }
        });

        binding.stepsToReproduce.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSteps();
            }
        });
    }

    private void setupAttachmentButton() {
        binding.attachScreenshot.setOnClickListener(v -> showAttachmentOptions());
    }

    private void showAttachmentOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.attach_screenshot)
                .setItems(new String[]{
                        getString(R.string.take_screenshot),
                        getString(R.string.choose_from_gallery)
                }, (dialog, which) -> {
                    if (which == 0) {
                        // Take screenshot option
                        Toast.makeText(this, R.string.hint_screenshot, Toast.LENGTH_SHORT).show();
                    } else {
                        // Choose from gallery option
                        pickImage.launch("image/*");
                    }
                })
                .show();
    }

    private void handleSelectedScreenshot(Uri uri) {
        screenshotUri = uri;
        binding.attachScreenshot.setText(R.string.screenshot_attached);
        binding.attachScreenshot.setIcon(getDrawable(R.drawable.ic_check));
        showSnackbar(getString(R.string.screenshot_attached_success));
    }

    private void setupSubmitButton() {
        binding.submitBug.setOnClickListener(v -> {
            if (validateInput()) {
                showLoading(true);
                submitBugReport();
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        if (!validateDescription()) {
            isValid = false;
        }

        if (!validateSteps()) {
            isValid = false;
        }

        return isValid;
    }

    private boolean validateDescription() {
        String description = binding.bugDescription.getText().toString().trim();
        if (TextUtils.isEmpty(description)) {
            binding.bugDescription.setError(getString(R.string.error_description_required));
            return false;
        }
        return true;
    }

    private boolean validateSteps() {
        String steps = binding.stepsToReproduce.getText().toString().trim();
        if (TextUtils.isEmpty(steps)) {
            binding.stepsToReproduce.setError(getString(R.string.error_steps_required));
            return false;
        }
        return true;
    }

    private void showLoading(boolean show) {
        binding.progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.submitBug.setEnabled(!show);
        binding.attachScreenshot.setEnabled(!show);
        binding.bugDescription.setEnabled(!show);
        binding.stepsToReproduce.setEnabled(!show);
    }

    private void submitBugReport() {
        String description = binding.bugDescription.getText().toString().trim();
        String steps = binding.stepsToReproduce.getText().toString().trim();
        String deviceInfo = DeviceUtils.getDeviceInfo(this);

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Bug Description:\n").append(description)
                .append("\n\nSteps to Reproduce:\n").append(steps)
                .append("\n\nDevice Information:\n").append(deviceInfo);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ShashiTheDev@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - VigiLens App");
        intent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());

        if (screenshotUri != null) {
            intent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        try {
            showLoading(false);
            startActivity(Intent.createChooser(intent, getString(R.string.choose_email_client)));
        } catch (android.content.ActivityNotFoundException ex) {
            showError(getString(R.string.no_email_apps_found));
        }
    }

    private void showError(String message) {
        showLoading(false);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> submitBugReport())
                .show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Simple TextWatcher adapter to avoid implementing unused methods
    private static class TextWatcherAdapter implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}