package com.hereblock.wallet.provider.service;

import com.hereblock.wallet.api.model.WalletTransferBO;
import com.hereblock.wallet.api.model.WalletTransferVO;
import com.hereblock.wallet.provider.entity.WalletTransfer;

import java.util.List;

public interface WalletTransferService {
    /**
     * 插入交易数据
     *
     * @param walletTransfer
     * @return
     */
    int insertWalletTransfer(WalletTransferBO walletTransfer);

    /**
     * 更新交易数据
     *
     * @param walletTransfer
     * @return
     */
    int updateWalletTransfer(WalletTransferBO walletTransfer);

    /**
     * 查询交易数据
     *
     * @param walletTransfer
     * @return
     */
    List<WalletTransferVO> selectWalletTransfer(WalletTransferBO walletTransfer);

    /**
     * 插入交易记录
     *
     * @param walletTransfer
     * @return
     */
    int insertTransferRecord(WalletTransferBO walletTransfer);

    /**
     * 根据流水号更新txId
     *
     * @param walletTransfer
     * @return
     */
    int updateWalletTransferByTransferNo(WalletTransferBO walletTransfer);

    /**
     * 根据地址查询归冷归热的数据
     *
     * @param walletTransfer
     * @return
     */
    WalletTransfer selectWalletTransferByTxId(WalletTransferBO walletTransfer);
}
