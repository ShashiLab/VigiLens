package app.shashi.VigiLens.utils.feedback;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import app.shashi.VigiLens.R;

public class GitHubRatingManager {

    private final Context context;
    private static final String GITHUB_REPO_URL = "https://github.com/ShashiLab/VigiLens";

    public GitHubRatingManager(Context context) {
        this.context = context;
    }

    public void showStarDialog() {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.rate_app);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.setCancelable(false);

        Button positiveButton = dialog.findViewById(R.id.rate_dialog_positive_button);
        TextView negativeButton = dialog.findViewById(R.id.rate_dialog_negative_button);

        positiveButton.setOnClickListener(v -> {
            openGitHubRepo();
            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openGitHubRepo() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}