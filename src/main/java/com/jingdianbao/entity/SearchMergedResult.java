package com.jingdianbao.entity;

import java.util.ArrayList;
import java.util.List;

public class SearchMergedResult {

    private String title = "";

    private String shop = "";

    private String type = "";

    private String keyword = "";

    private String sku = "";

    private int pcPage;

    private int pcPos;

    private int pcRank;

    private int h5Page;

    private int h5Pos;

    private int h5Rank;

    private int comment;

    private int goodComment;

    private int normalComment;

    private int badComment;

    private int picComment;

    private int discard;

    private List<String> coupons = new ArrayList<>();

    private List<Gift> gifts = new ArrayList<>();

    private List<Promotion> promotions = new ArrayList<>();

    private List<String> advert  = new ArrayList<>();

    private String price;

    private String url = "";

    private String img = "";

    private Category category = new Category();

    public SearchMergedResult(SearchResult searchResult) {
        this.title = searchResult.getTitle();
        this.shop = searchResult.getShop();
        this.type = searchResult.getType();
        this.keyword = searchResult.getKeyword();
        this.sku = searchResult.getSku();

        this.pcPage = searchResult.getPage();
        this.pcPos = searchResult.getPos();
        this.pcRank = searchResult.getRank();

        this.comment = searchResult.getComment();

        this.goodComment = searchResult.getGoodComment();
        this.normalComment = searchResult.getNormalComment();
        this.badComment = searchResult.getBadComment();
        this.picComment = searchResult.getPicComment();

        this.coupons = searchResult.getCoupons();
        this.advert = searchResult.getAdvert();
        this.price = searchResult.getPrice();
        this.gifts = searchResult.getGifts();
        this.promotions = searchResult.getPromotions();
        this.discard = searchResult.getDiscard();
        this.url = searchResult.getUrl();
        this.img = searchResult.getImg();
        this.category = searchResult.getCategory();
    }

    public void merge(SearchResult searchResult) {
        this.h5Page = searchResult.getPage();
        this.h5Pos = searchResult.getPos();
        this.h5Rank = searchResult.getRank();
    }

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

    public int getPcPage() {
        return pcPage;
    }

    public void setPcPage(int pcPage) {
        this.pcPage = pcPage;
    }

    public int getPcPos() {
        return pcPos;
    }

    public void setPcPos(int pcPos) {
        this.pcPos = pcPos;
    }

    public int getPcRank() {
        return pcRank;
    }

    public void setPcRank(int pcRank) {
        this.pcRank = pcRank;
    }

    public int getH5Page() {
        return h5Page;
    }

    public void setH5Page(int h5Page) {
        this.h5Page = h5Page;
    }

    public int getH5Pos() {
        return h5Pos;
    }

    public void setH5Pos(int h5Pos) {
        this.h5Pos = h5Pos;
    }

    public int getH5Rank() {
        return h5Rank;
    }

    public void setH5Rank(int h5Rank) {
        this.h5Rank = h5Rank;
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

    public List<String> getAdvert() {
        return advert;
    }

    public void setAdvert(List<String> advert) {
        this.advert = advert;
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
