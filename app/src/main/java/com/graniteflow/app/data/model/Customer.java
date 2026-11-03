package com.graniteflow.app.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Customer implements Serializable {
    private String id;
    private String companyName;
    private String address;
    private String cityProvince;
    private String country;
    private String contactPerson;
    private String phone;
    private String email;

    // Registration & References
    private String gstNumber;
    private String rbiCode;
    private String ieCode;
    private String lutNumber;
    private List<String> otherReferences;

    // Bank Details
    private String bankName;
    private String bankBranch;
    private String bankAddress;
    private String accountNumber;
    private String swiftCode;

    // Shipping Defaults
    private String defaultPortOfLoading;
    private String defaultPortOfDischarge;
    private String defaultFinalDestination;
    private String defaultFinalDestinationCountry;
    private String defaultHsCode;
    private String defaultTermsOfDelivery;
    private String defaultTermsOfPayment;
    private String defaultCurrency;

    public Customer() {
        this.id = UUID.randomUUID().toString();
        this.companyName = "";
        this.address = "";
        this.cityProvince = "";
        this.country = "";
        this.phone = "";
        this.defaultPortOfLoading = "";
        this.defaultPortOfDischarge = "";
        this.defaultFinalDestination = "";
        this.defaultFinalDestinationCountry = "";
        this.defaultHsCode = "";
        this.defaultTermsOfDelivery = "FOB";
        this.defaultTermsOfPayment = "";
        this.defaultCurrency = "USD";
        this.otherReferences = new ArrayList<>();
    }

    public Customer(String companyName, String address, String cityProvince, String country) {
        this();
        this.companyName = companyName;
        this.address = address;
        this.cityProvince = cityProvince;
        this.country = country;
        this.defaultFinalDestinationCountry = country;
    }

    public String getFormattedConsignee() {
        StringBuilder sb = new StringBuilder();
        if (companyName != null && !companyName.isEmpty()) sb.append(companyName).append("\n");
        if (address != null && !address.isEmpty()) sb.append(address).append("\n");
        if (cityProvince != null && !cityProvince.isEmpty()) sb.append(cityProvince).append("\n");
        if (country != null && !country.isEmpty()) sb.append(country);
        return sb.toString().trim();
    }

    public String getId() {
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }
    public void setId(String id) { this.id = id; }

    public String getCompanyName() { return companyName != null ? companyName : ""; }
    public String getName() { return getCompanyName(); }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public void setName(String name) { this.companyName = name; }

    public String getAddress() { return address != null ? address : ""; }
    public void setAddress(String address) { this.address = address; }

    public String getCityProvince() { return cityProvince != null ? cityProvince : ""; }
    public void setCityProvince(String cityProvince) { this.cityProvince = cityProvince; }

    public String getCountry() { return country != null ? country : ""; }
    public void setCountry(String country) { this.country = country; }

    public String getContactPerson() { return contactPerson != null ? contactPerson : ""; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone != null ? phone : ""; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }

    public String getGstNumber() { return gstNumber != null ? gstNumber : ""; }
    public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }

    public String getRbiCode() { return rbiCode != null ? rbiCode : ""; }
    public void setRbiCode(String rbiCode) { this.rbiCode = rbiCode; }

    public String getIeCode() { return ieCode != null ? ieCode : ""; }
    public void setIeCode(String ieCode) { this.ieCode = ieCode; }

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

    public String getAccountNumber() { return accountNumber != null ? accountNumber : ""; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getSwiftCode() { return swiftCode != null ? swiftCode : ""; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getDefaultPortOfLoading() { return defaultPortOfLoading != null ? defaultPortOfLoading : ""; }
    public void setDefaultPortOfLoading(String defaultPortOfLoading) { this.defaultPortOfLoading = defaultPortOfLoading; }

    public String getDefaultPortOfDischarge() { return defaultPortOfDischarge != null ? defaultPortOfDischarge : ""; }
    public void setDefaultPortOfDischarge(String defaultPortOfDischarge) { this.defaultPortOfDischarge = defaultPortOfDischarge; }

    public String getDefaultFinalDestination() { return defaultFinalDestination != null ? defaultFinalDestination : ""; }
    public void setDefaultFinalDestination(String defaultFinalDestination) { this.defaultFinalDestination = defaultFinalDestination; }

    public String getDefaultFinalDestinationCountry() { return defaultFinalDestinationCountry != null ? defaultFinalDestinationCountry : ""; }
    public void setDefaultFinalDestinationCountry(String defaultFinalDestinationCountry) { this.defaultFinalDestinationCountry = defaultFinalDestinationCountry; }

    public String getDefaultHsCode() { return defaultHsCode != null ? defaultHsCode : ""; }
    public void setDefaultHsCode(String defaultHsCode) { this.defaultHsCode = defaultHsCode; }

    public String getDefaultTermsOfDelivery() { return defaultTermsOfDelivery != null ? defaultTermsOfDelivery : "FOB"; }
    public void setDefaultTermsOfDelivery(String defaultTermsOfDelivery) { this.defaultTermsOfDelivery = defaultTermsOfDelivery; }

    public String getDefaultTermsOfPayment() { return defaultTermsOfPayment != null ? defaultTermsOfPayment : ""; }
    public void setDefaultTermsOfPayment(String defaultTermsOfPayment) { this.defaultTermsOfPayment = defaultTermsOfPayment; }

    public String getDefaultCurrency() { return defaultCurrency != null && !defaultCurrency.isEmpty() ? defaultCurrency : "USD"; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
