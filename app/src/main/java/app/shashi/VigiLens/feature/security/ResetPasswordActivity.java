package app.shashi.VigiLens.feature.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class ResetPasswordActivity extends AppCompatActivity {

    private final StringBuilder currentPasswordBuilder = new StringBuilder();
    private final StringBuilder newPasswordBuilder = new StringBuilder();
    private final StringBuilder confirmNewPasswordBuilder = new StringBuilder();
    private int currentStep = 1; 
    private final Handler handler = new Handler(); 

    private View[] pinCircles;
    private SharedPreferences sharedPreferences;
    private TextView passwordPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        passwordPrompt = findViewById(R.id.tv_password_prompt);
        pinCircles = new View[]{
                findViewById(R.id.pin_circle_1),
                findViewById(R.id.pin_circle_2),
                findViewById(R.id.pin_circle_3),
                findViewById(R.id.pin_circle_4)
        };

        setupNumericKeypad();
    }

    @Override
    protected void attachBaseContext(Context base) {
        
        sharedPreferences = base.getSharedPreferences("user_prefs", MODE_PRIVATE);
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }


    private void setupNumericKeypad() {
        int[] buttonIds = new int[]{
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6,
                R.id.btn_7, R.id.btn_8, R.id.btn_9, R.id.btn_del
        };

        View.OnClickListener listener = v -> {
            Button button = (Button) v;
            String buttonText = button.getText().toString();

            if (buttonText.equals("DEL")) {
                deleteLastDigit();
            } else {
                addDigit(buttonText);
            }
            updatePinCircles();
            handlePasswordStep();
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void deleteLastDigit() {
        switch (currentStep) {
            case 1:
                if (currentPasswordBuilder.length() > 0) {
                    currentPasswordBuilder.deleteCharAt(currentPasswordBuilder.length() - 1);
                }
                break;
            case 2:
                if (newPasswordBuilder.length() > 0) {
                    newPasswordBuilder.deleteCharAt(newPasswordBuilder.length() - 1);
                }
                break;
            case 3:
                if (confirmNewPasswordBuilder.length() > 0) {
                    confirmNewPasswordBuilder.deleteCharAt(confirmNewPasswordBuilder.length() - 1);
                }
                break;
        }
    }

    private void addDigit(String digit) {
        switch (currentStep) {
            case 1:
                if (currentPasswordBuilder.length() < 4) {
                    currentPasswordBuilder.append(digit);
                }
                break;
            case 2:
                if (newPasswordBuilder.length() < 4) {
                    newPasswordBuilder.append(digit);
                }
                break;
            case 3:
                if (confirmNewPasswordBuilder.length() < 4) {
                    confirmNewPasswordBuilder.append(digit);
                }
                break;
        }
    }

    private void updatePinCircles() {
        String currentPassword = currentStep == 1 ? currentPasswordBuilder.toString() :
                currentStep == 2 ? newPasswordBuilder.toString() : confirmNewPasswordBuilder.toString();

        for (int i = 0; i < pinCircles.length; i++) {
            pinCircles[i].setBackgroundResource(i < currentPassword.length() ? R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        }
    }

    private void handlePasswordStep() {
        if (currentStep == 1 && currentPasswordBuilder.length() == 4) {
            handler.postDelayed(this::checkCurrentPassword, 500);  
        } else if (currentStep == 2 && newPasswordBuilder.length() == 4) {
            handler.postDelayed(this::confirmNewPassword, 500);  
        } else if (currentStep == 3 && confirmNewPasswordBuilder.length() == 4) {
            handler.postDelayed(this::verifyNewPassword, 500);  
        }
    }

    private void checkCurrentPassword() {
        String savedPassword = sharedPreferences.getString("app_password", "");

        if (currentPasswordBuilder.toString().equals(savedPassword)) {
            currentStep = 2;
            passwordPrompt.setText(getString(R.string.title_enter_new_password));
            clearPinCircles();
        } else {
            vibrateAndShowError(getString(R.string.toast_incorrect_password));
            resetPasswordSetting();
        }
    }

    private void confirmNewPassword() {
        currentStep = 3;
        passwordPrompt.setText(getString(R.string.title_confirm_new_password));
        clearPinCircles();
    }

    private void verifyNewPassword() {
        if (newPasswordBuilder.toString().equals(confirmNewPasswordBuilder.toString())) {
            sharedPreferences.edit().putString("app_password", newPasswordBuilder.toString()).apply();
            Toast.makeText(this, getString(R.string.toast_password_reset_success), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            vibrateAndShowError(getString(R.string.toast_passwords_do_not_match));
            resetPasswordSetting();
        }
    }

    private void clearPinCircles() {
        
        for (View pinCircle : pinCircles) {
            pinCircle.setBackgroundResource(R.drawable.empty_pin_circle);
        }
    }

    private void resetPasswordSetting() {
        
        currentPasswordBuilder.setLength(0);
        newPasswordBuilder.setLength(0);
        confirmNewPasswordBuilder.setLength(0);
        currentStep = 1;  
        passwordPrompt.setText(getString(R.string.title_enter_current_password));  
        clearPinCircles();  
        handler.removeCallbacksAndMessages(null);  
    }

    private void vibrateAndShowError(String message) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            
            VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(effect);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
