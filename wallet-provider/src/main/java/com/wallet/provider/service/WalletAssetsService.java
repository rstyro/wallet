package com.hereblock.wallet.provider.service;

import com.hereblock.wallet.api.model.WalletAssetsBO;
import com.hereblock.wallet.api.model.WalletAssetsVO;

import java.util.List;

public interface WalletAssetsService {
    /**
     * 插入钱包资产数据
     *
     * @param walletAssets
     * @return
     */
    int insertWalletAssets(WalletAssetsBO walletAssets);

    /**
     * 更新钱包资产数据
     *
     * @param walletAssets
     * @return
     */
    int updateWalletAssets(WalletAssetsBO walletAssets);

    /**
     * 查询交易数据
     *
     * @param walletAssets
     * @return
     */
    List<WalletAssetsVO> selectWalletAssets(WalletAssetsBO walletAssets);

    /**
     * 根据钱包地址查询资产对象
     *
     * @param address
     * @return
     */
    WalletAssetsVO selectWalletAssetsByAddress(String address);

    /**
     * 查询交易数据
     *
     * @param walletAssets
     * @return
     */
    List<WalletAssetsVO> selectWalletAssetsByClause(WalletAssetsBO walletAssets);
}
