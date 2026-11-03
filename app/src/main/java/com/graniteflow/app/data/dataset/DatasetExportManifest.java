package com.graniteflow.app.data.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatasetExportManifest implements Serializable {
    private String manifestVersion;
    private long exportTimestamp;
    private int totalRecords;
    private int approvedRecords;
    private int candidateRecords;
    private int evaluatedRecords;
    private Map<String, Integer> documentTypeCounts;
    private Map<String, Integer> statusCounts;
    private List<String> parserVersionsRepresented;
    private String checksumSha256;

    public DatasetExportManifest() {
        this.manifestVersion = "1.0.0";
        this.exportTimestamp = System.currentTimeMillis();
        this.documentTypeCounts = new HashMap<>();
        this.statusCounts = new HashMap<>();
        this.parserVersionsRepresented = new ArrayList<>();
    }

    public String getManifestVersion() { return manifestVersion; }
    public void setManifestVersion(String manifestVersion) { this.manifestVersion = manifestVersion; }

    public long getExportTimestamp() { return exportTimestamp; }
    public void setExportTimestamp(long exportTimestamp) { this.exportTimestamp = exportTimestamp; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getApprovedRecords() { return approvedRecords; }
    public void setApprovedRecords(int approvedRecords) { this.approvedRecords = approvedRecords; }

    public int getCandidateRecords() { return candidateRecords; }
    public void setCandidateRecords(int candidateRecords) { this.candidateRecords = candidateRecords; }

    public int getEvaluatedRecords() { return evaluatedRecords; }
    public void setEvaluatedRecords(int evaluatedRecords) { this.evaluatedRecords = evaluatedRecords; }

    public Map<String, Integer> getDocumentTypeCounts() { return documentTypeCounts; }
    public void setDocumentTypeCounts(Map<String, Integer> documentTypeCounts) { this.documentTypeCounts = documentTypeCounts; }

    public Map<String, Integer> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Integer> statusCounts) { this.statusCounts = statusCounts; }

    public List<String> getParserVersionsRepresented() { return parserVersionsRepresented; }
    public void setParserVersionsRepresented(List<String> parserVersionsRepresented) { this.parserVersionsRepresented = parserVersionsRepresented; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
}
