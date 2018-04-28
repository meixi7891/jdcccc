package com.jingdianbao.entity;

public class SearchRequest {

    private String type;

    private String source;

    private String keyword;

    private String sku;

    private String sortType;

    private String shop;

    private String priceLow = "";

    private String priceHigh = "";

    private String startPage = "";

    private String endPage = "";


    public SearchRequest(String type, String source, String keyword, String sku, String sortType, String shop,String priceLow , String priceHigh,String startPage,String endPage) {
        this.type = type;
        this.source = source;
        this.keyword = keyword;
        this.sku = sku;
        this.sortType = sortType;
        this.shop = shop;
        this.priceLow = priceLow;
        this.priceHigh = priceHigh;
        this.startPage = startPage;
        this.endPage = endPage;
    }

    public SearchRequest() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getPriceLow() {
        return priceLow;
    }

    public void setPriceLow(String priceLow) {
        this.priceLow = priceLow;
    }

    public String getPriceHigh() {
        return priceHigh;
    }

    public void setPriceHigh(String priceHigh) {
        this.priceHigh = priceHigh;
    }

    public String getStartPage() {
        return startPage;
    }

    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    public String getEndPage() {
        return endPage;
    }

    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }
}
