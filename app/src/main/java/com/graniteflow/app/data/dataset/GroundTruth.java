package com.graniteflow.app.data.dataset;

import com.graniteflow.app.data.model.Block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GroundTruth implements Serializable {
    private String groundTruthId;
    private String datasetRecordId;
    private int version; // 1, 2, 3...
    private long createdAt;
    private long approvedAt;
    private String reviewerId;
    private String documentType;
    private String materialName;
    private String locationCode;
    private String inspectorName;
    private double defaultAllowanceCm;
    private List<Block> blocks;
    private List<String> editedFields; // e.g. ["blocks[0].grossLengthCm", "materialName"]
    private String changeSummary;
    private String validationOverrideReason;
    private boolean isApproved;

    public GroundTruth() {
        this.createdAt = System.currentTimeMillis();
        this.version = 1;
        this.blocks = new ArrayList<>();
        this.editedFields = new ArrayList<>();
        this.isApproved = false;
        this.defaultAllowanceCm = 10.0;
    }

    public GroundTruth(String groundTruthId, String datasetRecordId, int version, String reviewerId) {
        this();
        this.groundTruthId = groundTruthId;
        this.datasetRecordId = datasetRecordId;
        this.version = version;
        this.reviewerId = reviewerId;
    }

    public String getGroundTruthId() { return groundTruthId; }
    public void setGroundTruthId(String groundTruthId) { this.groundTruthId = groundTruthId; }

    public String getDatasetRecordId() { return datasetRecordId; }
    public void setDatasetRecordId(String datasetRecordId) { this.datasetRecordId = datasetRecordId; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getApprovedAt() { return approvedAt; }
    public void setApprovedAt(long approvedAt) { this.approvedAt = approvedAt; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public double getDefaultAllowanceCm() { return defaultAllowanceCm; }
    public void setDefaultAllowanceCm(double defaultAllowanceCm) { this.defaultAllowanceCm = defaultAllowanceCm; }

    public List<Block> getBlocks() { return blocks; }
    public void setBlocks(List<Block> blocks) { this.blocks = blocks; }

    public List<String> getEditedFields() { return editedFields; }
    public void setEditedFields(List<String> editedFields) { this.editedFields = editedFields; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public String getValidationOverrideReason() { return validationOverrideReason; }
    public void setValidationOverrideReason(String validationOverrideReason) { this.validationOverrideReason = validationOverrideReason; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }
}
