package com.hereblock.wallet.provider.service;

import com.alibaba.fastjson.JSONArray;
import com.hereblock.account.api.model.vo.CoinVO;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.model.WalletAssetsRecordReqBO;
import com.hereblock.wallet.api.model.WalletRequestBO;
import com.hereblock.wallet.api.model.WalletRespResultVO;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

public interface WalletService {
    CoinEnum getCode();

    /**
     * 获得钱包地址
     *
     * @param walletRequestBO
     * @return 钱包地址
     */
    String getNewAddress(WalletRequestBO walletRequestBO) throws Throwable;

    /**
     * 获取余额
     *
     * @param walletRequestBO
     * @return 余额
     */
    BigDecimal getBalance(WalletRequestBO walletRequestBO);

    /**
     * 提币
     *
     * @param walletRequestBO
     * @return 交易ID
     */
    WalletRespResultVO withDraw(WalletRequestBO walletRequestBO);

    /**
     * 归冷归热提币
     *
     * @param walletRequestBO
     * @param coin
     * @return 交易ID
     */
    WalletRespResultVO transferWithDraw(WalletRequestBO walletRequestBO, CoinVO coin);

    /**
     * 钱包归热
     */
    void transferMainAddress(CoinEnum coinType);

    /**
     * 钱包归冷
     */
    void transferColdAddress(CoinEnum coinType);

    /**
     * 调用返回最近发生的与钱包有关的交易清单。该调用需要节点 启用钱包功能。
     */
    JSONArray listTransactions();

    /**
     * 获取交易的详细信息
     */
    LinkedHashMap getTransaction(String txId);

    /**
     * 充值记录更新操作
     */
    void insertRechargeCoin(CoinEnum coinType);

    /**
     * 提币记录更新操作
     */
    void updateWithdrawCoin(CoinEnum coinType);

    /**
     * 为用户地址转手续费
     */
    void toAddressTransactionFee(CoinEnum coinType);

    /**
     * 根据钱包地址记录资产信息
     */
    Boolean recordAssetsInfoByAddress(WalletAssetsRecordReqBO walletAssetsRecordReqBO);

    /**
     * 处理提币异常数据
     */
    void dealExceptionWithdraw(CoinEnum coinType);
}
