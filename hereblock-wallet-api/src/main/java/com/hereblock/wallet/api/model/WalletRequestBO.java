package com.hereblock.wallet.api.model;

import com.hereblock.wallet.api.enums.CoinEnum;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author Goasin
 * @Since 2019-05-10
 */
public class WalletRequestBO {
    /**
     * 钱包地址
     */
    private String address;
    /**
     * 币数量
     */
    private BigDecimal amount;

    /**
     * 资产ID,只有USDT拥有这个属性，USDT该值为31
     */
    private Integer propertyid;

    /**
     * 钱包转出地址
     */
    private String fromAddress;

    /**
     * 钱包转入地址
     */
    private String toAddress;

    /**
     * 币ID
     */
    private int coinId;

    /**
     * 币种类型 ETH BTC USDT
     */
    private CoinEnum coinType;

    /**
     * 币种类型列表
     */
    private List<CoinEnum> coinTypeList;

    /**
     * 钱包密码，目前只有ETH拥有该属性
     */
    private String pwd;

    /**
     * 包含相关数据的字节字符串，如果是合约创建，则是初始化要用到的代码
     * 方法签名和编码参数的哈希
     */
    private String transData;
    /**
     * 交易类型 1 申请手续费 2 提币到主地址 3 转到冷地址 4 客户端提币
     */
    private Integer transferType;

    /**
     * 资产生成的提币流水号
     */
    private String withdrawOrderNo;

    /**
     * 钱包自身交易流水号
     */
    private String transferNo;

    /**
     * appId
     */
    private String appId;

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

    public String getTransferNo() {
        return transferNo;
    }

    public void setTransferNo(String transferNo) {
        this.transferNo = transferNo;
    }

    public String getWithdrawOrderNo() {
        return withdrawOrderNo;
    }

    public void setWithdrawOrderNo(String withdrawOrderNo) {
        this.withdrawOrderNo = withdrawOrderNo;
    }

    public Integer getTransferType() {
        return transferType;
    }

    public void setTransferType(Integer transferType) {
        this.transferType = transferType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getPropertyid() {
        return propertyid;
    }

    public void setPropertyid(Integer propertyid) {
        this.propertyid = propertyid;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public int getCoinId() {
        return coinId;
    }

    public void setCoinId(int coinId) {
        this.coinId = coinId;
    }

    public CoinEnum getCoinType() {
        return coinType;
    }

    public void setCoinType(CoinEnum coinType) {
        this.coinType = coinType;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getTransData() {
        return transData;
    }

    public void setTransData(String transData) {
        this.transData = transData;
    }

    public List<CoinEnum> getCoinTypeList() {
        return coinTypeList;
    }

    public void setCoinTypeList(List<CoinEnum> coinTypeList) {
        this.coinTypeList = coinTypeList;
    }
}
