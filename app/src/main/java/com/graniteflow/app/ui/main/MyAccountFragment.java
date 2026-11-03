package com.graniteflow.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.graniteflow.app.R;
import com.graniteflow.app.data.SessionManager;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.data.storage.LocalTransactionManager;
import com.graniteflow.app.model.User;
import com.graniteflow.app.ui.auth.AuthActivity;

public class MyAccountFragment extends Fragment {

    private EditText etCompanyName, etAddress1, etCity, etRbi, etIec, etGstin, etLut, etDefaultPortLoading;
    private EditText etBankName, etBankAddress, etBankAccNo, etBankSwift, etBackendUrl;
    private TextView tvAccountUserName, tvAccountUserRole, tvAccountUserEmail, tvUserInitials;
    private TextView tvAutoSaveBadge;
    private TextView btnThemeLight, btnThemeDark, btnThemeSystem;
    private GraniteRepository repository;
    private SessionManager sessionManager;

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    private boolean isPopulating = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_account, container, false);
        repository = GraniteRepository.getInstance(requireContext());
        sessionManager = SessionManager.getInstance(requireContext());

        tvAccountUserName = view.findViewById(R.id.tvAccountUserName);
        tvAccountUserRole = view.findViewById(R.id.tvAccountUserRole);
        tvAccountUserEmail = view.findViewById(R.id.tvAccountUserEmail);
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        tvAutoSaveBadge = view.findViewById(R.id.tvAutoSaveBadge);

        btnThemeLight = view.findViewById(R.id.btnThemeLight);
        btnThemeDark = view.findViewById(R.id.btnThemeDark);
        btnThemeSystem = view.findViewById(R.id.btnThemeSystem);

        etCompanyName = view.findViewById(R.id.etProfileCompanyName);
        etAddress1 = view.findViewById(R.id.etProfileAddress1);
        etCity = view.findViewById(R.id.etProfileCity);
        etRbi = view.findViewById(R.id.etProfileRbi);
        etIec = view.findViewById(R.id.etProfileIec);
        etGstin = view.findViewById(R.id.etProfileGstin);
        etLut = view.findViewById(R.id.etProfileLut);
        etDefaultPortLoading = view.findViewById(R.id.etProfileDefaultPortLoading);

        etBankName = view.findViewById(R.id.etBankName);
        etBankAddress = view.findViewById(R.id.etBankAddress);
        etBankAccNo = view.findViewById(R.id.etBankAccNo);
        etBankSwift = view.findViewById(R.id.etBankSwift);
        etBackendUrl = view.findViewById(R.id.etBackendUrl);

        Button btnSave = view.findViewById(R.id.btnSaveProfile);
        btnSave.setOnClickListener(v -> executeAutoSave(true));

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        View btnDataset = view.findViewById(R.id.btnOpenDatasetInventory);
        if (btnDataset != null) {
            btnDataset.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.graniteflow.app.ui.dataset.DatasetInventoryActivity.class));
            });
        }

        setupThemeSelector();
        populateCompanyData();
        populateUserData();
        setupAutoSaveWatchers();

        return view;
    }

    private void setupThemeSelector() {
        updateThemeButtonsState(sessionManager.getThemeMode());

        btnThemeLight.setOnClickListener(v -> {
            sessionManager.setThemeMode(SessionManager.THEME_LIGHT);
            requireActivity().recreate();
        });

        btnThemeDark.setOnClickListener(v -> {
            sessionManager.setThemeMode(SessionManager.THEME_DARK);
            requireActivity().recreate();
        });

        btnThemeSystem.setOnClickListener(v -> {
            sessionManager.setThemeMode(SessionManager.THEME_SYSTEM);
            requireActivity().recreate();
        });
    }

    private void updateThemeButtonsState(int currentMode) {
        btnThemeLight.setBackgroundResource(currentMode == SessionManager.THEME_LIGHT ? R.drawable.shape_tab_pill_active : R.drawable.shape_tab_pill_inactive);
        btnThemeLight.setTextColor(requireContext().getColor(currentMode == SessionManager.THEME_LIGHT ? R.color.text_on_pill : R.color.text_secondary));

        btnThemeDark.setBackgroundResource(currentMode == SessionManager.THEME_DARK ? R.drawable.shape_tab_pill_active : R.drawable.shape_tab_pill_inactive);
        btnThemeDark.setTextColor(requireContext().getColor(currentMode == SessionManager.THEME_DARK ? R.color.text_on_pill : R.color.text_secondary));

        btnThemeSystem.setBackgroundResource(currentMode == SessionManager.THEME_SYSTEM ? R.drawable.shape_tab_pill_active : R.drawable.shape_tab_pill_inactive);
        btnThemeSystem.setTextColor(requireContext().getColor(currentMode == SessionManager.THEME_SYSTEM ? R.color.text_on_pill : R.color.text_secondary));
    }

    private void populateUserData() {
        User user = sessionManager.getCurrentUser();
        CompanyProfile p = repository.getCompanyProfile();

        String effectiveCompany = (p != null && p.getCompanyName() != null && !p.getCompanyName().trim().isEmpty())
                ? p.getCompanyName().trim()
                : (user != null && user.getCompanyName() != null ? user.getCompanyName().trim() : "");

        if (user != null) {
            // Keep user session company name consistent with company profile
            if (!effectiveCompany.equalsIgnoreCase(user.getCompanyName())) {
                sessionManager.updateCurrentUserCompany(effectiveCompany);
                user.setCompanyName(effectiveCompany);
            }

            tvAccountUserName.setText(user.getName());
            String roleText = effectiveCompany.isEmpty() ? user.getRole() : effectiveCompany + " • " + user.getRole();
            tvAccountUserRole.setText(roleText);
            tvAccountUserEmail.setText(user.getEmail());
            if (user.getName() != null && user.getName().length() > 0) {
                String initials = user.getName().substring(0, 1).toUpperCase();
                String[] parts = user.getName().split(" ");
                if (parts.length > 1 && parts[1].length() > 0) {
                    initials += parts[1].substring(0, 1).toUpperCase();
                }
                tvUserInitials.setText(initials);
            } else {
                tvUserInitials.setText("");
            }
        } else {
            tvAccountUserName.setText("");
            tvAccountUserRole.setText("");
            tvAccountUserEmail.setText("");
            tvUserInitials.setText("");
        }
    }

    private void populateCompanyData() {
        isPopulating = true;
        try {
            CompanyProfile p = repository.getCompanyProfile();
            etCompanyName.setText(p.getCompanyName());
            etAddress1.setText(p.getAddressLine1());
            String cityPincode = p.getCityPincode() != null ? p.getCityPincode() : "";
            String country = p.getCountry() != null ? p.getCountry() : "";
            etCity.setText(cityPincode + (!country.isEmpty() ? ", " + country : ""));
            etRbi.setText(p.getRbiCode());
            etIec.setText(p.getIecCode());
            etGstin.setText(p.getGstin());
            if (etLut != null) etLut.setText(p.getLutNumber());
            if (etDefaultPortLoading != null) etDefaultPortLoading.setText(p.getDefaultPortOfLoading());

            etBankName.setText(p.getBankName());
            String branch = p.getBankBranch() != null ? p.getBankBranch() : "";
            String addr = p.getBankAddress() != null ? p.getBankAddress() : "";
            etBankAddress.setText(branch + (!branch.isEmpty() && !addr.isEmpty() ? "\n" : "") + addr);
            etBankAccNo.setText(p.getAccountNumber());
            etBankSwift.setText(p.getSwiftCode());

            etBackendUrl.setText(repository.getLocalData().getBackendUrl());
        } finally {
            isPopulating = false;
        }
    }

    private void setupAutoSaveWatchers() {
        // Real-time synchronization for Company Name
        etCompanyName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isPopulating) return;
                String currentText = s != null ? s.toString().trim() : "";
                if (!currentText.isEmpty()) {
                    User user = sessionManager.getCurrentUser();
                    String role = (user != null && user.getRole() != null) ? user.getRole() : "Exporter";
                    // Immediately reflect change in the profile header above
                    tvAccountUserRole.setText(currentText + " • " + role);
                    // Immediately reflect in MainActivity header
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).refreshHeader(currentText);
                    }
                }
                scheduleAutoSave();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Watchers for other fields
        TextWatcher genericWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isPopulating) return;
                scheduleAutoSave();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etAddress1.addTextChangedListener(genericWatcher);
        etCity.addTextChangedListener(genericWatcher);
        etRbi.addTextChangedListener(genericWatcher);
        etIec.addTextChangedListener(genericWatcher);
        etGstin.addTextChangedListener(genericWatcher);
        if (etLut != null) etLut.addTextChangedListener(genericWatcher);
        if (etDefaultPortLoading != null) etDefaultPortLoading.addTextChangedListener(genericWatcher);
        etBankName.addTextChangedListener(genericWatcher);
        etBankAddress.addTextChangedListener(genericWatcher);
        etBankAccNo.addTextChangedListener(genericWatcher);
        etBankSwift.addTextChangedListener(genericWatcher);
        etBackendUrl.addTextChangedListener(genericWatcher);
    }

    private void scheduleAutoSave() {
        if (tvAutoSaveBadge != null) {
            tvAutoSaveBadge.setText("Saving locally...");
        }
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveRunnable = () -> executeAutoSave(false);
        autoSaveHandler.postDelayed(autoSaveRunnable, 300);
    }

    private void executeAutoSave(boolean showToast) {
        if (!isAdded()) return;

        CompanyProfile p = repository.getCompanyProfile();
        String newName = etCompanyName.getText().toString().trim();
        if (!newName.isEmpty()) {
            p.setCompanyName(newName);
            p.setAccountHolder(newName);
        }
        p.setAddressLine1(etAddress1.getText().toString().trim());

        String cityCountry = etCity.getText().toString().trim();
        if (cityCountry.contains(",")) {
            int idx = cityCountry.lastIndexOf(",");
            p.setCityPincode(cityCountry.substring(0, idx).trim());
            p.setCountry(cityCountry.substring(idx + 1).trim());
        } else {
            p.setCityPincode(cityCountry);
        }

        p.setRbiCode(etRbi.getText().toString().trim());
        p.setIecCode(etIec.getText().toString().trim());
        p.setGstin(etGstin.getText().toString().trim());
        if (etLut != null) p.setLutNumber(etLut.getText().toString().trim());
        if (etDefaultPortLoading != null) p.setDefaultPortOfLoading(etDefaultPortLoading.getText().toString().trim());

        p.setBankName(etBankName.getText().toString().trim());

        String bankBranchAddr = etBankAddress.getText().toString().trim();
        if (bankBranchAddr.contains("\n")) {
            int idx = bankBranchAddr.indexOf("\n");
            p.setBankBranch(bankBranchAddr.substring(0, idx).trim());
            p.setBankAddress(bankBranchAddr.substring(idx + 1).trim());
        } else {
            p.setBankAddress(bankBranchAddr);
        }

        p.setAccountNumber(etBankAccNo.getText().toString().trim());
        p.setSwiftCode(etBankSwift.getText().toString().trim());

        // Update Backend URL if specified
        String url = etBackendUrl.getText().toString().trim();
        if (!url.isEmpty() && !url.equals(repository.getLocalData().getBackendUrl())) {
            repository.getLocalData().saveBackendUrl(url);
            repository.rebuildApiServices();
        }

        // Perform atomic ACID commit to local storage
        LocalTransactionManager.TransactionResult result = repository.saveCompanyProfileAtomic(p);

        if (result.success) {
            if (tvAutoSaveBadge != null) {
                tvAutoSaveBadge.setText("✓ Auto-saved locally");
            }
            if (showToast) {
                Toast.makeText(requireContext(), "Profile & preferences saved locally (ACID verified)!", Toast.LENGTH_SHORT).show();
            }
            User user = sessionManager.getCurrentUser();
            if (user != null) {
                String comp = user.getCompanyName();
                tvAccountUserRole.setText(comp + " • " + user.getRole());
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshHeader(comp);
                }
            }
        } else {
            if (tvAutoSaveBadge != null) {
                tvAutoSaveBadge.setText("⚠️ Save failed");
            }
            if (showToast) {
                Toast.makeText(requireContext(), "Save error: " + result.message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        executeAutoSave(false);
    }

    private void logout() {
        sessionManager.logout();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
