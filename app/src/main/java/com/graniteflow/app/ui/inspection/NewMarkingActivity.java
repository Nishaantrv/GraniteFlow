package com.graniteflow.app.ui.inspection;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.graniteflow.app.R;
import com.graniteflow.app.data.model.BlockTypePreset;
import com.graniteflow.app.data.model.CompanyProfile;
import com.graniteflow.app.data.model.Customer;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.ocr.GeminiAiScanEngine;
import com.graniteflow.app.ocr.GraniteSlipParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewMarkingActivity extends AppCompatActivity {

    public static final String EXTRA_SCANNED_BLOCKS = "extra_scanned_blocks";
    public static final String EXTRA_SCANNED_MATERIAL = "extra_scanned_material";
    public static final String EXTRA_SCANNED_CUSTOMER = "extra_scanned_customer";
    public static final String EXTRA_SCANNED_LOCATION = "extra_scanned_location";
    public static final String EXTRA_SCANNED_ALLOWANCE = "extra_scanned_allowance";
    public static final String EXTRA_SCAN_ENGINE = "extra_scan_engine";

    private GraniteRepository repository;
    private GeminiAiScanEngine aiScanEngine;
    private List<com.graniteflow.app.data.model.Block> pendingScannedBlocks = new ArrayList<>();
    private String scannedEngineName = null;

    private View cardSelectedCustomerSummary, btnChangeCustomerInNewMarking;
    private TextView tvSelectedCustomerName, tvSelectedCustomerAddress, tvSelectedCustomerMeta;

    private EditText etNmConsignee, etNmAddress, etNmPhone, etNmBuyer, etNmNotifyParty;
    private EditText etNmPreCarriage, etNmPlaceReceipt, etNmPortLoading, etNmVessel;
    private EditText etNmPortDischarge, etNmFinalDest, etNmFinalDestCountry, etNmCountryOrigin;
    private EditText etNmStoneType, etNmMarks, etNmCurrency, etNmPricePerCbm, etNmPaymentTerms, etNmHsCode;
    private EditText etNmAllowance, etNmStartBlockNo;

    private LinearLayout cardCalcModeNet, cardCalcModeGross, cardToggleEndToEnd;
    private TextView tvCalcModeNetTitle, tvCalcModeGrossTitle, tvEndToEndTitle, tvEndToEndStatusBadge;

    private String selectedCalculationMode = "net";
    private boolean enableEndToEnd = false;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_marking);

        repository = GraniteRepository.getInstance(this);
        aiScanEngine = new GeminiAiScanEngine();

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processOcrSlip(uri, null, null);
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) processOcrSlip(null, bitmap, null);
        });

        initViews();
        populateDefaults();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnNewMarkingBack);
        btnBack.setOnClickListener(v -> finish());

        View btnAiScan = findViewById(R.id.btnNewMarkingAiScan);
        btnAiScan.setOnClickListener(v -> showOcrPickerDialog());

        cardSelectedCustomerSummary = findViewById(R.id.cardSelectedCustomerSummary);
        tvSelectedCustomerName = findViewById(R.id.tvSelectedCustomerName);
        tvSelectedCustomerAddress = findViewById(R.id.tvSelectedCustomerAddress);
        tvSelectedCustomerMeta = findViewById(R.id.tvSelectedCustomerMeta);
        btnChangeCustomerInNewMarking = findViewById(R.id.btnChangeCustomerInNewMarking);

        if (cardSelectedCustomerSummary != null) {
            cardSelectedCustomerSummary.setOnClickListener(v -> showCustomerDirectoryPicker());
        }
        if (btnChangeCustomerInNewMarking != null) {
            btnChangeCustomerInNewMarking.setOnClickListener(v -> showCustomerDirectoryPicker());
        }

        TextView btnToggleAdv = findViewById(R.id.btnToggleAdvancedLogistics);
        View llAdv = findViewById(R.id.llAdvancedLogisticsContainer);
        if (btnToggleAdv != null && llAdv != null) {
            btnToggleAdv.setOnClickListener(v -> {
                boolean isVisible = llAdv.getVisibility() == View.VISIBLE;
                llAdv.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                btnToggleAdv.setText(isVisible
                        ? "▸ Shipping, Ports & Customs Details (Auto-filled)"
                        : "▾ Hide Shipping, Ports & Customs Details");
            });
        }

        etNmConsignee = findViewById(R.id.etNmConsignee);
        etNmAddress = findViewById(R.id.etNmAddress);
        etNmPhone = findViewById(R.id.etNmPhone);
        etNmBuyer = findViewById(R.id.etNmBuyer);
        etNmNotifyParty = findViewById(R.id.etNmNotifyParty);

        etNmPreCarriage = findViewById(R.id.etNmPreCarriage);
        etNmPlaceReceipt = findViewById(R.id.etNmPlaceReceipt);
        etNmPortLoading = findViewById(R.id.etNmPortLoading);
        etNmVessel = findViewById(R.id.etNmVessel);
        etNmPortDischarge = findViewById(R.id.etNmPortDischarge);
        etNmFinalDest = findViewById(R.id.etNmFinalDest);
        etNmFinalDestCountry = findViewById(R.id.etNmFinalDestCountry);
        etNmCountryOrigin = findViewById(R.id.etNmCountryOrigin);

        etNmStoneType = findViewById(R.id.etNmStoneType);
        etNmMarks = findViewById(R.id.etNmMarks);
        etNmCurrency = findViewById(R.id.etNmCurrency);
        etNmPricePerCbm = findViewById(R.id.etNmPricePerCbm);
        etNmPaymentTerms = findViewById(R.id.etNmPaymentTerms);
        etNmHsCode = findViewById(R.id.etNmHsCode);

        etNmAllowance = findViewById(R.id.etNmAllowance);
        etNmStartBlockNo = findViewById(R.id.etNmStartBlockNo);

        cardCalcModeNet = findViewById(R.id.cardCalcModeNet);
        cardCalcModeGross = findViewById(R.id.cardCalcModeGross);
        tvCalcModeNetTitle = findViewById(R.id.tvCalcModeNetTitle);
        tvCalcModeGrossTitle = findViewById(R.id.tvCalcModeGrossTitle);

        cardToggleEndToEnd = findViewById(R.id.cardToggleEndToEnd);
        tvEndToEndTitle = findViewById(R.id.tvEndToEndTitle);
        tvEndToEndStatusBadge = findViewById(R.id.tvEndToEndStatusBadge);

        cardCalcModeNet.setOnClickListener(v -> {
            selectedCalculationMode = "net";
            updateCalculationModeUi();
        });

        cardCalcModeGross.setOnClickListener(v -> {
            selectedCalculationMode = "gross";
            updateCalculationModeUi();
        });

        cardToggleEndToEnd.setOnClickListener(v -> {
            enableEndToEnd = !enableEndToEnd;
            updateEndToEndUi();
        });

        Button btnSubmit = findViewById(R.id.btnStartMarkingSession);

        if (getIntent().hasExtra(EXTRA_SCAN_ENGINE)) {
            scannedEngineName = getIntent().getStringExtra(EXTRA_SCAN_ENGINE);
        }

        if (getIntent().hasExtra(EXTRA_SCANNED_BLOCKS)) {
            try {
                @SuppressWarnings("unchecked")
                List<com.graniteflow.app.data.model.Block> blocks =
                        (List<com.graniteflow.app.data.model.Block>) getIntent().getSerializableExtra(EXTRA_SCANNED_BLOCKS);
                if (blocks != null && !blocks.isEmpty()) {
                    pendingScannedBlocks = blocks;
                    btnSubmit.setText("START MARKING (" + blocks.size() + " SCANNED BLOCKS) ›");
                }
            } catch (Exception ignored) {}
        }

        btnSubmit.setOnClickListener(v -> startMarkingSession(pendingScannedBlocks));
    }

    private void populateDefaults() {
        CompanyProfile cp = repository.getCompanyProfile();
        Customer cust = repository.getSelectedCustomer();
        if (cust == null) {
            cust = repository.getCustomer();
            if (cust != null) {
                repository.setSelectedCustomerId(cust.getId());
            }
        }

        if (cp != null) {
            etNmPortLoading.setText(cp.getDefaultPortOfLoading());
            etNmCurrency.setText(cp.getDefaultCurrency());
            etNmCountryOrigin.setText(cp.getCountry());
        }

        // Apply extras from AI Scan if present
        String scannedMat = getIntent().getStringExtra(EXTRA_SCANNED_MATERIAL);
        if (scannedMat != null && !scannedMat.isEmpty()) {
            etNmStoneType.setText(scannedMat);
        }

        String scannedLoc = getIntent().getStringExtra(EXTRA_SCANNED_LOCATION);
        if (scannedLoc != null && !scannedLoc.isEmpty()) {
            etNmMarks.setText(scannedLoc);
        }

        double scannedAllw = getIntent().getDoubleExtra(EXTRA_SCANNED_ALLOWANCE, -1.0);
        if (scannedAllw > 0) {
            etNmAllowance.setText(String.valueOf((int) scannedAllw));
        }

        String scannedCust = getIntent().getStringExtra(EXTRA_SCANNED_CUSTOMER);
        if (cust == null && scannedCust != null && !scannedCust.isEmpty()) {
            etNmConsignee.setText(scannedCust);
            if (tvSelectedCustomerName != null) {
                tvSelectedCustomerName.setText(scannedCust);
            }
            if (tvSelectedCustomerAddress != null) {
                tvSelectedCustomerAddress.setText("Scanned from quarry slip. Tap Change to link directory.");
            }
        }

        if (cust != null) {
            etNmConsignee.setText(cust.getCompanyName());
            String fullAddr = cust.getAddress();
            if (!cust.getCityProvince().isEmpty()
                    && !fullAddr.toLowerCase(Locale.ENGLISH).contains(cust.getCityProvince().toLowerCase(Locale.ENGLISH))) {
                fullAddr = fullAddr.isEmpty() ? cust.getCityProvince() : (fullAddr + ", " + cust.getCityProvince());
            }
            etNmAddress.setText(fullAddr);
            etNmPhone.setText(cust.getPhone());
            etNmBuyer.setText(cust.getCompanyName());
            etNmNotifyParty.setText(!cust.getContactPerson().isEmpty() ? cust.getContactPerson() : cust.getCompanyName());

            if (!cust.getDefaultPortOfLoading().isEmpty()) {
                etNmPortLoading.setText(cust.getDefaultPortOfLoading());
            }
            etNmPortDischarge.setText(cust.getDefaultPortOfDischarge());
            etNmFinalDest.setText(cust.getDefaultFinalDestination());
            String destCountry = !cust.getDefaultFinalDestinationCountry().isEmpty()
                    ? cust.getDefaultFinalDestinationCountry()
                    : cust.getCountry();
            etNmFinalDestCountry.setText(destCountry);

            if (!cust.getDefaultCurrency().isEmpty()) {
                etNmCurrency.setText(cust.getDefaultCurrency());
            }
            if (!cust.getDefaultTermsOfPayment().isEmpty()) {
                etNmPaymentTerms.setText(cust.getDefaultTermsOfPayment());
            }
            if (!cust.getDefaultHsCode().isEmpty()) {
                etNmHsCode.setText(cust.getDefaultHsCode());
            }

            if (tvSelectedCustomerName != null) {
                tvSelectedCustomerName.setText(cust.getCompanyName());
            }
            if (tvSelectedCustomerAddress != null) {
                tvSelectedCustomerAddress.setVisibility(View.GONE);
            }
            if (tvSelectedCustomerMeta != null) {
                tvSelectedCustomerMeta.setVisibility(View.GONE);
            }
        }
    }

    private void showCustomerDirectoryPicker() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_customer_directory);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.94);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etSearch = dialog.findViewById(R.id.etSearchDirectory);
        LinearLayout containerList = dialog.findViewById(R.id.containerCustomerList);
        View btnClose = dialog.findViewById(R.id.btnCloseDirectory);
        Button btnAddNew = dialog.findViewById(R.id.btnAddNewCustomerInDirectory);
        if (btnAddNew != null) {
            btnAddNew.setVisibility(View.GONE);
        }

        Runnable renderList = () -> {
            containerList.removeAllViews();
            String query = etSearch.getText().toString().trim().toLowerCase(Locale.ENGLISH);
            List<Customer> customers = repository.getCustomers();
            Customer active = repository.getSelectedCustomer();
            float d = getResources().getDisplayMetrics().density;

            for (Customer c : customers) {
                String name = c.getCompanyName();
                String country = c.getCountry();
                if (!query.isEmpty() && !name.toLowerCase(Locale.ENGLISH).contains(query)
                        && !country.toLowerCase(Locale.ENGLISH).contains(query)) {
                    continue;
                }

                boolean isSelected = active != null && c.getId().equals(active.getId());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundResource(isSelected ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
                int pad = (int) (12 * d);
                row.setPadding(pad, pad, pad, pad);
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rlp.bottomMargin = (int) (8 * d);
                row.setLayoutParams(rlp);

                TextView tvName = new TextView(this);
                tvName.setText(name + (isSelected ? "  ✓" : ""));
                tvName.setTextSize(13f);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setTextColor(getColor(isSelected ? R.color.brand_blue : R.color.text_primary));
                row.addView(tvName);

                TextView tvSub = new TextView(this);
                String sub = (country.isEmpty() ? "EXPORT" : country)
                        + (!c.getDefaultPortOfDischarge().isEmpty() ? " • POD: " + c.getDefaultPortOfDischarge() : "");
                tvSub.setText(sub);
                tvSub.setTextSize(11f);
                tvSub.setTextColor(getColor(R.color.text_secondary));
                row.addView(tvSub);

                row.setOnClickListener(v -> {
                    repository.setSelectedCustomerId(c.getId());
                    dialog.dismiss();
                    populateDefaults();
                    Toast.makeText(this, "Filled customer details: " + c.getCompanyName(), Toast.LENGTH_SHORT).show();
                });

                containerList.addView(row);
            }
        };

        renderList.run();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { renderList.run(); }
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateCalculationModeUi() {
        boolean isNet = "net".equalsIgnoreCase(selectedCalculationMode);
        cardCalcModeNet.setBackgroundResource(isNet ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
        tvCalcModeNetTitle.setText(isNet ? "✓ Net (Standard)" : "Net (Standard)");
        tvCalcModeNetTitle.setTextColor(getColor(isNet ? R.color.brand_blue : R.color.text_primary));

        cardCalcModeGross.setBackgroundResource(!isNet ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
        tvCalcModeGrossTitle.setText(!isNet ? "✓ Gross (Round Up)" : "Gross (Round Up)");
        tvCalcModeGrossTitle.setTextColor(getColor(!isNet ? R.color.brand_blue : R.color.text_primary));
    }

    private void updateEndToEndUi() {
        cardToggleEndToEnd.setBackgroundResource(enableEndToEnd ? R.drawable.shape_badge_blue : R.drawable.shape_input_rounded);
        tvEndToEndTitle.setTextColor(getColor(enableEndToEnd ? R.color.brand_blue : R.color.text_primary));
        tvEndToEndStatusBadge.setText(enableEndToEnd ? "✓ ON" : "OFF");
        tvEndToEndStatusBadge.setTextColor(getColor(enableEndToEnd ? R.color.brand_blue : R.color.text_secondary));
    }

    private void startMarkingSession(@Nullable List<com.graniteflow.app.data.model.Block> initialBlocks) {
        String stone = etNmStoneType.getText().toString().trim();
        if (stone.isEmpty()) {
            etNmStoneType.setError("Material name is required");
            etNmStoneType.requestFocus();
            return;
        }

        double allowance = 15.0;
        try { allowance = Double.parseDouble(etNmAllowance.getText().toString().trim()); } catch (Exception ignored) {}

        int startBlockNo = 1;
        try { startBlockNo = Integer.parseInt(etNmStartBlockNo.getText().toString().trim()); } catch (Exception ignored) {}

        double price = 700.0;
        try { price = Double.parseDouble(etNmPricePerCbm.getText().toString().trim()); } catch (Exception ignored) {}

        CompanyProfile cp = repository.getCompanyProfile();
        Customer cust = repository.getSelectedCustomer();
        if (cust == null) {
            cust = repository.getCustomer();
        }

        Inspection insp = new Inspection();
        insp.applyCustomerDefaults(cust, cp);

        String consignee = etNmConsignee.getText().toString().trim();
        if (!consignee.isEmpty()) {
            insp.setCustomer(consignee);
            insp.setConsigneeShort(consignee);
        }
        String addr = etNmAddress.getText().toString().trim();
        if (!addr.isEmpty()) insp.setConsigneeAddress(addr);
        String phone = etNmPhone.getText().toString().trim();
        if (!phone.isEmpty()) insp.setConsigneePhone(phone);
        String buyer = etNmBuyer.getText().toString().trim();
        if (!buyer.isEmpty()) insp.setBuyer(buyer);
        String notify = etNmNotifyParty.getText().toString().trim();
        if (!notify.isEmpty()) insp.setNotifyParty(notify);

        insp.setPreCarriageBy(etNmPreCarriage.getText().toString().trim());
        insp.setPlaceOfReceipt(etNmPlaceReceipt.getText().toString().trim());
        if (!etNmPortLoading.getText().toString().trim().isEmpty()) {
            insp.setPortOfLoading(etNmPortLoading.getText().toString().trim());
        }
        insp.setVessel(etNmVessel.getText().toString().trim());
        if (!etNmPortDischarge.getText().toString().trim().isEmpty()) {
            insp.setPortOfDischarge(etNmPortDischarge.getText().toString().trim());
        }
        if (!etNmFinalDest.getText().toString().trim().isEmpty()) {
            insp.setFinalDestination(etNmFinalDest.getText().toString().trim());
        }
        if (!etNmFinalDestCountry.getText().toString().trim().isEmpty()) {
            insp.setFinalDestinationCountry(etNmFinalDestCountry.getText().toString().trim());
        }
        if (!etNmCountryOrigin.getText().toString().trim().isEmpty()) {
            insp.setCountryOfOrigin(etNmCountryOrigin.getText().toString().trim());
        }

        insp.setMaterialName(stone);
        insp.setStoneDescription(stone);
        String marks = etNmMarks.getText().toString().trim();
        insp.setMarksAndNos(marks);
        insp.setLocationCode(marks.isEmpty() ? insp.getCustomer() : marks);
        if (!etNmCurrency.getText().toString().trim().isEmpty()) {
            insp.setCurrency(etNmCurrency.getText().toString().trim());
        }
        insp.setPricePerCbm(price);
        if (!etNmPaymentTerms.getText().toString().trim().isEmpty()) {
            insp.setTermsOfPayment(etNmPaymentTerms.getText().toString().trim());
        }
        if (!etNmHsCode.getText().toString().trim().isEmpty()) {
            insp.setHsCode(etNmHsCode.getText().toString().trim());
        }

        insp.setDefaultAllowanceCm(allowance);
        insp.setStartingBlockNumber(startBlockNo);
        insp.setCalculationMode(selectedCalculationMode);
        insp.setEnableEndToEnd(enableEndToEnd);
        insp.setApprovalState("DRAFT");

        if (scannedEngineName != null && !scannedEngineName.isEmpty()) {
            insp.setScanEngineUsed(scannedEngineName);
        } else if (initialBlocks != null && !initialBlocks.isEmpty()) {
            insp.setScanEngineUsed("Gemini Vision AI (gemini-3.8-flash)");
        } else {
            insp.setScanEngineUsed("Manual Entry");
        }

        List<BlockTypePreset> types = new ArrayList<>();
        types.add(new BlockTypePreset("1", "T1", allowance, price, startBlockNo));
        insp.setBlockTypes(types);

        if (initialBlocks != null && !initialBlocks.isEmpty()) {
            insp.getBlocks().addAll(initialBlocks);
            insp.recalculateAllBlocks(false);
        }

        repository.saveInspection(insp);

        Intent intent = new Intent(this, InspectionWorkspaceActivity.class);
        intent.putExtra(InspectionWorkspaceActivity.EXTRA_INSPECTION_ID, insp.getInspectionId());
        intent.putExtra(InspectionWorkspaceActivity.EXTRA_BLOCK_INDEX, 0);
        startActivity(intent);
        finish();
    }

    private void showOcrPickerDialog() {
        Dialog dialog = new Dialog(this);
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
                processOcrSlip(null, null, "sample_quarry_slip.png");
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    private void processOcrSlip(@Nullable Uri imageUri, @Nullable Bitmap bitmap, @Nullable String assetFileName) {
        Dialog progress = new Dialog(this);
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
                progress.dismiss();
                if (parsed.blocks.isEmpty()) {
                    Toast.makeText(NewMarkingActivity.this, "No block dimensions identified on slip.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (parsed.materialName != null && !parsed.materialName.isEmpty()
                        && etNmStoneType.getText().toString().trim().isEmpty()) {
                    etNmStoneType.setText(parsed.materialName);
                }
                if (parsed.locationCode != null && !parsed.locationCode.isEmpty()
                        && etNmMarks.getText().toString().trim().isEmpty()) {
                    etNmMarks.setText(parsed.locationCode);
                }
                if (etNmConsignee.getText().toString().trim().isEmpty()) {
                    etNmConsignee.setText(parsed.locationCode != null && !parsed.locationCode.isEmpty()
                            ? parsed.locationCode : "Quarry Slip Import");
                }
                Toast.makeText(NewMarkingActivity.this,
                        "✨ " + engineUsed + ": Extracted " + parsed.blocks.size() + " blocks!",
                        Toast.LENGTH_SHORT).show();
                scannedEngineName = engineUsed;
                startMarkingSession(parsed.blocks);
            }

            @Override
            public void onFailure(Exception e) {
                progress.dismiss();
                Toast.makeText(NewMarkingActivity.this, "AI Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (assetFileName != null) {
            aiScanEngine.processAssetImage(this, assetFileName, callback);
        } else if (bitmap != null) {
            aiScanEngine.processBitmap(bitmap, callback);
        } else if (imageUri != null) {
            aiScanEngine.processUri(this, imageUri, callback);
        } else {
            progress.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiScanEngine != null) aiScanEngine.close();
    }
}
