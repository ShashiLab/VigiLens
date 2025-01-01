package app.shashi.VigiLens.feature.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class DisablePasswordActivity extends AppCompatActivity {

    private final StringBuilder currentPasswordBuilder = new StringBuilder();
    private View[] pinCircles;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disable_password);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        findViewById(R.id.tv_password_prompt);
        pinCircles = new View[]{
                findViewById(R.id.pin_circle_1),
                findViewById(R.id.pin_circle_2),
                findViewById(R.id.pin_circle_3),
                findViewById(R.id.pin_circle_4)
        };

        setupNumericKeypad();

        
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                
                setResult(RESULT_CANCELED);  
                
                finish();
            }
        };

        
        getOnBackPressedDispatcher().addCallback(this, callback);
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
        if (currentPasswordBuilder.length() > 0) {
            currentPasswordBuilder.deleteCharAt(currentPasswordBuilder.length() - 1);
        }
    }

    private void addDigit(String digit) {
        if (currentPasswordBuilder.length() < 4) {
            currentPasswordBuilder.append(digit);
        }
    }

    private void updatePinCircles() {
        String currentPassword = currentPasswordBuilder.toString();

        for (int i = 0; i < pinCircles.length; i++) {
            pinCircles[i].setBackgroundResource(i < currentPassword.length() ? R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        }
    }

    private void handlePasswordStep() {
        if (currentPasswordBuilder.length() == 4) {
            new Handler().postDelayed(this::checkCurrentPassword, 100);
        }
    }

    private void checkCurrentPassword() {
        String savedPassword = sharedPreferences.getString("app_password", "");

        if (currentPasswordBuilder.toString().equals(savedPassword)) {
            
            sharedPreferences.edit().remove("app_password").apply();
            Toast.makeText(this, getString(R.string.toast_password_disabled_successfully), Toast.LENGTH_SHORT).show();

            
            setResult(RESULT_OK);  
            finish();
        } else {
            
            vibrateAndShowError(getString(R.string.toast_incorrect_password));
            resetPasswordEntry();
        }
    }

    private void resetPasswordEntry() {
        currentPasswordBuilder.setLength(0);
        clearPinCircles();
    }

    private void clearPinCircles() {
        for (View pinCircle : pinCircles) {
            pinCircle.setBackgroundResource(R.drawable.empty_pin_circle);
        }
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
