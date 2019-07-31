package com.hereblock.wallet.provider.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 钱包资产记录
 */
public class WalletAssetsRecord {
    /**
     * 主键
     */
    private Integer id;
    /**
     * appId
     */
    private String appId;
    /**
     * 币种  ETH BTC USDT
     */
    private String coinType;
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
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getCoinType() {
        return coinType;
    }

    public void setCoinType(String coinType) {
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
