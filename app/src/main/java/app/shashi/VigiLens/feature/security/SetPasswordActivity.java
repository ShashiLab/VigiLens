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

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class SetPasswordActivity extends AppCompatActivity {

    private final StringBuilder passwordBuilder = new StringBuilder();
    private final StringBuilder confirmPasswordBuilder = new StringBuilder();
    private boolean isConfirmingPassword = false;
    private final Handler handler = new Handler(); 

    private View[] pinCircles;
    private SharedPreferences sharedPreferences;
    private TextView passwordPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        
        if (sharedPreferences.contains("app_password")) {
            finish();
            return;
        }

        passwordPrompt = findViewById(R.id.tv_password_prompt);
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
                if (isConfirmingPassword) {
                    if (confirmPasswordBuilder.length() > 0) {
                        confirmPasswordBuilder.deleteCharAt(confirmPasswordBuilder.length() - 1);
                    }
                } else {
                    if (passwordBuilder.length() > 0) {
                        passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
                    }
                }
            } else {
                if (isConfirmingPassword) {
                    if (confirmPasswordBuilder.length() < 4) {
                        confirmPasswordBuilder.append(buttonText);
                    }
                } else {
                    if (passwordBuilder.length() < 4) {
                        passwordBuilder.append(buttonText);
                    }
                }
            }

            updatePinCircles();

            
            if (passwordBuilder.length() == 4 && !isConfirmingPassword) {
                handler.postDelayed(this::confirmPassword, 500);  
            } else if (confirmPasswordBuilder.length() == 4 && isConfirmingPassword) {
                handler.postDelayed(this::verifyPassword, 500);  
            }
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void updatePinCircles() {
        String currentPassword = isConfirmingPassword ? confirmPasswordBuilder.toString() : passwordBuilder.toString();
        for (int i = 0; i < pinCircles.length; i++) {
            pinCircles[i].setBackgroundResource(i < currentPassword.length() ? R.drawable.filled_pin_circle : R.drawable.empty_pin_circle);
        }
    }

    private void confirmPassword() {
        isConfirmingPassword = true;
        passwordPrompt.setText(getString(R.string.title_confirm_password));
        clearPinCircles();
    }

    private void verifyPassword() {
        if (passwordBuilder.toString().equals(confirmPasswordBuilder.toString())) {
            
            sharedPreferences.edit().putString("app_password", passwordBuilder.toString()).apply();
            Toast.makeText(this, getString(R.string.toast_password_set_success), Toast.LENGTH_SHORT).show();

            
            setResult(RESULT_OK);  
            finish();  
        } else {
            
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                
                VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            }
            Toast.makeText(this, getString(R.string.toast_passwords_do_not_match), Toast.LENGTH_SHORT).show();

            
            resetPasswordSetting();  
        }
    }

    private void clearPinCircles() {
        
        for (View pinCircle : pinCircles) {
            pinCircle.setBackgroundResource(R.drawable.empty_pin_circle);
        }
    }

    private void resetPasswordSetting() {
        
        isConfirmingPassword = false;
        passwordBuilder.setLength(0);
        confirmPasswordBuilder.setLength(0);
        passwordPrompt.setText(getString(R.string.title_set_password));  
        clearPinCircles();  
        handler.removeCallbacksAndMessages(null);  
    }
}
