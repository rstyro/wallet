package com.hereblock.wallet.provider.service;

import com.hereblock.wallet.api.model.WalletRequestBO;

import java.math.BigInteger;
import java.util.List;

/**
 * USDT 钱包接口
 * 参考资料：http://cw.hubwiz.com/card/c/omni-rpc-api/1/2/7/
 */
public abstract class UsdtWalletService {
    /**
     * 获取区块高度
     */
    public abstract BigInteger getblockcount();

    /**
     * 指定区块高度 所有交易hash
     */
    public abstract List<String> omniListBlockTransactions(Integer blockIndex);

    /**
     * 调用创建并广播发送一个简单交易
     *
     * @return
     */
    public abstract String omniFundedSend(WalletRequestBO walletRequestBO);
}
