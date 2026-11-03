package com.graniteflow.app.data.model;

import java.io.Serializable;

public class DocumentAuditRecord implements Serializable {
    private String documentId;
    private String documentType; // "INVOICE", "PACKING_LIST", "INSPECTION_REPORT"
    private int version;
    private long timestamp;
    private String modifiedBy;
    private String changesSummary;
    private String status;

    public DocumentAuditRecord() {}

    public DocumentAuditRecord(String documentId, String documentType, int version, String changesSummary, String status) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.version = version;
        this.timestamp = System.currentTimeMillis();
        this.modifiedBy = "Operator";
        this.changesSummary = changesSummary;
        this.status = status;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public String getChangesSummary() { return changesSummary; }
    public void setChangesSummary(String changesSummary) { this.changesSummary = changesSummary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
