package com.graniteflow.app.ui.main;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.graniteflow.app.R;
import com.graniteflow.app.data.SessionManager;
import com.graniteflow.app.model.User;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;
    private TextView tvHeaderSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sessionManager = SessionManager.getInstance(this);
        sessionManager.applyTheme();

        super.onCreate(savedInstanceState);

        if (!sessionManager.isLoggedIn() || sessionManager.getRegisteredAccounts().isEmpty()) {
            startActivity(new android.content.Intent(this, com.graniteflow.app.ui.auth.AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);

        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            tvHeaderSubtitle.setText(currentUser.getCompanyName() + " • " + currentUser.getName());
        }

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new MarkingFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_marking) {
                loadFragment(new MarkingFragment());
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(new HistoryFragment());
                return true;
            } else if (itemId == R.id.nav_account) {
                loadFragment(new MyAccountFragment());
                return true;
            }
            return false;
        });
    }

    public void updateHeaderSubtitle(String subtitle) {
        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(subtitle);
        }
    }

    public void refreshHeader() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null && tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(currentUser.getCompanyName() + " • " + currentUser.getName());
        }
    }

    public void refreshHeader(String companyName) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null && tvHeaderSubtitle != null) {
            String userName = currentUser.getName() != null ? currentUser.getName() : "";
            tvHeaderSubtitle.setText(companyName + " • " + userName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHeader();
    }

    public void switchToTab(int navItemId) {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(navItemId);
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
