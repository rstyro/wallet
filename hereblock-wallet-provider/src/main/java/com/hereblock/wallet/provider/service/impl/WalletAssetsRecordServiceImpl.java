package com.hereblock.wallet.provider.service.impl;

import com.hereblock.wallet.api.model.WalletAssetsRecordBO;
import com.hereblock.wallet.api.model.WalletAssetsRecordVO;
import com.hereblock.wallet.provider.entity.WalletAssetsRecord;
import com.hereblock.wallet.provider.mapper.WalletAssetsRecordMapper;
import com.hereblock.wallet.provider.service.WalletAssetsRecordService;
import com.hereblock.wallet.provider.util.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletAssetsRecordServiceImpl implements WalletAssetsRecordService {
    @Autowired
    private WalletAssetsRecordMapper walletAssetsRecordMapper;

    @Override
    public int insertWalletAssetsRecord(WalletAssetsRecordBO walletAssetsRecord) {
        if (walletAssetsRecord == null) return 0;
        WalletAssetsRecord walletAssetsRecordTmp = new WalletAssetsRecord();
        BeanUtil.copyProperties(walletAssetsRecord, walletAssetsRecordTmp);
        return walletAssetsRecordMapper.insertWalletAssetsRecord(walletAssetsRecordTmp);
    }

    @Override
    public List<WalletAssetsRecordVO> selectWalletAssetsRecord(WalletAssetsRecordBO walletAssetsRecord) {
        if (walletAssetsRecord == null) return null;
        WalletAssetsRecord walletAssetsRecordTmp = new WalletAssetsRecord();
        BeanUtil.copyProperties(walletAssetsRecord, walletAssetsRecordTmp);
        List<WalletAssetsRecord> walletAssetsRecordList = walletAssetsRecordMapper.selectWalletAssetsRecord(walletAssetsRecordTmp);
        return BeanUtil.copyListProperties(walletAssetsRecordList, WalletAssetsRecordVO.class);
    }
}
