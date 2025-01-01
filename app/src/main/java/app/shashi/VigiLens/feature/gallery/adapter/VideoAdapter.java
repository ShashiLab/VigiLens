package app.shashi.VigiLens.feature.gallery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.feature.gallery.ui.GalleryFragment;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<GalleryFragment.Video> videoList;
    private final Context context;
    private final VideoMenuListener menuListener;
    private final OnItemClickListener itemClickListener;

    
    public interface VideoMenuListener {
        void onOpenWith(GalleryFragment.Video video);
        void onShare(GalleryFragment.Video video);
        void onDetails(GalleryFragment.Video video);
        void onRename(GalleryFragment.Video video);
        void onDelete(GalleryFragment.Video video);
        void onSave(GalleryFragment.Video video);
    }

    
    public interface OnItemClickListener {
        void onItemClick(GalleryFragment.Video video);
    }

    public VideoAdapter(Context context, List<GalleryFragment.Video> videoList,
                        VideoMenuListener menuListener, OnItemClickListener itemClickListener) {
        this.context = context;
        this.videoList = videoList;
        this.menuListener = menuListener;
        this.itemClickListener = itemClickListener;
    }

    public void setVideoList(List<GalleryFragment.Video> videoList) {
        this.videoList = videoList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        GalleryFragment.Video video = videoList.get(position);
        holder.videoName.setText(video.getName());
        holder.videoDuration.setText(video.getDuration());
        holder.videoSize.setText(video.getSize());
        holder.videoDate.setText(video.getDate());

        
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(video.isSelected());

        
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            video.setSelected(isChecked);
        });

        
        Glide.with(context)
                .load(video.getThumbnailPath())
                .placeholder(R.drawable.ic_no_videos) 
                .into(holder.videoThumbnail);

        
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(video);
            }
        });

        
        holder.overflowMenu.setOnClickListener(v -> {
            showPopupMenu(v, video);
        });
    }

    private void showPopupMenu(View view, GalleryFragment.Video video) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.video_item_menu, popupMenu.getMenu());

        
        try {
            
            java.lang.reflect.Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    assert menuPopupHelper != null;
                    java.lang.reflect.Method setForceShowIcon = menuPopupHelper.getClass()
                            .getDeclaredMethod("setForceShowIcon", boolean.class);
                    setForceShowIcon.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        popupMenu.setOnMenuItemClickListener(item -> handleMenuItemClick(item, video));
        popupMenu.show();
    }


    private boolean handleMenuItemClick(MenuItem item, GalleryFragment.Video video) {
        int id = item.getItemId();
        if (id == R.id.action_open_with) {
            menuListener.onOpenWith(video);
            return true;
        } else if (id == R.id.action_save) {
            menuListener.onSave(video);
            return true;
        } else if (id == R.id.action_share) {
            menuListener.onShare(video);
            return true;
        } else if (id == R.id.action_details) {
            menuListener.onDetails(video);
            return true;
        } else if (id == R.id.action_rename) {
            menuListener.onRename(video);
            return true;
        } else if (id == R.id.action_delete) {
            menuListener.onDelete(video);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView videoThumbnail;
        TextView videoName, videoDuration, videoSize, videoDate;
        CheckBox checkBox;
        ImageButton overflowMenu;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoName = itemView.findViewById(R.id.video_name);
            videoDuration = itemView.findViewById(R.id.video_duration);
            videoSize = itemView.findViewById(R.id.video_size);
            videoDate = itemView.findViewById(R.id.video_date);
            checkBox = itemView.findViewById(R.id.check_box);
            overflowMenu = itemView.findViewById(R.id.overflow_menu);
        }
    }
}
