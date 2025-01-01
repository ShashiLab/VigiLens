package app.shashi.VigiLens.feature.gallery.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.gallery.adapter.VideoAdapter;
import app.shashi.VigiLens.feature.gallery.util.VideoSaver;

public class GalleryFragment extends Fragment implements VideoAdapter.VideoMenuListener, VideoAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private LinearLayout noVideosLayout;
    private boolean isSelectAll = false;
    private AdView adView;
    private GalleryViewModel videosViewModel;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        noVideosLayout = view.findViewById(R.id.no_videos_layout);

        setHasOptionsMenu(true);

        
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        
        videoAdapter = new VideoAdapter(requireContext(), new ArrayList<>(), this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(videoAdapter);

        
        videosViewModel = new ViewModelProvider(this).get(GalleryViewModel.class);

        
        if (videosViewModel.getVideoList().isEmpty()) {
            loadVideosAsync();
        } else {
            
            videoAdapter.setVideoList(videosViewModel.getVideoList());
            setupRecyclerView();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        checkForNewVideos();
    }

    private void setupRecyclerView() {
        if (videosViewModel.getVideoList().isEmpty()) {
            noVideosLayout.setVisibility(View.VISIBLE);
        } else {
            noVideosLayout.setVisibility(View.GONE);
            videoAdapter.notifyDataSetChanged();
        }
    }

    private void loadVideosAsync() {
        executorService.execute(() -> {
            List<Video> loadedVideos = processVideoDirectory();

            
            videosViewModel.addVideos(loadedVideos);

            
            mainHandler.post(() -> {
                videoAdapter.setVideoList(videosViewModel.getVideoList());
                videoAdapter.notifyDataSetChanged();
                if (loadedVideos.isEmpty()) {
                    noVideosLayout.setVisibility(View.VISIBLE);
                } else {
                    noVideosLayout.setVisibility(View.GONE);
                }
            });
        });
    }

    private void checkForNewVideos() {
        executorService.execute(() -> {
            List<Video> currentVideos = videosViewModel.getVideoList();
            List<Video> newVideos = new ArrayList<>();

            File moviesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (moviesDir != null && moviesDir.exists()) {
                File[] files = moviesDir.listFiles();
                if (files != null) {
                    
                    java.util.Arrays.sort(files, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".mp4")) {
                            boolean exists = false;
                            for (Video video : currentVideos) {
                                if (video.getThumbnailPath().equals(file.getAbsolutePath())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                try {
                                    Video video = processVideoFile(file);
                                    if (video != null) {
                                        newVideos.add(video);

                                        
                                        Video finalVideo = video;
                                        mainHandler.post(() -> {
                                            videosViewModel.getVideoList().add(0, finalVideo);
                                            videoAdapter.notifyItemInserted(0);
                                            recyclerView.scrollToPosition(0);
                                            noVideosLayout.setVisibility(View.GONE);
                                        });
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }

            
            if (newVideos.isEmpty() && videosViewModel.getVideoList().isEmpty()) {
                mainHandler.post(() -> noVideosLayout.setVisibility(View.VISIBLE));
            }
        });
    }

    
    private List<Video> processVideoDirectory() {
        List<Video> videoList = new ArrayList<>();
        File moviesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        if (moviesDir != null && moviesDir.exists()) {
            File[] files = moviesDir.listFiles();
            if (files != null) {
                
                java.util.Arrays.sort(files, (file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()));

                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".mp4")) {
                        try {
                            Video video = processVideoFile(file);
                            if (video != null) {
                                videoList.add(video);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return videoList;
    }

    
    private Video processVideoFile(File file) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());

            
            String durationFormatted;
            try {
                String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null) {
                    long durationMillis = Long.parseLong(duration);
                    durationFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                            (durationMillis / 1000) / 60,
                            (durationMillis / 1000) % 60);
                } else {
                    durationFormatted = "00:00"; 
                }
            } catch (NumberFormatException e) {
                durationFormatted = "00:00"; 
                e.printStackTrace();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a MM/dd/yyyy", Locale.getDefault());
            String date = sdf.format(new Date(file.lastModified()));
            String size = getReadableFileSize(file.length());
            String thumbnailPath = file.getAbsolutePath();

            return new Video(file.getName(), durationFormatted, size, date, thumbnailPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (adView != null) {
            adView.destroy();
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_video_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            requireActivity().onBackPressed();
            return true;
        }

        if (id == R.id.action_select_all) {
            if (videosViewModel.getVideoList().isEmpty()) {
                showNoVideosAlert();
            } else {
                isSelectAll = !isSelectAll;
                if (isSelectAll) {
                    selectAllVideos();
                    item.setTitle(getString(R.string.action_deselect_all));  
                } else {
                    deselectAllVideos();
                    item.setTitle(getString(R.string.action_select_all));  
                }
            }
            return true;
        }


        if (id == R.id.action_delete_selected) {
            if (hasSelectedVideos()) {
                confirmDeleteSelectedVideos();
            } else {
                showNoVideosSelectedAlert();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean hasSelectedVideos() {
        for (Video video : videosViewModel.getVideoList()) {
            if (video.isSelected()) {
                return true;
            }
        }
        return false;
    }

    private void showNoVideosAlert() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_no_videos)
                .setMessage(R.string.message_no_videos)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showNoVideosSelectedAlert() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_no_videos_selected)
                .setMessage(R.string.message_no_videos_selected)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void selectAllVideos() {
        for (Video video : videosViewModel.getVideoList()) {
            video.setSelected(true);
        }
        videoAdapter.notifyDataSetChanged();
    }

    private void deselectAllVideos() {
        for (Video video : videosViewModel.getVideoList()) {
            video.setSelected(false);
        }
        videoAdapter.notifyDataSetChanged();
    }

    private void confirmDeleteSelectedVideos() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_confirm_deletion)
                .setMessage(R.string.message_confirm_deletion)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteSelectedVideos())
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }

    private void deleteSelectedVideos() {
        List<Video> videosToDelete = new ArrayList<>();
        for (Video video : videosViewModel.getVideoList()) {
            if (video.isSelected()) {
                File file = new File(video.getThumbnailPath());
                if (file.delete()) {
                    videosToDelete.add(video);
                } else {
                    Toast.makeText(requireContext(), getString(R.string.toast_failed_to_delete_video, video.getName()), Toast.LENGTH_SHORT).show();
                }
            }
        }
        videosViewModel.getVideoList().removeAll(videosToDelete);
        videoAdapter.notifyDataSetChanged();

        if (videosViewModel.getVideoList().isEmpty()) {
            noVideosLayout.setVisibility(View.VISIBLE);
        }
    }

    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = getResources().getStringArray(R.array.file_size_units);
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    

    @Override
    public void onOpenWith(Video video) {
        File file = new File(video.getThumbnailPath());
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), getString(R.string.toast_no_app_to_open_video), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSave(Video video) {
        if (getActivity() == null) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.title_save_video));
        builder.setMessage(getString(R.string.message_watch_ad_support_us));

        builder.setPositiveButton(getString(R.string.btn_ok), (dialog, which) -> {
            
            VideoSaver saveVideoUtility = VideoSaver.getInstance(getActivity());
            saveVideoUtility.initializeSavingNotification();

            
            saveVideoUtility.saveVideoWithAd(getActivity(), video.getThumbnailPath(), new VideoSaver.SaveVideoCallback() {
                @Override
                public void onSaveSuccess() {
                    
                    saveVideoUtility.showSaveResultDialog(getActivity(), true, null);
                }

                @Override
                public void onSaveFailure(String errorMessage) {
                    
                    saveVideoUtility.showSaveResultDialog(getActivity(), false, errorMessage);
                }
            });
        });

        builder.setNegativeButton(getString(R.string.btn_cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onShare(Video video) {
        File file = new File(video.getThumbnailPath());
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, getString(R.string.title_share_video)));
    }

    @Override
    public void onDetails(Video video) {
        
        String name = video.getName();
        String duration = video.getDuration();
        String size = video.getSize();
        String date = video.getDate();
        String path = video.getThumbnailPath();

        
        File videoFile = new File(path);
        String lastModified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(videoFile.lastModified()));

        
        String nameLabel = getString(R.string.label_name);
        String durationLabel = getString(R.string.label_duration);
        String sizeLabel = getString(R.string.label_size);
        String dateLabel = getString(R.string.label_date);
        String pathLabel = getString(R.string.label_path);
        String lastModifiedLabel = getString(R.string.label_last_modified);

        
        String details = "<b>" + nameLabel + ":</b> " + name + "<br>" +
                "<b>" + durationLabel + ":</b> " + duration + "<br>" +
                "<b>" + sizeLabel + ":</b> " + size + "<br>" +
                "<b>" + dateLabel + ":</b> " + date + "<br>" +
                "<b>" + pathLabel + ":</b> " + path + "<br>" +
                "<b>" + lastModifiedLabel + ":</b> " + lastModified;

        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_video_details)  
                .setMessage(Html.fromHtml(details, Html.FROM_HTML_MODE_LEGACY))  
                .setPositiveButton(android.R.string.ok, null)  
                .show();
    }


    @Override
    public void onRename(Video video) {
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(getString(R.string.title_rename_video));  

        
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(video.getName().replace(".mp4", ""));  
        input.setHint(getString(R.string.hint_enter_new_name));  

        
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_padding);
        container.setPadding(padding, padding, padding, padding);
        container.addView(input);

        builder.setView(container);

        
        builder.setPositiveButton(getString(R.string.btn_rename), (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                renameVideoFile(video, newName);
            } else {
                Toast.makeText(requireContext(), getString(R.string.toast_name_empty_error), Toast.LENGTH_SHORT).show();
            }
        });

        
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();
    }
    private void renameVideoFile(Video video, String newName) {
        executorService.execute(() -> {
            File oldFile = new File(video.getThumbnailPath());
            File newFile = new File(oldFile.getParent(), newName + ".mp4");

            if (oldFile.renameTo(newFile)) {
                mainHandler.post(() -> {
                    video.setName(newFile.getName());
                    video.setThumbnailPath(newFile.getAbsolutePath());
                    videoAdapter.notifyDataSetChanged();
                    
                    Toast.makeText(requireContext(), getString(R.string.toast_video_rename_success), Toast.LENGTH_SHORT).show();
                });
            } else {
                mainHandler.post(() -> {
                    
                    Toast.makeText(requireContext(), getString(R.string.toast_video_rename_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    @Override
    public void onDelete(Video video) {
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.title_delete_video))  
                .setMessage(getString(R.string.message_delete_confirmation, video.getName()))  
                .setPositiveButton(R.string.action_delete, (dialogInterface, which) -> {
                    deleteSingleVideo(video);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }


    private void deleteSingleVideo(Video video) {
        executorService.execute(() -> {
            File file = new File(video.getThumbnailPath());
            if (file.delete()) {
                mainHandler.post(() -> {
                    videosViewModel.getVideoList().remove(video);
                    videoAdapter.notifyDataSetChanged();
                    
                    Toast.makeText(requireContext(), getString(R.string.toast_video_deleted_success), Toast.LENGTH_SHORT).show();
                    if (videosViewModel.getVideoList().isEmpty()) {
                        noVideosLayout.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                mainHandler.post(() -> {
                    
                    Toast.makeText(requireContext(), getString(R.string.toast_video_delete_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    

    @Override
    public void onItemClick(Video video) {
        
        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.putExtra("video_path", video.getThumbnailPath());
        startActivity(intent);
    }

    
    public static class Video {
        private String name;
        private final String duration;
        private final String size;
        private final String date;
        private String thumbnailPath;
        private boolean isSelected;

        public Video(String name, String duration, String size, String date, String thumbnailPath) {
            this.name = name;
            this.duration = duration;
            this.size = size;
            this.date = date;
            this.thumbnailPath = thumbnailPath;
            this.isSelected = false;
        }

        public String getName() {
            return name;
        }

        public void setName(String newName) {
            this.name = newName;
        }

        public String getDuration() {
            return duration;
        }

        public String getSize() {
            return size;
        }

        public String getDate() {
            return date;
        }

        public String getThumbnailPath() {
            return thumbnailPath;
        }

        public void setThumbnailPath(String newPath) {
            this.thumbnailPath = newPath;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }
    }
}
