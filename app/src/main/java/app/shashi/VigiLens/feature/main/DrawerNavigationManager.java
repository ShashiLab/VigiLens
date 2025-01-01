package app.shashi.VigiLens.feature.main;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.about.AboutActivity;
import app.shashi.VigiLens.feature.settings.quickaccess.ShortcutsSettingsActivity;
import app.shashi.VigiLens.support.FeedbackActivity;
import app.shashi.VigiLens.support.ReportBugActivity;

public class DrawerNavigationManager {
    private static final long DRAWER_CLOSE_DELAY = 200;

    private final Context context;
    private final DrawerLayout drawerLayout;
    private final NavigationView drawerNavView;
    private final NavController navController;
    private final MaterialToolbar toolbar;
    private final Handler handler;

    public DrawerNavigationManager(Context context, DrawerLayout drawerLayout, NavigationView drawerNavView,
                                   NavController navController, MaterialToolbar toolbar) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.drawerNavView = drawerNavView;
        this.navController = navController;
        this.toolbar = toolbar;
        this.handler = new Handler(Looper.getMainLooper());

        setupDrawer();
        setupVersionInfo();
    }

    private void setupDrawer() {
        toolbar.setNavigationOnClickListener(v -> toggleDrawer());
        NavigationUI.setupWithNavController(drawerNavView, navController);

        drawerNavView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_shortcuts) {
                navigateWithDelay(() -> {
                    Intent intent = new Intent(context, ShortcutsSettingsActivity.class);
                    context.startActivity(intent);
                });
                return true;
            } else if (itemId == R.id.navigation_about) {
                navigateWithDelay(() -> {
                    Intent intent = new Intent(context, AboutActivity.class);
                    context.startActivity(intent);
                });
                return true;
            } else if (itemId == R.id.navigation_report_bug) {
                navigateWithDelay(() -> {
                    Intent intent = new Intent(context, ReportBugActivity.class);
                    context.startActivity(intent);
                });
                return true;
            } else if (itemId == R.id.navigation_feedback) {
                navigateWithDelay(() -> {
                    Intent intent = new Intent(context, FeedbackActivity.class);
                    context.startActivity(intent);
                });
                return true;
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return handled;
        });
    }

    private void navigateWithDelay(Runnable navigation) {
        drawerLayout.closeDrawer(GravityCompat.START);
        handler.postDelayed(navigation, DRAWER_CLOSE_DELAY);
    }

    private void setupVersionInfo() {
        View headerView = drawerNavView.getHeaderView(0);
        if (headerView != null) {
            TextView versionText = headerView.findViewById(R.id.nav_header_version);
            TextView buildTypeText = headerView.findViewById(R.id.nav_header_build_type);

            String versionName = getAppVersion();
            String buildType = getBuildType();

            versionText.setText(context.getString(R.string.label_version, versionName));
            if (buildType != null && !buildType.equals("release")) {
                buildTypeText.setVisibility(View.VISIBLE);
                buildTypeText.setText(buildType.toUpperCase());
            } else {
                buildTypeText.setVisibility(View.GONE);
            }
        }
    }

    private String getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return context.getString(R.string.label_version_unknown);
        }
    }

    public String getBuildType() {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            return ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) ? "debug" : "release";
        } catch (PackageManager.NameNotFoundException e) {
            return "release";
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public boolean handleBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        return false;
    }
}