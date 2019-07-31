package com.hereblock.wallet.provider.mapper;

import com.hereblock.wallet.provider.entity.WalletTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 钱包交易dao
 */
@Mapper
@Component
public interface WalletTransferMapper {
    /**
     * 插入交易数据
     *
     * @param walletTransfer
     * @return
     */
    int insertWalletTransfer(WalletTransfer walletTransfer);

    /**
     * 更新交易数据
     *
     * @param walletTransfer
     * @return
     */
    int updateWalletTransfer(WalletTransfer walletTransfer);

    /**
     * 查询交易数据
     *
     * @param walletTransfer
     * @return
     */
    List<WalletTransfer> selectWalletTransfer(WalletTransfer walletTransfer);

    /**
     * 根据流水号更新txId
     *
     * @param walletTransfer
     * @return
     */
    int updateWalletTransferByTransferNo(WalletTransfer walletTransfer);

    /**
     * 根据txId查询归冷归热的数据
     *
     * @param walletTransfer
     * @return
     */
    WalletTransfer selectWalletTransferByTxId(WalletTransfer walletTransfer);

    /**
     * 查询提币异常记录
     *
     * @return
     */
    List<WalletTransfer> selectExceptionWalletTransfer(String coinType);

    int updateStatusByTransferNo(WalletTransfer walletTransfer);
}
