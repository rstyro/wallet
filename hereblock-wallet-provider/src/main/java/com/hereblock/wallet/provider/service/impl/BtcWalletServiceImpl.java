package com.hereblock.wallet.provider.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hereblock.account.api.model.RechargeAddressBO;
import com.hereblock.account.api.model.WithdrawBO;
import com.hereblock.account.api.model.vo.CoinVO;
import com.hereblock.account.api.model.vo.RechargeAddressVO;
import com.hereblock.account.rpc.UserCoinService;
import com.hereblock.common.exception.AppException;
import com.hereblock.common.model.ResponseData;
import com.hereblock.framework.mq.base.RMQMessageBuilder;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.enums.TransferStatusEnum;
import com.hereblock.wallet.api.enums.TransferTypeEnum;
import com.hereblock.wallet.api.model.*;
import com.hereblock.wallet.provider.constant.Constant;
import com.hereblock.wallet.provider.entity.WalletTransfer;
import com.hereblock.wallet.provider.mapper.WalletTransferMapper;
import com.hereblock.wallet.provider.mq.consumer.producer.WalletProducer;
import com.hereblock.wallet.provider.service.BtcWalletService;
import com.hereblock.wallet.provider.service.WalletService;
import com.hereblock.wallet.provider.service.WalletTransferService;
import com.hereblock.wallet.provider.support.WalletSupport;
import com.hereblock.wallet.provider.util.CoinRpcClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wf.bitcoin.javabitcoindrpcclient.BitcoinRawTxBuilder;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BtcWalletServiceImpl extends BtcWalletService implements WalletService {
    private Logger logger = LoggerFactory.getLogger(BtcWalletServiceImpl.class);
    @Autowired
    private CoinRpcClient client;
    @Value("${btc.fee.address}")
    private String fromAddress;
    @Value("${btc.server.url}")
    private String url;
    @Value("${btc.server.basicauth}")
    private String basicAuth;

    @Autowired
    @Qualifier("walletSupport")
    private WalletSupport walletSupport;
    @Autowired
    private UserCoinService userCoinService;
    @Autowired
    private WalletProducer walletProducer;
    @Autowired
    private WalletTransferService walletTransferService;
    @Autowired
    private WalletTransferMapper walletTransferMapper;

    @Override
    public CoinEnum getCode() {
        return CoinEnum.BTC;
    }

    @Override
    public String getNewAddress(WalletRequestBO walletRequestBO) throws Throwable {
        return client.getClient(url, basicAuth).invoke("getnewaddress", new Object[]{}, String.class);
    }

    @Override
    public BigDecimal getBalance(WalletRequestBO walletRequestBO) {
        try {
            return client.getClient(url, basicAuth).invoke("getbalance", new Object[]{}, BigDecimal.class);
        } catch (Throwable e) {
            logger.error("btc getBalance exception===>", e);
        }
        return null;
    }

    @Override
    public WalletRespResultVO withDraw(WalletRequestBO walletRequestBO) {
        CoinVO coin = walletSupport.getCoinByType(CoinEnum.BTC.getValue());
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
        String txHashId = this.sendTransaction(btcRpcClient, walletRequestBO.getFromAddress(),
                walletRequestBO.getToAddress(), walletRequestBO.getAmount(), coin.getWithdrawTxFee());
        WalletRespResultVO result = new WalletRespResultVO();
        result.setTxHashId(txHashId);
        return result;
    }

    @Override
    public WalletRespResultVO transferWithDraw(WalletRequestBO walletRequestBO, CoinVO coin) {
        return null;
    }

    @Override
    public void transferMainAddress(CoinEnum coinType) {
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        //todo 获取用户地址，目前从资产服务获取，需改成本地钱包获取
        RechargeAddressBO rechargeAddress = RechargeAddressBO.newBuilder().type(coinType.getValue()).build();
        ResponseData<List<RechargeAddressVO>> responseData = userCoinService.rechargeAddressList(rechargeAddress);
        List<RechargeAddressVO> rechargeAddressList = responseData.getData();
        logger.warn("transferMainAddress ==>>>> rechargeAddressListTmp:{}", JSON.toJSONString(rechargeAddressList));
        if (CollectionUtils.isEmpty(rechargeAddressList)) {
            return;
        }

        //是否要归集usdt子地址下面的矿工费btc到btc热钱包，是的话放开下面注释的4行代码
//        RechargeAddressBO rechargeUsdtAddress = RechargeAddressBO.newBuilder().type(CoinEnum.USDT.getValue()).build();
//        ResponseData<List<RechargeAddressVO>> responseUsdtData = userCoinService.rechargeAddressList(rechargeUsdtAddress);
//        List<RechargeAddressVO> rechargeUsdtAddressList = responseUsdtData.getData();
//        List<RechargeAddressVO> rechargeAddressListTmp = Stream.of(rechargeUsdtAddressList, rechargeAddressList)
//        .flatMap(Collection::stream).distinct().collect(Collectors.toList());

        for (RechargeAddressVO address : rechargeAddressList) {
            if (StringUtils.equals(address.getAddress(), coin.getMainAddress()) ||
                    StringUtils.equals(address.getAddress(), coin.getColdAddress())) continue;

            BigDecimal btcbalance = new BigDecimal("0");
            List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent(1, 99999999, address.getAddress());
            for (BitcoindRpcClient.Unspent unspent : unspents) {
                btcbalance = btcbalance.add(unspent.amount());
            }
            logger.warn("执行transferMainAddress===>>>> address.getAddress():{},btcbalance:{}", address.getAddress(), btcbalance);
            if (btcbalance.compareTo(coin.getOutQtyToMainAddress()) > 0) {
                BigDecimal amount = btcbalance.subtract(coin.getWithdrawTxFee());
                String txId = this.sendTransaction(btcRpcClient, address.getAddress(), coin.getMainAddress(), amount, coin.getWithdrawTxFee());
                logger.warn("执行transferMainAddress===>>>> address.getAddress():{},btcbalance:{},txId:{}", address.getAddress(), btcbalance, txId);
                //logger.info("transferMainAddress btc to main address ===>>> txId : {}", txId);
                LinkedHashMap transaction = this.getTransaction(txId);
                logger.warn("执行transferMainAddress===>>>> address.getAddress():{},btcbalance:{},transaction:{}", address.getAddress(), btcbalance, JSON.toJSONString(transaction));
                if (org.springframework.util.CollectionUtils.isEmpty(transaction)) {
                    continue;
                }
                BigDecimal fee = new BigDecimal(transaction.getOrDefault("fee", "0").toString());

                walletSupport.insertTransferRecord(coinType.toString(), address.getAddress(), coin.getMainAddress(),
                        TransferTypeEnum.WITHDRAW_TO_MAIN_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                        amount, fee, txId, new BigDecimal("0"), new BigDecimal("0"));
            }
        }
    }

    private String sendTransaction(BitcoindRpcClient btcRpcClient, String fromAddress, String targetAddress, BigDecimal amount, BigDecimal txFee) {
        logger.warn("执行sendTransaction方法，fromAddress={}, targetAddress={}, amount={}, txFee={}",
                fromAddress, targetAddress, amount, txFee);
        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent(1, 99999999, fromAddress);
        BigDecimal moneySpent = BigDecimal.ZERO;
        BigDecimal totalUnSpent = BigDecimal.ZERO;
        String changeAddress = fromAddress;
        BitcoinRawTxBuilder builder = new BitcoinRawTxBuilder(btcRpcClient);

        for (BitcoindRpcClient.Unspent unspent : unspents) {
            totalUnSpent = totalUnSpent.add(unspent.amount());
        }

        for (BitcoindRpcClient.Unspent unspent : unspents) {
            moneySpent = moneySpent.add(unspent.amount());
            builder.in(new BitcoindRpcClient.BasicTxInput(unspent.txid(), unspent.vout()));
            if (moneySpent.compareTo(amount.add(txFee)) >= 0.00006) {
                break;
            }
        }
        logger.warn("执行sendTransaction方法，totalUnSpent={}，moneySpent={}, txFee={}, amount={}", totalUnSpent, moneySpent, txFee, amount);
        if (moneySpent.compareTo(amount.add(txFee)) < 0) {
            throw new AppException("热钱包剩余资产不足");
        }
        BigDecimal moneyChange = moneySpent.subtract(amount.add(txFee));
        builder.out(targetAddress, amount);
        if (moneyChange.compareTo(BigDecimal.ZERO) > 0) {
            builder.out(changeAddress, moneyChange);
        }
        return builder.send();
    }

    @Override
    public void transferColdAddress(CoinEnum coinType) {
        //logger.debug("btc transferColdAddress======> coinType:{}", JSON.toJSONString(coinType));
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        BigDecimal btcBalance = new BigDecimal("0");
        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent(1, 99999999, coin.getMainAddress());
        for (BitcoindRpcClient.Unspent unspent : unspents) {
            btcBalance = btcBalance.add(unspent.amount());
        }
        //logger.debug("transferColdAddress btc main address balance :{}", btcBalance.doubleValue());
        BigDecimal outQtyToColdAddress = coin.getOutQtyToColdAddress();
        if (btcBalance.compareTo(outQtyToColdAddress) > 0) {
            //需要转入冷钱包
            BigDecimal amount = btcBalance.subtract(outQtyToColdAddress);
            WalletRequestBO walletRequestBO = new WalletRequestBO();
            walletRequestBO.setToAddress(coin.getColdAddress());
            walletRequestBO.setAmount(amount);
            logger.warn("执行transferColdAddress, amount:{},coin.getMainAddress:{},coin.getColdAddress:{}，" +
                    "coin.getWithdrawTxFee:{}", amount, coin.getMainAddress(), coin.getColdAddress(), coin.getWithdrawTxFee());
            String txId = this.sendBtcByAmount(btcRpcClient, coin.getColdAddress(), amount, coin.getMainAddress(), coin.getWithdrawTxFee());
            logger.warn("btc transferColdAddress======> txId:{}", txId);
            LinkedHashMap transaction = this.getTransaction(txId);
            logger.warn("btc transferColdAddress======> transaction:{}", JSON.toJSONString(transaction));
            if (org.springframework.util.CollectionUtils.isEmpty(transaction)) {
                return;
            }
            BigDecimal fee = new BigDecimal(transaction.getOrDefault("fee", "0").toString());

            walletSupport.insertTransferRecord(coinType.toString(), coin.getMainAddress(), coin.getColdAddress(),
                    TransferTypeEnum.WITHDRAW_TO_COLD_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                    amount, fee, txId, new BigDecimal("0"), new BigDecimal("0"));
            logger.warn("执行transferColdAddress, insertTransferRecord");

        }
    }

    /**
     * btc 转账
     *
     * @param btcRpcClient
     * @param to           转入目标地址
     * @param amount       转账数量
     * @param mainAddress  转出地址，从主地址转出
     * @param fee          转账手续费
     * @return txid 交易hash值
     */
    private String sendBtcByAmount(BitcoindRpcClient btcRpcClient, String to, BigDecimal amount, String mainAddress, BigDecimal fee) {
        List<BitcoindRpcClient.Unspent> listUnspent =
                btcRpcClient.listUnspent(1, 99999999, mainAddress);
        logger.warn("sendBtcByAmount, listUnspent:{}", JSON.toJSONString(listUnspent));

        BigDecimal balance = BigDecimal.ZERO;
        amount = amount.add(fee);
        List<JSONObject> createrawtransactionArgs = Lists.newArrayList();
        List<JSONObject> omniCreaterawtxChangeArgs = Lists.newArrayList();
        //从大到小排序
        listUnspent = listUnspent.stream().sorted(
                Comparator.comparing(BitcoindRpcClient.Unspent::amount).reversed()).collect(Collectors.toList());//逆序
        for (BitcoindRpcClient.Unspent unspent : listUnspent) {
            logger.warn("====================, unspent.address:{},mainAddress:{}", unspent.address(), mainAddress);
//            if (StringUtils.equals(unspent.address(), mainAddress)) {
            if (mainAddress.equals(unspent.address())) {
                logger.warn("**********************, unspent.account:{},amount:{}", unspent.amount(), amount);
                JSONObject createrawFeeMap = new JSONObject();
                createrawFeeMap.put("txid", unspent.txid());
                createrawFeeMap.put("vout", unspent.vout());
                createrawtransactionArgs.add(createrawFeeMap);
                JSONObject omniCreaterawtxFeeMap = new JSONObject();
                omniCreaterawtxFeeMap.put("txid", unspent.txid());
                omniCreaterawtxFeeMap.put("vout", unspent.vout());
                omniCreaterawtxFeeMap.put("scriptPubKey", unspent.scriptPubKey());
                omniCreaterawtxFeeMap.put("amount", unspent.amount());
                omniCreaterawtxChangeArgs.add(omniCreaterawtxFeeMap);
                if (unspent.amount().compareTo(amount) >= 0) {
                    balance = balance.add(unspent.amount());
                    break;
                } else {
                    balance = balance.add(unspent.amount());
                    if (balance.compareTo(amount) > 0) break;
                }
            }
        }

        logger.warn("执行sendBtcByAmount方法, balance:{}", JSON.toJSONString(balance));
        if (balance.compareTo(amount) < 0
                || CollectionUtils.isEmpty(createrawtransactionArgs)
                || CollectionUtils.isEmpty(omniCreaterawtxChangeArgs)) {
            logger.warn("未找到合适的手续费地址");
            throw new AppException("未找到合适的手续费地址");
        }

        LinkedHashMap<String, BigDecimal> adrressJson = new LinkedHashMap<>();
        //到账地址
        adrressJson.put(to, amount.subtract(fee).setScale(8, BigDecimal.ROUND_HALF_DOWN));
        //找零地址
        if (!(balance.subtract(amount).compareTo(BigDecimal.ZERO) == 0)) {
            adrressJson.put(mainAddress, balance.subtract(amount).setScale(8, BigDecimal.ROUND_HALF_DOWN));
        }
        try {
            String createRawTransaction = StringUtils.defaultIfBlank((String) walletSupport.createRawTransaction(createrawtransactionArgs, adrressJson), "");
            LinkedHashMap signrawtransaction = (LinkedHashMap) walletSupport.signRawTransaction(createRawTransaction, omniCreaterawtxChangeArgs);
            //广播
            return StringUtils.defaultIfBlank((String) walletSupport.sendRawTransaction(
                    StringUtils.defaultIfBlank((String) signrawtransaction.get("hex"), "")), "");
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


    @Override
    public Object listunspent() {
        try {
            return client.getClient(url, basicAuth).invoke("listunspent", new Object[]{}, Object.class);
        } catch (Throwable e) {
            logger.error("listunspent exception===>", e);
        }
        return null;

    }

    @Override
    public void insertRechargeCoin(CoinEnum type) {
        walletSupport.insertRechargeCoin(type);
    }

    @Override
    public void updateWithdrawCoin(CoinEnum type) {
        if (type.getValue() != CoinEnum.BTC.getValue()) return;
        List<WalletTransferVO> transferList = walletSupport.selectWalletTransfer(
                type.toString(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
        logger.warn("btc withdraw transferList=:{}", JSON.toJSONString(transferList));
        if (transferList == null) return;
        for (WalletTransferVO transfer : transferList) {
            String txId = transfer.getTxId();
            WithdrawBO withdraw = new WithdrawBO();
            logger.warn("==================btc withdraw txId=:{}", txId);
            if (StringUtils.isBlank(txId)) continue;
            LinkedHashMap transaction = this.getTransaction(txId);
            if (org.springframework.util.CollectionUtils.isEmpty(transaction)) return;
            logger.warn("btc withdraw transaction:{}", JSON.toJSONString(transaction));
            BigDecimal confirmations = new BigDecimal(transaction.getOrDefault("confirmations", "0").toString());
            if (confirmations.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            BigDecimal fee = new BigDecimal(transaction.getOrDefault("fee", "0").toString());
            withdraw.setTxFee(fee.abs());
            withdraw.setConfirmTime(new Date());
            withdraw.setTxId(txId);

            walletSupport.updateWalletTransfer(txId, fee.abs(), transfer.getTransferNo(),
                    TransferStatusEnum.TRANSFER_CONFIRM.getValue(), null, null);
            walletSupport.recordAssetsData(transfer.getQty(), transfer.getAddress(),
                    transfer.getCoinType(), TransferTypeEnum.CLIENT_WITHDRAW.getValue());

            //ResponseData response = userCoinService.withdrawCallBack(withdraw);
            WalletTransferBO params = new WalletTransferBO();
            params.setTxId(txId);
            WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
            logger.warn("====walletTransfer:{}", JSON.toJSONString(walletTransfer));
            //过滤掉归冷归热，归冷归热下不调用资产变更状态
            if (walletTransfer != null) continue;
            if (StringUtils.isNotBlank(transfer.getWithdrawOrderNo())) {
                withdraw.setOrderNo(Long.valueOf(transfer.getWithdrawOrderNo()));
            }
            Message sendMessage = RMQMessageBuilder.of(withdraw).topic(Constant.ACCOUNT_CENTER_TOPIC_WITHDRAW_COMPLETE).build();
            walletProducer.syncSend(sendMessage);
            logger.warn("btc 提币已经确认，通知下游系统更新提币状态：toAddress:{}, txId:{}, withdraw:{}", transfer.getToAddress(),
                    withdraw.getTxId(), JSON.toJSONString(withdraw));
        }
    }

    @Override
    public LinkedHashMap getTransaction(String txId) {
        return walletSupport.getTransaction(txId);
    }

    @Override
    public JSONArray listTransactions() {
        return walletSupport.listTransactions();
    }

    @Override
    public void toAddressTransactionFee(CoinEnum coinType) {
        WalletRequestBO walletRequestBO = new WalletRequestBO();
        walletRequestBO.setFromAddress("151vcKhgfXNVhgW8UcWTUY5rAMoTrRDMyS");
        walletRequestBO.setToAddress("1LMkRJW3R2ayucqd1JtghmYQXnw5HftNfq");
        walletRequestBO.setCoinType(coinType);
        walletRequestBO.setAmount(new BigDecimal("0.001"));
        WalletRespResultVO result = withDraw(walletRequestBO);
        logger.warn("=====================aaaaa==========result:{}", JSON.toJSONString(result));
        /*CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        if(coin == null) return;
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
        BigDecimal btcbalance = new BigDecimal("0");
        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent(1, 99999999, fromAddress);
        for (BitcoindRpcClient.Unspent unspent : unspents) {
            btcbalance = btcbalance.add(unspent.amount());
        }
        if (btcbalance.compareTo(new BigDecimal(0.0004)) > 0) {
            BigDecimal amount = new BigDecimal("0.0003");
            String txId = this.sendTransaction(btcRpcClient,fromAddress,coin.getMainAddress(),amount,coin.getWithdrawTxFee());
            LinkedHashMap transMap = this.getTransaction(txId);

            walletSupport.insertTransferRecord(coinType.toString(),fromAddress,coin.getMainAddress(),
                    TransferTypeEnum.APPLY_FEE.getValue(),TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                    amount,new BigDecimal(transMap.getOrDefault("fee","0").toString()),txId,new BigDecimal("0"),new BigDecimal("0"));

        }*/
    }

    @Override
    public Boolean recordAssetsInfoByAddress(WalletAssetsRecordReqBO walletAssetsRecordReqBO) {
        return walletSupport.recordAssetsData(walletAssetsRecordReqBO.getAccount(), walletAssetsRecordReqBO.getAddress(),
                walletAssetsRecordReqBO.getCoinType().toString(), walletAssetsRecordReqBO.getTransferType());
    }

    @Override
    public void dealExceptionWithdraw(CoinEnum coinType) {
        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        List<WalletTransfer> list = walletTransferMapper.selectExceptionWalletTransfer(coinType.toString());
        logger.warn("========1111111111111111111===============>> list : {}", JSON.toJSONString(list));
        if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
            for (WalletTransfer walletTransfer : list) {
                WalletRequestBO walletRequestBO = new WalletRequestBO();
                walletRequestBO.setFromAddress(coin.getMainAddress());
                walletRequestBO.setAddress(walletTransfer.getToAddress());
                walletRequestBO.setToAddress(walletTransfer.getToAddress());
                walletRequestBO.setAmount(walletTransfer.getQty());
                WalletRespResultVO withdraw = this.withDraw(walletRequestBO);
                logger.warn("========222222222222222===============>> withdraw : {}", JSON.toJSONString(withdraw));
                if (withdraw == null || StringUtils.isBlank(withdraw.getTxHashId())) continue;
                try {
                    WalletTransfer walletTransferParam = new WalletTransfer();
                    walletTransferParam.setCoinType(coinType.toString());
                    walletTransferParam.setTransferNo(walletTransfer.getTransferNo());
                    walletTransferParam.setStatus(TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
                    walletTransferParam.setTxId(withdraw.getTxHashId());
                    walletTransferMapper.updateStatusByTransferNo(walletTransferParam);
                    withdraw.setWithdrawOrderNo(walletTransfer.getWithdrawOrderNo());
                    Message sendMessage = RMQMessageBuilder.of(withdraw).topic(Constant.ACCOUNT_CENTER_TOPIC_WITHDRAW_RESEND).build();
                    walletProducer.syncSend(sendMessage);
                    logger.warn("重新提BTC币成功，walletTransfer：{}, sendMessage{}", JSON.toJSONString(walletTransfer), JSON.toJSONString(sendMessage));
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }
}
