package app.shashi.VigiLens.feature.gallery.ui;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class GalleryViewModel extends ViewModel {
    private final List<GalleryFragment.Video> videoList;

    public GalleryViewModel() {
        videoList = new ArrayList<>();
    }

    public List<GalleryFragment.Video> getVideoList() {
        return videoList;
    }

    public void addVideos(List<GalleryFragment.Video> newVideos) {
        videoList.addAll(newVideos);
    }

    public void clearVideos() {
        videoList.clear();
    }
}
