package com.graniteflow.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.graniteflow.app.model.User;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SessionManager {
    private static final String PREF_NAME = "graniteflow_session";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_COMPANY = "user_company";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_REGISTERED_USERS = "registered_users_v1";

    public static final int THEME_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int THEME_LIGHT = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int THEME_DARK = AppCompatDelegate.MODE_NIGHT_YES;

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public static class RegisteredAccount {
        public String id;
        public String name;
        public String email;
        public String companyName;
        public String role;
        public String passwordHash;

        public RegisteredAccount() {}

        public RegisteredAccount(String id, String name, String email, String companyName, String role, String passwordHash) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.companyName = companyName;
            this.role = role;
            this.passwordHash = passwordHash;
        }
    }

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public List<RegisteredAccount> getRegisteredAccounts() {
        String json = prefs.getString(KEY_REGISTERED_USERS, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<RegisteredAccount>>() {}.getType();
            List<RegisteredAccount> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static class SessionSnapshot {
        public final boolean isLoggedIn;
        public final User user;
        public final String registeredAccountsJson;

        public SessionSnapshot(boolean isLoggedIn, User user, String registeredAccountsJson) {
            this.isLoggedIn = isLoggedIn;
            this.user = user;
            this.registeredAccountsJson = registeredAccountsJson;
        }
    }

    public synchronized SessionSnapshot createSnapshot() {
        return new SessionSnapshot(
                isLoggedIn(),
                getCurrentUser(),
                prefs.getString(KEY_REGISTERED_USERS, null)
        );
    }

    public synchronized boolean restoreSnapshot(SessionSnapshot snapshot) {
        if (snapshot == null) return false;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, snapshot.isLoggedIn);
        if (snapshot.user != null) {
            editor.putString(KEY_USER_ID, snapshot.user.getId());
            editor.putString(KEY_USER_NAME, snapshot.user.getName());
            editor.putString(KEY_USER_EMAIL, snapshot.user.getEmail());
            editor.putString(KEY_USER_COMPANY, snapshot.user.getCompanyName());
            editor.putString(KEY_USER_ROLE, snapshot.user.getRole());
        } else {
            editor.remove(KEY_USER_ID);
            editor.remove(KEY_USER_NAME);
            editor.remove(KEY_USER_EMAIL);
            editor.remove(KEY_USER_COMPANY);
            editor.remove(KEY_USER_ROLE);
        }
        if (snapshot.registeredAccountsJson != null) {
            editor.putString(KEY_REGISTERED_USERS, snapshot.registeredAccountsJson);
        } else {
            editor.remove(KEY_REGISTERED_USERS);
        }
        return editor.commit();
    }

    public synchronized boolean updateCurrentUserCompany(String newCompanyName) {
        if (newCompanyName == null || newCompanyName.trim().isEmpty()) {
            return false;
        }
        String trimmed = newCompanyName.trim();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_COMPANY, trimmed);

        // Keep registered accounts in lockstep for Consistency
        List<RegisteredAccount> accounts = getRegisteredAccounts();
        User currentUser = getCurrentUser();
        if (currentUser != null && !accounts.isEmpty()) {
            for (RegisteredAccount acc : accounts) {
                if ((currentUser.getId() != null && currentUser.getId().equals(acc.id)) ||
                    (currentUser.getEmail() != null && currentUser.getEmail().equalsIgnoreCase(acc.email))) {
                    acc.companyName = trimmed;
                    break;
                }
            }
            editor.putString(KEY_REGISTERED_USERS, gson.toJson(accounts));
        }

        return editor.commit();
    }

    private synchronized boolean saveRegisteredAccounts(List<RegisteredAccount> accounts) {
        return prefs.edit().putString(KEY_REGISTERED_USERS, gson.toJson(accounts)).commit();
    }

    public boolean isEmailRegistered(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase(Locale.ENGLISH);
        for (RegisteredAccount acc : getRegisteredAccounts()) {
            if (acc.email != null && acc.email.trim().toLowerCase(Locale.ENGLISH).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public User registerAccount(String name, String email, String companyName, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ENGLISH);
        List<RegisteredAccount> accounts = getRegisteredAccounts();
        for (RegisteredAccount acc : accounts) {
            if (acc.email != null && acc.email.trim().toLowerCase(Locale.ENGLISH).equals(normalizedEmail)) {
                return null; // Already exists
            }
        }

        String id = "usr_" + System.currentTimeMillis();
        String role = "Exporter";
        String hash = hashPassword(normalizedEmail, rawPassword);

        RegisteredAccount newAcc = new RegisteredAccount(
                id,
                name.trim(),
                normalizedEmail,
                companyName != null && !companyName.trim().isEmpty() ? companyName.trim() : name.trim(),
                role,
                hash
        );
        accounts.add(newAcc);
        saveRegisteredAccounts(accounts);

        User user = new User(newAcc.id, newAcc.name, newAcc.email, newAcc.companyName, newAcc.role);
        saveUser(user);
        return user;
    }

    public User authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) return null;
        String normalizedEmail = email.trim().toLowerCase(Locale.ENGLISH);
        String hash = hashPassword(normalizedEmail, rawPassword);

        List<RegisteredAccount> accounts = getRegisteredAccounts();
        for (RegisteredAccount acc : accounts) {
            if (acc.email != null && acc.email.trim().toLowerCase(Locale.ENGLISH).equals(normalizedEmail)) {
                if (hash.equals(acc.passwordHash)) {
                    User user = new User(acc.id, acc.name, acc.email, acc.companyName, acc.role);
                    saveUser(user);
                    return user;
                }
                return null; // Wrong password
            }
        }
        return null; // User not found
    }

    private String hashPassword(String email, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = "graniteflow_salt_v1:" + email.toLowerCase(Locale.ENGLISH) + ":" + password;
            byte[] encoded = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encoded.length);
            for (byte b : encoded) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf((email + ":" + password).hashCode());
        }
    }

    public synchronized boolean saveUser(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, user.getId());
        editor.putString(KEY_USER_NAME, user.getName());
        editor.putString(KEY_USER_EMAIL, user.getEmail());
        editor.putString(KEY_USER_COMPANY, user.getCompanyName());
        editor.putString(KEY_USER_ROLE, user.getRole());

        // Keep registered accounts in lockstep for Consistency
        List<RegisteredAccount> accounts = getRegisteredAccounts();
        for (RegisteredAccount acc : accounts) {
            if ((user.getId() != null && user.getId().equals(acc.id)) ||
                (user.getEmail() != null && user.getEmail().equalsIgnoreCase(acc.email))) {
                acc.name = user.getName();
                acc.companyName = user.getCompanyName();
                break;
            }
        }
        editor.putString(KEY_REGISTERED_USERS, gson.toJson(accounts));
        return editor.commit();
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;
        User user = new User();
        user.setId(prefs.getString(KEY_USER_ID, ""));
        user.setName(prefs.getString(KEY_USER_NAME, ""));
        user.setEmail(prefs.getString(KEY_USER_EMAIL, ""));
        user.setCompanyName(prefs.getString(KEY_USER_COMPANY, ""));
        user.setRole(prefs.getString(KEY_USER_ROLE, ""));
        return user;
    }

    public synchronized boolean clearAllLoginsAndSessions() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_COMPANY);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_REGISTERED_USERS);
        return editor.commit();
    }

    public synchronized boolean logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_USER_COMPANY);
        editor.remove(KEY_USER_ROLE);
        return editor.commit();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_LIGHT);
    }

    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).commit();
        applyTheme();
    }

    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode());
    }
}
