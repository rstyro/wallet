package com.hereblock.wallet.provider.service.impl;

import com.hereblock.wallet.api.model.WalletTransferBO;
import com.hereblock.wallet.api.model.WalletTransferVO;
import com.hereblock.wallet.provider.entity.WalletTransfer;
import com.hereblock.wallet.provider.mapper.WalletTransferMapper;
import com.hereblock.wallet.provider.service.WalletTransferService;
import com.hereblock.wallet.provider.util.BeanUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WalletTransferServiceImpl implements WalletTransferService {
    @Autowired
    private WalletTransferMapper walletTransferMapper;

    @Transactional
    @Override
    public int insertWalletTransfer(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return 0;
        WalletTransfer transfer = new WalletTransfer();
        BeanUtil.copyProperties(walletTransfer, transfer);
        return walletTransferMapper.insertWalletTransfer(transfer);
    }

    @Transactional
    @Override
    public int updateWalletTransfer(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return 0;
        WalletTransfer transfer = new WalletTransfer();
        BeanUtil.copyProperties(walletTransfer, transfer);
        return walletTransferMapper.updateWalletTransfer(transfer);
    }

    @Override
    public List<WalletTransferVO> selectWalletTransfer(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return null;
        WalletTransfer transfer = new WalletTransfer();
        BeanUtils.copyProperties(walletTransfer, transfer);
        List<WalletTransfer> transfers = walletTransferMapper.selectWalletTransfer(transfer);
        return BeanUtil.copyListProperties(transfers, WalletTransferVO.class);
    }

    @Override
    public int insertTransferRecord(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return 0;
        return this.insertWalletTransfer(walletTransfer);
    }

    @Override
    public int updateWalletTransferByTransferNo(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return 0;
        WalletTransfer transfer = new WalletTransfer();
        BeanUtils.copyProperties(walletTransfer, transfer);
        return walletTransferMapper.updateWalletTransferByTransferNo(transfer);
    }

    @Override
    public WalletTransfer selectWalletTransferByTxId(WalletTransferBO walletTransfer) {
        if (walletTransfer == null) return null;
        WalletTransfer transfer = new WalletTransfer();
        BeanUtils.copyProperties(walletTransfer, transfer);
        return walletTransferMapper.selectWalletTransferByTxId(transfer);
    }
}
