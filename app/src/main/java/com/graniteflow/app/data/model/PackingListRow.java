package com.graniteflow.app.data.model;

import java.io.Serializable;

public class PackingListRow implements Serializable {
    private String blockNumber;
    private double lengthCm;
    private double heightCm;
    private double widthCm;
    private double quantityCbm;
    private double weightMt;
    private String description;

    public PackingListRow() {}

    public PackingListRow(String blockNumber, double length, double height, double width) {
        this.blockNumber = blockNumber;
        this.lengthCm = length;
        this.heightCm = height;
        this.widthCm = width;
        this.quantityCbm = Math.round(((length * height * width) / 1_000_000.0) * 1000.0) / 1000.0;
        this.weightMt = 0.0;
    }

    public PackingListRow(String blockNumber, double length, double height, double width, double quantityCbm) {
        this.blockNumber = blockNumber;
        this.lengthCm = length;
        this.heightCm = height;
        this.widthCm = width;
        this.quantityCbm = quantityCbm;
        this.weightMt = 0.0;
    }

    public String getFormattedDimensions() {
        return (int) lengthCm + " x " + (int) heightCm + " x " + (int) widthCm;
    }

    public String getBlockNumber() { return blockNumber; }
    public void setBlockNumber(String blockNumber) { this.blockNumber = blockNumber; }

    public double getLengthCm() { return lengthCm; }
    public void setLengthCm(double lengthCm) { this.lengthCm = lengthCm; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWidthCm() { return widthCm; }
    public void setWidthCm(double widthCm) { this.widthCm = widthCm; }

    public double getQuantityCbm() { return quantityCbm; }
    public void setQuantityCbm(double quantityCbm) { this.quantityCbm = quantityCbm; }

    public double getWeightMt() { return weightMt; }
    public void setWeightMt(double weightMt) { this.weightMt = weightMt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
