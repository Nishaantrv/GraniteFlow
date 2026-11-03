package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PackingListDocument implements Serializable {
    private String documentTitle; // "PACKING LIST"
    private String invoiceNumber;
    private String date;
    private String exporterRefNo;
    private String buyerOrderNo;
    private String buyerOrderDate;
    private String otherReferences;

    private CompanyProfile exporter;
    private Customer consignee;
    private String buyerIfDifferent;

    private String preCarriageBy;
    private String placeOfReceipt;
    private String countryOfOrigin;
    private String countryOfDestination;
    private String vesselFlightNo;
    private String portOfLoading;
    private String termsOfDeliveryPayment;
    private String portOfDischarge;
    private String finalDestination;

    private String descriptionHeader;
    private List<PackingListRow> rows;

    private int totalBlocksCount;
    private double totalCbm;
    private double totalWeightMt;
    private String signatureTitle;

    private String status; // "DRAFT", "NEEDS_REVIEW", "APPROVED", "ISSUED"
    private int version;
    private long lastModified;

    public PackingListDocument() {
        this.documentTitle = "PACKING LIST";
        this.invoiceNumber = "";
        this.date = "";
        this.exporter = new CompanyProfile();
        this.consignee = new Customer();
        this.countryOfOrigin = "INDIA";
        this.countryOfDestination = "";
        this.portOfLoading = "";
        this.termsOfDeliveryPayment = "";
        this.portOfDischarge = "";
        this.finalDestination = "";
        this.descriptionHeader = "";
        this.signatureTitle = "";
        this.rows = new ArrayList<>();
        this.status = "DRAFT";
        this.version = 1;
        this.lastModified = System.currentTimeMillis();
    }

    public void recalculateTotals() {
        double cbmSum = 0;
        double weightSum = 0;
        if (rows != null) {
            for (PackingListRow row : rows) {
                cbmSum += row.getQuantityCbm();
                weightSum += row.getWeightMt();
            }
            this.totalBlocksCount = rows.size();
        } else {
            this.totalBlocksCount = 0;
        }
        this.totalCbm = Math.round(cbmSum * 1000.0) / 1000.0;
        this.totalWeightMt = Math.round(weightSum * 1000.0) / 1000.0;
    }

    // Getters and Setters
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getExporterRefNo() { return exporterRefNo; }
    public void setExporterRefNo(String exporterRefNo) { this.exporterRefNo = exporterRefNo; }

    public String getBuyerOrderNo() { return buyerOrderNo; }
    public void setBuyerOrderNo(String buyerOrderNo) { this.buyerOrderNo = buyerOrderNo; }

    public String getBuyerOrderDate() { return buyerOrderDate; }
    public void setBuyerOrderDate(String buyerOrderDate) { this.buyerOrderDate = buyerOrderDate; }

    public String getOtherReferences() { return otherReferences; }
    public void setOtherReferences(String otherReferences) { this.otherReferences = otherReferences; }

    public CompanyProfile getExporter() { return exporter; }
    public void setExporter(CompanyProfile exporter) { this.exporter = exporter; }

    public Customer getConsignee() { return consignee; }
    public void setConsignee(Customer consignee) { this.consignee = consignee; }

    public String getBuyerIfDifferent() { return buyerIfDifferent; }
    public void setBuyerIfDifferent(String buyerIfDifferent) { this.buyerIfDifferent = buyerIfDifferent; }

    public String getPreCarriageBy() { return preCarriageBy; }
    public void setPreCarriageBy(String preCarriageBy) { this.preCarriageBy = preCarriageBy; }

    public String getPlaceOfReceipt() { return placeOfReceipt; }
    public void setPlaceOfReceipt(String placeOfReceipt) { this.placeOfReceipt = placeOfReceipt; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getCountryOfDestination() { return countryOfDestination; }
    public void setCountryOfDestination(String countryOfDestination) { this.countryOfDestination = countryOfDestination; }

    public String getVesselFlightNo() { return vesselFlightNo; }
    public void setVesselFlightNo(String vesselFlightNo) { this.vesselFlightNo = vesselFlightNo; }

    public String getPortOfLoading() { return portOfLoading; }
    public void setPortOfLoading(String portOfLoading) { this.portOfLoading = portOfLoading; }

    public String getTermsOfDeliveryPayment() { return termsOfDeliveryPayment; }
    public void setTermsOfDeliveryPayment(String termsOfDeliveryPayment) { this.termsOfDeliveryPayment = termsOfDeliveryPayment; }

    public String getPortOfDischarge() { return portOfDischarge; }
    public void setPortOfDischarge(String portOfDischarge) { this.portOfDischarge = portOfDischarge; }

    public String getFinalDestination() { return finalDestination; }
    public void setFinalDestination(String finalDestination) { this.finalDestination = finalDestination; }

    public String getDescriptionHeader() { return descriptionHeader; }
    public void setDescriptionHeader(String descriptionHeader) { this.descriptionHeader = descriptionHeader; }

    public List<PackingListRow> getRows() {
        if (rows == null) rows = new ArrayList<>();
        return rows;
    }
    public void setRows(List<PackingListRow> rows) {
        this.rows = rows;
        recalculateTotals();
    }

    public int getTotalBlocksCount() { return totalBlocksCount; }
    public void setTotalBlocksCount(int totalBlocksCount) { this.totalBlocksCount = totalBlocksCount; }

    public double getTotalCbm() { return totalCbm; }
    public void setTotalCbm(double totalCbm) { this.totalCbm = totalCbm; }

    public double getTotalWeightMt() { return totalWeightMt; }
    public void setTotalWeightMt(double totalWeightMt) { this.totalWeightMt = totalWeightMt; }

    public String getSignatureTitle() { return signatureTitle; }
    public void setSignatureTitle(String signatureTitle) { this.signatureTitle = signatureTitle; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}
