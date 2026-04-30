package com.shopez.model;

import java.util.UUID;

public class DigitalProduct extends Product {

    private String downloadUrl;
    private String licenseKey;

    public DigitalProduct() {
    }

    @Override
    public String getProductType() {
        return "DIGITAL";
    }

    @Override
    public double calculateFinalPrice() {
        return price;
    }

    public String generateLicense() {
        return UUID.randomUUID().toString();
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }
}
