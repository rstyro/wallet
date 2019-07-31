package com.hereblock.wallet.provider.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hereblock.account.api.model.RechargeAddressBO;
import com.hereblock.account.api.model.WithdrawBO;
import com.hereblock.account.api.model.vo.CoinVO;
import com.hereblock.account.api.model.vo.RechargeAddressVO;
import com.hereblock.account.rpc.UserCoinService;
import com.hereblock.common.exception.AppException;
import com.hereblock.common.model.ResponseData;
import com.hereblock.framework.api.model.ResponseCode;
import com.hereblock.framework.mq.base.RMQMessageBuilder;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.enums.TransferStatusEnum;
import com.hereblock.wallet.api.enums.TransferTypeEnum;
import com.hereblock.wallet.api.model.*;
import com.hereblock.wallet.provider.constant.Constant;
import com.hereblock.wallet.provider.entity.WalletTransfer;
import com.hereblock.wallet.provider.mapper.WalletTransferMapper;
import com.hereblock.wallet.provider.mq.consumer.producer.WalletProducer;
import com.hereblock.wallet.provider.service.UsdtWalletService;
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
import org.springframework.util.ObjectUtils;
import org.web3j.utils.Strings;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * USDT
 * 参考文档：http://cw.hubwiz.com/card/c/omni-rpc-api/1/1/1/
 */
@Service
public class UsdtWalletServiceImpl extends UsdtWalletService implements WalletService {
    private Logger logger = LoggerFactory.getLogger(UsdtWalletServiceImpl.class);
    @Value("${btc.server.url}")
    private String url;
    @Value("${btc.server.basicauth}")
    private String basicAuth;
    @Value("${btc.fee.usdt.address}")
    private String BTCAbsenceAddress;//btc矿工费地址
    @Value("${btc.fee.amount}")
    private BigDecimal amount;//往子地址打btc手续费，目前配置是0.0001

    @Autowired
    private CoinRpcClient client;
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
        return CoinEnum.USDT;
    }

    @Override
    public String getNewAddress(WalletRequestBO walletRequestBO) throws Throwable {
        return client.getClient(url, basicAuth).invoke("getnewaddress", new Object[]{}, String.class);
    }

    @Override
    public BigDecimal getBalance(WalletRequestBO walletRequestBO) {
        try {
            LinkedHashMap map = (LinkedHashMap) client.getClient(url, basicAuth).invoke("omni_getbalance",
                    new Object[]{walletRequestBO.getAddress(), new Integer(walletRequestBO.getPropertyid())}, Object.class);
            if (map == null) {
                return new BigDecimal(0);
            }
            return new BigDecimal((String) map.get("balance"));
        } catch (Throwable e) {
            logger.error("usdt getBalance exception===>", e);
        }
        return null;
    }

    @Override
    public WalletRespResultVO withDraw(WalletRequestBO walletRequestBO) {
        CoinVO coin = walletSupport.getCoinByType(CoinEnum.USDT.getValue());
        return transferWithDraw(walletRequestBO, coin);
    }

    @Override
    public WalletRespResultVO transferWithDraw(WalletRequestBO walletRequestBO, CoinVO coin) {
        if (coin == null) return null;
        walletRequestBO.setAddress(walletRequestBO.getFromAddress());
        Object balance = this.getBalance(walletRequestBO);
        if (balance == null) {
            return null;
        }
        try {
            String txId = this.omniFundedSend(walletRequestBO);
//            String txId = this.sendByRawTransaction(walletRequestBO.getFromAddress(), walletRequestBO.getToAddress(), walletRequestBO.getAmount(),
//                    coin.getPropertyId());
            WalletRespResultVO result = new WalletRespResultVO();
            result.setTxHashId(txId);
            return result;
        } catch (Throwable e) {
            logger.error("usdt withDraw exception===>", e);
        }
        return null;
    }

    public WalletRespResultVO transferWithDrawMainAddress(WalletRequestBO walletRequestBO, CoinVO coin) {
        if (coin == null) return null;
        walletRequestBO.setAddress(walletRequestBO.getFromAddress());
        try {
            String txId = this.sendByRawTransactionMainAddress(walletRequestBO.getFromAddress(), walletRequestBO.getToAddress(), walletRequestBO.getAmount(),
                    coin.getPropertyId());
            WalletRespResultVO result = new WalletRespResultVO();
            result.setTxHashId(txId);
            return result;
        } catch (Throwable e) {
            logger.error("usdt withDraw exception===>", e);
        }
        return null;
    }

    public String sendByRawTransactionMainAddress(String from, String to, BigDecimal balance, Integer propertyId)
            throws AppException {

        //找零地址
        String changeAddress = from;
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent();
        logger.warn("usdt to main address sendByRawTransaction===>>> unspents : {}", JSON.toJSONString(unspents));
        List<JSONObject> createrawtransactionArgs = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("txid", e.txid());
                    jsonObject.put("vout", e.vout());
                    return jsonObject;
                }).collect(Collectors.toList());
        logger.warn("usdt to main address sendByRawTransaction===>>> createrawtransactionArgs : {}", JSON.toJSONString(createrawtransactionArgs));
        if (CollectionUtils.isEmpty(createrawtransactionArgs)) {
            //说明并没有转入记录
            return null;
        }

        List<JSONObject> omniCreaterawtxChangeArgs = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("txid", e.txid());
                    jsonObject.put("vout", e.vout());
                    jsonObject.put("scriptPubKey", e.scriptPubKey());
                    jsonObject.put("value", e.amount());
                    return jsonObject;
                }).collect(Collectors.toList());
        BigDecimal amount = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.warn("usdt to main address sendByRawTransaction===>>> omniCreaterawtxChangeArgs : {},amount:{}",
                JSON.toJSONString(omniCreaterawtxChangeArgs), amount);
        if (amount.compareTo(new BigDecimal("0.0001").add(new BigDecimal("0.00000546"))) < 0) {
            CoinVO coin = walletSupport.getCoinByType(CoinEnum.USDT.getValue());
            Optional<BitcoindRpcClient.Unspent> first = unspents.stream()
                    .filter(unspent -> !unspent.address().equalsIgnoreCase(from) &&
                            !StringUtils.equalsIgnoreCase(unspent.address(), to))
                    .filter(e -> {
                        String address = e.address();
                        try {
                            WalletRequestBO walletRequestBO = new WalletRequestBO();
                            walletRequestBO.setAddress(address);
                            walletRequestBO.setPropertyid(propertyId);
                            BigDecimal getbalance = this.getBalance(walletRequestBO);
                            return getbalance.compareTo(coin.getOutQtyToMainAddress()) < 0;
                        } catch (Exception e1) {
                            throw new AppException(ResponseCode.SYSTEM_EXCEPTION);
                        }

                    })
                    .filter(e -> e.amount().compareTo(new BigDecimal("0.0001")) > 0)
                    .findFirst();

            BitcoindRpcClient.Unspent unspentTmp = null;
            if (first.isPresent()) {
//                throw new MyException("没有足够的BTC余额");
                unspentTmp = first.get();
            } else {
                Optional<BitcoindRpcClient.Unspent> fromUnspentOptional = unspents.stream()
                        .filter(unspent -> unspent.address().equalsIgnoreCase(to) &&
                                unspent.amount().compareTo(new BigDecimal("0.0001").add(new BigDecimal("0.00000546"))) >= 0)
                        .min(Comparator.comparing(unspent -> unspent.amount()));

                if (fromUnspentOptional.isPresent()) {
                    unspentTmp = fromUnspentOptional.get();
                } else {
                    //throw new MyException("没有足够的BTC余额");
                    return null;
                }


            }
            JSONObject createrawFeeMap = new JSONObject();
            createrawFeeMap.put("txid", unspentTmp.txid());
            createrawFeeMap.put("vout", unspentTmp.vout());
            createrawtransactionArgs.add(createrawFeeMap);

            JSONObject omniCreaterawtxFeeMap = new JSONObject();
            omniCreaterawtxFeeMap.put("txid", unspentTmp.txid());
            omniCreaterawtxFeeMap.put("vout", unspentTmp.vout());
            omniCreaterawtxFeeMap.put("scriptPubKey", unspentTmp.scriptPubKey());
            omniCreaterawtxFeeMap.put("value", unspentTmp.amount());
            omniCreaterawtxChangeArgs.add(omniCreaterawtxFeeMap);
        }
        logger.warn("usdt to main address sendByRawTransaction=====6666666666==============>>> omniCreaterawtxChangeArgs : {},createrawtransactionArgs:{}",
                JSON.toJSONString(omniCreaterawtxChangeArgs), createrawtransactionArgs);

//        BigDecimal fee = new BigDecimal("0.0001");
        try {
            //调用穿件一个用于简单发送交易的载荷。 构造发送代币类型和代币数量数据（payload）

            logger.warn("执行omni_createpayload_simplesend命令, propertyId：{}，balance.toString:{}", propertyId, balance.toString());
            String createpayloadSimplesend = client.getClient(url, basicAuth).invoke(
                    "omni_createpayload_simplesend",
                    new Object[]{propertyId, balance.toString()}, String.class);

            //构造交易基本数据
            String createrawtransaction = StringUtils.defaultIfBlank((String) walletSupport.createRawTransaction(
                    createrawtransactionArgs, new HashMap<>()), "");

            //        调用将一个op-return操作载荷添加到交易中。
            //        如果未提供裸交易，那么本调用将创建一个新的奇偶阿姨。
            //        如果数据编码失败，那么交易将不会被修改。在交易数据中加上omni代币数据
            logger.warn("执行omni_createrawtx_opreturn命令, createrawtransaction：{}，createpayloadSimplesend:{}",
                    createrawtransaction, createpayloadSimplesend);
            String omniCreaterawtxOpreturn = client.getClient(url, basicAuth).invoke(
                    "omni_createrawtx_opreturn",
                    new Object[]{createrawtransaction, createpayloadSimplesend}, String.class);

            //        omni_createrawtx_reference调用讲一个参考输出添加到交易中。
            //        如果没有提供裸交易，那么调用将创建一个新的交易。
            //        输出值被设置为尘埃交易阈值。 在交易数据上加上接收地址

            logger.warn("执行omni_createrawtx_reference命令, omniCreaterawtxOpreturn：{}，to:{}", createrawtransaction, to);
            String omniCreaterawtxReference = client.getClient(url, basicAuth).invoke(
                    "omni_createrawtx_reference", new Object[]{omniCreaterawtxOpreturn, to}, String.class);

            //        omni_createrawtx_reference调用讲一个参考输出添加到交易中。
            //        如果没有提供裸交易，那么调用将创建一个新的交易。  在交易数据上指定矿工费用
            //        输出值被设置为尘埃交易阈值。Specify miner fee and attach change output (as needed) 此时 to为找零地址
            logger.warn("执行omni_createrawtx_change命令, omniCreaterawtxReference：{}，omniCreaterawtxChangeArgs:{}，" +
                    "changeAddress:{}", omniCreaterawtxReference, omniCreaterawtxChangeArgs, changeAddress);
            CoinVO coin = walletSupport.getCoinByType(CoinEnum.USDT.getValue());
            String omniCreaterawtxChange = client.getClient(url, basicAuth).invoke("omni_createrawtx_change",
                    new Object[]{
                            omniCreaterawtxReference,
                            omniCreaterawtxChangeArgs,
                            changeAddress,
                            coin.getWithdrawTxFee()}, String.class);
            //签名
            logger.warn("执行signrawtransaction命令, omniCreaterawtxChange：{}", omniCreaterawtxChange);
            LinkedHashMap signrawtransaction = client.getClient(url, basicAuth).invoke(
                    "signrawtransaction", new Object[]{omniCreaterawtxChange}, LinkedHashMap.class);
            //广播
            logger.warn("执行sendrawtransaction命令, signrawtransaction：{}", JSON.toJSONString(signrawtransaction));
            String txId = StringUtils.defaultIfBlank((String) walletSupport.sendRawTransaction(
                    StringUtils.defaultIfBlank((String) signrawtransaction.get("hex"), "")), "");
            logger.warn("执行完毕 usdt sendByRawTransaction txId:{}", txId);
            return txId;
        } catch (Throwable e) {
            logger.error("usdt sendByRawTransaction exception  ", e);
        }
        return null;
    }

    public String sendByRawTransaction(String from, String to, BigDecimal balance, Integer propertyId) throws AppException {
        //找零地址
        String changeAddress = from;
        BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
//        BigDecimal amount = new BigDecimal("0");
        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent(0, 999999);
        logger.warn("usdt to main address sendByRawTransaction===>>> from : {}, to : {}, balance : {}, propertyId : {}", from, to, JSON.toJSONString(balance), propertyId);
        logger.warn("usdt to main address sendByRawTransaction===>>> unspents : {}", JSON.toJSONString(unspents));
        List<JSONObject> createrawtransactionArgs = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("txid", e.txid());
                    jsonObject.put("vout", e.vout());
                    return jsonObject;
                }).collect(Collectors.toList());
        logger.warn("usdt to main address sendByRawTransaction===>>> createrawtransactionArgs : {}", JSON.toJSONString(createrawtransactionArgs));
        if (CollectionUtils.isEmpty(createrawtransactionArgs)) {
            //说明并没有转入记录
            return null;
        }

        List<JSONObject> omniCreaterawtxChangeArgs = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("txid", e.txid());
                    jsonObject.put("vout", e.vout());
                    jsonObject.put("scriptPubKey", e.scriptPubKey());
                    jsonObject.put("value", e.amount());
                    return jsonObject;
                }).collect(Collectors.toList());
        BigDecimal amount = unspents.stream()
                .filter(unspent -> unspent.address().equalsIgnoreCase(from))
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.warn("usdt to main address sendByRawTransaction===>>> omniCreaterawtxChangeArgs : {},amount:{}  from : {},to:{} ",
                JSON.toJSONString(omniCreaterawtxChangeArgs), amount, from, to);
        if (amount.compareTo(new BigDecimal("0.0001").add(new BigDecimal("0.00000546"))) < 0) {
            CoinVO coin = walletSupport.getCoinByType(CoinEnum.USDT.getValue());
            Optional<BitcoindRpcClient.Unspent> first = unspents.stream()
                    .filter(unspent -> !unspent.address().equalsIgnoreCase(from) &&
                            !StringUtils.equalsIgnoreCase(unspent.address(), from))
                    .filter(e -> {
                        String address = e.address();
                        try {
                            WalletRequestBO walletRequestBO = new WalletRequestBO();
                            walletRequestBO.setAddress(address);
                            walletRequestBO.setPropertyid(propertyId);
                            BigDecimal getbalance = this.getBalance(walletRequestBO);
                            return getbalance.compareTo(coin.getOutQtyToMainAddress()) < 0;
                        } catch (Exception e1) {
                            throw new AppException(ResponseCode.SYSTEM_EXCEPTION);
                        }

                    })
                    .filter(e -> e.amount().compareTo(new BigDecimal("0.0001")) > 0)
                    .findFirst();
            logger.warn("=============7878787878787=======>>>from : {},to:{} first : {}", from, to, JSON.toJSONString(first));
            BitcoindRpcClient.Unspent unspentTmp = null;
            if (first.isPresent()) {
//                throw new MyException("没有足够的BTC余额");
                unspentTmp = first.get();
                logger.warn("=============123123123=======>>>from : {},to:{} unspentTmp : {}", from, to, JSON.toJSONString(unspentTmp));
            } else {
                Optional<BitcoindRpcClient.Unspent> fromUnspentOptional = unspents.stream()
                        .filter(unspent -> unspent.address().equalsIgnoreCase(from) &&
                                unspent.amount().compareTo(new BigDecimal("0.0001").add(new BigDecimal("0.00000546"))) >= 0)
                        .min(Comparator.comparing(unspent -> unspent.amount()));
                logger.warn("=============78978978979=======>>> from : {},to:{} fromUnspentOptional : {}", from, to, JSON.toJSONString(fromUnspentOptional));
                if (fromUnspentOptional.isPresent()) {
                    unspentTmp = fromUnspentOptional.get();
                    logger.warn("=============456456456=======>>> from : {},to:{} unspentTmp : {}", from, to, JSON.toJSONString(unspentTmp));
                } else {
                    logger.warn("=============没有足够的BTC余额=======>>> from : {},to:{}", from, to);
                    //throw new MyException("没有足够的BTC余额");
                    return null;
                }


            }
            JSONObject createrawFeeMap = new JSONObject();
            createrawFeeMap.put("txid", unspentTmp.txid());
            createrawFeeMap.put("vout", unspentTmp.vout());
            createrawtransactionArgs.add(createrawFeeMap);

            JSONObject omniCreaterawtxFeeMap = new JSONObject();
            omniCreaterawtxFeeMap.put("txid", unspentTmp.txid());
            omniCreaterawtxFeeMap.put("vout", unspentTmp.vout());
            omniCreaterawtxFeeMap.put("scriptPubKey", unspentTmp.scriptPubKey());
            omniCreaterawtxFeeMap.put("value", unspentTmp.amount());
            omniCreaterawtxChangeArgs.add(omniCreaterawtxFeeMap);
        }
        logger.warn("usdt to main address sendByRawTransaction=====6666666666==============>>> omniCreaterawtxChangeArgs : {},createrawtransactionArgs:{}",
                JSON.toJSONString(omniCreaterawtxChangeArgs), createrawtransactionArgs);
        try {
            //调用穿件一个用于简单发送交易的载荷。 构造发送代币类型和代币数量数据（payload）

            logger.warn("执行omni_createpayload_simplesend命令, propertyId：{}，balance.toString:{}", propertyId, balance.toString());
            String createpayloadSimplesend = client.getClient(url, basicAuth).invoke(
                    "omni_createpayload_simplesend",
                    new Object[]{propertyId, balance.toString()}, String.class);

            //构造交易基本数据
            String createrawtransaction = StringUtils.defaultIfBlank((String) walletSupport.createRawTransaction(
                    createrawtransactionArgs, new HashMap<>()), "");

            //        调用将一个op-return操作载荷添加到交易中。
            //        如果未提供裸交易，那么本调用将创建一个新的奇偶阿姨。
            //        如果数据编码失败，那么交易将不会被修改。在交易数据中加上omni代币数据
            logger.warn("执行omni_createrawtx_opreturn命令, createrawtransaction：{}，createpayloadSimplesend:{}",
                    createrawtransaction, createpayloadSimplesend);
            String omniCreaterawtxOpreturn = client.getClient(url, basicAuth).invoke(
                    "omni_createrawtx_opreturn",
                    new Object[]{createrawtransaction, createpayloadSimplesend}, String.class);

            //        omni_createrawtx_reference调用讲一个参考输出添加到交易中。
            //        如果没有提供裸交易，那么调用将创建一个新的交易。
            //        输出值被设置为尘埃交易阈值。 在交易数据上加上接收地址

            logger.warn("执行omni_createrawtx_reference命令, omniCreaterawtxOpreturn：{}，to:{}", createrawtransaction, to);
            String omniCreaterawtxReference = client.getClient(url, basicAuth).invoke(
                    "omni_createrawtx_reference", new Object[]{omniCreaterawtxOpreturn, to}, String.class);

            //        omni_createrawtx_reference调用讲一个参考输出添加到交易中。
            //        如果没有提供裸交易，那么调用将创建一个新的交易。  在交易数据上指定矿工费用
            //        输出值被设置为尘埃交易阈值。Specify miner fee and attach change output (as needed) 此时 to为找零地址
            logger.warn("执行omni_createrawtx_change命令, omniCreaterawtxReference：{}，omniCreaterawtxChangeArgs:{}，" +
                    "changeAddress:{}", omniCreaterawtxReference, omniCreaterawtxChangeArgs, changeAddress);
            CoinVO coin = walletSupport.getCoinByType(CoinEnum.USDT.getValue());
            String omniCreaterawtxChange = client.getClient(url, basicAuth).invoke("omni_createrawtx_change",
                    new Object[]{
                            omniCreaterawtxReference,
                            omniCreaterawtxChangeArgs,
                            changeAddress,
                            coin.getWithdrawTxFee()}, String.class);
            //签名
            logger.warn("执行signrawtransaction命令, omniCreaterawtxChange：{}", omniCreaterawtxChange);
            LinkedHashMap signrawtransaction = client.getClient(url, basicAuth).invoke(
                    "signrawtransaction", new Object[]{omniCreaterawtxChange}, LinkedHashMap.class);
            //广播
            logger.warn("执行sendrawtransaction命令, signrawtransaction：{}", JSON.toJSONString(signrawtransaction));
            String txId = StringUtils.defaultIfBlank((String) walletSupport.sendRawTransaction(
                    StringUtils.defaultIfBlank((String) signrawtransaction.get("hex"), "")), "");
            logger.warn("执行完毕 usdt sendByRawTransaction txId:{}", txId);
            return txId;
        } catch (Throwable e) {
            logger.error("usdt sendByRawTransaction exception  ", e);
        }
        return null;
    }


    @Override
    public void transferMainAddress(CoinEnum coinType) {
        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        if (coin == null) return;
        RechargeAddressBO rechargeAddress = RechargeAddressBO.newBuilder().type(coinType.getValue()).build();
        ResponseData<List<RechargeAddressVO>> responseData = userCoinService.rechargeAddressList(rechargeAddress);
        if (responseData == null) return;
        List<RechargeAddressVO> rechargeAddressList = responseData.getData();
        if (CollectionUtils.isNotEmpty(rechargeAddressList)) {
            for (RechargeAddressVO address : rechargeAddressList) {
                if (StringUtils.equals(address.getAddress(), coin.getMainAddress())
                        || StringUtils.equals(address.getAddress(), coin.getColdAddress())) continue;
                //todo 过滤旷工费地址，冷热钱包地址，这些地址不做归集
                WalletRequestBO walletRequestBO = new WalletRequestBO();
                walletRequestBO.setAddress(address.getAddress());
                walletRequestBO.setPropertyid(coin.getPropertyId());
                BigDecimal balance = this.getBalance(walletRequestBO);
                logger.warn("执行归热, balance:{}, walletRequestBO:{}", balance, JSON.toJSONString(walletRequestBO));
                if (balance.compareTo(coin.getOutQtyToMainAddress()) > 0) {
                    walletRequestBO.setFromAddress(address.getAddress());
                    walletRequestBO.setToAddress(coin.getMainAddress());
                    walletRequestBO.setAmount(balance);
                    String txId = this.omniFundedSend(walletRequestBO);//this.transferWithDrawMainAddress(walletRequestBO, coin);
                    logger.warn("usdt to main address ===>>> txId : {}", txId);
                    if (StringUtils.isBlank(txId)) continue;

                    walletSupport.insertTransferRecord(coinType.toString(), address.getAddress(), coin.getMainAddress(),
                            TransferTypeEnum.WITHDRAW_TO_MAIN_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                            balance, coin.getWithdrawTxFee(), txId, new BigDecimal("0"), new BigDecimal("0"));
                }
            }
        }
    }

    @Override
    public void transferColdAddress(CoinEnum coinType) {
        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        if (coin == null) return;
        WalletRequestBO walletRequestBO = new WalletRequestBO();
        walletRequestBO.setAddress(coin.getMainAddress());
        walletRequestBO.setPropertyid(coin.getPropertyId());
        BigDecimal balance = this.getBalance(walletRequestBO);
        BigDecimal outQtyToColdAddress = coin.getOutQtyToColdAddress();
        if (balance.compareTo(outQtyToColdAddress) >= 0) {
            BigDecimal subtract = balance.subtract(outQtyToColdAddress).setScale(16, BigDecimal.ROUND_HALF_DOWN);
            walletRequestBO.setFromAddress(coin.getMainAddress());
            walletRequestBO.setToAddress(coin.getColdAddress());
            walletRequestBO.setAmount(subtract);
            WalletRespResultVO result = this.withDraw(walletRequestBO);
            if (result == null) return;
            walletSupport.insertTransferRecord(coinType.toString(), coin.getMainAddress(), coin.getColdAddress(),
                    TransferTypeEnum.WITHDRAW_TO_COLD_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                    balance, coin.getWithdrawTxFee(), result.getTxHashId(), new BigDecimal("0"), new BigDecimal("0"));
        }
    }

    @Override
    public void insertRechargeCoin(CoinEnum type) {
        walletSupport.insertRechargeCoin(type);
    }

    @Override
    public void updateWithdrawCoin(CoinEnum type) {
        if (type.getValue() != CoinEnum.USDT.getValue()) return;
        List<WalletTransferVO> transferList = walletSupport.selectWalletTransfer(
                type.toString(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
        if (transferList == null) return;

        byte coinType = type.getValue();
        CoinVO btcCoin = walletSupport.getCoinByType(coinType);
        Integer propertyId = 0;
        if (btcCoin != null) {
            propertyId = btcCoin.getPropertyId();
        }
        for (WalletTransferVO transfer : transferList) {
            String txId = transfer.getTxId();
            if (Strings.isEmpty(txId)) {
                continue;
            }
            if (StringUtils.isBlank(txId)) continue;

            WithdrawBO withdrawParams = new WithdrawBO();
            withdrawParams.setTxId(txId);
            LinkedHashMap transaction = walletSupport.getUsdtTransaction(txId);
            if (ObjectUtils.isEmpty(transaction)) continue;
            logger.warn("===》transaction:{}", JSON.toJSONString(transaction));
            //usdt 获取交易记录返回参数才有valid
            if (transaction == null || !transaction.containsKey("valid")) continue;
            //Boolean valid = Boolean.valueOf((String) transaction.get("valid"));
            boolean valid = (boolean) transaction.get("valid");
            if (valid) {
                withdrawParams.setStatus((byte) 4);
                withdrawParams.setConfirmTime(new Date());
            } else {
                withdrawParams.setStatus(TransferStatusEnum.TRANSFER_FAILURE.getValue().byteValue());
                walletSupport.updateWalletTransfer(txId, null, transfer.getTransferNo(),
                        TransferStatusEnum.TRANSFER_FAILURE.getValue(), null, null);
            }

            Object propertyid = transaction.getOrDefault("propertyid", "-1");
            logger.warn("===》propertyid:{}", propertyid);
            if (!propertyid.toString().equals(String.valueOf(propertyId))) continue;

            BigDecimal fee = new BigDecimal(transaction.getOrDefault("fee", "0").toString());
            withdrawParams.setTxFee(fee.abs());
            walletSupport.updateWalletTransfer(txId, fee.abs(), transfer.getTransferNo(),
                    TransferStatusEnum.TRANSFER_CONFIRM.getValue(), null, null);
            logger.warn("===》transfer:{}", JSON.toJSONString(transfer));

            walletSupport.recordAssetsData(transfer.getQty(), transfer.getAddress(), transfer.getCoinType(),
                    TransferTypeEnum.CLIENT_WITHDRAW.getValue());
            logger.warn("发送消息withdraw : {}", JSON.toJSONString(withdrawParams));
            WalletTransferBO params = new WalletTransferBO();
            params.setTxId(txId);
            WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
            if (walletTransfer != null) continue;//过滤掉归冷归热，归冷归热下不调用资产变更状态
            if (StringUtils.isNotBlank(transfer.getWithdrawOrderNo())) {
                withdrawParams.setOrderNo(Long.valueOf(transfer.getWithdrawOrderNo()));
            }
            Message sendMessage = RMQMessageBuilder.of(withdrawParams).topic(Constant.ACCOUNT_CENTER_TOPIC_WITHDRAW_COMPLETE).build();
            walletProducer.syncSend(sendMessage);
            logger.warn("发送消息withdraw完毕 : {}", JSON.toJSONString(withdrawParams));

            //ResponseData response = userCoinService.withdrawCallBack(withdrawParams);
            //logger.info("usdt 提币记录状态：{}，txId:{}",response.isSuccess(),withdrawParams.getTxId());
        }
    }

    @Override
    public BigInteger getblockcount() {
        try {
            String blockCount = client.getClient(url, basicAuth).invoke("getblockcount",
                    new Object[]{}, String.class);
            //logger.debug("blockCount===>>> :{} ", blockCount);
            return new BigInteger(blockCount);
        } catch (Throwable e) {
            logger.error("usdt getblockcount exception===>", e);
        }
        return null;
    }

    @Override
    public List<String> omniListBlockTransactions(Integer blockIndex) {
        try {
            List<String> transactions = (ArrayList<String>) client.getClient(url, basicAuth).invoke(
                    "omni_listblocktransactions", new Object[]{blockIndex}, Object.class);
            return transactions;
        } catch (Throwable e) {
            logger.error("usdt omniListBlockTransactions exception===>", e);
        }
        return null;
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
    public String omniFundedSend(WalletRequestBO walletRequestBO) {
        try {
            logger.warn("usdt omniFundedSend walletRequestBO={}，BTCAbsenceAddress:{}", JSON.toJSONString(walletRequestBO), BTCAbsenceAddress);
            String txid = client.getClient(url, basicAuth).invoke("omni_funded_send",
                    new Object[]{walletRequestBO.getFromAddress(), walletRequestBO.getToAddress(),
                            new Integer(walletRequestBO.getPropertyid()).intValue(), walletRequestBO.getAmount().toString(),
                            BTCAbsenceAddress}, String.class);
            return txid;
        } catch (Throwable e) {
            logger.error("usdt omniFundedSend exception===>", e);
        }
        return null;
    }

    @Override
    public void toAddressTransactionFee(CoinEnum coinType) {
//        CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
//        if (coin == null) return;
//        RechargeAddressBO rechargeAddress = RechargeAddressBO.newBuilder().type(coinType.getValue()).build();
//        ResponseData<List<RechargeAddressVO>> result = userCoinService.rechargeAddressList(rechargeAddress);
//        logger.warn("0000000======> result:{}, size:{}", JSON.toJSONString(result), result.getData().size());
//        if (result != null) {
//            BitcoindRpcClient btcRpcClient = walletSupport.getBitcoindRpcClient();
//            List<RechargeAddressVO> rechargeAddressList = result.getData();
//
//            for (RechargeAddressVO vo : rechargeAddressList) {
//                WalletRequestBO walletRequestBO = new WalletRequestBO();
//                walletRequestBO.setAddress(vo.getAddress());
//                walletRequestBO.setPropertyid(coin.getPropertyId());
//                BigDecimal usdtBalance = this.getBalance(walletRequestBO);
//                if (usdtBalance == null) {
//                    logger.warn("1111111111**********************======> usdtBalance:{},", usdtBalance);
//                    continue;
//                }
//                logger.warn("2222222222222**********************======> Address:{},usdtbalance:{}", vo.getAddress(), usdtBalance);
//                if (usdtBalance.compareTo(new BigDecimal(StringUtils.defaultIfBlank(coin.getOutQtyToMainAddress().toPlainString(), "0"))) > 0) {
//                    BigDecimal btcbalance = BigDecimal.ZERO;
//                    List<BitcoindRpcClient.Unspent> unspentList = btcRpcClient.listUnspent(3, 99999999, vo.getAddress());
//                    logger.warn("unspents======> unspents:{}", JSON.toJSONString(unspentList));
//
//                    for (BitcoindRpcClient.Unspent unspent : unspentList) {
//                        btcbalance = btcbalance.add(unspent.amount());
//                    }
//                    logger.warn("333333333333333======> btcbalance:{},address:{}", JSON.toJSONString(btcbalance), vo.getAddress());
//                    //手续费余额不够
//                    if (btcbalance.compareTo(new BigDecimal("0.0004")) < 0) {
//                        BigDecimal transferAmount = new BigDecimal("0");
//                        List<BitcoindRpcClient.Unspent> unspents = btcRpcClient.listUnspent();
//                        logger.warn("444444444444======> unspents:{},address:{}", JSON.toJSONString(unspents), vo.getAddress());
//                        BigDecimal balance = BigDecimal.ZERO;
//                        BigDecimal fee = coin.getWithdrawTxFee();//从后管配置中获取旷工费
//                        transferAmount = amount.add(fee);
//                        logger.warn("dsfsdfdsfsdfdsf======> fee:{},transferAmount:{}", fee, transferAmount);
//                        List<JSONObject> createrawtransactionArgs = new ArrayList<>();
//                        List<JSONObject> omniCreaterawtxChangeArgs = new ArrayList<>();
//                        unspents = unspents.stream().sorted(Comparator.comparing(BitcoindRpcClient.Unspent::amount).reversed()).collect(Collectors.toList());//逆序
//                        for (BitcoindRpcClient.Unspent unspent : unspents) {
//                            logger.warn("55555555555555======> unspent.address:{},address:{},热钱包地址:{}", unspent.address(), vo.getAddress(), coin.getMainAddress());
//                            if (StringUtils.equalsIgnoreCase(unspent.address(), BTCAbsenceAddress)) {
//                                logger.warn("666666666666======> unspent.address:{},unspent.amount:{},transferAmount:{}", unspent.address(), unspent.amount(), transferAmount);
//                                if (unspent.amount().compareTo(transferAmount) >= 0) {
//                                    JSONObject createrawFeeMap = new JSONObject();
//                                    createrawFeeMap.put("txid", unspent.txid());
//                                    createrawFeeMap.put("vout", unspent.vout());
//                                    createrawtransactionArgs.add(createrawFeeMap);
//
//                                    JSONObject omniCreaterawtxFeeMap = new JSONObject();
//                                    omniCreaterawtxFeeMap.put("txid", unspent.txid());
//                                    omniCreaterawtxFeeMap.put("vout", unspent.vout());
//                                    omniCreaterawtxFeeMap.put("scriptPubKey", unspent.scriptPubKey());
//                                    omniCreaterawtxFeeMap.put("amount", unspent.amount());
//                                    omniCreaterawtxChangeArgs.add(omniCreaterawtxFeeMap);
//                                    BigDecimal bigDecimal = unspent.amount();
//                                    balance = balance.add(bigDecimal).setScale(16, BigDecimal.ROUND_HALF_DOWN);
//                                    logger.warn("777777777777======> unspent.address:{},unspent.amount:{},transferAmount:{}，balance:{}", unspent.address(), unspent.amount(), transferAmount, balance);
//                                    break;
//                                }
//                            }
//                        }
//                        logger.warn("888888888======> address:{},createrawtransactionArgs:{}", vo.getAddress(), JSON.toJSONString(createrawtransactionArgs));
//                        if (CollectionUtils.isEmpty(createrawtransactionArgs)) {
//                            //倒序排序
//                            unspents = unspents.stream().sorted(Comparator.comparing(BitcoindRpcClient.Unspent::amount).reversed()).collect(Collectors.toList());//逆序
//                            for (BitcoindRpcClient.Unspent unspent : unspents) {
//                                logger.warn("999999999======> address:{},unspent.address():{},矿工费地址:{}", vo.getAddress(), JSON.toJSONString(unspent.address()), BTCAbsenceAddress);
//                                if (StringUtils.equalsIgnoreCase(unspent.address(), BTCAbsenceAddress)) {
//                                    logger.warn("!!!!!!!======> address:{},unspent.address:{},unspent.amount:{}", vo.getAddress(), JSON.toJSONString(unspent.address()), unspent.amount());
//                                    BigDecimal bigDecimal = unspent.amount();
//                                    balance = balance.add(bigDecimal).setScale(16, BigDecimal.ROUND_HALF_DOWN);
//                                    JSONObject createrawFeeMap = new JSONObject();
//                                    createrawFeeMap.put("txid", unspent.txid());
//                                    createrawFeeMap.put("vout", unspent.vout());
//                                    createrawtransactionArgs.add(createrawFeeMap);
//
//                                    JSONObject omniCreaterawtxFeeMap = new JSONObject();
//                                    omniCreaterawtxFeeMap.put("txid", unspent.txid());
//                                    omniCreaterawtxFeeMap.put("vout", unspent.vout());
//                                    omniCreaterawtxFeeMap.put("scriptPubKey", unspent.scriptPubKey());
//                                    omniCreaterawtxFeeMap.put("amount", unspent.amount());
//                                    omniCreaterawtxChangeArgs.add(omniCreaterawtxFeeMap);
//                                    if (balance.compareTo(transferAmount) > 0) {
//                                        logger.warn("@@@@@@========未找到合适的手续费地址========");
//                                        break;
//                                    }
//                                }
//                            }
//                            if (balance.compareTo(transferAmount) < 0) {
//                                logger.warn("$$$$$$$$$$$$$$$$$$$========未找到合适的手续费地址===balance:{},transferAmount{}=====", balance, transferAmount);
//                                continue;
//                            }
//                        }
//
//                        if (CollectionUtils.isEmpty(createrawtransactionArgs) ||
//                                CollectionUtils.isEmpty(omniCreaterawtxChangeArgs)) {
//                            logger.warn("****************======未找到合适的手续费地址========createrawtransactionArgs:{}," +
//                                    "omniCreaterawtxChangeArgs:{}", createrawtransactionArgs, omniCreaterawtxChangeArgs);
//                            continue;
//                        }
//                        LinkedHashMap<String, BigDecimal> adrressJson = new LinkedHashMap<>();
//                        //用户提USDT，需要给用户打BTC作为旷工，这里给每个用户子地址打BTC手续费，目前配置是0.0001个BTC
//                        adrressJson.put(vo.getAddress(), amount);
//
//                        //找零地址
//                        if (!(balance.subtract(transferAmount).compareTo(BigDecimal.ZERO) == 0)) {
//                            adrressJson.put(BTCAbsenceAddress, balance.subtract(transferAmount));
//                        }
//
//                        String createrawtransaction = StringUtils.defaultIfBlank((String)
//                                walletSupport.createRawTransaction(createrawtransactionArgs, adrressJson), "");
//                        //签名
//                        LinkedHashMap signrawtransaction = (LinkedHashMap) walletSupport.signRawTransaction(createrawtransaction, omniCreaterawtxChangeArgs);
//                        logger.warn("#############======> signrawtransaction:{},hex:{}", JSON.toJSONString(signrawtransaction), JSON.toJSONString(signrawtransaction.get("hex")));
//                        //广播
//                        String txId = StringUtils.defaultIfBlank((String) walletSupport.sendRawTransaction(
//                                StringUtils.defaultIfBlank((String) signrawtransaction.get("hex"), "")), "");
//
//                        logger.warn("#############======> Address:{},txId:{}", vo.getAddress(), JSON.toJSONString(txId));
////                        LinkedHashMap transMap = walletSupport.getTransaction(txId);
//
//                        walletSupport.insertTransferRecord(coinType.toString(), BTCAbsenceAddress, vo.getAddress(),
//                                TransferTypeEnum.APPLY_FEE.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
//                                amount, coin.getWithdrawTxFee(), txId, new BigDecimal("0"), new BigDecimal("0"));
//                    }
//                }
//            }
//        }
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
        if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
            for (WalletTransfer walletTransfer : list) {
                WalletRequestBO walletRequestBO = new WalletRequestBO();
                walletRequestBO.setFromAddress(coin.getMainAddress());
                walletRequestBO.setAddress(walletTransfer.getToAddress());
                walletRequestBO.setToAddress(walletTransfer.getToAddress());
                walletRequestBO.setAmount(walletTransfer.getQty());
                walletRequestBO.setPropertyid(coin.getPropertyId());
                WalletRespResultVO withdraw = this.withDraw(walletRequestBO);
                if (withdraw == null || StringUtils.isEmpty(withdraw.getTxHashId())) continue;
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
                    logger.warn("重新提USDT币成功，walletTransfer：{}, sendMessage{}", JSON.toJSONString(walletTransfer), JSON.toJSONString(sendMessage));
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }
}

