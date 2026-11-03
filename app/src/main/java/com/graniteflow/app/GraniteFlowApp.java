package com.graniteflow.app;

import android.app.Application;
import com.graniteflow.app.data.SessionManager;
import com.graniteflow.app.data.repository.GraniteRepository;

public class GraniteFlowApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize theme preference
        SessionManager.getInstance(this).applyTheme();
        // Initialize repository and seed data if needed
        GraniteRepository.getInstance(this);
    }
}
