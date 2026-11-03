package com.graniteflow.app.data.model;

import java.io.Serializable;

public class Order implements Serializable {
    private String orderId;
    private String customerId;
    private String customerName;
    private String materialName;
    private String productType;
    private String destinationPort;
    private String destinationCountry;
    private String status;
    private long createdAt;

    public Order() {
        this.orderId = "";
        this.customerName = "";
        this.materialName = "";
        this.productType = "ROUGH GRANITE BLOCKS";
        this.destinationPort = "";
        this.destinationCountry = "";
        this.status = "ACTIVE";
        this.createdAt = System.currentTimeMillis();
    }

    public Order(String orderId, String customerName, String materialName, String destinationPort) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.materialName = materialName;
        this.productType = "ROUGH GRANITE BLOCKS";
        this.destinationPort = destinationPort;
        this.destinationCountry = "CHINA";
        this.status = "ACTIVE";
        this.createdAt = System.currentTimeMillis();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDestinationPort() { return destinationPort; }
    public void setDestinationPort(String destinationPort) { this.destinationPort = destinationPort; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
