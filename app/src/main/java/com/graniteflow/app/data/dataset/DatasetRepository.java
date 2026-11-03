package com.graniteflow.app.data.dataset;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.graniteflow.app.engine.DatasetEvaluationEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DatasetRepository {

    private static final String PREF_NAME = "graniteflow_dataset_prefs";
    private static final String KEY_RECORDS = "key_dataset_records";
    private static final String KEY_PREDICTIONS = "key_predictions_map";
    private static final String KEY_GROUND_TRUTHS = "key_ground_truths_map";
    private static final String KEY_EVALUATIONS = "key_evaluations_map";

    private static DatasetRepository instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    private DatasetRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        seedInitialDatasetIfEmpty();
    }

    public static synchronized DatasetRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DatasetRepository(context);
        }
        return instance;
    }

    // Records
    public synchronized List<DatasetRecord> getAllRecords() {
        String json = prefs.getString(KEY_RECORDS, null);
        if (json != null) {
            Type type = new TypeToken<List<DatasetRecord>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    public synchronized DatasetRecord getRecord(String recordId) {
        List<DatasetRecord> list = getAllRecords();
        for (DatasetRecord r : list) {
            if (r.getDatasetRecordId().equals(recordId)) return r;
        }
        return null;
    }

    public synchronized void saveRecord(DatasetRecord record) {
        List<DatasetRecord> list = new ArrayList<>(getAllRecords());
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getDatasetRecordId().equals(record.getDatasetRecordId())) {
                idx = i;
                break;
            }
        }
        record.setUpdatedAt(System.currentTimeMillis());
        if (idx >= 0) {
            list.set(idx, record);
        } else {
            list.add(0, record);
        }
        prefs.edit().putString(KEY_RECORDS, gson.toJson(list)).apply();
    }

    // Predictions
    public synchronized OcrPrediction getPrediction(String recordId) {
        String json = prefs.getString(KEY_PREDICTIONS + "_" + recordId, null);
        if (json != null) {
            return gson.fromJson(json, OcrPrediction.class);
        }
        return null;
    }

    public synchronized void savePrediction(OcrPrediction prediction) {
        if (prediction == null || prediction.getDatasetRecordId() == null) return;
        prefs.edit().putString(KEY_PREDICTIONS + "_" + prediction.getDatasetRecordId(), gson.toJson(prediction)).apply();
    }

    // Ground Truths (Versioned)
    public synchronized GroundTruth getGroundTruth(String recordId, int version) {
        String json = prefs.getString(KEY_GROUND_TRUTHS + "_" + recordId + "_v" + version, null);
        if (json != null) {
            return gson.fromJson(json, GroundTruth.class);
        }
        return null;
    }

    public synchronized void saveGroundTruth(GroundTruth gt) {
        if (gt == null || gt.getDatasetRecordId() == null) return;
        prefs.edit().putString(KEY_GROUND_TRUTHS + "_" + gt.getDatasetRecordId() + "_v" + gt.getVersion(), gson.toJson(gt)).apply();

        // Update active ground truth version in record
        DatasetRecord record = getRecord(gt.getDatasetRecordId());
        if (record != null) {
            record.setActiveGroundTruthVersion(gt.getVersion());
            if (gt.isApproved()) {
                record.setRecordStatus(DatasetRecord.STATUS_APPROVED);
                record.setApprovedAt(gt.getApprovedAt());
                record.setApprovedBy(gt.getReviewerId());
            } else {
                record.setRecordStatus(DatasetRecord.STATUS_NEEDS_CORRECTION);
            }
            saveRecord(record);
        }
    }

    // Evaluations
    public synchronized EvaluationResult getEvaluation(String evaluationId) {
        String json = prefs.getString(KEY_EVALUATIONS + "_" + evaluationId, null);
        if (json != null) {
            return gson.fromJson(json, EvaluationResult.class);
        }
        return null;
    }

    public synchronized void saveEvaluation(EvaluationResult result) {
        if (result == null || result.getEvaluationId() == null) return;
        prefs.edit().putString(KEY_EVALUATIONS + "_" + result.getEvaluationId(), gson.toJson(result)).apply();

        DatasetRecord record = getRecord(result.getDatasetRecordId());
        if (record != null) {
            record.setEvaluationId(result.getEvaluationId());
            if (DatasetRecord.STATUS_APPROVED.equals(record.getRecordStatus())) {
                record.setRecordStatus(DatasetRecord.STATUS_EVALUATED);
            }
            saveRecord(record);
        }
    }

    public synchronized List<EvaluationResult> getEvaluationsForParser(String parserVersion) {
        List<EvaluationResult> results = new ArrayList<>();
        List<DatasetRecord> records = getAllRecords();
        for (DatasetRecord rec : records) {
            if (rec.getEvaluationId() != null) {
                EvaluationResult ev = getEvaluation(rec.getEvaluationId());
                if (ev != null && (parserVersion == null || parserVersion.equals(ev.getParserVersion()))) {
                    results.add(ev);
                }
            }
        }
        return results;
    }

    /**
     * Seeds initial realistic dataset records representing granite measurement slips.
     */
    private void seedInitialDatasetIfEmpty() {
        // Purge any legacy mock seeded records
        List<DatasetRecord> records = new ArrayList<>(getAllRecords());
        boolean removed = records.removeIf(r -> r.getDatasetRecordId() != null && 
                (r.getDatasetRecordId().contains("DS-REC-001") || r.getDatasetRecordId().contains("DS-REC-002")));
        if (removed) {
            prefs.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
        }
    }

    /**
     * Exports a portable ZIP archive containing manifest.json and structured records.
     */
    public File exportDatasetArchive(File outputDir) throws IOException {
        if (!outputDir.exists()) outputDir.mkdirs();
        File zipFile = new File(outputDir, "graniteflow_golden_dataset_" + System.currentTimeMillis() + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            List<DatasetRecord> records = getAllRecords();

            // 1. Build Manifest
            DatasetExportManifest manifest = new DatasetExportManifest();
            manifest.setTotalRecords(records.size());

            int approved = 0, candidate = 0, evaluated = 0;
            Map<String, Integer> docTypes = new HashMap<>();
            Map<String, Integer> statuses = new HashMap<>();

            for (DatasetRecord r : records) {
                if (DatasetRecord.STATUS_APPROVED.equals(r.getRecordStatus())) approved++;
                if (DatasetRecord.STATUS_CANDIDATE.equals(r.getRecordStatus())) candidate++;
                if (DatasetRecord.STATUS_EVALUATED.equals(r.getRecordStatus())) evaluated++;

                docTypes.put(r.getDocumentType(), docTypes.getOrDefault(r.getDocumentType(), 0) + 1);
                statuses.put(r.getRecordStatus(), statuses.getOrDefault(r.getRecordStatus(), 0) + 1);

                if (!manifest.getParserVersionsRepresented().contains(r.getParserVersion())) {
                    manifest.getParserVersionsRepresented().add(r.getParserVersion());
                }

                // Add record files into ZIP: records/<id>/
                String baseFolder = "records/" + r.getDatasetRecordId() + "/";

                // prediction.json
                OcrPrediction pred = getPrediction(r.getDatasetRecordId());
                if (pred != null) {
                    zos.putNextEntry(new ZipEntry(baseFolder + "prediction.json"));
                    zos.write(gson.toJson(pred).getBytes());
                    zos.closeEntry();
                }

                // ground_truth_v001.json
                if (r.getActiveGroundTruthVersion() > 0) {
                    GroundTruth gt = getGroundTruth(r.getDatasetRecordId(), r.getActiveGroundTruthVersion());
                    if (gt != null) {
                        String gtName = String.format("ground_truth_v%03d.json", gt.getVersion());
                        zos.putNextEntry(new ZipEntry(baseFolder + gtName));
                        zos.write(gson.toJson(gt).getBytes());
                        zos.closeEntry();
                    }
                }

                // evaluation.json
                if (r.getEvaluationId() != null) {
                    EvaluationResult ev = getEvaluation(r.getEvaluationId());
                    if (ev != null) {
                        zos.putNextEntry(new ZipEntry(baseFolder + "evaluation_v001.json"));
                        zos.write(gson.toJson(ev).getBytes());
                        zos.closeEntry();
                    }
                }
            }

            manifest.setApprovedRecords(approved);
            manifest.setCandidateRecords(candidate);
            manifest.setEvaluatedRecords(evaluated);
            manifest.setDocumentTypeCounts(docTypes);
            manifest.setStatusCounts(statuses);

            // Write manifest.json
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(gson.toJson(manifest).getBytes());
            zos.closeEntry();
        }

        return zipFile;
    }

    /**
     * Exports evaluation metrics in tabular CSV format.
     */
    public String exportEvaluationCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("DatasetRecordId,DocumentId,DocumentType,Material,Status,ParserVersion,RowsExpected,RowsPredicted,AlignedRows,DimAccuracy,CbmAccuracy,Precision,Recall,F1\n");
        for (DatasetRecord r : getAllRecords()) {
            if (r.getEvaluationId() != null) {
                EvaluationResult ev = getEvaluation(r.getEvaluationId());
                if (ev != null) {
                    sb.append(String.format("%s,%s,%s,%s,%s,%s,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f\n",
                            r.getDatasetRecordId(), r.getSourceDocumentId(), r.getDocumentType(), r.getMaterial(),
                            r.getRecordStatus(), r.getParserVersion(),
                            ev.getRowsExpected(), ev.getRowsPredicted(), ev.getAlignedRows(),
                            ev.getDimensionAccuracy(), ev.getCbmAccuracy(),
                            ev.getPrecision(), ev.getRecall(), ev.getF1()));
                }
            }
        }
        return sb.toString();
    }
}
