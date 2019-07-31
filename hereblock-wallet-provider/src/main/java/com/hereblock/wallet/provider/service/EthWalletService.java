package com.hereblock.wallet.provider.service;

import com.hereblock.wallet.api.model.WalletRequestBO;
import com.hereblock.wallet.api.model.WalletRespResultVO;

import java.math.BigInteger;
import java.util.Map;

/**
 * ETH 钱包接口
 */
public abstract class EthWalletService {
    /**
     * 验证指定的密码并提交交易
     *
     * @param walletRequestBO
     * @return
     */
    public abstract WalletRespResultVO personalSendTransaction(WalletRequestBO walletRequestBO);

    /**
     * 返回当前的gas价格，单位：wei
     *
     * @return 以wei为单位的当前gas价格
     */
    public abstract int ethGasPrice();

    /**
     * 估计调用需要耗费的gas量
     *
     * @param params
     * @return
     */
    public abstract String estimateGas(Map<String, Object> params);

    /**
     * 当前块编号
     */
    public abstract BigInteger getBlockNumber();

    /**
     * 返回指定块内的交易数量，使用块编号指定块。
     */
    public abstract BigInteger getBlockTransactionCountByNumber(BigInteger blockNumber);

    /**
     * 返回指定编号的块内具有指定索引序号的交易。
     *
     * @param blockNumber 整数块编号，或字符串"earliest"、"latest" 或"pending"
     * @param index       交易索引序号
     * @return
     */
    public abstract Object getTransactionByBlockNumberAndIndex(BigInteger blockNumber, BigInteger index);

    /**
     * 交易哈希
     *
     * @param txhash
     * @return
     */
    public abstract Object getTransactionByHash(String txhash);

    /**
     * 获取本地交易池
     *
     * @return
     */
    public abstract Object txpoolContent();


    /**
     * 返回指定交易的收据，使用哈希指定交易。
     * <p>
     * 需要指出的是，挂起的交易其收据无效。
     *
     * @param txhash
     * @return
     */
    public abstract Object getTransactionReceipt(String txhash);

}
