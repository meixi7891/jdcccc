package com.jingdianbao.entity;

public class DmpRequest {
    private String userName;

    private String password;

    private String sku;


    public DmpRequest(String userName, String password, String sku) {
        this.userName = userName;
        this.password = password;
        this.sku = sku;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}
