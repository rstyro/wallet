package com.hereblock.wallet.provider.mapper;

import com.hereblock.wallet.provider.entity.WalletAssets;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface WalletAssetsMapper {
    /**
     * 插入钱包资产数据
     *
     * @param walletAssets
     * @return
     */
    int insertWalletAssets(WalletAssets walletAssets);

    /**
     * 更新钱包资产数据
     *
     * @param walletAssets
     * @return
     */
    int updateWalletAssets(WalletAssets walletAssets);

    /**
     * 查询交易数据
     *
     * @param walletAssets
     * @return
     */
    List<WalletAssets> selectWalletAssets(WalletAssets walletAssets);

    /**
     * 根据钱包地址查询资产对象
     *
     * @param address
     * @return
     */
    WalletAssets selectWalletAssetsByAddress(@Param("address") String address);

    /**
     * 查询交易数据
     *
     * @param walletAssets
     * @return
     */
    List<WalletAssets> selectWalletAssetsByClause(WalletAssets walletAssets);
}
