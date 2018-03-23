package com.jingdianbao.entity;

public class DmpResult {


    private String name;

    private String shop;

    private String sku;

    private String url;

    private String brand;

    private String category;

    private int coverCount;


    public String errorMessage = "";

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
        this.url = "https://item.m.jd.com/product/" + sku + ".html";
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCoverCount() {
        return coverCount;
    }

    public void setCoverCount(int coverCount) {
        this.coverCount = coverCount;
    }
}
