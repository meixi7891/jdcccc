package com.jingdianbao.entity;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {

    private String title = "";

    private String shop = "";

    private String type = "";

    private String keyword = "";

    private String sku = "";

    private int page;

    private int pos;

    private int rank;

    private int goodComment;

    private int normalComment;

    private int badComment;

    private int picComment;

    private int comment;

    private int discard;

    private String price;

    private List<String> coupons = new ArrayList<>();

    private List<Gift> gifts = new ArrayList<>();

    private List<Promotion> promotions = new ArrayList<>();

    private List<String> advert = new ArrayList<>();

    private String url = "";

    private String img = "";

    public List<String> getAdvert() {
        return advert;
    }

    public void setAdvert(List<String> advert) {
        this.advert = advert;
    }

    private Category category = new Category();

    private String brand;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getComment() {
        return comment;
    }

    public void setComment(int comment) {
        this.comment = comment;
    }

    public int getDiscard() {
        return discard;
    }

    public void setDiscard(int discard) {
        this.discard = discard;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getGoodComment() {
        return goodComment;
    }

    public void setGoodComment(int goodComment) {
        this.goodComment = goodComment;
    }

    public int getNormalComment() {
        return normalComment;
    }

    public void setNormalComment(int normalComment) {
        this.normalComment = normalComment;
    }

    public int getBadComment() {
        return badComment;
    }

    public void setBadComment(int badComment) {
        this.badComment = badComment;
    }

    public int getPicComment() {
        return picComment;
    }

    public void setPicComment(int picComment) {
        this.picComment = picComment;
    }

    public List<String> getCoupons() {
        return coupons;
    }

    public void setCoupons(List<String> coupons) {
        this.coupons = coupons;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public List<Gift> getGifts() {
        return gifts;
    }

    public void setGifts(List<Gift> gifts) {
        this.gifts = gifts;
    }

    public List<Promotion> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<Promotion> promotions) {
        this.promotions = promotions;
    }
}
