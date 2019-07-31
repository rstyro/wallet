package com.hereblock.wallet.api.model;

import java.util.Map;

public class AddressVO {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 币种对应的生成地址
     */
    private Map<String, String> addressMap;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, String> getAddressMap() {
        return addressMap;
    }

    public void setAddressMap(Map<String, String> addressMap) {
        this.addressMap = addressMap;
    }
}
