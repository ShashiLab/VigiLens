package app.shashi.VigiLens.feature.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.main.MainActivity;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class EnterPasswordActivity extends AppCompatActivity {
    private static final int PIN_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 300000; // 5 minutes

    private final StringBuilder passwordInput = new StringBuilder();
    private Vibrator vibrator;
    private SharedPreferences sharedPreferences;
    private int failedAttempts;
    private long lockoutEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initVibrator();
        initPreferences();
        setupBiometric();
        setupNumericKeypad();
    }

    private void initVibrator() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            vibrator = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void initPreferences() {
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        failedAttempts = sharedPreferences.getInt("failed_attempts", 0);
        lockoutEndTime = sharedPreferences.getLong("lockout_end_time", 0);
    }

    private void setupBiometric() {
        ImageView fingerprintIcon = findViewById(R.id.iv_fingerprint);
        if (sharedPreferences.getBoolean("biometric_authentication_prompt", false)) {
            fingerprintIcon.setVisibility(View.VISIBLE);
            fingerprintIcon.setOnClickListener(v -> {
                performHapticFeedback(v);
                showBiometricPrompt();
            });
            showBiometricPrompt();
        }
    }

    private void setupNumericKeypad() {
        int[] buttonIds = {R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5,
                R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_0, R.id.btn_del};

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this::handleKeyPress);
        }
    }

    private void handleKeyPress(View v) {
        if (isLockedOut()) {
            showLockoutMessage();
            return;
        }

        performHapticFeedback(v);
        Button button = (Button) v;

        if (button.getId() == R.id.btn_del) {
            if (passwordInput.length() > 0) {
                passwordInput.deleteCharAt(passwordInput.length() - 1);
                updatePinCircles();
            }
        } else if (passwordInput.length() < PIN_LENGTH) {
            passwordInput.append(button.getText().toString());
            updatePinCircles();
            if (passwordInput.length() == PIN_LENGTH) {
                new Handler().postDelayed(this::verifyPassword, 100);
            }
        }
    }

    private boolean isLockedOut() {
        if (SystemClock.elapsedRealtime() < lockoutEndTime) {
            return true;
        } else if (lockoutEndTime > 0) {
            resetLockout();
        }
        return false;
    }

    private void resetLockout() {
        failedAttempts = 0;
        lockoutEndTime = 0;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("failed_attempts", 0);
        editor.putLong("lockout_end_time", 0);
        editor.apply();
    }

    private void showLockoutMessage() {
        long remainingTime = (lockoutEndTime - SystemClock.elapsedRealtime()) / 1000;
        Toast.makeText(this, getString(R.string.toast_account_locked, remainingTime), Toast.LENGTH_SHORT).show();
    }

    private void verifyPassword() {
        if (passwordInput.toString().equals(sharedPreferences.getString("app_password", ""))) {
            onAuthenticationSuccess();
        } else {
            onAuthenticationFailure();
        }
    }

    private void onAuthenticationSuccess() {
        resetLockout();
        sharedPreferences.edit().putBoolean("isAuthenticated", true).apply();
        handleRedirect();
    }

    private void onAuthenticationFailure() {
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            lockoutEndTime = SystemClock.elapsedRealtime() + LOCKOUT_DURATION;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("failed_attempts", failedAttempts);
            editor.putLong("lockout_end_time", lockoutEndTime);
            editor.apply();
            showLockoutMessage();
        }
        vibrateError();
        Toast.makeText(this, getString(R.string.toast_incorrect_password), Toast.LENGTH_SHORT).show();
        passwordInput.setLength(0);
        updatePinCircles();
    }

    private void handleRedirect() {
        Intent previousIntent = getIntent();
        if (previousIntent.hasExtra("redirectActivity")) {
            try {
                Class<?> clazz = Class.forName(previousIntent.getStringExtra("redirectActivity"));
                startActivity(new Intent(this, clazz));
            } catch (ClassNotFoundException e) {
                startActivity(new Intent(this, MainActivity.class));
            }
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        onAuthenticationSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.toast_authentication_failed), Toast.LENGTH_SHORT).show();
                        vibrateError();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.title_biometric_prompt))
                .setNegativeButtonText(getString(R.string.btn_use_password))
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void performHapticFeedback(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }
    }

    private void vibrateError() {
        if (vibrator != null && vibrator.hasVibrator() &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 80, 50, 80},
                    new int[]{0, 255, 0, 255}, -1));
        }
    }

    private void updatePinCircles() {
        int length = passwordInput.length();
        findViewById(R.id.pin_circle_1).setBackgroundResource(length > 0 ?
                R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        findViewById(R.id.pin_circle_2).setBackgroundResource(length > 1 ?
                R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        findViewById(R.id.pin_circle_3).setBackgroundResource(length > 2 ?
                R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        findViewById(R.id.pin_circle_4).setBackgroundResource(length > 3 ?
                R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (vibrator != null) vibrator.cancel();
    }
}