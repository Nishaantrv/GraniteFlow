package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Inspection implements Serializable {
    private String inspectionId;
    private String customer; // consignee
    private String consigneeShort;
    private String consigneeAddress;
    private String consigneePhone;
    private String buyer;
    private String notifyParty;

    // Logistics
    private String preCarriageBy;
    private String placeOfReceipt;
    private String portOfLoading;
    private String vessel;
    private String portOfDischarge;
    private String finalDestination;
    private String finalDestinationCountry;
    private String countryOfOrigin;

    // Commercial
    private String orderId;
    private String invoiceNumber;
    private String invoiceDate;
    private String exporterRefNumber;
    private String buyerOrderNoAndDate;
    private String hsCode;
    private String marksAndNos;
    private String currency; // "USD"
    private double pricePerCbm; // default base price
    private String termsOfDelivery;
    private String termsOfPayment;

    // Stone & Session Configuration
    private String date;
    private long createdAtTimestamp;
    private String inspectorName;
    private String locationCode; // quarryCode / Kuppam Code
    private String materialName; // stoneType
    private String stoneDescription;
    private double defaultAllowanceCm;
    private String remarks;
    private String approvalState; // "DRAFT", "COMPLETED", "APPROVED"

    private String calculationMode; // "net" (standard) or "gross" (round UP)
    private int startingBlockNumber; // default 1
    private boolean enableEndToEnd; // physical raw dims mode
    private String scanEngineUsed; // e.g. "Gemini Vision AI (gemini-3.8-flash)", "GraniteFlow Python OCR", "Google ML Kit (Offline Local OCR)", "Manual Entry"
    private String originalSlipImagePath;
    private String rawOcrPredictionJson;

    private List<BlockTypePreset> blockTypes;
    private List<Block> blocks;

    public Inspection() {
        this.inspectionId = "INSP-" + (System.currentTimeMillis() % 100000);
        this.createdAtTimestamp = System.currentTimeMillis();
        this.date = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH).format(new Date());
        this.invoiceDate = this.date;
        this.inspectorName = "Lead Inspector";
        this.customer = "";
        this.consigneeShort = "";
        this.consigneeAddress = "";
        this.consigneePhone = "";
        this.buyer = "";
        this.notifyParty = "";

        this.preCarriageBy = "ROAD";
        this.placeOfReceipt = "QUARRY";
        this.portOfLoading = "CHENNAI PORT";
        this.vessel = "";
        this.portOfDischarge = "";
        this.finalDestination = "";
        this.finalDestinationCountry = "";
        this.countryOfOrigin = "INDIA";

        this.invoiceNumber = "";
        this.exporterRefNumber = "";
        this.buyerOrderNoAndDate = "";
        this.hsCode = "";
        this.marksAndNos = "";
        this.locationCode = "";
        this.materialName = "";
        this.stoneDescription = "";
        this.defaultAllowanceCm = 10.0;
        this.remarks = "";
        this.approvalState = "DRAFT";

        this.calculationMode = "net";
        this.currency = "USD";
        this.pricePerCbm = 0.0;
        this.termsOfDelivery = "FOB";
        this.termsOfPayment = "LC AT SIGHT";
        this.startingBlockNumber = 1;
        this.enableEndToEnd = false;
        this.scanEngineUsed = "Manual Entry";

        this.blockTypes = new ArrayList<>();
        this.blockTypes.add(new BlockTypePreset("1", "T1", 15.0, 0.0, 1));
        this.blockTypes.add(new BlockTypePreset("2", "T2", 10.0, 0.0, 1));
        this.blockTypes.add(new BlockTypePreset("3", "T3", 20.0, 0.0, 1));

        this.blocks = new ArrayList<>();
    }

    public void applyCustomerDefaults(Customer c, CompanyProfile cp) {
        if (cp != null) {
            if (cp.getDefaultPortOfLoading() != null && !cp.getDefaultPortOfLoading().isEmpty()) {
                this.portOfLoading = cp.getDefaultPortOfLoading();
            }
            if (cp.getDefaultCurrency() != null && !cp.getDefaultCurrency().isEmpty()) {
                this.currency = cp.getDefaultCurrency();
            }
            if (cp.getCountry() != null && !cp.getCountry().isEmpty()) {
                this.countryOfOrigin = cp.getCountry();
            }
        }
        if (c != null) {
            this.customer = c.getCompanyName();
            this.consigneeShort = c.getCompanyName();
            String addr = c.getAddress();
            if (c.getCityProvince() != null && !c.getCityProvince().isEmpty()
                    && (addr == null || !addr.toLowerCase(Locale.ENGLISH).contains(c.getCityProvince().toLowerCase(Locale.ENGLISH)))) {
                addr = (addr == null || addr.isEmpty()) ? c.getCityProvince() : (addr + ", " + c.getCityProvince());
            }
            this.consigneeAddress = addr != null ? addr : "";
            this.consigneePhone = c.getPhone();
            this.buyer = c.getCompanyName();
            this.notifyParty = !c.getContactPerson().isEmpty() ? c.getContactPerson() : c.getCompanyName();
            if (!c.getDefaultPortOfLoading().isEmpty()) this.portOfLoading = c.getDefaultPortOfLoading();
            if (!c.getDefaultPortOfDischarge().isEmpty()) this.portOfDischarge = c.getDefaultPortOfDischarge();
            if (!c.getDefaultFinalDestination().isEmpty()) this.finalDestination = c.getDefaultFinalDestination();
            if (!c.getDefaultFinalDestinationCountry().isEmpty()) {
                this.finalDestinationCountry = c.getDefaultFinalDestinationCountry();
            } else if (!c.getCountry().isEmpty()) {
                this.finalDestinationCountry = c.getCountry();
            }
            if (!c.getDefaultHsCode().isEmpty()) this.hsCode = c.getDefaultHsCode();
            if (!c.getDefaultTermsOfDelivery().isEmpty()) this.termsOfDelivery = c.getDefaultTermsOfDelivery();
            if (!c.getDefaultTermsOfPayment().isEmpty()) this.termsOfPayment = c.getDefaultTermsOfPayment();
            if (!c.getDefaultCurrency().isEmpty()) this.currency = c.getDefaultCurrency();
        }
    }

    public Inspection createDuplicateHeaderSession() {
        Inspection dup = new Inspection();
        dup.setCustomer(this.customer);
        dup.setConsigneeShort(this.consigneeShort);
        dup.setConsigneeAddress(this.consigneeAddress);
        dup.setConsigneePhone(this.consigneePhone);
        dup.setBuyer(this.buyer);
        dup.setNotifyParty(this.notifyParty);
        dup.setPreCarriageBy(this.preCarriageBy);
        dup.setPlaceOfReceipt(this.placeOfReceipt);
        dup.setPortOfLoading(this.portOfLoading);
        dup.setVessel(this.vessel);
        dup.setPortOfDischarge(this.portOfDischarge);
        dup.setFinalDestination(this.finalDestination);
        dup.setFinalDestinationCountry(this.finalDestinationCountry);
        dup.setCountryOfOrigin(this.countryOfOrigin);
        dup.setHsCode(this.hsCode);
        dup.setMarksAndNos(this.marksAndNos);
        dup.setLocationCode(this.locationCode);
        dup.setMaterialName(this.materialName);
        dup.setStoneDescription(this.stoneDescription);
        dup.setCurrency(this.currency);
        dup.setPricePerCbm(this.pricePerCbm);
        dup.setTermsOfDelivery(this.termsOfDelivery);
        dup.setTermsOfPayment(this.termsOfPayment);
        dup.setDefaultAllowanceCm(this.defaultAllowanceCm);
        dup.setStartingBlockNumber(this.startingBlockNumber);
        dup.setCalculationMode(this.calculationMode);
        dup.setEnableEndToEnd(this.enableEndToEnd);
        dup.setApprovalState("DRAFT");

        List<BlockTypePreset> copiedTypes = new ArrayList<>();
        for (BlockTypePreset p : getBlockTypes()) {
            copiedTypes.add(new BlockTypePreset(p.getId(), p.getName(), p.getAllowance(), p.getPricePerCbm(), p.getStartingNumber()));
        }
        dup.setBlockTypes(copiedTypes);
        return dup;
    }

    public BlockTypePreset getPresetById(String typeId) {
        if (blockTypes != null) {
            for (BlockTypePreset p : blockTypes) {
                if (p.getId().equals(typeId)) return p;
            }
        }
        return null;
    }

    /**
     * Compute next sequential block number for a specific type.
     * If 0 blocks of this type exist, start at preset.startingNumber.
     * Else start at max(blockNo) + 1.
     */
    public int getNextBlockNumber(String typeId) {
        BlockTypePreset preset = getPresetById(typeId);
        int startingNo = (preset != null && preset.getStartingNumber() > 0)
                ? preset.getStartingNumber()
                : (startingBlockNumber > 0 ? startingBlockNumber : 1);

        int maxNo = -1;
        if (blocks != null) {
            for (Block b : blocks) {
                if (typeId.equals(b.getType())) {
                    int num = b.getNumericBlockNo();
                    if (num > maxNo) {
                        maxNo = num;
                    }
                }
            }
        }

        if (maxNo < 0) {
            return startingNo;
        }
        return maxNo + 1;
    }

    /**
     * Easy Backtrack & Retroactive Recalculation:
     * When preset allowance, price, or starting number changes,
     * immediately update and recalculate all existing blocks of this type.
     */
    public void updateBlockTypePreset(BlockTypePreset updated) {
        if (blockTypes == null) blockTypes = new ArrayList<>();
        BlockTypePreset existing = getPresetById(updated.getId());
        boolean startingNumberChanged = false;

        if (existing != null) {
            startingNumberChanged = existing.getStartingNumber() != updated.getStartingNumber();
            existing.setName(updated.getName());
            existing.setAllowance(updated.getAllowance());
            existing.setPricePerCbm(updated.getPricePerCbm());
            existing.setStartingNumber(updated.getStartingNumber());
        } else {
            blockTypes.add(updated);
        }

        // Recalculate blocks of this type
        if (blocks != null) {
            int seq = updated.getStartingNumber();
            for (Block b : blocks) {
                if (updated.getId().equals(b.getType())) {
                    if (startingNumberChanged) {
                        b.setBlockNumber(String.valueOf(seq++));
                    }
                    b.setAllowanceCm(updated.getAllowance());
                    b.setPricePerCbm(updated.getPricePerCbm() > 0 ? updated.getPricePerCbm() : this.pricePerCbm);
                    b.setCalculationMode(this.calculationMode);
                    b.recalculate();
                }
            }
        }
    }

    public void recalculateAllBlocks(boolean renumberFromStart) {
        if (blocks == null) return;
        if (renumberFromStart) {
            int seq = startingBlockNumber > 0 ? startingBlockNumber : 1;
            for (Block b : blocks) {
                b.setBlockNumber(String.valueOf(seq++));
            }
        } else {
            com.graniteflow.app.engine.BlockNumberRuleEngine.enforceSequentialBlockNumbers(blocks, startingBlockNumber > 0 ? startingBlockNumber : 101);
        }
        for (Block b : blocks) {
            BlockTypePreset preset = getPresetById(b.getType());
            if (preset != null) {
                b.setAllowanceCm(preset.getAllowance());
                b.setPricePerCbm(preset.getPricePerCbm() > 0 ? preset.getPricePerCbm() : this.pricePerCbm);
            } else {
                b.setAllowanceCm(defaultAllowanceCm);
                b.setPricePerCbm(this.pricePerCbm);
            }
            b.setCalculationMode(this.calculationMode);
            b.recalculate();
        }
    }

    public double calculateTotalGrossCbm() {
        double sum = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                sum += b.getGrossCbm();
            }
        }
        return Math.round(sum * 1000.0) / 1000.0;
    }

    public double calculateTotalNetCbm() {
        double sum = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                sum += b.getNetCbm();
            }
        }
        return Math.round(sum * 1000.0) / 1000.0;
    }

    public double calculateTotalEndToEndCbm() {
        double sum = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                double l1 = b.getL1EndToEnd() > 0 ? b.getL1EndToEnd() : b.getGrossLengthCm();
                double l2 = b.getL2EndToEnd() > 0 ? b.getL2EndToEnd() : b.getGrossHeightCm();
                double l3 = b.getL3EndToEnd() > 0 ? b.getL3EndToEnd() : b.getGrossWidthCm();
                sum += (l1 * l2 * l3) / 1_000_000.0;
            }
        }
        return Math.round(sum * 1000.0) / 1000.0;
    }

    public double calculateTotalValue() {
        double sum = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                double rate = b.getPricePerCbm() > 0 ? b.getPricePerCbm() : this.pricePerCbm;
                sum += b.getNetCbm() * rate;
            }
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    public int getTotalBlocksCount() {
        return blocks != null ? blocks.size() : 0;
    }

    // Getters and Setters
    public String getInspectionId() { return inspectionId; }
    public void setInspectionId(String inspectionId) { this.inspectionId = inspectionId; }

    public String getCustomer() { return customer != null ? customer : ""; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getConsigneeShort() { return consigneeShort != null ? consigneeShort : ""; }
    public void setConsigneeShort(String consigneeShort) { this.consigneeShort = consigneeShort; }

    public String getConsigneeAddress() { return consigneeAddress != null ? consigneeAddress : ""; }
    public void setConsigneeAddress(String consigneeAddress) { this.consigneeAddress = consigneeAddress; }

    public String getConsigneePhone() { return consigneePhone != null ? consigneePhone : ""; }
    public void setConsigneePhone(String consigneePhone) { this.consigneePhone = consigneePhone; }

    public String getBuyer() { return buyer != null ? buyer : ""; }
    public void setBuyer(String buyer) { this.buyer = buyer; }

    public String getNotifyParty() { return notifyParty != null ? notifyParty : ""; }
    public void setNotifyParty(String notifyParty) { this.notifyParty = notifyParty; }

    public String getPreCarriageBy() { return preCarriageBy != null ? preCarriageBy : ""; }
    public void setPreCarriageBy(String preCarriageBy) { this.preCarriageBy = preCarriageBy; }

    public String getPlaceOfReceipt() { return placeOfReceipt != null ? placeOfReceipt : ""; }
    public void setPlaceOfReceipt(String placeOfReceipt) { this.placeOfReceipt = placeOfReceipt; }

    public String getPortOfLoading() { return portOfLoading != null ? portOfLoading : ""; }
    public void setPortOfLoading(String portOfLoading) { this.portOfLoading = portOfLoading; }

    public String getVessel() { return vessel != null ? vessel : ""; }
    public void setVessel(String vessel) { this.vessel = vessel; }

    public String getPortOfDischarge() { return portOfDischarge != null ? portOfDischarge : ""; }
    public void setPortOfDischarge(String portOfDischarge) { this.portOfDischarge = portOfDischarge; }

    public String getFinalDestination() { return finalDestination != null ? finalDestination : ""; }
    public void setFinalDestination(String finalDestination) { this.finalDestination = finalDestination; }

    public String getFinalDestinationCountry() { return finalDestinationCountry != null ? finalDestinationCountry : ""; }
    public void setFinalDestinationCountry(String finalDestinationCountry) { this.finalDestinationCountry = finalDestinationCountry; }

    public String getCountryOfOrigin() { return countryOfOrigin != null && !countryOfOrigin.isEmpty() ? countryOfOrigin : "INDIA"; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getOrderId() { return orderId != null ? orderId : ""; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getInvoiceNumber() { return invoiceNumber != null ? invoiceNumber : ""; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getInvoiceDate() { return invoiceDate != null ? invoiceDate : getDate(); }
    public void setInvoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getExporterRefNumber() { return exporterRefNumber != null ? exporterRefNumber : ""; }
    public void setExporterRefNumber(String exporterRefNumber) { this.exporterRefNumber = exporterRefNumber; }

    public String getBuyerOrderNoAndDate() { return buyerOrderNoAndDate != null ? buyerOrderNoAndDate : ""; }
    public void setBuyerOrderNoAndDate(String buyerOrderNoAndDate) { this.buyerOrderNoAndDate = buyerOrderNoAndDate; }

    public String getHsCode() { return hsCode != null ? hsCode : ""; }
    public void setHsCode(String hsCode) { this.hsCode = hsCode; }

    public String getMarksAndNos() { return marksAndNos != null ? marksAndNos : ""; }
    public void setMarksAndNos(String marksAndNos) { this.marksAndNos = marksAndNos; }

    public String getTermsOfDelivery() { return termsOfDelivery != null ? termsOfDelivery : "FOB"; }
    public void setTermsOfDelivery(String termsOfDelivery) { this.termsOfDelivery = termsOfDelivery; }

    public String getTermsOfPayment() { return termsOfPayment != null ? termsOfPayment : ""; }
    public void setTermsOfPayment(String termsOfPayment) { this.termsOfPayment = termsOfPayment; }

    public String getDate() { return date != null ? date : "14.09.2026"; }
    public void setDate(String date) { this.date = date; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp > 0 ? createdAtTimestamp : System.currentTimeMillis(); }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public String getInspectorName() { return inspectorName != null ? inspectorName : ""; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public String getLocationCode() { return locationCode != null ? locationCode : ""; }
    public void setLocationCode(String locationCode) { this.locationCode = locationCode; }

    public String getMaterialName() { return materialName != null ? materialName : ""; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getStoneDescription() { return stoneDescription != null ? stoneDescription : ""; }
    public void setStoneDescription(String stoneDescription) { this.stoneDescription = stoneDescription; }

    public double getDefaultAllowanceCm() { return defaultAllowanceCm; }
    public void setDefaultAllowanceCm(double defaultAllowanceCm) { this.defaultAllowanceCm = defaultAllowanceCm; }

    public String getRemarks() { return remarks != null ? remarks : ""; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getApprovalState() { return approvalState != null ? approvalState : "DRAFT"; }
    public void setApprovalState(String approvalState) { this.approvalState = approvalState; }

    public String getCalculationMode() { return calculationMode != null ? calculationMode : "net"; }
    public void setCalculationMode(String calculationMode) {
        this.calculationMode = calculationMode;
        if (blocks != null) {
            for (Block b : blocks) {
                b.setCalculationMode(calculationMode);
                b.recalculate();
            }
        }
    }

    public String getCurrency() { return currency != null && !currency.isEmpty() ? currency : "USD"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getPricePerCbm() { return pricePerCbm; }
    public void setPricePerCbm(double pricePerCbm) { this.pricePerCbm = pricePerCbm; }

    public int getStartingBlockNumber() { return startingBlockNumber > 0 ? startingBlockNumber : 1; }
    public void setStartingBlockNumber(int startingBlockNumber) { this.startingBlockNumber = startingBlockNumber; }

    public boolean isEnableEndToEnd() { return enableEndToEnd; }
    public void setEnableEndToEnd(boolean enableEndToEnd) { this.enableEndToEnd = enableEndToEnd; }

    public String getScanEngineUsed() {
        return (scanEngineUsed != null && !scanEngineUsed.isEmpty()) ? scanEngineUsed : "Manual Entry";
    }
    public void setScanEngineUsed(String scanEngineUsed) {
        this.scanEngineUsed = scanEngineUsed;
    }

    public String getOriginalSlipImagePath() { return originalSlipImagePath; }
    public void setOriginalSlipImagePath(String originalSlipImagePath) { this.originalSlipImagePath = originalSlipImagePath; }

    public String getRawOcrPredictionJson() { return rawOcrPredictionJson; }
    public void setRawOcrPredictionJson(String rawOcrPredictionJson) { this.rawOcrPredictionJson = rawOcrPredictionJson; }

    public List<BlockTypePreset> getBlockTypes() {
        if (blockTypes == null || blockTypes.isEmpty()) {
            blockTypes = new ArrayList<>();
            blockTypes.add(new BlockTypePreset("1", "T1", defaultAllowanceCm > 0 ? defaultAllowanceCm : 15.0, pricePerCbm, getStartingBlockNumber()));
            blockTypes.add(new BlockTypePreset("2", "T2", 10.0, pricePerCbm, getStartingBlockNumber()));
            blockTypes.add(new BlockTypePreset("3", "T3", 20.0, pricePerCbm, getStartingBlockNumber()));
        }
        return blockTypes;
    }
    public void setBlockTypes(List<BlockTypePreset> blockTypes) { this.blockTypes = blockTypes; }

    public List<Block> getBlocks() {
        if (blocks == null) blocks = new ArrayList<>();
        return blocks;
    }
    public void setBlocks(List<Block> blocks) { this.blocks = blocks; }
}
