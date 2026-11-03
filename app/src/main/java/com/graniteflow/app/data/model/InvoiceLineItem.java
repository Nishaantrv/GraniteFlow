package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InvoiceLineItem implements Serializable {
    private String id;
    private String marksAndNos; // e.g. "Block no. 138, 139, 140, 142, 144 to 151, 153, 159"
    private String pkgKind;
    private String description;
    private double quantityCbm;
    private double ratePerCbm;
    private double amount;
    private List<String> associatedBlockNumbers;
    private boolean manualAmountOverride;

    public InvoiceLineItem() {
        this.id = String.valueOf(System.currentTimeMillis()) + "_" + Math.round(Math.random() * 1000);
        this.associatedBlockNumbers = new ArrayList<>();
    }

    public InvoiceLineItem(String marksAndNos, double quantityCbm, double ratePerCbm) {
        this.id = String.valueOf(System.currentTimeMillis()) + "_" + Math.round(Math.random() * 1000);
        this.marksAndNos = marksAndNos;
        this.quantityCbm = quantityCbm;
        this.ratePerCbm = ratePerCbm;
        this.amount = Math.round(quantityCbm * ratePerCbm * 100.0) / 100.0;
        this.associatedBlockNumbers = new ArrayList<>();
        this.manualAmountOverride = false;
    }

    public void recalculate() {
        if (!manualAmountOverride) {
            this.amount = Math.round(quantityCbm * ratePerCbm * 100.0) / 100.0;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMarksAndNos() { return marksAndNos; }
    public void setMarksAndNos(String marksAndNos) { this.marksAndNos = marksAndNos; }

    public String getPkgKind() { return pkgKind; }
    public void setPkgKind(String pkgKind) { this.pkgKind = pkgKind; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getQuantityCbm() { return quantityCbm; }
    public void setQuantityCbm(double quantityCbm) {
        this.quantityCbm = quantityCbm;
        recalculate();
    }

    public double getRatePerCbm() { return ratePerCbm; }
    public void setRatePerCbm(double ratePerCbm) {
        this.ratePerCbm = ratePerCbm;
        recalculate();
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = amount;
        this.manualAmountOverride = true;
    }

    public List<String> getAssociatedBlockNumbers() {
        if (associatedBlockNumbers == null) associatedBlockNumbers = new ArrayList<>();
        return associatedBlockNumbers;
    }
    public void setAssociatedBlockNumbers(List<String> associatedBlockNumbers) {
        this.associatedBlockNumbers = associatedBlockNumbers;
    }

    public boolean isManualAmountOverride() { return manualAmountOverride; }
    public void setManualAmountOverride(boolean manualAmountOverride) {
        this.manualAmountOverride = manualAmountOverride;
    }
}
