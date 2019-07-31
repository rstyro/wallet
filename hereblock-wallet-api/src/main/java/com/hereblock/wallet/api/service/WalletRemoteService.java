package com.hereblock.wallet.api.service;

import com.hereblock.common.model.ResponseData;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.model.AddressVO;
import com.hereblock.wallet.api.model.WalletAssetsRecordReqBO;
import com.hereblock.wallet.api.model.WalletRequestBO;
import com.hereblock.wallet.api.model.WalletRespResultVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;

/**
 * 钱包接口
 *
 * @Author Goasin
 * @Since 2019-05-10
 */
public interface WalletRemoteService {
    /**
     * 获得钱包地址
     *
     * @param walletRequestBO
     * @return 钱包地址
     */
    @RequestMapping(value = "/wallet/address", method = RequestMethod.POST)
    ResponseData<String> getNewAddress(@RequestBody WalletRequestBO walletRequestBO);

    /**
     * 查询用户所有资产地址
     *
     * @param walletRequestBO
     * @return 钱包地址
     */
    @RequestMapping(value = "/wallet/user/address/list", method = RequestMethod.POST)
    ResponseData<AddressVO> queryUserAllAddress(@RequestBody WalletRequestBO walletRequestBO);

    /**
     * 获取余额
     *
     * @param walletRequestBO
     * @return 余额
     */
    @RequestMapping(value = "/wallet/balance", method = RequestMethod.POST)
    ResponseData<BigDecimal> getBalance(@RequestBody WalletRequestBO walletRequestBO);

    /**
     * 提币
     *
     * @param walletRequestBO
     * @return 交易ID
     */
    @RequestMapping(value = "/wallet/with-draw", method = RequestMethod.POST)
    ResponseData<WalletRespResultVO> withDraw(@RequestBody WalletRequestBO walletRequestBO);

    /**
     * 钱包归热
     */
    @RequestMapping(value = "/wallet/transfer-main-address", method = RequestMethod.POST)
    void transferMainAddress(@RequestBody CoinEnum coinType);

    /**
     * 钱包归冷
     */
    @RequestMapping(value = "/wallet/transfer-cold-address", method = RequestMethod.POST)
    void transferColdAddress(@RequestBody CoinEnum coinType);

    /**
     * 定时任务获取链上充值记录，更新用户资产
     */
    @RequestMapping(value = "/wallet/insert-recharge-coin", method = RequestMethod.POST)
    void insertRechargeCoin(@RequestBody CoinEnum coinType);

    /**
     * 定时任务获取链上提现已确认记录，更新用户提币记录
     */
    @RequestMapping(value = "/wallet/update-withdraw-coin", method = RequestMethod.POST)
    void updateWithdrawCoin(@RequestBody CoinEnum coinType);

    /**
     * 为用户地址转手续费，已改成指定矿工费地址，此接口废弃
     *
     * @param coinType
     */
    @RequestMapping(value = "/wallet/to-address-transaction-fee", method = RequestMethod.POST)
    void toAddressTransactionFee(@RequestBody CoinEnum coinType);

    /**
     * 根据钱包地址记录资产信息
     *
     * @param walletAssetsRecordReqBO
     * @return
     */
    @RequestMapping(value = "/wallet/record-assets-info-by-address", method = RequestMethod.POST)
    ResponseData<Boolean> recordAssetsInfoByAddress(@RequestBody WalletAssetsRecordReqBO walletAssetsRecordReqBO);

    /**
     * 处理提币异常数据
     */
    @RequestMapping(value = "/wallet/deal-exception", method = RequestMethod.POST)
    void dealExceptionWithdraw(@RequestBody CoinEnum coinType);
}
