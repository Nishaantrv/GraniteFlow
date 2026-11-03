package com.graniteflow.app.ui.inspection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.graniteflow.app.R;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.data.repository.GraniteRepository;
import com.graniteflow.app.data.storage.LocalDataManager;
import com.graniteflow.app.ui.document.UnifiedExportDocumentsActivity;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.app.Dialog;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FinishDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_INSPECTION_ID = "extra_inspection_id";

    private GraniteRepository repository;
    private Inspection inspection;

    private TextView tvFdTotalBlocks, tvFdTotalNetCbm, tvFdTotalGrossCbm, tvFdValueLabel, tvFdTotalValue;
    private TextView tvScanEngineIcon, tvScanEngineTitle, tvScanEngineDesc, tvScanEngineBadge;
    private EditText etFdConsignee, etFdAddress, etFdBuyer, etFdNotifyParty;
    private EditText etFdPreCarriage, etFdPlaceReceipt, etFdVessel, etFdPortLoading;
    private EditText etFdPortDischarge, etFdFinalDest, etFdFinalDestCountry, etFdCountryOrigin;
    private EditText etFdInvoiceNo, etFdExporterRef, etFdTermsDelivery, etFdTermsPayment;
    private EditText etFdCurrency, etFdPricePerCbm, etFdHsCode, etFdMarks, etFdPhone;
    private EditText etFdStoneType, etFdStoneDescription, etFdQuarryCode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_details);

        repository = GraniteRepository.getInstance(this);
        String inspId = getIntent().getStringExtra(EXTRA_INSPECTION_ID);
        if (inspId != null) {
            inspection = repository.getInspectionById(inspId);
        }
        if (inspection == null) {
            inspection = repository.getActiveInspection();
        }
        if (inspection == null) {
            finish();
            return;
        }

        initViews();
        populateFromInspection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inspection != null) {
            Inspection refreshed = repository.getInspectionById(inspection.getInspectionId());
            if (refreshed != null) {
                inspection = refreshed;
                updateSummaryGrid();
            }
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnFinishBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnEditBlocks = findViewById(R.id.btnFinishEditBlocks);
        btnEditBlocks.setOnClickListener(v -> {
            saveHeaderFields(false);
            Intent intent = new Intent(this, BlockListEditActivity.class);
            intent.putExtra(BlockListEditActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
            startActivity(intent);
        });

        tvFdTotalBlocks = findViewById(R.id.tvFdTotalBlocks);
        tvFdTotalNetCbm = findViewById(R.id.tvFdTotalNetCbm);
        tvFdTotalGrossCbm = findViewById(R.id.tvFdTotalGrossCbm);
        tvFdValueLabel = findViewById(R.id.tvFdValueLabel);
        tvFdTotalValue = findViewById(R.id.tvFdTotalValue);

        tvScanEngineIcon = findViewById(R.id.tvScanEngineIcon);
        tvScanEngineTitle = findViewById(R.id.tvScanEngineTitle);
        tvScanEngineDesc = findViewById(R.id.tvScanEngineDesc);
        tvScanEngineBadge = findViewById(R.id.tvScanEngineBadge);

        etFdConsignee = findViewById(R.id.etFdConsignee);
        etFdAddress = findViewById(R.id.etFdAddress);
        etFdBuyer = findViewById(R.id.etFdBuyer);
        etFdNotifyParty = findViewById(R.id.etFdNotifyParty);
        etFdPreCarriage = findViewById(R.id.etFdPreCarriage);
        etFdPlaceReceipt = findViewById(R.id.etFdPlaceReceipt);
        etFdVessel = findViewById(R.id.etFdVessel);
        etFdPortLoading = findViewById(R.id.etFdPortLoading);
        etFdPortDischarge = findViewById(R.id.etFdPortDischarge);
        etFdFinalDest = findViewById(R.id.etFdFinalDest);
        etFdFinalDestCountry = findViewById(R.id.etFdFinalDestCountry);
        etFdCountryOrigin = findViewById(R.id.etFdCountryOrigin);

        etFdInvoiceNo = findViewById(R.id.etFdInvoiceNo);
        etFdExporterRef = findViewById(R.id.etFdExporterRef);
        etFdTermsDelivery = findViewById(R.id.etFdTermsDelivery);
        etFdTermsPayment = findViewById(R.id.etFdTermsPayment);
        etFdCurrency = findViewById(R.id.etFdCurrency);
        etFdPricePerCbm = findViewById(R.id.etFdPricePerCbm);
        etFdHsCode = findViewById(R.id.etFdHsCode);
        etFdMarks = findViewById(R.id.etFdMarks);
        etFdPhone = findViewById(R.id.etFdPhone);

        etFdStoneType = findViewById(R.id.etFdStoneType);
        etFdStoneDescription = findViewById(R.id.etFdStoneDescription);
        etFdQuarryCode = findViewById(R.id.etFdQuarryCode);

        Button btnFinishAndExport = findViewById(R.id.btnFinishAndExport);
        btnFinishAndExport.setOnClickListener(v -> showFinalVerificationDialog());
    }

    private void showFinalVerificationDialog() {
        saveHeaderFields(false);
        String cust = etFdConsignee.getText().toString().trim();
        if (cust.isEmpty()) cust = inspection.getCustomer();
        if (cust.isEmpty()) cust = inspection.getBuyer();
        if (cust.isEmpty()) cust = "this customer";

        String invoiceNo = etFdInvoiceNo.getText().toString().trim();
        String reportDesc = !invoiceNo.isEmpty() ? ("Invoice #" + invoiceNo) : ("Report " + inspection.getInspectionId());

        new AlertDialog.Builder(this)
                .setTitle("🎯 Confirm Final Verified Values")
                .setMessage("Are these the final verified changes for " + reportDesc + " for customer \"" + cust + "\"?\n\n" +
                        "If YES, the actual verified values will be compared against predicted OCR values to compute the loss function and continuously train your local OCR model.")
                .setPositiveButton("Yes, Train & Export", (dialog, which) -> submitGroundTruthAndTrain())
                .setNeutralButton("Export Without Training", (dialog, which) -> completeAndExport())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitGroundTruthAndTrain() {
        saveHeaderFields(true);
        Dialog progress = new Dialog(this);
        progress.setContentView(android.R.layout.simple_list_item_1);
        TextView tv = progress.findViewById(android.R.id.text1);
        if (tv != null) {
            tv.setText("⚡ Computing Loss & Training Local OCR Model...");
            tv.setPadding(36, 36, 36, 36);
        }
        progress.setCancelable(false);
        progress.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String baseUrl = new LocalDataManager(this).getBackendUrl();
                if (!baseUrl.endsWith("/")) baseUrl += "/";
                String endpoint = baseUrl + "api/v1/ocr/train-feedback";

                Gson gson = new Gson();
                String predJson = inspection.getRawOcrPredictionJson();
                if (predJson == null || predJson.trim().isEmpty()) {
                    predJson = gson.toJson(inspection);
                }
                String actualJson = gson.toJson(inspection);

                MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("prediction", predJson)
                        .addFormDataPart("actual", actualJson)
                        .addFormDataPart("customer", inspection.getCustomer())
                        .addFormDataPart("documentType", "QUARRY_MEASUREMENT_SLIP");

                String imgPath = inspection.getOriginalSlipImagePath();
                if (imgPath != null && !imgPath.isEmpty()) {
                    File imgFile = new File(imgPath);
                    if (imgFile.exists() && imgFile.length() > 0) {
                        builder.addFormDataPart("file", imgFile.getName(),
                                RequestBody.create(imgFile, MediaType.parse("image/jpeg")));
                    }
                }

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(builder.build())
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String respBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        JsonObject jsonResp = JsonParser.parseString(respBody).getAsJsonObject();
                        String msg = jsonResp.has("message") ? jsonResp.get("message").getAsString() : "Model updated!";
                        JsonObject lossObj = jsonResp.has("loss") && jsonResp.get("loss").isJsonObject()
                                ? jsonResp.getAsJsonObject("loss") : null;
                        String lossStr = (lossObj != null && lossObj.has("total_loss"))
                                ? String.valueOf(lossObj.get("total_loss").getAsDouble()) : "0.00";

                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(this, "🎯 OCR Model Trained! (Loss: " + lossStr + ")\n" + msg, Toast.LENGTH_LONG).show();
                            completeAndExport();
                        });
                    } else {
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(this, "⚠️ Model server returned error (" + response.code() + "). Proceeding to export.", Toast.LENGTH_SHORT).show();
                            completeAndExport();
                        });
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "⚠️ Training server offline: " + e.getMessage() + ". Saved locally.", Toast.LENGTH_SHORT).show();
                    completeAndExport();
                });
            }
        });
    }

    private void completeAndExport() {
        saveHeaderFields(true);
        Toast.makeText(this, "Inspection completed & ready for export!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, UnifiedExportDocumentsActivity.class);
        intent.putExtra(UnifiedExportDocumentsActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
        startActivity(intent);
    }

    private void populateFromInspection() {
        updateSummaryGrid();

        String engine = inspection.getScanEngineUsed();
        if (tvScanEngineTitle != null) {
            tvScanEngineTitle.setText(engine);
            if (engine.contains("Gemini")) {
                if (tvScanEngineIcon != null) tvScanEngineIcon.setText("✨");
                if (tvScanEngineDesc != null) tvScanEngineDesc.setText("Extracted via Google Gemini Multimodal Vision API");
                if (tvScanEngineBadge != null) {
                    tvScanEngineBadge.setText("CLOUD AI");
                    tvScanEngineBadge.setTextColor(getColor(R.color.brand_blue));
                }
            } else if (engine.contains("Python")) {
                if (tvScanEngineIcon != null) tvScanEngineIcon.setText("⚡");
                if (tvScanEngineDesc != null) tvScanEngineDesc.setText("Extracted via GraniteFlow Local Python OCR Model Server");
                if (tvScanEngineBadge != null) {
                    tvScanEngineBadge.setText("LOCAL SERVER");
                    tvScanEngineBadge.setTextColor(getColor(R.color.brand_blue));
                }
            } else if (engine.contains("ML Kit")) {
                if (tvScanEngineIcon != null) tvScanEngineIcon.setText("📱");
                if (tvScanEngineDesc != null) tvScanEngineDesc.setText("Extracted via on-device Google ML Kit Text Recognition");
                if (tvScanEngineBadge != null) {
                    tvScanEngineBadge.setText("LOCAL OCR");
                    tvScanEngineBadge.setTextColor(getColor(R.color.brand_blue));
                }
            } else {
                if (tvScanEngineIcon != null) tvScanEngineIcon.setText("✍️");
                if (tvScanEngineDesc != null) tvScanEngineDesc.setText("Direct manual quarry measurement entry");
                if (tvScanEngineBadge != null) {
                    tvScanEngineBadge.setText("MANUAL ENTRY");
                    tvScanEngineBadge.setTextColor(getColor(R.color.text_secondary));
                }
            }
        }

        etFdConsignee.setText(inspection.getCustomer());
        etFdAddress.setText(inspection.getConsigneeAddress());
        etFdBuyer.setText(inspection.getBuyer());
        etFdNotifyParty.setText(inspection.getNotifyParty());
        etFdPreCarriage.setText(inspection.getPreCarriageBy());
        etFdPlaceReceipt.setText(inspection.getPlaceOfReceipt());
        etFdVessel.setText(inspection.getVessel());
        etFdPortLoading.setText(inspection.getPortOfLoading());
        etFdPortDischarge.setText(inspection.getPortOfDischarge());
        etFdFinalDest.setText(inspection.getFinalDestination());
        etFdFinalDestCountry.setText(inspection.getFinalDestinationCountry());
        etFdCountryOrigin.setText(inspection.getCountryOfOrigin());

        etFdInvoiceNo.setText(inspection.getInvoiceNumber());
        etFdExporterRef.setText(inspection.getExporterRefNumber());
        etFdTermsDelivery.setText(inspection.getTermsOfDelivery());
        etFdTermsPayment.setText(inspection.getTermsOfPayment());
        etFdCurrency.setText(inspection.getCurrency());
        etFdPricePerCbm.setText(inspection.getPricePerCbm() > 0 ? String.valueOf((int) inspection.getPricePerCbm()) : "0");
        etFdHsCode.setText(inspection.getHsCode());
        etFdMarks.setText(inspection.getMarksAndNos());
        etFdPhone.setText(inspection.getConsigneePhone());

        etFdStoneType.setText(inspection.getMaterialName());
        etFdStoneDescription.setText(inspection.getStoneDescription());
        etFdQuarryCode.setText(inspection.getLocationCode());
    }

    private void updateSummaryGrid() {
        tvFdTotalBlocks.setText(String.valueOf(inspection.getTotalBlocksCount()));
        tvFdTotalNetCbm.setText(String.format(Locale.ENGLISH, "%.3f", inspection.calculateTotalNetCbm()));
        tvFdTotalGrossCbm.setText(String.format(Locale.ENGLISH, "%.3f", inspection.calculateTotalGrossCbm()));
        String curr = inspection.getCurrency();
        tvFdValueLabel.setText("VALUE (" + curr + ")");
        tvFdTotalValue.setText(String.format(Locale.ENGLISH, "%s %,.0f", curr, inspection.calculateTotalValue()));
    }

    private void saveHeaderFields(boolean markCompleted) {
        inspection.setCustomer(etFdConsignee.getText().toString().trim());
        inspection.setConsigneeAddress(etFdAddress.getText().toString().trim());
        inspection.setBuyer(etFdBuyer.getText().toString().trim());
        inspection.setNotifyParty(etFdNotifyParty.getText().toString().trim());
        inspection.setPreCarriageBy(etFdPreCarriage.getText().toString().trim());
        inspection.setPlaceOfReceipt(etFdPlaceReceipt.getText().toString().trim());
        inspection.setVessel(etFdVessel.getText().toString().trim());
        inspection.setPortOfLoading(etFdPortLoading.getText().toString().trim());
        inspection.setPortOfDischarge(etFdPortDischarge.getText().toString().trim());
        inspection.setFinalDestination(etFdFinalDest.getText().toString().trim());
        inspection.setFinalDestinationCountry(etFdFinalDestCountry.getText().toString().trim());
        inspection.setCountryOfOrigin(etFdCountryOrigin.getText().toString().trim());

        inspection.setInvoiceNumber(etFdInvoiceNo.getText().toString().trim());
        inspection.setExporterRefNumber(etFdExporterRef.getText().toString().trim());
        inspection.setTermsOfDelivery(etFdTermsDelivery.getText().toString().trim());
        inspection.setTermsOfPayment(etFdTermsPayment.getText().toString().trim());
        inspection.setCurrency(etFdCurrency.getText().toString().trim().isEmpty() ? "USD" : etFdCurrency.getText().toString().trim());

        try {
            double p = Double.parseDouble(etFdPricePerCbm.getText().toString().trim());
            inspection.setPricePerCbm(p);
        } catch (Exception ignored) {}

        inspection.setHsCode(etFdHsCode.getText().toString().trim());
        inspection.setMarksAndNos(etFdMarks.getText().toString().trim());
        inspection.setConsigneePhone(etFdPhone.getText().toString().trim());

        inspection.setMaterialName(etFdStoneType.getText().toString().trim());
        inspection.setStoneDescription(etFdStoneDescription.getText().toString().trim());
        inspection.setLocationCode(etFdQuarryCode.getText().toString().trim());

        if (markCompleted) {
            inspection.setApprovalState("COMPLETED");
        }
        inspection.recalculateAllBlocks(false);
        repository.saveInspection(inspection);
    }
}
