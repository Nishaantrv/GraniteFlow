package com.graniteflow.app.data.dataset;

import java.io.Serializable;

public class DatasetRecord implements Serializable {
    public static final String STATUS_CANDIDATE = "CANDIDATE";
    public static final String STATUS_NEEDS_CORRECTION = "NEEDS_CORRECTION";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EVALUATED = "EVALUATED";

    private String datasetRecordId;
    private String sourceDocumentId;
    private long createdAt;
    private long updatedAt;
    private String documentType;
    private String material;
    private String sourceFilePath;
    private String mimeType;
    private String sourceFileSha256;
    private String ocrEngine;
    private String parserVersion;
    private String recordStatus;
    private String approvedBy;
    private long approvedAt;
    private int activeGroundTruthVersion;
    private String evaluationId;
    private String datasetSplit; // "TRAIN", "VAL", "TEST", "EVALUATION_ONLY"
    private String overrideReason;

    public DatasetRecord() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.recordStatus = STATUS_CANDIDATE;
        this.activeGroundTruthVersion = 0;
        this.datasetSplit = "EVALUATION_ONLY";
    }

    public DatasetRecord(String datasetRecordId, String sourceDocumentId, String documentType, String material) {
        this();
        this.datasetRecordId = datasetRecordId;
        this.sourceDocumentId = sourceDocumentId;
        this.documentType = documentType;
        this.material = material;
    }

    // Getters and Setters
    public String getDatasetRecordId() { return datasetRecordId; }
    public void setDatasetRecordId(String datasetRecordId) { this.datasetRecordId = datasetRecordId; }

    public String getSourceDocumentId() { return sourceDocumentId; }
    public void setSourceDocumentId(String sourceDocumentId) { this.sourceDocumentId = sourceDocumentId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getSourceFileSha256() { return sourceFileSha256; }
    public void setSourceFileSha256(String sourceFileSha256) { this.sourceFileSha256 = sourceFileSha256; }

    public String getOcrEngine() { return ocrEngine; }
    public void setOcrEngine(String ocrEngine) { this.ocrEngine = ocrEngine; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public String getRecordStatus() { return recordStatus; }
    public void setRecordStatus(String recordStatus) { this.recordStatus = recordStatus; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(long approvedAt) { this.approvedAt = approvedAt; }

    public int getActiveGroundTruthVersion() { return activeGroundTruthVersion; }
    public void setActiveGroundTruthVersion(int activeGroundTruthVersion) { this.activeGroundTruthVersion = activeGroundTruthVersion; }

    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }

    public String getDatasetSplit() { return datasetSplit; }
    public void setDatasetSplit(String datasetSplit) { this.datasetSplit = datasetSplit; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
