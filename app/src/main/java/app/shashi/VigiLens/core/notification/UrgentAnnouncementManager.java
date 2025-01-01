package app.shashi.VigiLens.core.notification;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.shashi.VigiLens.R;

public class UrgentAnnouncementManager {

    private final FirebaseRemoteConfig mFirebaseRemoteConfig;

    public UrgentAnnouncementManager(Context context) {
        
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.defaults_urgent_announcement);

        
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) 
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        
        fetchRemoteConfig(context);
    }

    
    private void fetchRemoteConfig(Context context) {
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        
                        boolean displayMessageBox = mFirebaseRemoteConfig.getBoolean("urgent_announcement_show");
                        if (displayMessageBox) {
                            showPopupMessage(context); 
                        }
                    } else {
                        
                        boolean displayMessageBox = mFirebaseRemoteConfig.getBoolean("urgent_announcement_show");
                        if (displayMessageBox) {
                            showPopupMessage(context); 
                        }
                        
                    }
                });
    }

    
    private void showPopupMessage(Context context) {
        
        String heading = mFirebaseRemoteConfig.getString("urgent_announcement_heading");
        String message = mFirebaseRemoteConfig.getString("urgent_announcement_message");
        boolean closable = mFirebaseRemoteConfig.getBoolean("urgent_announcement_closable");

        
        SpannableStringBuilder spannableMessage = parseLinks(context, message);

        
        ContextThemeWrapper themedContext = new ContextThemeWrapper(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);

        
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(themedContext)
                .setTitle(heading)  
                .setMessage(spannableMessage)  
                .setCancelable(false);  

        
        if (closable) {
            dialogBuilder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        }

        
        AlertDialog alertDialog = dialogBuilder.create();

        
        alertDialog.show();
        ((TextView) alertDialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    
    private SpannableStringBuilder parseLinks(Context context, String message) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        
        Pattern pattern = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(message);

        int lastMatchEnd = 0;
        while (matcher.find()) {
            
            spannableStringBuilder.append(message, lastMatchEnd, matcher.start());

            
            final String linkText = matcher.group(1);  
            final String url = matcher.group(2);       

            
            SpannableStringBuilder clickablePart = new SpannableStringBuilder(linkText);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(browserIntent);
                }
            };
            clickablePart.setSpan(clickableSpan, 0, linkText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            
            spannableStringBuilder.append(clickablePart);

            
            lastMatchEnd = matcher.end();
        }

        
        spannableStringBuilder.append(message, lastMatchEnd, message.length());

        return spannableStringBuilder;
    }
}
