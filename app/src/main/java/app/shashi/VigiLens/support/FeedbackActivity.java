package app.shashi.VigiLens.support;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.databinding.ActivityFeedbackBinding;
import app.shashi.VigiLens.utils.DeviceUtils;

public class FeedbackActivity extends AppCompatActivity {
    private ActivityFeedbackBinding binding;
    private float userRating = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupInputFields();
        setupRatingBar();
        setupSubmitButton();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.topAppBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_feedback);
        }
    }

    private void setupInputFields() {
        binding.feedbackText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFeedback();
            }
        });

        binding.userSuggestions.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSuggestions();
            }
        });
    }

    private void setupRatingBar() {
        binding.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            userRating = rating;
            updateRatingFeedback(rating);
        });
    }

    private void updateRatingFeedback(float rating) {
        String feedback;
        if (rating <= 1) {
            feedback = getString(R.string.rating_very_dissatisfied);
        } else if (rating <= 2) {
            feedback = getString(R.string.rating_dissatisfied);
        } else if (rating <= 3) {
            feedback = getString(R.string.rating_neutral);
        } else if (rating <= 4) {
            feedback = getString(R.string.rating_satisfied);
        } else {
            feedback = getString(R.string.rating_very_satisfied);
        }
        binding.ratingFeedback.setText(feedback);
    }

    private void setupSubmitButton() {
        binding.submitFeedback.setOnClickListener(v -> {
            if (validateInput()) {
                showLoading(true);
                submitFeedback();
            }
        });
    }

    private boolean validateInput() {
        boolean isValid = true;

        if (userRating == 0) {
            showError(getString(R.string.error_rating_required));
            isValid = false;
        }

        if (!validateFeedback()) {
            isValid = false;
        }

        return isValid;
    }

    private boolean validateFeedback() {
        String feedback = binding.feedbackText.getText().toString().trim();
        if (TextUtils.isEmpty(feedback)) {
            binding.feedbackText.setError(getString(R.string.error_feedback_required));
            return false;
        }
        return true;
    }

    private boolean validateSuggestions() {
        // Suggestions are optional, so always return true
        return true;
    }

    private void showLoading(boolean show) {
        binding.progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.submitFeedback.setEnabled(!show);
        binding.feedbackText.setEnabled(!show);
        binding.userSuggestions.setEnabled(!show);
        binding.ratingBar.setEnabled(!show);
    }

    private void submitFeedback() {
        String feedback = binding.feedbackText.getText().toString().trim();
        String suggestions = binding.userSuggestions.getText().toString().trim();
        String deviceInfo = DeviceUtils.getDeviceInfo(this);

        StringBuilder emailBody = new StringBuilder();
        emailBody.append("Rating: ").append(userRating).append("/5")
                .append("\n\nFeedback:\n").append(feedback);

        if (!TextUtils.isEmpty(suggestions)) {
            emailBody.append("\n\nSuggestions for Improvement:\n").append(suggestions);
        }

        emailBody.append("\n\nDevice Information:\n").append(deviceInfo);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"ShashiTheDev@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "App Feedback - VigiLens App");
        intent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());

        try {
            showLoading(false);
            startActivity(Intent.createChooser(intent, getString(R.string.choose_email_client)));
            showThankYouDialog();
        } catch (android.content.ActivityNotFoundException ex) {
            showError(getString(R.string.no_email_apps_found));
        }
    }

    private void showThankYouDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_thank_you)
                .setMessage(R.string.message_thank_you)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showError(String message) {
        showLoading(false);
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> submitFeedback())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class TextWatcherAdapter implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}