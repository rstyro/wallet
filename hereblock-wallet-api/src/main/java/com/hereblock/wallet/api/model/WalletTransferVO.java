package com.hereblock.wallet.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

public class WalletTransferVO {
    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 客户端提币流水号
     */
    private String withdrawOrderNo;
    /**
     * 交易类型 1 申请手续费 2 提币到主地址 3 转到冷地址 4 客户端提币
     */
    private Integer transferType;
    /**
     * 币种类型
     */
    private String coinType;
    /**
     * 转出地址
     */
    private String fromAddress;
    /**
     * 转入地址
     */
    private String toAddress;
    /**
     * 提币数量
     */
    private BigDecimal qty;
    /**
     * 交易hash id
     */
    private String txId;
    /**
     * 转出手续费
     */
    private BigDecimal txFee;
    /**
     * 交易状态 0 失败 1 已产生交易hash，并进入txPool 2 离开txPool 3 已确认
     */
    private Integer status;
    /**
     * gasPrice
     */
    private BigDecimal gasPrice;
    /**
     * gasLimit
     */
    private BigDecimal gasLimit;
    /**
     * 确认时间
     */
    private Date confirmTime;
    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 修改时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 旧状态
     */
    private Integer oldStatus;
    /**
     * 交易号
     */
    private String transferNo;

    /**
     * 用户子地址
     */
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTransferNo() {
        return transferNo;
    }

    public void setTransferNo(String transferNo) {
        this.transferNo = transferNo;
    }

    public Integer getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Integer oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getCoinType() {
        return coinType;
    }

    public void setCoinType(String coinType) {
        this.coinType = coinType;
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

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public BigDecimal getTxFee() {
        return txFee;
    }

    public void setTxFee(BigDecimal txFee) {
        this.txFee = txFee;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public BigDecimal getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigDecimal gasPrice) {
        this.gasPrice = gasPrice;
    }

    public BigDecimal getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigDecimal gasLimit) {
        this.gasLimit = gasLimit;
    }

    public Date getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(Date confirmTime) {
        this.confirmTime = confirmTime;
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
