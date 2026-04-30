package com.shopez.model;

public class PhysicalProduct extends Product {

    private double weightKg;
    private String dimensions;

    public PhysicalProduct() {
    }

    @Override
    public String getProductType() {
        return "PHYSICAL";
    }

    @Override
    public double calculateFinalPrice() {
        return price + calculateShipping();
    }

    public double calculateShipping() {
        return weightKg * 2.5;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public String getDimensions() {
        return dimensions;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
}
