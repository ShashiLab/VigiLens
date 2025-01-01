package app.shashi.VigiLens.about;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.android.material.snackbar.Snackbar;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {
    private static final String TAG = "AboutActivity";
    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupVersionInfo();
        setupClickListeners();
    }

    private void setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupVersionInfo() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            long versionCode = PackageInfoCompat.getLongVersionCode(packageInfo);
            String versionName = packageInfo.versionName;
            binding.versionText.setText(getString(R.string.label_version, versionName));
        } catch (Exception e) {
            binding.versionText.setText(getString(R.string.label_version_unknown));
        }
    }

    private void setupClickListeners() {
        // Source Code Button
        binding.sourceCodeButton.setOnClickListener(v ->
                openUrl(getString(R.string.url_source_code)));

        // Terms of Use Button
        binding.termsButton.setOnClickListener(v ->
                openUrl(getString(R.string.url_terms)));

        // Privacy Policy Button
        binding.privacyButton.setOnClickListener(v ->
                openUrl(getString(R.string.url_privacy)));

        // Website Container
        binding.websiteContainer.setOnClickListener(v ->
                openUrl(getString(R.string.url_website)));

        // Email Container
        binding.emailContainer.setOnClickListener(v -> sendEmail());
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showError(getString(R.string.error_url_open));
        }
    }

    private void sendEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.url_support_email)));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
            startActivity(Intent.createChooser(intent, getString(R.string.email_client_chooser)));
        } catch (Exception e) {
            showError(getString(R.string.error_sending_email));
        }
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}