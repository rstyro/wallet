package com.hereblock.wallet.provider.service;

import com.hereblock.wallet.api.model.WalletAssetsRecordBO;
import com.hereblock.wallet.api.model.WalletAssetsRecordVO;

import java.util.List;

public interface WalletAssetsRecordService {
    /**
     * 插入钱包资产流水数据
     *
     * @param walletAssetsRecord
     * @return
     */
    int insertWalletAssetsRecord(WalletAssetsRecordBO walletAssetsRecord);

    /**
     * 查询交易流水数据
     *
     * @param walletAssetsRecord
     * @return
     */
    List<WalletAssetsRecordVO> selectWalletAssetsRecord(WalletAssetsRecordBO walletAssetsRecord);
}
