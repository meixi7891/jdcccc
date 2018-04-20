package com.jingdianbao.entity;

public class CommentResult {

    private String sku;

    private String title;

    private String img;

    private String url;

    private int goodComment;

    private int normalComment;

    private int badComment;

    private int picComment;

    private int comment;

    private int discard;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
