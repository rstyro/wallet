package com.hereblock.wallet.api.model;

import java.math.BigDecimal;

/**
 * ETH钱包相应结果
 */
public class WalletRespResultVO {
    /**
     * 钱包转出地址
     */
    private String fromAddress;

    /**
     * 钱包转入地址
     */
    private String toAddress;

    /**
     * 默认是自动确定，交易的gas价格，默认是网络gas价格的平均值
     */
    private String gasPrice;

    /**
     * 交易携带的货币量，以wei为单位。如果合约创建交易，则为初始的基金
     */
    private BigDecimal value;

    /**
     * 交易可使用的gas，未使用的gas会退回
     */
    private String gas;


    /**
     * 包含相关数据的字节字符串，如果是合约创建，则是初始化要用到的代码
     * 方法签名和编码参数的哈希
     */
    private String data;


    /**
     * 交易的hash ID
     */
    private String txHashId;
    /**
     * 客户端提币流水号
     */
    private String withdrawOrderNo;

    public String getWithdrawOrderNo() {
        return withdrawOrderNo;
    }

    public void setWithdrawOrderNo(String withdrawOrderNo) {
        this.withdrawOrderNo = withdrawOrderNo;
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

    public String getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(String gasPrice) {
        this.gasPrice = gasPrice;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getGas() {
        return gas;
    }

    public void setGas(String gas) {
        this.gas = gas;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTxHashId() {
        return txHashId;
    }

    public void setTxHashId(String txHashId) {
        this.txHashId = txHashId;
    }

}
