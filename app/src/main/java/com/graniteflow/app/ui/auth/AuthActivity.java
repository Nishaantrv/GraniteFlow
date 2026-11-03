package com.graniteflow.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.graniteflow.app.R;
import com.graniteflow.app.data.SessionManager;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.databinding.ActivityAuthBinding;
import com.graniteflow.app.model.User;
import com.graniteflow.app.ui.main.MainActivity;

public class AuthActivity extends AppCompatActivity {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 60_000L; // 1 minute lockout

    private ActivityAuthBinding binding;
    private SessionManager sessionManager;
    private boolean isSignUpMode = false;
    private boolean isPasswordVisible = false;

    private int failedAttempts = 0;
    private long lockoutUntil = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        sessionManager = SessionManager.getInstance(this);
        sessionManager.applyTheme();

        super.onCreate(savedInstanceState);

        // Purge any legacy mock accounts or sessions
        List<SessionManager.RegisteredAccount> accounts = new java.util.ArrayList<>(sessionManager.getRegisteredAccounts());
        boolean removedMock = accounts.removeIf(a -> a.email != null && 
                (a.email.toLowerCase().contains("dakshinexports") || 
                 a.email.toLowerCase().contains("example.com") || 
                 a.name.toLowerCase().contains("rajesh")));
        if (removedMock) {
            sessionManager.clearAllLoginsAndSessions();
        }

        // If already logged in with a valid account, navigate directly to MainActivity
        if (sessionManager.isLoggedIn() && !sessionManager.getRegisteredAccounts().isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        } else if (sessionManager.isLoggedIn()) {
            sessionManager.logout();
        }

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ensure all login inputs are completely blank
        clearFormInputs();

        setupListeners();
        // Default to Sign In mode
        setMode(false);
    }

    private void setupListeners() {
        binding.tabSignIn.setOnClickListener(v -> setMode(false));
        binding.tabSignUp.setOnClickListener(v -> setMode(true));
        binding.btnSwitchModeLink.setOnClickListener(v -> setMode(!isSignUpMode));

        binding.btnSubmitAuth.setOnClickListener(v -> handleSubmit());

        binding.btnTogglePasswordVisibility.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            int selection = binding.etPassword.getSelectionEnd();
            if (isPasswordVisible) {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                binding.btnTogglePasswordVisibility.setText("HIDE");
            } else {
                binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                binding.btnTogglePasswordVisibility.setText("SHOW");
            }
            if (selection >= 0) {
                binding.etPassword.setSelection(selection);
            }
        });

        binding.btnToggleTheme.setOnClickListener(v -> {
            int currentMode = sessionManager.getThemeMode();
            int nextMode = (currentMode == SessionManager.THEME_DARK) ? SessionManager.THEME_LIGHT : SessionManager.THEME_DARK;
            sessionManager.setThemeMode(nextMode);
            recreate();
        });
    }

    private void clearFormInputs() {
        if (binding == null) return;
        binding.etEmail.setText("");
        binding.etPassword.setText("");
        binding.etName.setText("");
        binding.etCompany.setText("");
        binding.etConfirmPassword.setText("");
        binding.tvErrorMessage.setVisibility(View.GONE);
    }

    private void setMode(boolean signUp) {
        isSignUpMode = signUp;
        clearFormInputs();

        if (signUp) {
            binding.tvAppSubtitle.setText("Create an account to get started");

            binding.tabSignIn.setBackgroundResource(R.drawable.shape_tab_pill_inactive);
            binding.tabSignIn.setTextColor(getColor(R.color.text_secondary));

            binding.tabSignUp.setBackgroundResource(R.drawable.shape_tab_pill_active);
            binding.tabSignUp.setTextColor(getColor(R.color.text_on_pill));

            binding.layoutName.setVisibility(View.VISIBLE);
            binding.layoutCompany.setVisibility(View.VISIBLE);
            binding.layoutConfirmPassword.setVisibility(View.VISIBLE);
            binding.tvPasswordHint.setVisibility(View.VISIBLE);

            binding.btnSubmitAuth.setText("Sign Up");
            binding.tvSwitchModePrompt.setText("Already have an account? ");
            binding.btnSwitchModeLink.setText("Sign in");
        } else {
            binding.tvAppSubtitle.setText("Sign in to continue");

            binding.tabSignIn.setBackgroundResource(R.drawable.shape_tab_pill_active);
            binding.tabSignIn.setTextColor(getColor(R.color.text_on_pill));

            binding.tabSignUp.setBackgroundResource(R.drawable.shape_tab_pill_inactive);
            binding.tabSignUp.setTextColor(getColor(R.color.text_secondary));

            binding.layoutName.setVisibility(View.GONE);
            binding.layoutCompany.setVisibility(View.GONE);
            binding.layoutConfirmPassword.setVisibility(View.GONE);
            binding.tvPasswordHint.setVisibility(View.GONE);

            binding.btnSubmitAuth.setText("Sign In");
            binding.tvSwitchModePrompt.setText("Don't have an account? ");
            binding.btnSwitchModeLink.setText("Sign up");
        }
    }

    private void handleSubmit() {
        binding.tvErrorMessage.setVisibility(View.GONE);

        if (System.currentTimeMillis() < lockoutUntil) {
            long secLeft = Math.max(1, (lockoutUntil - System.currentTimeMillis()) / 1000);
            showError("Too many failed attempts. Please wait " + secLeft + "s before trying again.");
            return;
        }

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password.");
            return;
        }

        if (isSignUpMode) {
            String name = binding.etName.getText().toString().trim();
            String company = binding.etCompany.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString();

            if (name.length() < 2) {
                showError("Please enter your display name (minimum 2 characters).");
                return;
            }

            if (password.length() < 6) {
                showError("Password must be at least 6 characters.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match.");
                return;
            }

            if (sessionManager.isEmailRegistered(email)) {
                showError("An account with this email already exists. Try signing in instead.");
                return;
            }

            User registered = sessionManager.registerAccount(name, email, company, password);
            if (registered == null) {
                showError("Could not create account. Please try again.");
                return;
            }

            // Sync company name to CompanyProfile if provided
            if (!company.isEmpty()) {
                GraniteRepository repo = GraniteRepository.getInstance(this);
                CompanyProfile profile = repo.getCompanyProfile();
                if (profile != null) {
                    profile.setCompanyName(company);
                    repo.saveCompanyProfile(profile);
                }
            }

            failedAttempts = 0;
            Toast.makeText(this, "Account created! Welcome, " + registered.getName(), Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            User authenticated = sessionManager.authenticate(email, password);
            if (authenticated == null) {
                failedAttempts++;
                if (failedAttempts >= MAX_ATTEMPTS) {
                    lockoutUntil = System.currentTimeMillis() + LOCKOUT_MS;
                    failedAttempts = 0;
                    showError("Too many failed attempts. Locked for 60 seconds.");
                } else {
                    if (!sessionManager.isEmailRegistered(email)) {
                        showError("No account found for this email. Tap 'Sign Up' to create one.");
                    } else {
                        showError("Invalid email or password. Please try again.");
                    }
                }
                return;
            }

            failedAttempts = 0;
            Toast.makeText(this, "Welcome back, " + authenticated.getName(), Toast.LENGTH_SHORT).show();
            navigateToMain();
        }
    }

    private void showError(String message) {
        binding.tvErrorMessage.setText(message);
        binding.tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
