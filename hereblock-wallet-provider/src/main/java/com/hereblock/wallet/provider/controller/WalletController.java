package com.hereblock.wallet.provider.controller;

import com.google.common.collect.Maps;
import com.hereblock.common.model.ResponseData;
import com.hereblock.framework.api.model.ResponseCode;
import com.hereblock.framework.service.utils.IDGenerator;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.enums.TransferStatusEnum;
import com.hereblock.wallet.api.enums.TransferTypeEnum;
import com.hereblock.wallet.api.model.*;
import com.hereblock.wallet.api.service.WalletRemoteService;
import com.hereblock.wallet.provider.entity.WalletTransfer;
import com.hereblock.wallet.provider.service.ServiceFactory;
import com.hereblock.wallet.provider.service.WalletAssetsService;
import com.hereblock.wallet.provider.service.WalletTransferService;
import com.hereblock.wallet.provider.support.WalletSupport;
import com.hereblock.wallet.provider.util.RandomNumber;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 钱包接口实现类
 *
 * @Author Goasin
 * @Since 2019-05-10
 */
@RestController
public class WalletController implements WalletRemoteService {
    private Logger logger = LoggerFactory.getLogger(WalletController.class);
    @Resource
    ServiceFactory serviceFactory;

    @Autowired
    private IDGenerator idGenerator;

    @Autowired
    private WalletTransferService walletTransferService;
    @Autowired
    private WalletAssetsService walletAssetsService;

    @Autowired
    private WalletSupport walletSupport;

    @Override
    public ResponseData<String> getNewAddress(WalletRequestBO walletRequestBO) {
        try {
            if (walletRequestBO == null || StringUtils.isBlank(walletRequestBO.getUserId())) {
                return ResponseData.failure(ResponseCode.ILLEGAL_REQUESTS);
            }
            WalletAssetsBO walletAssetsParams = new WalletAssetsBO();
            walletAssetsParams.setUserId(walletRequestBO.getUserId());
            walletAssetsParams.setAppId(walletRequestBO.getAppId());
            walletAssetsParams.setCoinType(walletRequestBO.getCoinType().toString());
            List<WalletAssetsVO> list = walletAssetsService.selectWalletAssets(walletAssetsParams);
            if (CollectionUtils.isNotEmpty(list)) {
                return ResponseData.success(list.get(0).getAddress());
            }
            String address = serviceFactory.getWalletService(walletRequestBO.getCoinType()).getNewAddress(walletRequestBO);
            this.setWalletAssets(walletRequestBO, address);
            return ResponseData.success(address);
        } catch (Throwable e) {
            logger.error("getNewAddress : {}", e.getMessage(), e);
            return ResponseData.failure(ResponseCode.SYSTEM_EXCEPTION);
        }
    }

    @Override
    public ResponseData<AddressVO> queryUserAllAddress(WalletRequestBO walletRequestBO) {
        if (walletRequestBO == null || StringUtils.isBlank(walletRequestBO.getUserId())) {
            return ResponseData.failure(ResponseCode.ILLEGAL_REQUESTS);
        }
        WalletAssetsBO walletAssets = new WalletAssetsBO();
        walletAssets.setUserId(walletRequestBO.getUserId());
        walletAssets.setAppId(walletRequestBO.getAppId());
        List<WalletAssetsVO> list = walletAssetsService.selectWalletAssets(walletAssets);
        AddressVO addressVO = new AddressVO();
        addressVO.setUserId(walletRequestBO.getUserId());
        Map<String, String> addressMap = Maps.newHashMap();
        for (WalletAssetsVO walletAssetsVO : list) {
            addressMap.put(walletAssetsVO.getCoinType(), walletAssetsVO.getAddress());
        }
        addressVO.setAddressMap(addressMap);
        return ResponseData.success(addressVO);
    }

    private void setWalletAssets(WalletRequestBO walletRequestBO, String address) {
        WalletAssetsBO walletAssets = new WalletAssetsBO();
        walletAssets.setAddress(address);
        walletAssets.setAppId(walletRequestBO.getAppId());
        walletAssets.setCoinType(walletRequestBO.getCoinType().toString());
        walletAssets.setTotalAccount(new BigDecimal("0"));
        walletAssets.setVersion(0);
        walletAssets.setUserId(walletRequestBO.getUserId());
        walletAssets.setCreateTime(new Date());
        walletAssets.setUpdateTime(new Date());
        walletAssetsService.insertWalletAssets(walletAssets);
    }

    @Override
    public ResponseData<BigDecimal> getBalance(WalletRequestBO walletRequestBO) {
        BigDecimal balance = serviceFactory.getWalletService(walletRequestBO.getCoinType()).getBalance(walletRequestBO);
        return ResponseData.success(balance);
    }

    @Override
    public ResponseData<WalletRespResultVO> withDraw(WalletRequestBO walletRequestBO) {
        String transferNo = RandomNumber.getRandomNumberId();
        try {
            WalletTransferBO walletTransfer = new WalletTransferBO();
            walletTransfer.setTransferNo(transferNo);
            walletTransfer.setAddress(walletRequestBO.getAddress());
            walletTransfer.setCoinType(walletRequestBO.getCoinType().toString());
            walletTransfer.setFromAddress(walletRequestBO.getFromAddress());
            walletTransfer.setToAddress(walletRequestBO.getToAddress());
            walletTransfer.setTransferType(TransferTypeEnum.CLIENT_WITHDRAW.getValue());
            walletTransfer.setStatus(TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
            walletTransfer.setQty(walletRequestBO.getAmount());
            walletTransfer.setWithdrawOrderNo(walletRequestBO.getWithdrawOrderNo());
            walletTransfer.setCreateTime(new Date());
            walletTransferService.insertTransferRecord(walletTransfer);
            walletRequestBO.setTransferNo(transferNo);
            walletRequestBO.setTransferType(TransferTypeEnum.CLIENT_WITHDRAW.getValue());
            WalletRespResultVO result = serviceFactory.getWalletService(
                    walletRequestBO.getCoinType()).withDraw(walletRequestBO);

            if (result == null || StringUtils.isBlank(result.getTxHashId())) {
                WalletTransfer transferTmp = new WalletTransfer();
                transferTmp.setTransferNo(transferNo);
                transferTmp.setStatus(TransferStatusEnum.TRANSFER_FAILURE.getValue());
                walletSupport.updateStatusByTransferNo(transferTmp);
                return ResponseData.success();
            }
            if (StringUtils.isNotBlank(result.getTxHashId())) {
                WalletTransferBO transferParam = new WalletTransferBO();
                transferParam.setTransferNo(transferNo);
                transferParam.setTxId(result.getTxHashId());
                walletTransferService.updateWalletTransferByTransferNo(transferParam);
            }
            return ResponseData.success(result);
        } catch (Exception e) {
            logger.error("withDraw : {}", e.getMessage());
            WalletTransfer transferTmp = new WalletTransfer();
            transferTmp.setTransferNo(transferNo);
            transferTmp.setStatus(TransferStatusEnum.TRANSFER_FAILURE.getValue());
            walletSupport.updateStatusByTransferNo(transferTmp);
            return ResponseData.success();
        }
    }

    @Override
    public void transferMainAddress(CoinEnum coinType) {
        serviceFactory.getWalletService(coinType).transferMainAddress(coinType);
    }

    @Override
    public void transferColdAddress(CoinEnum coinType) {
        serviceFactory.getWalletService(coinType).transferColdAddress(coinType);
    }

    @Override
    public void insertRechargeCoin(CoinEnum coinType) {
        serviceFactory.getWalletService(coinType).insertRechargeCoin(coinType);
    }

    @Override
    public void updateWithdrawCoin(CoinEnum coinType) {
        serviceFactory.getWalletService(coinType).updateWithdrawCoin(coinType);
    }

    @Override
    public void toAddressTransactionFee(CoinEnum coinType) {
//        serviceFactory.getWalletService(coinType).toAddressTransactionFee(coinType);
    }

    @Override
    public ResponseData<Boolean> recordAssetsInfoByAddress(WalletAssetsRecordReqBO walletAssetsRecordReqBO) {
        CoinEnum coinType = walletAssetsRecordReqBO.getCoinType();
        Boolean flag = false;
        try {
            flag = serviceFactory.getWalletService(coinType).recordAssetsInfoByAddress(walletAssetsRecordReqBO);
        } catch (Exception e) {
            logger.error("recordAssetsInfoByAddress exception ", e);
        }
        return ResponseData.success(flag);
    }

    @Override
    public void dealExceptionWithdraw(CoinEnum coinType) {
        serviceFactory.getWalletService(coinType).dealExceptionWithdraw(coinType);
    }
}
