package com.hereblock.wallet.api.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 钱包资产
 */
public class WalletAssetsBO {
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
     * 总资产
     */
    private BigDecimal totalAccount;
    /**
     * 版本号
     */
    private Integer version;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 上个版本号
     */
    private Integer oldVersion;

    /**
     * 用户ID
     */
    private String userId;

    private List<String> coinTypes;

    public List<String> getCoinTypes() {
        return coinTypes;
    }

    public void setCoinTypes(List<String> coinTypes) {
        this.coinTypes = coinTypes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getOldVersion() {
        return oldVersion;
    }

    public void setOldVersion(Integer oldVersion) {
        this.oldVersion = oldVersion;
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

    public BigDecimal getTotalAccount() {
        return totalAccount;
    }

    public void setTotalAccount(BigDecimal totalAccount) {
        this.totalAccount = totalAccount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
