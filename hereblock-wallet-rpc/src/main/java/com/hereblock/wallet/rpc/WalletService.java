package com.hereblock.wallet.rpc;

import com.hereblock.common.model.ResponseData;
import com.hereblock.wallet.api.WalletInterface;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.model.AddressVO;
import com.hereblock.wallet.api.model.WalletAssetsRecordReqBO;
import com.hereblock.wallet.api.model.WalletRequestBO;
import com.hereblock.wallet.api.model.WalletRespResultVO;
import com.hereblock.wallet.api.service.WalletRemoteService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @Author Goasin
 * @Since 2019-04-25
 */
@FeignClient(name = WalletInterface.SERVICE_NAME, fallback = WalletService.HystrixClientFallback.class)
public interface WalletService extends WalletRemoteService {
    @Component
    public static class HystrixClientFallback implements WalletService {
        @Override
        public ResponseData<String> getNewAddress(WalletRequestBO walletRequestBO) {
            return null;
        }

        @Override
        public ResponseData<BigDecimal> getBalance(WalletRequestBO walletRequestBO) {
            return null;
        }

        @Override
        public ResponseData<WalletRespResultVO> withDraw(WalletRequestBO walletRequestBO) {
            return null;
        }

        @Override
        public void transferMainAddress(CoinEnum coinType) {

        }

        @Override
        public void transferColdAddress(CoinEnum coinType) {

        }

        @Override
        public void insertRechargeCoin(CoinEnum coinType) {

        }

        @Override
        public void updateWithdrawCoin(CoinEnum coinType) {

        }

        @Override
        public void toAddressTransactionFee(CoinEnum coinType) {

        }

        @Override
        public ResponseData<Boolean> recordAssetsInfoByAddress(WalletAssetsRecordReqBO walletAssetsRecordReqBO) {
            return null;
        }

        @Override
        public ResponseData<AddressVO> queryUserAllAddress(WalletRequestBO walletRequestBO) {
            return null;
        }

        @Override
        public void dealExceptionWithdraw(CoinEnum coinType) {

        }
    }
}
