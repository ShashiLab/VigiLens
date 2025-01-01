package app.shashi.VigiLens.feature.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import app.shashi.VigiLens.R;
import app.shashi.VigiLens.core.notification.UrgentAnnouncementManager;
import app.shashi.VigiLens.utils.locale.LocaleManager;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;
    private DrawerNavigationManager drawerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        
        new UrgentAnnouncementManager(this);

        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView drawerNavView = findViewById(R.id.nav_view_drawer);

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_record, R.id.navigation_videos, R.id.navigation_settings)
                .setOpenableLayout(drawerLayout)  
                .build();

        
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        
        drawerManager = new DrawerNavigationManager(this, drawerLayout, drawerNavView, navController, toolbar);

        
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                
                if (!drawerManager.handleBackPressed()) {
                    
                    finish();  
                }
            }
        };

        
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public boolean onSupportNavigateUp() {
        
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController, drawerLayout)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void attachBaseContext(Context base) {
        
        sharedPreferences = base.getSharedPreferences("user_prefs", MODE_PRIVATE);
        super.attachBaseContext(LocaleManager.updateBaseContextLocale(base));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.apply();
    }
}
