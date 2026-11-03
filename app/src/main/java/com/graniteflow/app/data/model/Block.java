package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Block implements Serializable {
    private String id;
    private String blockNumber;
    private double grossLengthCm;
    private double grossHeightCm;
    private double grossWidthCm;
    private double allowanceCm;

    private String type; // "1", "2", "3" (e.g. T1, T2)
    private double pricePerCbm;
    private double value;
    private String calculationMode; // "net" or "gross"

    private double netLengthCm;
    private double netHeightCm;
    private double netWidthCm;

    private double grossCbm;
    private double netCbm;

    private String remarks;
    private List<BlockPhoto> photos;

    // Optional End-to-End measurements
    private double l1EndToEnd;
    private double l2EndToEnd;
    private double l3EndToEnd;
    private String defectDescription;
    private boolean isMathVerified;
    private double confidence = 0.95;

    public boolean isMathVerified() {
        return isMathVerified;
    }

    public void setMathVerified(boolean mathVerified) {
        isMathVerified = mathVerified;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Block() {
        this.id = UUID.randomUUID().toString();
        this.photos = new ArrayList<>();
        this.allowanceCm = 15.0;
        this.type = "1";
        this.pricePerCbm = 0.0;
        this.value = 0.0;
        this.calculationMode = "net";
        this.remarks = "";
    }

    public Block(String blockNumber, double grossLength, double grossHeight, double grossWidth, double allowance) {
        this.id = UUID.randomUUID().toString();
        this.blockNumber = blockNumber;
        this.grossLengthCm = grossLength;
        this.grossHeightCm = grossHeight;
        this.grossWidthCm = grossWidth;
        this.allowanceCm = allowance;
        this.type = "1";
        this.pricePerCbm = 0.0;
        this.value = 0.0;
        this.calculationMode = "net";
        this.photos = new ArrayList<>();
        this.remarks = "";
        recalculate();
    }

    public Block(String blockNumber, double l1, double l2, double l3, double allowance, String type, double pricePerCbm, String calculationMode) {
        this.id = UUID.randomUUID().toString();
        this.blockNumber = blockNumber;
        this.grossLengthCm = l1;
        this.grossHeightCm = l2;
        this.grossWidthCm = l3;
        this.allowanceCm = allowance;
        this.type = type != null ? type : "1";
        this.pricePerCbm = pricePerCbm;
        this.calculationMode = calculationMode != null ? calculationMode : "net";
        this.photos = new ArrayList<>();
        this.remarks = "";
        recalculate();
    }

    /**
     * Business calculation rule:
     * - Gross CBM: (l1 * l2 * l3) / 1,000,000.0
     * - If calculationMode == "gross": billable CBM is Gross CBM rounded UP to 3 decimals
     * - If calculationMode == "net": net dimensions = max(0, gross - allowance), CBM = (netL * netH * netW) / 1,000,000.0
     * - Value = netCbm * pricePerCbm
     */
    public void recalculate() {
        this.grossCbm = Math.round(((grossLengthCm * grossHeightCm * grossWidthCm) / 1_000_000.0) * 1000.0) / 1000.0;

        if ("gross".equalsIgnoreCase(calculationMode)) {
            // Gross mode: Round UP (ceiling) to 3 decimals
            this.netLengthCm = grossLengthCm;
            this.netHeightCm = grossHeightCm;
            this.netWidthCm = grossWidthCm;
            this.netCbm = Math.ceil(grossCbm * 1000.0) / 1000.0;
        } else {
            // Standard Net mode with allowance
            this.netLengthCm = Math.max(0, grossLengthCm - allowanceCm);
            this.netHeightCm = Math.max(0, grossHeightCm - allowanceCm);
            this.netWidthCm = Math.max(0, grossWidthCm - allowanceCm);
            this.netCbm = Math.round(((netLengthCm * netHeightCm * netWidthCm) / 1_000_000.0) * 1000.0) / 1000.0;
        }

        this.value = Math.round(netCbm * pricePerCbm * 100.0) / 100.0;
    }

    public String getFormattedNetMeasurement() {
        return (int) netLengthCm + " x " + (int) netHeightCm + " x " + (int) netWidthCm;
    }

    public String getFormattedGrossMeasurement() {
        return (int) grossLengthCm + " x " + (int) grossHeightCm + " x " + (int) grossWidthCm;
    }

    // Getters and Setters
    public String getId() { return id != null ? id : blockNumber; }
    public void setId(String id) { this.id = id; }

    public String getBlockNumber() { return blockNumber; }
    public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }

    public int getNumericBlockNo() {
        try {
            return Integer.parseInt(blockNumber);
        } catch (Exception e) {
            String digits = com.graniteflow.app.engine.BlockNumberRuleEngine.cleanToDigits(blockNumber);
            if (digits != null) {
                try {
                    return Integer.parseInt(digits);
                } catch (Exception ignored) {}
            }
            return 0;
        }
    }

    public double getGrossLengthCm() { return grossLengthCm; }
    public void setGrossLengthCm(double grossLengthCm) {
        this.grossLengthCm = grossLengthCm;
        recalculate();
    }

    public double getGrossHeightCm() { return grossHeightCm; }
    public void setGrossHeightCm(double grossHeightCm) {
        this.grossHeightCm = grossHeightCm;
        recalculate();
    }

    public double getGrossWidthCm() { return grossWidthCm; }
    public void setGrossWidthCm(double grossWidthCm) {
        this.grossWidthCm = grossWidthCm;
        recalculate();
    }

    public double getAllowanceCm() { return allowanceCm; }
    public void setAllowanceCm(double allowanceCm) {
        this.allowanceCm = allowanceCm;
        recalculate();
    }

    public String getType() { return type != null ? type : "1"; }
    public void setType(String type) { this.type = type; }

    public double getPricePerCbm() { return pricePerCbm; }
    public void setPricePerCbm(double pricePerCbm) {
        this.pricePerCbm = pricePerCbm;
        recalculate();
    }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getCalculationMode() { return calculationMode != null ? calculationMode : "net"; }
    public void setCalculationMode(String calculationMode) {
        this.calculationMode = calculationMode;
        recalculate();
    }

    public double getNetLengthCm() { return netLengthCm; }
    public double getNetHeightCm() { return netHeightCm; }
    public double getNetWidthCm() { return netWidthCm; }

    public double getGrossCbm() { return grossCbm; }
    public void setGrossCbm(double grossCbm) { this.grossCbm = grossCbm; }

    public double getNetCbm() { return netCbm; }
    public void setNetCbm(double netCbm) { this.netCbm = netCbm; }

    public String getRemarks() { return remarks != null ? remarks : ""; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public List<BlockPhoto> getPhotos() {
        if (photos == null) photos = new ArrayList<>();
        return photos;
    }
    public void setPhotos(List<BlockPhoto> photos) { this.photos = photos; }

    public double getL1EndToEnd() { return l1EndToEnd; }
    public void setL1EndToEnd(double l1EndToEnd) { this.l1EndToEnd = l1EndToEnd; }

    public double getL2EndToEnd() { return l2EndToEnd; }
    public void setL2EndToEnd(double l2EndToEnd) { this.l2EndToEnd = l2EndToEnd; }

    public double getL3EndToEnd() { return l3EndToEnd; }
    public void setL3EndToEnd(double l3EndToEnd) { this.l3EndToEnd = l3EndToEnd; }

    public String getDefectDescription() { return defectDescription; }
    public void setDefectDescription(String defectDescription) { this.defectDescription = defectDescription; }
}
