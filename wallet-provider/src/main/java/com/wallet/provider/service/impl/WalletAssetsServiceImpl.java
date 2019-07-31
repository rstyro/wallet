package com.hereblock.wallet.provider.service.impl;

import com.hereblock.wallet.api.model.WalletAssetsBO;
import com.hereblock.wallet.api.model.WalletAssetsVO;
import com.hereblock.wallet.provider.entity.WalletAssets;
import com.hereblock.wallet.provider.mapper.WalletAssetsMapper;
import com.hereblock.wallet.provider.service.WalletAssetsService;
import com.hereblock.wallet.provider.util.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletAssetsServiceImpl implements WalletAssetsService {
    @Autowired
    private WalletAssetsMapper walletAssetsMapper;

    @Override
    public int insertWalletAssets(WalletAssetsBO walletAssets) {
        if (walletAssets == null) return 0;
        WalletAssets walletAssetsTmp = new WalletAssets();
        BeanUtil.copyProperties(walletAssets, walletAssetsTmp);
        return walletAssetsMapper.insertWalletAssets(walletAssetsTmp);
    }

    @Override
    public int updateWalletAssets(WalletAssetsBO walletAssets) {
        if (walletAssets == null) return 0;
        WalletAssets walletAssetsTmp = new WalletAssets();
        BeanUtil.copyProperties(walletAssets, walletAssetsTmp);
        return walletAssetsMapper.updateWalletAssets(walletAssetsTmp);
    }

    @Override
    public List<WalletAssetsVO> selectWalletAssets(WalletAssetsBO walletAssets) {
        if (walletAssets == null) return null;
        WalletAssets walletAssetsTmp = new WalletAssets();
        BeanUtils.copyProperties(walletAssets, walletAssetsTmp);
        List<WalletAssets> walletAssetsList = walletAssetsMapper.selectWalletAssets(walletAssetsTmp);
        return BeanUtil.copyListProperties(walletAssetsList, WalletAssetsVO.class);
    }

    @Override
    public WalletAssetsVO selectWalletAssetsByAddress(String address) {
        if (StringUtils.isBlank(address)) return null;
        WalletAssets walletAssets = walletAssetsMapper.selectWalletAssetsByAddress(address);
        if (walletAssets == null) return null;
        WalletAssetsVO walletAssetsTmp = new WalletAssetsVO();
        BeanUtils.copyProperties(walletAssets, walletAssetsTmp);
        return walletAssetsTmp;
    }

    @Override
    public List<WalletAssetsVO> selectWalletAssetsByClause(WalletAssetsBO walletAssets) {
        if (walletAssets == null) return null;
        WalletAssets walletAssetsTmp = new WalletAssets();
        BeanUtils.copyProperties(walletAssets, walletAssetsTmp);
        List<WalletAssets> walletAssetsList = walletAssetsMapper.selectWalletAssetsByClause(walletAssetsTmp);
        return BeanUtil.copyListProperties(walletAssetsList, WalletAssetsVO.class);
    }
}
