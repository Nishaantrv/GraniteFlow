package com.graniteflow.app.data.model;

import java.io.Serializable;

public class BlockTypePreset implements Serializable {
    private String id; // "1", "2", "3"
    private String name; // "T1", "T2", "FRESH", "LOT"
    private double allowance; // e.g. 15.0
    private double pricePerCbm; // e.g. 450.0
    private int startingNumber; // e.g. 1 or 301

    public BlockTypePreset() {
        this.id = "1";
        this.name = "T1";
        this.allowance = 15.0;
        this.pricePerCbm = 0.0;
        this.startingNumber = 1;
    }

    public BlockTypePreset(String id, String name, double allowance, double pricePerCbm, int startingNumber) {
        this.id = id;
        this.name = name;
        this.allowance = allowance;
        this.pricePerCbm = pricePerCbm;
        this.startingNumber = startingNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : "T" + id; }
    public void setName(String name) { this.name = name; }

    public double getAllowance() { return allowance; }
    public void setAllowance(double allowance) { this.allowance = allowance; }

    public double getPricePerCbm() { return pricePerCbm; }
    public void setPricePerCbm(double pricePerCbm) { this.pricePerCbm = pricePerCbm; }

    public int getStartingNumber() { return startingNumber; }
    public void setStartingNumber(int startingNumber) { this.startingNumber = startingNumber; }
}
