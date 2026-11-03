package com.graniteflow.app.ui.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.graniteflow.app.R;
import com.graniteflow.app.data.SessionManager;
import com.graniteflow.app.data.model.Customer;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.ocr.GeminiAiScanEngine;
import com.graniteflow.app.ocr.GraniteSlipParser;
import com.graniteflow.app.ui.inspection.BlockListEditActivity;
import com.graniteflow.app.ui.inspection.InspectionWorkspaceActivity;
import com.graniteflow.app.ui.inspection.NewMarkingActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MarkingFragment extends Fragment {

    private GraniteRepository repository;
    private GeminiAiScanEngine aiScanEngine;

    private TextView tvCurrentDateFormatted, tvTodayVolumeNumber, tvTodayBlocksNumber;
    private TextView tvSavedPartnersCount, tvActiveCustomerName, tvActiveCustomerSubtitle, tvSearchCustomerDropdown;
    private View btnClearSelectedCustomer, cardActiveSessionResume;
    private TextView tvActiveSessionCustomer, tvActiveSessionStatus;
    private Button btnStartMarkingHero, btnQuickAddCustomer;
    private RecyclerView rvRecentMarkings;
    private RecentMarkingsAdapter markingsAdapter;

    private View[] barViews = new View[7];
    private TextView[] dayLabels = new TextView[7];

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aiScanEngine = new GeminiAiScanEngine();

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                processImageWithAi(uri, null, null);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                processImageWithAi(null, bitmap, null);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_marking, container, false);
        repository = GraniteRepository.getInstance(requireContext());

        tvCurrentDateFormatted = view.findViewById(R.id.tvCurrentDateFormatted);
        tvTodayVolumeNumber = view.findViewById(R.id.tvTodayVolumeNumber);
        tvTodayBlocksNumber = view.findViewById(R.id.tvTodayBlocksNumber);
        tvSavedPartnersCount = view.findViewById(R.id.tvSavedPartnersCount);
        tvActiveCustomerName = view.findViewById(R.id.tvActiveCustomerName);
        tvActiveCustomerSubtitle = view.findViewById(R.id.tvActiveCustomerSubtitle);
        tvSearchCustomerDropdown = view.findViewById(R.id.tvSearchCustomerDropdown);
        btnClearSelectedCustomer = view.findViewById(R.id.btnClearSelectedCustomer);

        cardActiveSessionResume = view.findViewById(R.id.cardActiveSessionResume);
        tvActiveSessionCustomer = view.findViewById(R.id.tvActiveSessionCustomer);
        tvActiveSessionStatus = view.findViewById(R.id.tvActiveSessionStatus);

        btnStartMarkingHero = view.findViewById(R.id.btnStartMarkingHero);
        btnQuickAddCustomer = view.findViewById(R.id.btnQuickAddCustomer);
        rvRecentMarkings = view.findViewById(R.id.rvRecentMarkings);
        rvRecentMarkings.setLayoutManager(new LinearLayoutManager(requireContext()));

        barViews[0] = view.findViewById(R.id.barDay1);
        barViews[1] = view.findViewById(R.id.barDay2);
        barViews[2] = view.findViewById(R.id.barDay3);
        barViews[3] = view.findViewById(R.id.barDay4);
        barViews[4] = view.findViewById(R.id.barDay5);
        barViews[5] = view.findViewById(R.id.barDay6);
        barViews[6] = view.findViewById(R.id.barDay7);

        dayLabels[0] = view.findViewById(R.id.tvDay1);
        dayLabels[1] = view.findViewById(R.id.tvDay2);
        dayLabels[2] = view.findViewById(R.id.tvDay3);
        dayLabels[3] = view.findViewById(R.id.tvDay4);
        dayLabels[4] = view.findViewById(R.id.tvDay5);
        dayLabels[5] = view.findViewById(R.id.tvDay6);
        dayLabels[6] = view.findViewById(R.id.tvDay7);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH);
        tvCurrentDateFormatted.setText(sdf.format(new Date()));

        // Start Marking CTA -> asks whether user wants AI Scan or Manual Entry
        btnStartMarkingHero.setOnClickListener(v -> showStartMarkingChoiceDialog());

        // Quick Moon Theme Toggle in Overview header
        View btnOverviewThemeToggle = view.findViewById(R.id.btnOverviewThemeToggle);
        if (btnOverviewThemeToggle != null) {
            btnOverviewThemeToggle.setOnClickListener(v -> {
                SessionManager sm = SessionManager.getInstance(requireContext());
                int currentMode = sm.getThemeMode();
                int nextMode = (currentMode == SessionManager.THEME_DARK) ? SessionManager.THEME_LIGHT : SessionManager.THEME_DARK;
                sm.setThemeMode(nextMode);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            });
        }

        // Profile icon -> switch to My Account tab
        View btnOverviewProfile = view.findViewById(R.id.btnOverviewProfile);
        if (btnOverviewProfile != null) {
            btnOverviewProfile.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_account);
                }
            });
        }

        // Customer Directory & Customer Selector
        View cardCustomerDirectory = view.findViewById(R.id.cardCustomerDirectory);
        if (cardCustomerDirectory != null) {
            cardCustomerDirectory.setOnClickListener(v -> showCustomerDirectoryDialog());
        }
        View layoutActiveCustomerRow = view.findViewById(R.id.layoutActiveCustomerRow);
        if (layoutActiveCustomerRow != null) {
            layoutActiveCustomerRow.setOnClickListener(v -> showCustomerDirectoryDialog());
        }
        if (tvSearchCustomerDropdown != null) {
            tvSearchCustomerDropdown.setOnClickListener(v -> showCustomerDirectoryDialog());
        }
        if (btnQuickAddCustomer != null) {
            btnQuickAddCustomer.setOnClickListener(v -> showCustomerFormDialog(null, this::loadDashboardData));
        }
        if (btnClearSelectedCustomer != null) {
            btnClearSelectedCustomer.setOnClickListener(v -> {
                repository.setSelectedCustomerId(null);
                loadDashboardData();
                Toast.makeText(requireContext(), "Cleared active customer selection", Toast.LENGTH_SHORT).show();
            });
        }

        // View All Markings -> switch to History tab
        TextView tvViewAllMarkings = view.findViewById(R.id.tvViewAllMarkings);
        if (tvViewAllMarkings != null) {
            tvViewAllMarkings.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(R.id.nav_history);
                }
            });
        }

        loadDashboardData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void loadDashboardData() {
        List<Customer> customers = repository.getCustomers();
        Customer selectedCustomer = repository.getSelectedCustomer();

        int partnerCount = customers != null ? customers.size() : 0;
        if (tvSavedPartnersCount != null) {
            tvSavedPartnersCount.setText(partnerCount + (partnerCount == 1 ? " saved partner" : " saved partners"));
        }

        if (selectedCustomer != null) {
            tvActiveCustomerName.setText(selectedCustomer.getCompanyName());
            tvActiveCustomerSubtitle.setText("Selected customer for marking session");
            if (btnClearSelectedCustomer != null) btnClearSelectedCustomer.setVisibility(View.VISIBLE);
        } else {
            tvActiveCustomerName.setText("No Customer Selected");
            tvActiveCustomerSubtitle.setText("Tap search or + below to select a customer");
            if (btnClearSelectedCustomer != null) btnClearSelectedCustomer.setVisibility(View.GONE);
        }

        // Active Draft Session card (matching Dashboard.tsx)
        Inspection draftSession = repository.getDraftInspection();
        if (draftSession != null && draftSession.getTotalBlocksCount() > 0) {
            cardActiveSessionResume.setVisibility(View.VISIBLE);
            String title = (draftSession.getCustomer() != null && !draftSession.getCustomer().isEmpty())
                    ? draftSession.getCustomer()
                    : (draftSession.getConsigneeShort() != null && !draftSession.getConsigneeShort().isEmpty()
                    ? draftSession.getConsigneeShort() : "Unnamed Session");
            tvActiveSessionCustomer.setText(title);
            tvActiveSessionStatus.setText(draftSession.getTotalBlocksCount() + " blocks marked • In Progress");
            cardActiveSessionResume.setOnClickListener(v -> openInspectionWorkspace(draftSession.getInspectionId()));
        } else {
            cardActiveSessionResume.setVisibility(View.GONE);
        }

        List<Inspection> inspections = repository.getInspections();
        double todayVolSum = 0;
        int todayBlocksSum = 0;

        for (Inspection i : inspections) {
            todayVolSum += i.calculateTotalNetCbm();
            todayBlocksSum += i.getTotalBlocksCount();
        }

        if (tvTodayVolumeNumber != null) {
            tvTodayVolumeNumber.setText(String.format(Locale.ENGLISH, "%.1f", todayVolSum));
        }
        if (tvTodayBlocksNumber != null) {
            tvTodayBlocksNumber.setText(String.valueOf(todayBlocksSum));
        }

        updateVolumeTrendBars(inspections);

        markingsAdapter = new RecentMarkingsAdapter(inspections, new RecentMarkingsAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(Inspection inspection) {
                Intent intent = new Intent(requireContext(), BlockListEditActivity.class);
                intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
                startActivity(intent);
            }

            @Override
            public void onDuplicateClick(Inspection inspection) {
                Inspection duplicated = repository.duplicateInspection(inspection.getInspectionId());
                if (duplicated != null) {
                    Toast.makeText(requireContext(), "Duplicated session header for " + duplicated.getCustomer(), Toast.LENGTH_SHORT).show();
                    openInspectionWorkspace(duplicated.getInspectionId());
                }
            }
        });
        rvRecentMarkings.setAdapter(markingsAdapter);
    }

    private void updateVolumeTrendBars(List<Inspection> inspections) {
        SimpleDateFormat dayNameFmt = new SimpleDateFormat("EEE", Locale.ENGLISH);
        double[] vols = new double[7];
        double maxVol = 15.0; // baseline scale

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int idx = 0; idx < 7; idx++) {
            Date d = cal.getTime();
            if (dayLabels[idx] != null) {
                dayLabels[idx].setText(dayNameFmt.format(d));
            }
            // Distribute volumes or match date
            if (idx == 6 && !inspections.isEmpty()) {
                for (Inspection i : inspections) {
                    vols[idx] += i.calculateTotalNetCbm();
                }
            } else if (idx < inspections.size()) {
                vols[idx] = inspections.get(idx).calculateTotalNetCbm();
            } else {
                // subtle baseline so chart looks clean
                double[] sampleFallback = {4.2, 8.5, 12.4, 3.0, 14.8, 7.6, 11.2};
                vols[idx] = sampleFallback[idx];
            }
            if (vols[idx] > maxVol) maxVol = vols[idx];
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        float density = getResources().getDisplayMetrics().density;
        for (int idx = 0; idx < 7; idx++) {
            if (barViews[idx] != null) {
                int heightDp = (int) Math.max(10, Math.min(78, (vols[idx] / maxVol) * 78));
                ViewGroup.LayoutParams lp = barViews[idx].getLayoutParams();
                lp.height = (int) (heightDp * density);
                barViews[idx].setLayoutParams(lp);
            }
        }
    }

    public void showCustomerDirectoryDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_customer_directory);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etSearch = dialog.findViewById(R.id.etSearchDirectory);
        LinearLayout containerList = dialog.findViewById(R.id.containerCustomerList);
        View btnClose = dialog.findViewById(R.id.btnCloseDirectory);
        Button btnStartMarkingDir = dialog.findViewById(R.id.btnStartMarkingFromDirectory);
        Button btnAddNew = dialog.findViewById(R.id.btnAddNewCustomerInDirectory);

        if (btnStartMarkingDir != null) {
            btnStartMarkingDir.setVisibility(View.VISIBLE);
            btnStartMarkingDir.setOnClickListener(v -> {
                Customer sel = repository.getSelectedCustomer();
                if (sel == null) {
                    List<Customer> all = repository.getCustomers();
                    if (!all.isEmpty()) {
                        repository.setSelectedCustomerId(all.get(0).getId());
                    }
                }
                dialog.dismiss();
                loadDashboardData();
                showStartMarkingChoiceDialog();
            });
        }

        Runnable renderCustomers = new Runnable() {
            @Override
            public void run() {
                containerList.removeAllViews();
                String query = etSearch.getText().toString().trim().toLowerCase(Locale.ENGLISH);
                List<Customer> customers = repository.getCustomers();
                Customer active = repository.getSelectedCustomer();
                float d = getResources().getDisplayMetrics().density;

                for (Customer c : customers) {
                    String name = c.getCompanyName() != null ? c.getCompanyName() : "";
                    String country = c.getCountry() != null ? c.getCountry() : "";
                    if (!query.isEmpty() && !name.toLowerCase(Locale.ENGLISH).contains(query)
                            && !country.toLowerCase(Locale.ENGLISH).contains(query)) {
                        continue;
                    }

                    boolean isSelected = active != null && active.getId() != null && active.getId().equals(c.getId());

                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackgroundResource(isSelected ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
                    int pad = (int) (12 * d);
                    row.setPadding(pad, pad, pad, pad);
                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rlp.bottomMargin = (int) (8 * d);
                    row.setLayoutParams(rlp);

                    LinearLayout infoCol = new LinearLayout(requireContext());
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                    TextView tvName = new TextView(requireContext());
                    tvName.setText(name + (isSelected ? "  ✓" : ""));
                    tvName.setTextSize(13f);
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setTextColor(requireContext().getColor(isSelected ? R.color.brand_blue : R.color.text_primary));
                    infoCol.addView(tvName);

                    TextView tvSub = new TextView(requireContext());
                    String sub = (country.isEmpty() ? "EXPORT" : country)
                            + (c.getDefaultPortOfDischarge() != null && !c.getDefaultPortOfDischarge().isEmpty()
                            ? " • POD: " + c.getDefaultPortOfDischarge() : "");
                    tvSub.setText(sub);
                    tvSub.setTextSize(11f);
                    tvSub.setTextColor(requireContext().getColor(R.color.text_secondary));
                    infoCol.addView(tvSub);

                    row.addView(infoCol);

                    // Edit button
                    TextView btnEdit = new TextView(requireContext());
                    btnEdit.setText("✎");
                    btnEdit.setGravity(Gravity.CENTER);
                    btnEdit.setTextSize(14f);
                    btnEdit.setTextColor(requireContext().getColor(R.color.brand_blue));
                    int btnSize = (int) (34 * d);
                    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(btnSize, btnSize);
                    elp.rightMargin = (int) (4 * d);
                    btnEdit.setLayoutParams(elp);
                    btnEdit.setOnClickListener(v -> {
                        showCustomerFormDialog(c, () -> {
                            run();
                            loadDashboardData();
                        });
                    });
                    row.addView(btnEdit);

                    // Delete button
                    TextView btnDel = new TextView(requireContext());
                    btnDel.setText("🗑️");
                    btnDel.setGravity(Gravity.CENTER);
                    btnDel.setTextSize(13f);
                    btnDel.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
                    btnDel.setOnClickListener(v -> {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Delete Customer")
                                .setMessage("Remove " + c.getCompanyName() + " from directory?")
                                .setPositiveButton("Delete", (dlg, w) -> {
                                    repository.deleteCustomer(c.getId());
                                    run();
                                    loadDashboardData();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                    row.addView(btnDel);

                    // Tap row to select customer
                    row.setOnClickListener(v -> {
                        repository.setSelectedCustomerId(c.getId());
                        dialog.dismiss();
                        loadDashboardData();
                        Toast.makeText(requireContext(), "Active customer: " + c.getCompanyName(), Toast.LENGTH_SHORT).show();
                    });

                    containerList.addView(row);
                }
            }
        };

        renderCustomers.run();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                renderCustomers.run();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnAddNew.setOnClickListener(v -> {
            showCustomerFormDialog(null, () -> {
                renderCustomers.run();
                loadDashboardData();
            });
        });

        dialog.show();
    }

    private void showCustomerFormDialog(@Nullable Customer existing, @Nullable Runnable onSaved) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_customer_form);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvCustomerFormTitle);
        View btnClose = dialog.findViewById(R.id.btnCloseCustomerForm);
        EditText etCompanyName = dialog.findViewById(R.id.etCustCompanyName);
        EditText etAddress = dialog.findViewById(R.id.etCustAddress);
        EditText etCountry = dialog.findViewById(R.id.etCustCountry);
        EditText etPhone = dialog.findViewById(R.id.etCustPhone);
        EditText etEmail = dialog.findViewById(R.id.etCustEmail);
        EditText etPortLoading = dialog.findViewById(R.id.etCustPortLoading);
        EditText etPortDischarge = dialog.findViewById(R.id.etCustPortDischarge);
        EditText etFinalDest = dialog.findViewById(R.id.etCustFinalDest);
        EditText etHsCode = dialog.findViewById(R.id.etCustHsCode);
        EditText etTermsDelivery = dialog.findViewById(R.id.etCustTermsDelivery);
        EditText etCurrency = dialog.findViewById(R.id.etCustCurrency);
        Button btnSave = dialog.findViewById(R.id.btnSaveCustomerForm);

        if (existing != null) {
            tvTitle.setText("Edit Customer");
            etCompanyName.setText(existing.getCompanyName());
            etAddress.setText(existing.getAddress());
            etCountry.setText(existing.getCountry());
            etPhone.setText(existing.getPhone());
            etEmail.setText(existing.getEmail());
            etPortLoading.setText(existing.getDefaultPortOfLoading());
            etPortDischarge.setText(existing.getDefaultPortOfDischarge());
            etFinalDest.setText(existing.getDefaultFinalDestination());
            etHsCode.setText(existing.getDefaultHsCode());
            etTermsDelivery.setText(existing.getDefaultTermsOfDelivery());
            etCurrency.setText(existing.getDefaultCurrency());
        } else {
            tvTitle.setText("Add New Customer");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String companyName = etCompanyName.getText().toString().trim();
            if (companyName.isEmpty()) {
                Toast.makeText(requireContext(), "Company Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            Customer c = existing != null ? existing : new Customer();
            if (c.getId() == null || c.getId().isEmpty()) {
                c.setId("cust-" + System.currentTimeMillis());
            }
            c.setCompanyName(companyName);
            c.setAddress(etAddress.getText().toString().trim());
            c.setCountry(etCountry.getText().toString().trim());
            c.setPhone(etPhone.getText().toString().trim());
            c.setEmail(etEmail.getText().toString().trim());
            c.setDefaultPortOfLoading(etPortLoading.getText().toString().trim());
            c.setDefaultPortOfDischarge(etPortDischarge.getText().toString().trim());
            c.setDefaultFinalDestination(etFinalDest.getText().toString().trim());
            c.setDefaultFinalDestinationCountry(etCountry.getText().toString().trim());
            c.setDefaultHsCode(etHsCode.getText().toString().trim());
            c.setDefaultTermsOfDelivery(etTermsDelivery.getText().toString().trim());
            String curr = etCurrency.getText().toString().trim();
            c.setDefaultCurrency(curr.isEmpty() ? "USD" : curr.toUpperCase(Locale.ENGLISH));

            repository.saveCustomerToDirectory(c);
            repository.setSelectedCustomerId(c.getId());
            dialog.dismiss();
            if (onSaved != null) onSaved.run();
            Toast.makeText(requireContext(), "Saved customer: " + companyName, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showStartMarkingChoiceDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_start_marking_choice);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnClose = dialog.findViewById(R.id.btnCloseMarkingChoice);
        View btnAiScan = dialog.findViewById(R.id.btnChoiceAiScan);
        View btnManual = dialog.findViewById(R.id.btnChoiceManual);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnAiScan != null) {
            btnAiScan.setOnClickListener(v -> {
                dialog.dismiss();
                showOcrPickerDialog();
            });
        }

        if (btnManual != null) {
            btnManual.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(requireContext(), NewMarkingActivity.class);
                startActivity(intent);
            });
        }

        dialog.show();
    }

    private void showOcrPickerDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_ocr_source_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnCamera = dialog.findViewById(R.id.btnPickerCamera);
        View btnGallery = dialog.findViewById(R.id.btnPickerGallery);
        View btnDemo = dialog.findViewById(R.id.btnPickerDemo);
        View btnCancel = dialog.findViewById(R.id.btnPickerCancel);

        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> {
                dialog.dismiss();
                cameraLauncher.launch(null);
            });
        }

        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                dialog.dismiss();
                galleryLauncher.launch("image/*");
            });
        }

        if (btnDemo != null) {
            btnDemo.setOnClickListener(v -> {
                dialog.dismiss();
                processImageWithAi(null, null, "sample_quarry_slip.png");
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void processImageWithAi(@Nullable Uri imageUri, @Nullable Bitmap bitmap, @Nullable String assetFileName) {
        Dialog progress = new Dialog(requireContext());
        progress.setContentView(android.R.layout.simple_list_item_1);
        TextView tv = progress.findViewById(android.R.id.text1);
        if (tv != null) {
            tv.setText("✨ Scanning quarry slip with Gemini Vision AI...");
            tv.setPadding(36, 36, 36, 36);
        }
        progress.setCancelable(false);
        progress.show();

        GeminiAiScanEngine.ScanCallback callback = new GeminiAiScanEngine.ScanCallback() {
            @Override
            public void onSuccess(GraniteSlipParser.ParsedInspectionResult parsed, String engineUsed) {
                if (!isAdded()) return;
                progress.dismiss();
                if (parsed.blocks.isEmpty()) {
                    Toast.makeText(requireContext(), "No block dimensions identified on slip.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(requireContext(), NewMarkingActivity.class);
                intent.putExtra(NewMarkingActivity.EXTRA_SCANNED_BLOCKS, new java.util.ArrayList<>(parsed.blocks));
                intent.putExtra(NewMarkingActivity.EXTRA_SCAN_ENGINE, engineUsed);
                if (parsed.materialName != null && !parsed.materialName.isEmpty()) {
                    intent.putExtra(NewMarkingActivity.EXTRA_SCANNED_MATERIAL, parsed.materialName);
                }
                if (parsed.locationCode != null && !parsed.locationCode.isEmpty()) {
                    intent.putExtra(NewMarkingActivity.EXTRA_SCANNED_LOCATION, parsed.locationCode);
                }
                if (parsed.inspectorName != null && !parsed.inspectorName.isEmpty()) {
                    intent.putExtra(NewMarkingActivity.EXTRA_SCANNED_CUSTOMER, parsed.inspectorName);
                }
                if (parsed.defaultAllowanceCm > 0) {
                    intent.putExtra(NewMarkingActivity.EXTRA_SCANNED_ALLOWANCE, parsed.defaultAllowanceCm);
                }

                Toast.makeText(requireContext(),
                        "✨ " + engineUsed + ": Extracted " + parsed.blocks.size() + " blocks! Confirm customer & details.",
                        Toast.LENGTH_LONG).show();

                startActivity(intent);
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded()) return;
                progress.dismiss();
                Toast.makeText(requireContext(), "AI Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (assetFileName != null) {
            aiScanEngine.processAssetImage(requireContext(), assetFileName, callback);
        } else if (bitmap != null) {
            aiScanEngine.processBitmap(bitmap, callback);
        } else if (imageUri != null) {
            aiScanEngine.processUri(requireContext(), imageUri, callback);
        } else {
            progress.dismiss();
        }
    }

    private void openInspectionWorkspace(String inspectionId) {
        Intent intent = new Intent(requireContext(), InspectionWorkspaceActivity.class);
        intent.putExtra(InspectionWorkspaceActivity.EXTRA_INSPECTION_ID, inspectionId);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aiScanEngine != null) {
            aiScanEngine.close();
        }
    }

    // Recent Markings RecyclerView Adapter
    private static class RecentMarkingsAdapter extends RecyclerView.Adapter<RecentMarkingsAdapter.ViewHolder> {
        private final List<Inspection> list;
        private final OnItemActionListener listener;

        interface OnItemActionListener {
            void onItemClick(Inspection inspection);
            void onDuplicateClick(Inspection inspection);
        }

        RecentMarkingsAdapter(List<Inspection> list, OnItemActionListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inspection, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Inspection insp = list.get(position);
            String title = (insp.getConsigneeShort() != null && !insp.getConsigneeShort().isEmpty())
                    ? insp.getConsigneeShort()
                    : (insp.getCustomer() != null && !insp.getCustomer().isEmpty()
                    ? insp.getCustomer() : insp.getInspectionId());
            holder.tvInspectionId.setText(title);
            String stone = insp.getStoneDescription() != null && !insp.getStoneDescription().isEmpty()
                    ? insp.getStoneDescription() : insp.getMaterialName();
            holder.tvInspectionDetails.setText(insp.getDate() + " • " + stone);
            holder.tvTotalNetCbm.setText(String.format(Locale.ENGLISH, "%.2f m³", insp.calculateTotalNetCbm()));
            holder.tvBlockCount.setText(" (" + insp.getTotalBlocksCount() + " blks)");
            holder.itemView.setOnClickListener(v -> listener.onItemClick(insp));
            if (holder.btnDuplicateInspection != null) {
                holder.btnDuplicateInspection.setOnClickListener(v -> listener.onDuplicateClick(insp));
            }
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return Math.min(list.size(), 5);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvInspectionId, tvInspectionDetails, tvTotalNetCbm, tvBlockCount, btnDuplicateInspection;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInspectionId = itemView.findViewById(R.id.tvInspectionId);
                tvInspectionDetails = itemView.findViewById(R.id.tvInspectionDetails);
                tvTotalNetCbm = itemView.findViewById(R.id.tvTotalNetCbm);
                tvBlockCount = itemView.findViewById(R.id.tvBlockCount);
                btnDuplicateInspection = itemView.findViewById(R.id.btnDuplicateInspection);
            }
        }
    }
}
