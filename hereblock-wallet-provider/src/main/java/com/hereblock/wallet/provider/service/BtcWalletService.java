package com.hereblock.wallet.provider.service;

import java.util.LinkedHashMap;

/**
 * BTC 钱包接口
 * 参考资料：http://cw.hubwiz.com/card/c/bitcoin-json-rpc-api/1/5/1/
 */
public abstract class BtcWalletService {
    /**
     * 返回未交易的钱包数组对象
     *
     * @return
     */
    public abstract Object listunspent();

    /**
     * 调用获取指定钱包内交易的详细信息。该调用需要节点 启用钱包功能。
     */
    public abstract LinkedHashMap getTransaction(String txId);

}
