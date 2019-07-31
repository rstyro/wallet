package com.hereblock.wallet.provider.mapper;

import com.hereblock.wallet.provider.entity.WalletAssetsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface WalletAssetsRecordMapper {
    /**
     * 插入钱包资产流水数据
     *
     * @param walletAssetsRecord
     * @return
     */
    int insertWalletAssetsRecord(WalletAssetsRecord walletAssetsRecord);

    /**
     * 查询交易流水数据
     *
     * @param walletAssetsRecord
     * @return
     */
    List<WalletAssetsRecord> selectWalletAssetsRecord(WalletAssetsRecord walletAssetsRecord);
}
