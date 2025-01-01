package app.shashi.VigiLens.feature.gallery.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.gallery.util.VideoSaver;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class VideoPlayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private String videoPath;
    private File videoFile;
    private MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_playback);

        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);  
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        
        VideoSaver saveVideoUtility = VideoSaver.getInstance(this);
        saveVideoUtility.loadRewardedAd();

        videoView = findViewById(R.id.video_view);

        videoPath = getIntent().getStringExtra("video_path");
        videoFile = new File(videoPath);

        if (videoFile.exists()) {
            videoView.setVideoURI(Uri.parse(videoPath));

            
            String videoFileName = videoFile.getName();
            Objects.requireNonNull(getSupportActionBar()).setTitle(videoFileName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            videoView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (mediaController.isShowing()) {
                        mediaController.hide();
                    } else {
                        mediaController.show();
                    }
                }
                return true;
            });

            videoView.start();
        } else {
            showNoVideoDialog();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }

    private void showNoVideoDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_error)
                .setIcon(R.drawable.ic_error)
                .setMessage(R.string.message_video_not_available)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_video_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_details) {
            showDetails();
            return true;
        } else if (itemId == R.id.action_rename) {
            renameVideo();
            return true;
        } else if (itemId == R.id.action_save) {
            confirmSave();
            return true;
        } else if (itemId == R.id.action_delete) {
            confirmDelete();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void showDetails() {
        
        String name = videoFile.getName();
        String path = videoFile.getAbsolutePath();
        long size = videoFile.length();
        String formattedSize = getReadableFileSize(size);
        long lastModified = videoFile.lastModified();
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(lastModified));

        
        String nameLabel = getString(R.string.label_name);
        String pathLabel = getString(R.string.label_path);
        String sizeLabel = getString(R.string.label_size);
        String dateLabel = getString(R.string.label_last_modified);

        
        String details = "<b>" + nameLabel + ":</b> " + name + "<br>" +
                "<b>" + pathLabel + ":</b> " + path + "<br>" +
                "<b>" + sizeLabel + ":</b> " + formattedSize + "<br>" +
                "<b>" + dateLabel + ":</b> " + formattedDate;

        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.title_video_details)  
                .setMessage(Html.fromHtml(details, Html.FROM_HTML_MODE_LEGACY))  
                .setPositiveButton(R.string.btn_ok, null)  
                .show();
    }

    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = getResources().getStringArray(R.array.file_size_units);
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void renameVideo() {
        final EditText input = new EditText(this);
        String nameWithoutExtension = videoFile.getName().replaceFirst("[.][^.]+$", "");
        input.setText(nameWithoutExtension);

        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this)
                .setView(input)
                .setPositiveButton(getString(R.string.btn_rename), null)
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            Button renameButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancelButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            renameButton.setOnClickListener(v -> {
                String newName = input.getText().toString().trim() + ".mp4";
                File newFile = new File(videoFile.getParent(), newName);

                if (newFile.exists()) {
                    Toast.makeText(this, getString(R.string.toast_video_rename_exists, newName), Toast.LENGTH_SHORT).show();
                } else {
                    if (videoFile.renameTo(newFile)) {
                        videoFile = newFile;
                        videoPath = newFile.getAbsolutePath();
                        videoView.setVideoURI(Uri.parse(videoPath));
                        Objects.requireNonNull(getSupportActionBar()).setTitle(newFile.getName());  
                        Toast.makeText(this, getString(R.string.toast_video_rename_success, newName), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.toast_video_rename_failed, Toast.LENGTH_SHORT).show();
                    }
                }

                alertDialog.dismiss();
            });

            cancelButton.setOnClickListener(v -> alertDialog.dismiss());
        });

        alertDialog.show();
    }

    private void confirmDelete() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.title_delete_video));
        builder.setMessage(getString(R.string.message_conform_delete));
        builder.setPositiveButton(getString(R.string.action_delete), (dialog, which) -> deleteVideo());
        builder.setNegativeButton(getString(R.string.btn_cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    private void deleteVideo() {
        if (videoFile.exists() && videoFile.delete()) {
            Toast.makeText(this, getString(R.string.toast_video_deleted_success), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, getString(R.string.toast_video_delete_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmSave() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.title_save_video));
        builder.setMessage(getString(R.string.message_watch_ad_support_us));
        builder.setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
            
            VideoSaver saveVideoUtility = VideoSaver.getInstance(this);

            
            saveVideoUtility.initializeSavingNotification();

            saveVideoUtility.saveVideoWithAd(this, videoPath, new VideoSaver.SaveVideoCallback() {
                @Override
                public void onSaveSuccess() {
                    
                    
                }

                @Override
                public void onSaveFailure(String errorMessage) {
                    
                    
                }
            });
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
