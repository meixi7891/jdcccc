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

    private int discard;

    private Promotion promotion;

    private String price;

    private List<String> adverts;

    private CommentResult commentEntity;

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

        this.commentEntity.setComment(searchResult.getComment());
        this.commentEntity.setDiscard(searchResult.getDiscard());

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

    public CommentResult getCommentEntity() {
        return commentEntity;
    }

    public void setCommentEntity(CommentResult commentEntity) {
        this.commentEntity = commentEntity;
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

    public Promotion getPromotion() {
        return promotion;
    }

    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public List<String> getAdverts() {
        return adverts;
    }

    public void setAdverts(List<String> adverts) {
        this.adverts = adverts;
    }
}
