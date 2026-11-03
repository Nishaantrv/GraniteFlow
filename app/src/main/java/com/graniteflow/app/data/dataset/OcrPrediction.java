package com.graniteflow.app.data.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OcrPrediction implements Serializable {
    private String predictionId;
    private String datasetRecordId;
    private String ocrEngine;
    private String parserVersion;
    private long createdAt;
    private String rawOcrText;
    private List<PredictedBlock> blocks;
    private String diagnosticsJson;

    public static class PredictedBlock implements Serializable {
        private String blockNumber;
        private double grossLengthCm;
        private double grossHeightCm;
        private double grossWidthCm;
        private double allowanceCm;
        private double netLengthCm;
        private double netHeightCm;
        private double netWidthCm;
        private double netCbm;
        private String remarks;
        private String rawRowText;
        private double confidence; // Genuine confidence if supplied, or -1.0 if not available

        public PredictedBlock() {
            this.confidence = -1.0;
        }

        public PredictedBlock(String blockNumber, double grossL, double grossH, double grossW, double allowance) {
            this.blockNumber = blockNumber;
            this.grossLengthCm = grossL;
            this.grossHeightCm = grossH;
            this.grossWidthCm = grossW;
            this.allowanceCm = allowance;
            this.netLengthCm = Math.max(0, grossL - allowance);
            this.netHeightCm = Math.max(0, grossH - allowance);
            this.netWidthCm = Math.max(0, grossW - allowance);
            this.netCbm = Math.round(((netLengthCm * netHeightCm * netWidthCm) / 1_000_000.0) * 1000.0) / 1000.0;
            this.confidence = -1.0;
        }

        public String getBlockNumber() { return blockNumber; }
        public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }

        public double getGrossLengthCm() { return grossLengthCm; }
        public void setGrossLengthCm(double grossLengthCm) { this.grossLengthCm = grossLengthCm; }

        public double getGrossHeightCm() { return grossHeightCm; }
        public void setGrossHeightCm(double grossHeightCm) { this.grossHeightCm = grossHeightCm; }

        public double getGrossWidthCm() { return grossWidthCm; }
        public void setGrossWidthCm(double grossWidthCm) { this.grossWidthCm = grossWidthCm; }

        public double getAllowanceCm() { return allowanceCm; }
        public void setAllowanceCm(double allowanceCm) { this.allowanceCm = allowanceCm; }

        public double getNetLengthCm() { return netLengthCm; }
        public void setNetLengthCm(double netLengthCm) { this.netLengthCm = netLengthCm; }

        public double getNetHeightCm() { return netHeightCm; }
        public void setNetHeightCm(double netHeightCm) { this.netHeightCm = netHeightCm; }

        public double getNetWidthCm() { return netWidthCm; }
        public void setNetWidthCm(double netWidthCm) { this.netWidthCm = netWidthCm; }

        public double getNetCbm() { return netCbm; }
        public void setNetCbm(double netCbm) { this.netCbm = netCbm; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }

        public String getRawRowText() { return rawRowText; }
        public void setRawRowText(String rawRowText) { this.rawRowText = rawRowText; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
    }

    public OcrPrediction() {
        this.createdAt = System.currentTimeMillis();
        this.blocks = new ArrayList<>();
    }

    public OcrPrediction(String predictionId, String datasetRecordId, String ocrEngine, String parserVersion) {
        this();
        this.predictionId = predictionId;
        this.datasetRecordId = datasetRecordId;
        this.ocrEngine = ocrEngine;
        this.parserVersion = parserVersion;
    }

    public String getPredictionId() { return predictionId; }
    public void setPredictionId(String predictionId) { this.predictionId = predictionId; }

    public String getDatasetRecordId() { return datasetRecordId; }
    public void setDatasetRecordId(String datasetRecordId) { this.datasetRecordId = datasetRecordId; }

    public String getOcrEngine() { return ocrEngine; }
    public void setOcrEngine(String ocrEngine) { this.ocrEngine = ocrEngine; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getRawOcrText() { return rawOcrText; }
    public void setRawOcrText(String rawOcrText) { this.rawOcrText = rawOcrText; }

    public List<PredictedBlock> getBlocks() { return blocks; }
    public void setBlocks(List<PredictedBlock> blocks) { this.blocks = blocks; }

    public String getDiagnosticsJson() { return diagnosticsJson; }
    public void setDiagnosticsJson(String diagnosticsJson) { this.diagnosticsJson = diagnosticsJson; }
}
