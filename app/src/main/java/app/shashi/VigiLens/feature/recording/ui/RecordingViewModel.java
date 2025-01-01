package app.shashi.VigiLens.feature.recording.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecordingViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public RecordingViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Record fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}