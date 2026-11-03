package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompanyProfile implements Serializable {
    private String companyName;
    private String addressLine1;
    private String addressLine2;
    private String cityPincode;
    private String country;
    private String rbiCode;
    private String iecCode;
    private String gstin;
    private String lutNumber;
    private List<String> otherReferences;
    private String bankName;
    private String bankBranch;
    private String bankAddress;
    private String accountHolder;
    private String accountNumber;
    private String swiftCode;
    private String defaultTerms;
    private String defaultCurrency;
    private String defaultPortOfLoading;
    private String signatureTitle;
    private String signatureImagePath;

    public CompanyProfile() {
        this.companyName = "";
        this.addressLine1 = "";
        this.addressLine2 = "";
        this.cityPincode = "";
        this.country = "";
        this.rbiCode = "";
        this.iecCode = "";
        this.gstin = "";
        this.lutNumber = "";
        this.otherReferences = new ArrayList<>();
        this.bankName = "";
        this.bankBranch = "";
        this.bankAddress = "";
        this.accountHolder = "";
        this.accountNumber = "";
        this.swiftCode = "";
        this.defaultTerms = "";
        this.defaultCurrency = "USD";
        this.defaultPortOfLoading = "";
        this.signatureTitle = "";
        this.signatureImagePath = "";
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (companyName != null && !companyName.isEmpty()) sb.append(companyName).append("\n");
        if (addressLine1 != null && !addressLine1.isEmpty()) sb.append(addressLine1).append("\n");
        if (addressLine2 != null && !addressLine2.isEmpty()) sb.append(addressLine2).append("\n");
        if (cityPincode != null && !cityPincode.isEmpty()) sb.append(cityPincode).append("\n");
        if (country != null && !country.isEmpty()) sb.append(country);
        return sb.toString().trim();
    }

    public String getFormattedOtherReferences() {
        if (otherReferences != null && !otherReferences.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String ref : otherReferences) {
                if (ref != null && !ref.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(ref.trim());
                }
            }
            if (sb.length() > 0) return sb.toString();
        }
        return "RBI CODE NO: " + (rbiCode != null ? rbiCode : "") + "\n" +
               "IE CODE NO : " + (iecCode != null ? iecCode : "") + "\n" +
               "GSTIN NO: " + (gstin != null ? gstin : "");
    }

    public String getFormattedBankDetails() {
        return "BANK DETAILS :\n" +
               (bankName != null ? bankName : "") + "\n" +
               (bankBranch != null ? bankBranch : "") + "\n" +
               (bankAddress != null ? bankAddress : "") + "\n" +
               "ACCOUNT : " + (accountHolder != null ? accountHolder : "") + "\n" +
               "A/C.NO." + (accountNumber != null ? accountNumber : "") + "\n" +
               "SWIFT: " + (swiftCode != null ? swiftCode : "");
    }

    // Getters and Setters
    public String getCompanyName() { return companyName != null ? companyName : ""; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getAddressLine1() { return addressLine1 != null ? addressLine1 : ""; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2 != null ? addressLine2 : ""; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCityPincode() { return cityPincode != null ? cityPincode : ""; }
    public void setCityPincode(String cityPincode) { this.cityPincode = cityPincode; }

    public String getCountry() { return country != null ? country : "INDIA"; }
    public void setCountry(String country) { this.country = country; }

    public String getRbiCode() { return rbiCode != null ? rbiCode : ""; }
    public void setRbiCode(String rbiCode) { this.rbiCode = rbiCode; }

    public String getIecCode() { return iecCode != null ? iecCode : ""; }
    public void setIecCode(String iecCode) { this.iecCode = iecCode; }

    public String getGstin() { return gstin != null ? gstin : ""; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getLutNumber() { return lutNumber != null ? lutNumber : ""; }
    public void setLutNumber(String lutNumber) { this.lutNumber = lutNumber; }

    public List<String> getOtherReferences() {
        if (otherReferences == null) otherReferences = new ArrayList<>();
        return otherReferences;
    }
    public void setOtherReferences(List<String> otherReferences) { this.otherReferences = otherReferences; }

    public String getBankName() { return bankName != null ? bankName : ""; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankBranch() { return bankBranch != null ? bankBranch : ""; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }

    public String getBankAddress() { return bankAddress != null ? bankAddress : ""; }
    public void setBankAddress(String bankAddress) { this.bankAddress = bankAddress; }

    public String getAccountHolder() { return accountHolder != null ? accountHolder : ""; }
    public void setAccountHolder(String accountHolder) { this.accountHolder = accountHolder; }

    public String getAccountNumber() { return accountNumber != null ? accountNumber : ""; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getSwiftCode() { return swiftCode != null ? swiftCode : ""; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getDefaultTerms() { return defaultTerms != null ? defaultTerms : ""; }
    public void setDefaultTerms(String defaultTerms) { this.defaultTerms = defaultTerms; }

    public String getDefaultCurrency() { return defaultCurrency != null && !defaultCurrency.isEmpty() ? defaultCurrency : "USD"; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }

    public String getDefaultPortOfLoading() { return defaultPortOfLoading != null ? defaultPortOfLoading : ""; }
    public void setDefaultPortOfLoading(String defaultPortOfLoading) { this.defaultPortOfLoading = defaultPortOfLoading; }

    public String getSignatureTitle() { return signatureTitle != null ? signatureTitle : ""; }
    public void setSignatureTitle(String signatureTitle) { this.signatureTitle = signatureTitle; }

    public String getSignatureImagePath() { return signatureImagePath != null ? signatureImagePath : ""; }
    public void setSignatureImagePath(String signatureImagePath) { this.signatureImagePath = signatureImagePath; }
}
