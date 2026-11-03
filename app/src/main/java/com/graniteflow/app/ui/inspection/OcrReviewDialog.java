package com.graniteflow.app.ui.inspection;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.graniteflow.app.R;
import com.graniteflow.app.data.api.ExtractionRiskContract;
import com.graniteflow.app.data.dataset.DatasetRecord;
import com.graniteflow.app.data.dataset.DatasetRepository;
import com.graniteflow.app.data.dataset.EvaluationResult;
import com.graniteflow.app.data.dataset.GroundTruth;
import com.graniteflow.app.data.dataset.OcrPrediction;
import com.graniteflow.app.data.model.Block;
import com.graniteflow.app.data.model.Inspection;
import com.graniteflow.app.engine.DatasetEvaluationEngine;
import com.graniteflow.app.ocr.GraniteSlipParser;

import java.util.ArrayList;

public class OcrReviewDialog extends Dialog {

    public interface OnOcrApprovedListener {
        void onApproved(Inspection approvedInspection);
    }

    private final OnOcrApprovedListener listener;
    private final DatasetRepository datasetRepository;
    private final GraniteSlipParser.ParsedInspectionResult parsedResult;
    private final Uri imageUri;

    public OcrReviewDialog(@NonNull Context context, OnOcrApprovedListener listener) {
        this(context, null, null, listener);
    }

    public OcrReviewDialog(@NonNull Context context,
                           GraniteSlipParser.ParsedInspectionResult parsedResult,
                           Uri imageUri,
                           OnOcrApprovedListener listener) {
        super(context);
        this.listener = listener;
        this.datasetRepository = DatasetRepository.getInstance(context);
        this.parsedResult = parsedResult != null ? parsedResult : createDefaultFallbackResult();
        this.imageUri = imageUri;
    }

    private static GraniteSlipParser.ParsedInspectionResult createDefaultFallbackResult() {
        GraniteSlipParser.ParsedInspectionResult res = new GraniteSlipParser.ParsedInspectionResult();
        res.materialName = "";
        res.locationCode = "";
        res.defaultAllowanceCm = 10.0;
        return res;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_ocr_review);
        if (getWindow() != null) {
            getWindow().setLayout((int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.95),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvEngineInfo = findViewById(R.id.tvOcrEngineInfo);
        ImageView ivDocPreview = findViewById(R.id.ivOcrDocumentPreview);
        TextView tvRawOcrSnippet = findViewById(R.id.tvRawOcrSnippet);
        LinearLayout layoutRiskIndicator = findViewById(R.id.layoutRiskIndicator);
        TextView tvRiskBadge = findViewById(R.id.tvRiskBadge);
        TextView tvRiskDetails = findViewById(R.id.tvRiskDetails);
        LinearLayout layoutOcrVerificationBadge = findViewById(R.id.layoutOcrVerificationBadge);
        TextView tvOcrVerificationText = findViewById(R.id.tvOcrVerificationText);

        int verifiedCount = 0;
        for (Block b : parsedResult.blocks) {
            if (b.isMathVerified()) verifiedCount++;
        }

        if (layoutOcrVerificationBadge != null && tvOcrVerificationText != null) {
            if (verifiedCount == parsedResult.blocks.size() && !parsedResult.blocks.isEmpty()) {
                layoutOcrVerificationBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
                tvOcrVerificationText.setText("✓ 100% MATHEMATICALLY VERIFIED (CBM Checksum)");
            } else if (verifiedCount > 0) {
                layoutOcrVerificationBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0284C7));
                tvOcrVerificationText.setText(String.format("✓ %d/%d BLOCKS FORMULA VERIFIED", verifiedCount, parsedResult.blocks.size()));
            } else {
                layoutOcrVerificationBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF59E0B));
                tvOcrVerificationText.setText("⚠️ UNVERIFIED CHECKSUM • PLEASE REVIEW");
            }
        }

        EditText etMaterial = findViewById(R.id.etOcrMaterial);
        EditText etLocation = findViewById(R.id.etOcrLocation);
        TextView tvSummary = findViewById(R.id.tvOcrBlocksSummary);

        RadioButton rbApproveGroundTruth = findViewById(R.id.rbApproveGroundTruth);
        RadioButton rbDatasetCandidate = findViewById(R.id.rbDatasetCandidate);
        Button btnApprove = findViewById(R.id.btnApproveOcr);
        Button btnCancel = findViewById(R.id.btnCancelOcr);

        tvEngineInfo.setText("Engine: Google Gemini 3.8 Flash Vision AI | ML Kit Fallback");
        etMaterial.setText(parsedResult.materialName);
        etLocation.setText(parsedResult.locationCode);

        tvSummary.setText(String.format("Predicted %d blocks • %.3f M³ volume • %.0f cm allowance",
                parsedResult.blocks.size(),
                parsedResult.calculateTotalNetCbm(),
                parsedResult.defaultAllowanceCm));

        // Display raw OCR text
        if (parsedResult.rawOcrText != null && !parsedResult.rawOcrText.trim().isEmpty()) {
            tvRawOcrSnippet.setText(parsedResult.rawOcrText.trim());
        } else {
            tvRawOcrSnippet.setText("No raw OCR text detected.");
        }

        // Display image preview
        if (imageUri != null) {
            Glide.with(getContext()).load(imageUri).into(ivDocPreview);
        } else {
            ivDocPreview.setImageResource(R.drawable.sample_quarry_slip);
        }

        // Run Supervised ML / Heuristic Extraction Risk Assessment
        ExtractionRiskContract.RiskInferenceRequest riskReq =
                new ExtractionRiskContract.RiskInferenceRequest(
                        "QUARRY_MEASUREMENT_SLIP",
                        "Google ML Kit",
                        "v1.0.0",
                        parsedResult.blocks
                );
        ExtractionRiskContract.RiskInferenceResponse riskResp =
                ExtractionRiskContract.RiskInferenceResponse.createOfflineFallbackResponse(riskReq);

        boolean hasDiscrepancy = false;
        StringBuilder riskReasons = new StringBuilder();
        for (ExtractionRiskContract.BlockRiskEvaluation eval : riskResp.getBlockEvaluations()) {
            if (eval.getRiskCategory() == ExtractionRiskContract.RiskCategory.NEEDS_REVIEW) {
                hasDiscrepancy = true;
                for (String r : eval.getReasonCodes()) {
                    if (!r.equals("OFFLINE_HEURISTIC_EVALUATED")) {
                        riskReasons.append(r).append(" ");
                    }
                }
            }
        }

        if (hasDiscrepancy) {
            tvRiskBadge.setText("⚠️ NEEDS REVIEW");
            tvRiskBadge.setTextColor(0xFFB45309);
            layoutRiskIndicator.setBackgroundColor(0xFFFEF3C7);
            tvRiskDetails.setText("Extraction anomaly detected: " + riskReasons.toString().trim());
            tvRiskDetails.setTextColor(0xFF92400E);
        } else {
            tvRiskBadge.setText("🛡️ LOW OBSERVED RISK");
            tvRiskBadge.setTextColor(0xFF065F46);
            layoutRiskIndicator.setBackgroundColor(0xFFECFDF5);
            tvRiskDetails.setText("All " + parsedResult.blocks.size() + " extracted block measurements verified with formula consistency.");
            tvRiskDetails.setTextColor(0xFF047857);
        }

        btnCancel.setOnClickListener(v -> dismiss());

        btnApprove.setOnClickListener(v -> {
            String material = etMaterial.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            // 1. Build Canonical Inspection
            Inspection approved = new Inspection();
            approved.setInspectionId("INSP-OCR-" + (System.currentTimeMillis() % 100000));
            approved.setMaterialName(material);
            approved.setLocationCode(location);
            approved.setDate(parsedResult.date.isEmpty() ? "12/10/2026" : parsedResult.date);
            approved.setDefaultAllowanceCm(parsedResult.defaultAllowanceCm);
            approved.setApprovalState("APPROVED");
            approved.setBlocks(new ArrayList<>(parsedResult.blocks));

            // 2. Handle Golden Dataset Workflow Choice
            if (rbApproveGroundTruth.isChecked()) {
                Log.d("GraniteFlowOCR", "[HUMAN APPROVAL] User explicitly approved extraction as Ground Truth v1.");
                // Human-verified Ground Truth creation
                String recId = "DS-REC-" + (System.currentTimeMillis() % 100000);
                DatasetRecord rec = new DatasetRecord(recId, "DOC-" + approved.getInspectionId(), "QUARRY_MEASUREMENT_SLIP", material);
                rec.setOcrEngine("Google ML Kit");
                rec.setParserVersion("v1.0.0");
                rec.setRecordStatus(DatasetRecord.STATUS_APPROVED);
                rec.setApprovedBy("Inspector");
                rec.setApprovedAt(System.currentTimeMillis());
                rec.setActiveGroundTruthVersion(1);
                datasetRepository.saveRecord(rec);

                // Save Prediction snapshot
                OcrPrediction pred = new OcrPrediction("PRED-" + (System.currentTimeMillis() % 100000), recId, "Google ML Kit", "v1.0.0");
                pred.setRawOcrText(parsedResult.rawOcrText != null ? parsedResult.rawOcrText : ("QUARRY SLIP: " + location + " " + material));
                for (Block b : approved.getBlocks()) {
                    pred.getBlocks().add(new OcrPrediction.PredictedBlock(
                            b.getBlockNumber(),
                            b.getGrossLengthCm(),
                            b.getGrossHeightCm(),
                            b.getGrossWidthCm(),
                            b.getAllowanceCm()
                    ));
                }
                datasetRepository.savePrediction(pred);

                // Save Ground Truth v1
                GroundTruth gt = new GroundTruth("GT-" + (System.currentTimeMillis() % 100000), recId, 1, "Inspector");
                gt.setMaterialName(material);
                gt.setLocationCode(location);
                gt.setApproved(true);
                gt.setApprovedAt(System.currentTimeMillis());
                gt.setBlocks(approved.getBlocks());
                datasetRepository.saveGroundTruth(gt);

                // Run deterministic evaluation
                EvaluationResult eval = DatasetEvaluationEngine.evaluate(pred, gt);
                eval.setEvaluationId("EVAL-" + (System.currentTimeMillis() % 100000));
                datasetRepository.saveEvaluation(eval);

                Log.d("GraniteFlowOCR", "[Golden Dataset] Ground Truth v1 (" + gt.getGroundTruthId() +
                        ") and immutable prediction snapshot (" + pred.getPredictionId() + ") saved. Match accuracy: " +
                        (eval.getDimensionAccuracy() * 100) + "%");

                Toast.makeText(getContext(), "Approved as Golden Dataset Ground Truth v1!", Toast.LENGTH_SHORT).show();

            } else if (rbDatasetCandidate.isChecked()) {
                Log.d("GraniteFlowOCR", "[HUMAN APPROVAL] User saved unverified record as Golden Dataset Candidate.");
                // Save as Candidate for later review
                String recId = "DS-REC-" + (System.currentTimeMillis() % 100000);
                DatasetRecord rec = new DatasetRecord(recId, "DOC-" + approved.getInspectionId(), "QUARRY_MEASUREMENT_SLIP", material);
                rec.setOcrEngine("Google ML Kit");
                rec.setParserVersion("v1.0.0");
                rec.setRecordStatus(DatasetRecord.STATUS_CANDIDATE);
                datasetRepository.saveRecord(rec);

                OcrPrediction pred = new OcrPrediction("PRED-" + (System.currentTimeMillis() % 100000), recId, "Google ML Kit", "v1.0.0");
                pred.setRawOcrText(parsedResult.rawOcrText != null ? parsedResult.rawOcrText : "RAW OCR UNVERIFIED");
                datasetRepository.savePrediction(pred);

                Toast.makeText(getContext(), "Saved as Candidate in Golden Dataset inventory.", Toast.LENGTH_SHORT).show();
            }

            dismiss();
            if (listener != null) {
                listener.onApproved(approved);
            }
        });
    }
}
