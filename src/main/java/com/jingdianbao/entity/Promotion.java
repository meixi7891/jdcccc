package com.jingdianbao.entity;

import java.util.ArrayList;
import java.util.List;

public class Promotion {

    private List<Gift> giftList = new ArrayList<>();

    private List<String> coupons = new ArrayList<>();

    private List<PromotionItem> promotionItemList = new ArrayList<>();

    public List<Gift> getGiftList() {
        return giftList;
    }

    public void setGiftList(List<Gift> giftList) {
        this.giftList = giftList;
    }

    public List<String> getCoupons() {
        return coupons;
    }

    public void setCoupons(List<String> coupons) {
        this.coupons = coupons;
    }

    public List<PromotionItem> getPromotionItemList() {
        return promotionItemList;
    }

    public void setPromotionItemList(List<PromotionItem> promotionItemList) {
        this.promotionItemList = promotionItemList;
    }
}
