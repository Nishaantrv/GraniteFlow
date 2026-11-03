package com.graniteflow.app.data.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationResult implements Serializable {

    public static final String ERR_OCR_TEXT = "OCR_TEXT_RECOGNITION_FAILURE";
    public static final String ERR_BLOCK_NUMBER = "BLOCK_NUMBER_FAILURE";
    public static final String ERR_DIMENSION_EXTRACTION = "DIMENSION_EXTRACTION_FAILURE";
    public static final String ERR_CBM_MISMATCH = "CBM_CALCULATION_OR_MISMATCH";
    public static final String ERR_MISSING_ROW = "ROW_MISSING_FAILURE";
    public static final String ERR_EXTRA_ROW = "EXTRA_ROW_DETECTED";
    public static final String ERR_BUSINESS_RULE = "BUSINESS_RULE_VALIDATION_FAILURE";
    public static final String ERR_UNKNOWN = "UNKNOWN";

    private String evaluationId;
    private String datasetRecordId;
    private int groundTruthVersion;
    private String ocrEngine;
    private String parserVersion;
    private String appBuildVersion;
    private long evaluatedAt;

    private int rowsExpected;
    private int rowsPredicted;
    private int alignedRows;
    private int missingRows;
    private int extraRows;

    private int blockNumberExactMatches;
    private int dimensionExactMatches;
    private int cbmMatches;

    private double precision; // Safe denominator, -1 if undefined
    private double recall;    // Safe denominator, -1 if undefined
    private double f1;        // Safe denominator, -1 if undefined

    private double dimensionAccuracy;
    private double cbmAccuracy;
    private double fieldCoverage;

    private Map<String, Integer> errorCategoryCounts;
    private List<RowEvaluationDetail> rowDetails;
    private String notes;

    public static class RowEvaluationDetail implements Serializable {
        public String expectedBlockNumber;
        public String predictedBlockNumber;
        public boolean blockNumberMatch;
        public boolean dimensionsMatch;
        public boolean cbmMatch;
        public double expectedCbm;
        public double predictedCbm;
        public String errorCategory;
        public String remarks;

        public RowEvaluationDetail() {}
    }

    public EvaluationResult() {
        this.evaluatedAt = System.currentTimeMillis();
        this.errorCategoryCounts = new HashMap<>();
        this.rowDetails = new ArrayList<>();
        this.appBuildVersion = "1.0.0-debug";
        this.precision = -1.0;
        this.recall = -1.0;
        this.f1 = -1.0;
    }

    public void addError(String category) {
        if (category == null || category.isEmpty()) category = ERR_UNKNOWN;
        int count = errorCategoryCounts.getOrDefault(category, 0);
        errorCategoryCounts.put(category, count + 1);
    }

    // Getters and Setters
    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }

    public String getDatasetRecordId() { return datasetRecordId; }
    public void setDatasetRecordId(String datasetRecordId) { this.datasetRecordId = datasetRecordId; }

    public int getGroundTruthVersion() { return groundTruthVersion; }
    public void setGroundTruthVersion(int groundTruthVersion) { this.groundTruthVersion = groundTruthVersion; }

    public String getOcrEngine() { return ocrEngine; }
    public void setOcrEngine(String ocrEngine) { this.ocrEngine = ocrEngine; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public String getAppBuildVersion() { return appBuildVersion; }
    public void setAppBuildVersion(String appBuildVersion) { this.appBuildVersion = appBuildVersion; }

    public long getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(long evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public int getRowsExpected() { return rowsExpected; }
    public void setRowsExpected(int rowsExpected) { this.rowsExpected = rowsExpected; }

    public int getRowsPredicted() { return rowsPredicted; }
    public void setRowsPredicted(int rowsPredicted) { this.rowsPredicted = rowsPredicted; }

    public int getAlignedRows() { return alignedRows; }
    public void setAlignedRows(int alignedRows) { this.alignedRows = alignedRows; }

    public int getMissingRows() { return missingRows; }
    public void setMissingRows(int missingRows) { this.missingRows = missingRows; }

    public int getExtraRows() { return extraRows; }
    public void setExtraRows(int extraRows) { this.extraRows = extraRows; }

    public int getBlockNumberExactMatches() { return blockNumberExactMatches; }
    public void setBlockNumberExactMatches(int blockNumberExactMatches) { this.blockNumberExactMatches = blockNumberExactMatches; }

    public int getDimensionExactMatches() { return dimensionExactMatches; }
    public void setDimensionExactMatches(int dimensionExactMatches) { this.dimensionExactMatches = dimensionExactMatches; }

    public int getCbmMatches() { return cbmMatches; }
    public void setCbmMatches(int cbmMatches) { this.cbmMatches = cbmMatches; }

    public double getPrecision() { return precision; }
    public void setPrecision(double precision) { this.precision = precision; }

    public double getRecall() { return recall; }
    public void setRecall(double recall) { this.recall = recall; }

    public double getF1() { return f1; }
    public void setF1(double f1) { this.f1 = f1; }

    public double getDimensionAccuracy() { return dimensionAccuracy; }
    public void setDimensionAccuracy(double dimensionAccuracy) { this.dimensionAccuracy = dimensionAccuracy; }

    public double getCbmAccuracy() { return cbmAccuracy; }
    public void setCbmAccuracy(double cbmAccuracy) { this.cbmAccuracy = cbmAccuracy; }

    public double getFieldCoverage() { return fieldCoverage; }
    public void setFieldCoverage(double fieldCoverage) { this.fieldCoverage = fieldCoverage; }

    public Map<String, Integer> getErrorCategoryCounts() { return errorCategoryCounts; }
    public void setErrorCategoryCounts(Map<String, Integer> errorCategoryCounts) { this.errorCategoryCounts = errorCategoryCounts; }

    public List<RowEvaluationDetail> getRowDetails() { return rowDetails; }
    public void setRowDetails(List<RowEvaluationDetail> rowDetails) { this.rowDetails = rowDetails; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
