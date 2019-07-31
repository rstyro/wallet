package com.hereblock.wallet.api.model;

import com.hereblock.wallet.api.enums.CoinEnum;

import java.math.BigDecimal;

public class WalletAssetsRecordReqBO {
    /**
     * appId
     */
    private String appId;
    /**
     * 币种  ETH BTC USDT
     */
    private CoinEnum coinType;
    /**
     * 钱包地址
     */
    private String address;
    /**
     * 流水号
     */
    private String orderNo;
    /**
     * 金额
     */
    private BigDecimal account;
    /**
     * 交易类型 1 充币 2 提币 3 买币 4 卖币 5划转
     */
    private Integer transferType;
    /**
     * 用户ID
     */
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public CoinEnum getCoinType() {
        return coinType;
    }

    public void setCoinType(CoinEnum coinType) {
        this.coinType = coinType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getAccount() {
        return account;
    }

    public void setAccount(BigDecimal account) {
        this.account = account;
    }

    public Integer getTransferType() {
        return transferType;
    }

    public void setTransferType(Integer transferType) {
        this.transferType = transferType;
    }
}
