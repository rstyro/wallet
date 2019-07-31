package com.hereblock.wallet.provider.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hereblock.account.api.model.RechargeAddressBO;
import com.hereblock.account.api.model.RechargeBO;
import com.hereblock.account.api.model.WithdrawBO;
import com.hereblock.account.api.model.vo.CoinVO;
import com.hereblock.account.api.model.vo.RechargeAddressVO;
import com.hereblock.account.api.model.vo.RechargeVO;
import com.hereblock.account.rpc.UserCoinService;
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
import com.hereblock.wallet.provider.service.EthWalletService;
import com.hereblock.wallet.provider.service.WalletService;
import com.hereblock.wallet.provider.service.WalletTransferService;
import com.hereblock.wallet.provider.support.WalletSupport;
import com.hereblock.wallet.provider.util.Base64Utils;
import com.hereblock.wallet.provider.util.CoinRpcClient;
import com.hereblock.wallet.provider.util.EthClient;
import com.hereblock.wallet.provider.util.HttpClient4UtilSSL;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EthWalletServiceImpl extends EthWalletService implements WalletService {
    private Logger logger = LoggerFactory.getLogger(EthWalletServiceImpl.class);
    @Value("${eth.server.url}")
    private String url;
    @Value("${eth.queryAccountUrl}")
    private String queryAccountUrl;
    @Value("${eth.queryTransactionUrl}")
    private String queryTransactionUrl;

    @Autowired
    private CoinRpcClient client;
    @Autowired
    private UserCoinService userCoinService;
    @Autowired
    @Qualifier("walletSupport")
    private WalletSupport walletSupport;
    @Autowired
    private WalletTransferService walletTransferService;
    @Autowired
    private WalletTransferMapper walletTransferMapper;
    @Autowired
    private WalletProducer walletProducer;

    @Override
    public CoinEnum getCode() {
        return CoinEnum.ETH;
    }

    private ThreadLocal<Set<BigInteger>> blockNumberSet = ThreadLocal.withInitial(HashSet::new);

    @Override
    public String getNewAddress(WalletRequestBO walletRequestBO) throws Throwable {
        Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
        return ((Admin) web3j).personalNewAccount(walletRequestBO.getPwd()).send().getResult();
    }

    @Override
    public BigDecimal getBalance(WalletRequestBO walletRequestBO) {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            BigInteger balance = web3j.ethGetBalance(walletRequestBO.getAddress(), DefaultBlockParameter.valueOf("pending")).send().getBalance();
            return new BigDecimal(balance).divide(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN);
        } catch (Throwable e) {
            logger.error("eth getBalance exception===>", e);
        }
        return null;
    }

    @Override
    public WalletRespResultVO withDraw(WalletRequestBO walletRequestBO) {
        CoinVO ethCoin = walletSupport.getCoinByType(CoinEnum.ETH.getValue());
        return transferWithDraw(walletRequestBO, ethCoin);
    }

    @Override
    public WalletRespResultVO transferWithDraw(WalletRequestBO walletRequestBO, CoinVO coin) {
        if (coin != null) {
            String password = Base64Utils.getFromBase64(Base64Utils.getFromBase64(coin.getMainAddressPassword()));
//            walletRequestBO.setFromAddress(ethCoin.getMainAddress());
            walletRequestBO.setPwd(password);
            return this.personalSendTransaction(walletRequestBO);
        }
        return null;
    }

    @Override
    public void transferMainAddress(CoinEnum coinType) {
        //logger.debug("transferMainAddress======> coinType:{}", JSON.toJSONString(coinType));
        CoinVO ethCoin = walletSupport.getCoinByType(CoinEnum.ETH.getValue());
        RechargeAddressBO rechargeAddress = new RechargeAddressBO();
        rechargeAddress.setType(CoinEnum.ETH.getValue());
        ResponseData<List<RechargeAddressVO>> result = userCoinService.rechargeAddressList(rechargeAddress);
        if (result != null) {
            List<RechargeAddressVO> rechargeAddressList = result.getData();
            rechargeAddressList = rechargeAddressList.stream()
                    .filter(address -> StringUtils.isNoneBlank(address.getAddress()))
                    .collect(Collectors.toList());
            for (RechargeAddressVO rcgAddress : rechargeAddressList) {
                if (StringUtils.equals(rcgAddress.getAddress(), ethCoin.getMainAddress()) ||
                        StringUtils.equals(rcgAddress.getAddress(), ethCoin.getColdAddress())) continue;
                WalletRequestBO walletRequestBO = new WalletRequestBO();
                walletRequestBO.setAddress(rcgAddress.getAddress());
                BigDecimal qty = this.getBalance(walletRequestBO);
                BigDecimal outQtyToMainAddress = ethCoin.getOutQtyToMainAddress();
                if (qty.compareTo(outQtyToMainAddress) > 0) {
                    String to = ethCoin.getMainAddress();
                    String from = rcgAddress.getAddress();
                    if (!walletSupport.isCanTrans(qty, 18)) continue;
                    BigDecimal gasPrice = walletSupport.dbGasPrice2EthNum((int) Math.round(this.ethGasPrice() * 1.5));
                    // 余额转eth 单位
                    BigDecimal amount = qty.multiply(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN);
                    amount = amount.subtract(gasPrice.multiply(new BigDecimal(21000)).setScale(16, BigDecimal.ROUND_HALF_DOWN));
                    WalletRequestBO walletRequest = new WalletRequestBO();
                    walletRequest.setFromAddress(from);
                    walletRequest.setToAddress(to);
                    walletRequest.setPwd(rcgAddress.getPassword());
                    walletRequest.setAmount(amount.divide(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN));
                    try {
                        WalletRespResultVO resp = this.transferWithDraw(walletRequest, ethCoin);
                        if (resp == null) continue;
                        logger.warn("transferMainAddress eth to main address ===>>> resp : {}", JSON.toJSONString(resp));
                        BigDecimal txFee = new BigDecimal(Numeric.toBigInt(resp.getGasPrice())).
                                divide(new BigDecimal(10).pow(18)).multiply(
                                new BigDecimal(Numeric.toBigInt(resp.getGas()))).
                                setScale(16, BigDecimal.ROUND_HALF_DOWN);

                        walletSupport.insertTransferRecord(coinType.toString(), from, to,
                                TransferTypeEnum.WITHDRAW_TO_MAIN_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                                amount.divide(new BigDecimal(10).pow(18)), txFee, resp.getTxHashId(), new BigDecimal(Numeric.toBigInt(resp.getGas())),
                                new BigDecimal(Numeric.toBigInt(resp.getGasPrice())).divide(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN));
                    } catch (Throwable e) {
                        logger.error("", e);
                    }
                }
            }
        }
    }

    @Override
    public void transferColdAddress(CoinEnum coinType) {
        //logger.debug("transferColdAddress======> coinType:{}", JSON.toJSONString(coinType));
        CoinVO ethCoin = walletSupport.getCoinByType(CoinEnum.ETH.getValue());
        WalletRequestBO walletRequestBO = new WalletRequestBO();
        walletRequestBO.setAddress(ethCoin.getMainAddress());
        BigDecimal balance = this.getBalance(walletRequestBO);

        if (balance != null && balance.compareTo(ethCoin.getOutQtyToColdAddress()) > 0) {
            BigDecimal qty = balance.subtract(ethCoin.getOutQtyToColdAddress());
            WalletRequestBO walletRequest = new WalletRequestBO();
            walletRequest.setFromAddress(ethCoin.getMainAddress());
            walletRequest.setToAddress(ethCoin.getColdAddress());
            walletRequest.setPwd(ethCoin.getMainAddressPassword());
            walletRequest.setAmount(qty);
            try {
                WalletRespResultVO resp = this.withDraw(walletRequest);
                //logger.debug("transferColdAddress resp:{}",JSON.toJSONString(resp));
                if (resp == null) return;
                BigDecimal txFee = new BigDecimal(Numeric.toBigInt(resp.getGasPrice())).divide(new BigDecimal(10).pow(18)).multiply(new BigDecimal(Numeric.toBigInt(resp.getGas()))).setScale(16, BigDecimal.ROUND_HALF_DOWN);
                walletSupport.insertTransferRecord(coinType.toString(), ethCoin.getMainAddress(), ethCoin.getColdAddress(),
                        TransferTypeEnum.WITHDRAW_TO_COLD_ADDRESS.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                        qty, txFee, resp.getTxHashId(), new BigDecimal(Numeric.toBigInt(resp.getGas())),
                        new BigDecimal(Numeric.toBigInt(resp.getGasPrice())).divide(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN));
            } catch (Throwable e) {
                logger.error("", e);
            }
        }
    }

    /**
     * 从链上获取eth矿工费
     *
     * @return
     */
    @Override
    public int ethGasPrice() {
        try {
            String ethGasPrice = client.getClient(url, "").invoke("eth_gasPrice", new Object[]{}, String.class);
            return walletSupport.ethGasPrice2DbGasPrice(ethGasPrice);
        } catch (Throwable e) {
            logger.error("eth ethGasPrice exception===>", e);
        }
        return 0;
    }

    @Override
    public WalletRespResultVO personalSendTransaction(WalletRequestBO walletRequestBO) {
        try {
            Map<String, Object> params = new HashMap<String, Object>(16);
            params.put("from", walletRequestBO.getFromAddress());
            params.put("to", walletRequestBO.getToAddress());
            int ethGasPrice = this.ethGasPrice();
            //eth矿工费
            String gasPrice = walletSupport.dbGasPrice2EthGasPrice((int) Math.round(ethGasPrice * 1.5));
            params.put("gasPrice", gasPrice);
            params.put("value", walletSupport.dbNum2EthNum(walletRequestBO.getAmount()));
            String gas = this.estimateGas(params);
            params.put("gas", gas);
            String txHashId = client.getClient(url, "").invoke("personal_sendTransaction", new Object[]{params, ""}, String.class);
            WalletRespResultVO result = new WalletRespResultVO();
            result.setData(walletRequestBO.getTransData());
            result.setFromAddress(walletRequestBO.getFromAddress());
            result.setGas(gas);
            result.setGasPrice(gasPrice);
            result.setToAddress(walletRequestBO.getToAddress());
            result.setTxHashId(txHashId);
            result.setValue(walletRequestBO.getAmount());
            return result;
        } catch (Throwable e) {
            logger.error("eth personalSendTransaction exception===>", e);
        }
        return null;
    }

    /**
     * 执行并估算一个交易需要的gas用量
     *
     * @param params
     * @return
     */
    @Override
    public String estimateGas(Map<String, Object> params) {
        if (ObjectUtils.isEmpty(params)) {
            return null;
        }
        try {
            String gaslimit = client.getClient(url, "").invoke("eth_estimateGas", new Object[]{params}, String.class);
            return walletSupport.toHexString(walletSupport.toNum(gaslimit));
        } catch (Throwable e) {
            logger.error("eth estimateGas exception===>", e);
        }
        return null;
    }

    @Override
    public void updateWithdrawCoin(CoinEnum type) {
        if (type.getValue() != CoinEnum.ETH.getValue()) return;
        List<WalletTransferVO> transferList = walletSupport.selectWalletTransfer(type.toString(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
        ArrayList<Map<String, String>> allblockingTxList = this.getAllTxpoolBlockings();
        if (transferList == null || allblockingTxList == null) return;
        List<String> txIds = Lists.newArrayList();
        for (WalletTransferVO transfer : transferList) {
            String txId = transfer.getTxId();
            if (txId.contains("0x")) {//说明已离开txpool 更改此 此状态
                WalletTransferBO transferParam = new WalletTransferBO();
                transferParam.setTxId(txId);
                transferParam.setTransferNo(transfer.getTransferNo());
                transferParam.setStatus(TransferStatusEnum.TRANSFER_OUT_TX_POOL.getValue());
                walletTransferService.updateWalletTransfer(transferParam);
            }
        }
        this.updateWithdraw(type);
    }

    private void updateWithdraw(CoinEnum type) {
        List<WalletTransferVO> transferList = walletSupport.selectWalletTransfer(type.toString(), TransferStatusEnum.TRANSFER_OUT_TX_POOL.getValue());
        if (transferList == null) return;
        for (WalletTransferVO transfer : transferList) {
            String txId = transfer.getTxId();
            try {
                String url = "https://api.etherscan.io/api";
                Map<String, String> requestMap = Maps.newHashMap();
                requestMap.put("module", "transaction");
                requestMap.put("action", "gettxreceiptstatus");
                requestMap.put("txhash", txId);
                requestMap.put("apikey", "");
                JSONObject object = null;
                try {
                    String jsonString = HttpClient4UtilSSL.Get(url, requestMap);
                    object = JSONObject.parseObject(jsonString);
                } catch (Exception e) {
                    logger.error("执行updateWithdraw eth Exception ==> : ", e);
                }

                if (object == null || "0".equals(object.get("status"))) continue;
                String statusObject = object.getString("status");
                if (statusObject != null) {
                    BigInteger status = Numeric.toBigInt(statusObject);
                    Object transaction = this.getTransactionByHash(txId);
                    if (transaction == null) continue;
                    JSONObject transactionJson = JSONObject.parseObject(JSON.toJSONString(transaction));
                    //获取使用费用
                    String gasPriceETH = transactionJson.getString("gasPrice");
                    String gasUsed = transactionJson.getString("gas");
                    //计算txFee
                    BigDecimal txFee = new BigDecimal(gasPriceETH)
                            .multiply(new BigDecimal(gasUsed)).divide(new BigDecimal(10).pow(18)).setScale(
                                    16, BigDecimal.ROUND_HALF_DOWN);
                    WithdrawBO withdrawParam = new WithdrawBO();
                    withdrawParam.setTxFee(txFee);
                    withdrawParam.setTxId(txId);
                    BigDecimal gasPrice = new BigDecimal(gasPriceETH).divide(new BigDecimal(10).pow(18)).setScale(
                            16, BigDecimal.ROUND_HALF_DOWN);
                    withdrawParam.setGasPrice(gasPrice);
                    BigDecimal gasLimit = new BigDecimal(gasUsed);
                    withdrawParam.setGasLimit(gasLimit);
                    if (status.intValue() == 1) {
                        walletSupport.updateWalletTransfer(txId, txFee, transfer.getTransferNo(),
                                TransferStatusEnum.TRANSFER_CONFIRM.getValue(), gasPrice, gasLimit);
                        walletSupport.recordAssetsData(transfer.getQty(), transfer.getAddress(),
                                transfer.getCoinType(), TransferTypeEnum.CLIENT_WITHDRAW.getValue());

                        WalletTransferBO params = new WalletTransferBO();
                        params.setTxId(txId);
                        //过滤掉归冷归热，归冷归热下不调用资产变更状态
                        WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
                        if (walletTransfer != null) continue;

                        withdrawParam.setStatus((byte) 4);
                        withdrawParam.setConfirmTime(new Date());
                        if (StringUtils.isNotBlank(transfer.getWithdrawOrderNo())) {
                            withdrawParam.setOrderNo(Long.valueOf(transfer.getWithdrawOrderNo()));
                        }
                        Message sendMessage = RMQMessageBuilder.of(withdrawParam).topic(
                                Constant.ACCOUNT_CENTER_TOPIC_WITHDRAW_COMPLETE).build();
                        walletProducer.syncSend(sendMessage);
                    } else if (status.intValue() == 0) {
                        //说明失败
                        walletSupport.updateWalletTransfer(txId, null, transfer.getTransferNo(),
                                TransferStatusEnum.TRANSFER_FAILURE.getValue(), null, null);
                    }
                }
            } catch (Exception e) {
                logger.error("eth updateWithdrawCoin exception", e);
            }
        }
    }

    /**
     * 将所有txpoolContent blocking抽取
     *
     * @return
     */
    public ArrayList<Map<String, String>> getAllTxpoolBlockings() {
        Map<String, Map<String, Map<String, Map<String, String>>>> stringMapMap = null;
        try {
            stringMapMap = JSONObject.parseObject(JSON.toJSONString(this.txpoolContent()), Map.class);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
        if (stringMapMap == null) {
            return null;
        }
        Set<String> statusKeys = stringMapMap.keySet();
        ArrayList<Map<String, String>> allblockingTxList = Lists.newArrayList();
        for (String statusKey : statusKeys) {
            Map<String, Map<String, Map<String, String>>> mapMap = stringMapMap.get(statusKey);
            Set<String> allAddresses = mapMap.keySet();
            for (String allAddress : allAddresses) {
                Map<String, Map<String, String>> queueMap = mapMap.get(allAddress);
                Set<String> queueKeys = queueMap.keySet();
                for (String queueKey : queueKeys) {
                    Map<String, String> stringStringMap = queueMap.get(queueKey);
                    allblockingTxList.add(stringStringMap);
                }
            }
        }
        allblockingTxList.sort(Comparator.comparing(e -> Numeric.toBigInt(e.get("nonce"))));
        return allblockingTxList;
    }

    @Override
    public BigInteger getBlockNumber() {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (IOException e) {
            logger.error("eth getBlockNumber exception===>", e);
        }
        return null;
    }

    @Override
    public BigInteger getBlockTransactionCountByNumber(BigInteger blockNumber) {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            return web3j.ethGetBlockTransactionCountByNumber(DefaultBlockParameter.valueOf(blockNumber)).send().getTransactionCount();
        } catch (IOException e) {
            logger.error("eth getBlockTransactionCountByNumber exception===>", e);
        }
        return null;
    }

    @Override
    public Object getTransactionByBlockNumberAndIndex(BigInteger blockNumber, BigInteger index) {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            return web3j.ethGetTransactionByBlockNumberAndIndex(DefaultBlockParameter.valueOf(blockNumber), index).send().getResult();
        } catch (IOException e) {
            logger.error("eth getTransactionByBlockNumberAndIndex exception===>", e);
        }
        return null;
    }

    @Override
    public JSONArray listTransactions() {
        return null;
    }

    @Override
    public LinkedHashMap getTransaction(String txId) {
        return null;
    }

    @Override
    public void insertRechargeCoin(CoinEnum type) {
        if (type.getValue() != CoinEnum.ETH.getValue()) return;
        //todo 从资产服务获取用户子地址和充值记录 需改成从钱包数据库获取
        RechargeAddressBO rechargeAddress = new RechargeAddressBO();
        rechargeAddress.setType(type.getValue());
        ResponseData<List<RechargeAddressVO>> rechargeAddressResult = userCoinService.rechargeAddressList(rechargeAddress);
        CoinVO ethCoin = walletSupport.getCoinByType(type.getValue());
        if (rechargeAddressResult != null) {
            List<RechargeAddressVO> rechargeAddressList = rechargeAddressResult.getData();
            for (RechargeAddressVO vo : rechargeAddressList) {
                String url = "http://api.etherscan.io/api";
                Map<String, String> requestMap = Maps.newHashMap();
                requestMap.put("module", "account");
                requestMap.put("action", "txlist");
                requestMap.put("address", vo.getAddress());
                requestMap.put("startblock", "0");
                requestMap.put("endblock", "99999999");
                requestMap.put("sort", "");
                requestMap.put("apikey", "");
                JSONObject object = null;
                try {
                    String jsonString = HttpClient4UtilSSL.Get(url, requestMap);
                    object = JSONObject.parseObject(jsonString);
                } catch (Exception e) {
                    logger.error("====eth=======insertRechargeCoin===Exception : ", e);
                }

                logger.warn("====eth=======insertRechargeCoin===getTransactionByAddress : {}", JSON.toJSONString(object));
                if (object == null || "0".equals(object.get("status"))) continue;
                JSONArray array = (JSONArray) JSONArray.parseArray(JSON.toJSONString(object.get("result")));
                for (int i = 0; i < array.size(); i++) {
                    Map<String, Object> map = ((JSONObject) array.get(i));
                    logger.warn("====eth=======insertRechargeCoin===map : {}", JSON.toJSONString(map));
                    String hash = StringUtils.defaultIfBlank((String) map.get("hash"), "");
                    RechargeBO rechargeBO = new RechargeBO();
                    rechargeBO.setTxId(hash);
                    ResponseData<RechargeVO> recharge = userCoinService.getRechargeByTxId(rechargeBO);
                    logger.warn("====eth=======insertRechargeCoin===hash : {}", hash);
                    if (recharge.getData() != null || !"1".equals(StringUtils.defaultIfBlank((String) map.get("txreceipt_status"), "")))
                        continue;
                    String from = StringUtils.defaultIfBlank((String) map.get("from"), "");
                    rechargeBO.setFromAddress(from);
                    String to = StringUtils.defaultIfBlank((String) map.get("to"), "");
                    rechargeBO.setToAddress(to);
                    rechargeBO.setCoinType(CoinEnum.ETH.getValue());
                    if (ethCoin != null) {
                        rechargeBO.setCoinId(ethCoin.getId());
                    }
                    BigDecimal realMoney = new BigDecimal(StringUtils.defaultIfBlank((String) map.get("value"), "0")).divide(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN);
                    rechargeBO.setQty(realMoney);
                    rechargeBO.setUserId(vo.getUserId());
                    String gasLimit = StringUtils.defaultIfBlank((String) map.get("gas"), "0");
                    String gasPrice = StringUtils.defaultIfBlank((String) map.get("gasPrice"), "0");
                    BigDecimal txFee = new BigDecimal(gasPrice).divide(new BigDecimal(10).pow(18)).multiply(new BigDecimal(gasLimit)).setScale(16, BigDecimal.ROUND_HALF_DOWN);

                    WalletTransferBO params = new WalletTransferBO();
                    params.setTxId(hash);
                    WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
                    if (walletTransfer != null) continue;

                    logger.warn("====eth=======insertRechargeCoin===begin : {}", JSON.toJSONString(rechargeBO));
                    walletSupport.insertTransferRecord(type.toString(), from, to,
                            TransferTypeEnum.CLIENT_RECHARGE.getValue(), TransferStatusEnum.TRANSFER_CONFIRM.getValue(),
                            realMoney, txFee, hash, new BigDecimal(gasPrice).divide(new BigDecimal(10).pow(18)), new BigDecimal(gasLimit));
                    Message sendMessage = RMQMessageBuilder.of(rechargeBO).topic(Constant.ACCOUNT_CENTER_TOPIC_RECHARGE_SUCCESS).build();
                    walletProducer.syncSend(sendMessage);
                }
            }
        }
    }

    @Override
    public Object getTransactionByHash(String txhash) {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            return web3j.ethGetTransactionByHash(txhash).send().getTransaction();
        } catch (IOException e) {
            logger.error("eth getTransactionByHash exception===>", e);
        }
        return null;
    }

    @Override
    public Object txpoolContent() {
        try {
            return client.getClient(url, "").invoke("txpool_content", new Object[]{}, Object.class);
        } catch (Throwable e) {
            logger.error("geth txpoolContent exception===>", e);
        }
        return null;
    }

    @Override
    public Object getTransactionReceipt(String txhash) {
        try {
            Web3j web3j = EthClient.getInstance().getAdminWeb3j(url);
            return web3j.ethGetTransactionReceipt(txhash).send().getTransactionReceipt();
        } catch (IOException e) {
            logger.error("eth getTransactionReceipt exception===>", e);
        }
        return null;
    }

    @Override
    public void toAddressTransactionFee(CoinEnum coinType) {
        WalletRequestBO walletRequestBO = new WalletRequestBO();
        walletRequestBO.setFromAddress("0x18691efe53dd20729a8b042308d18d7161fc1805");
        walletRequestBO.setToAddress("0x1b0db67f679de5c239988e2ac46010e3209a7f29");
        walletRequestBO.setCoinType(coinType);
        walletRequestBO.setAmount(new BigDecimal("0.005"));
        WalletRespResultVO result = withDraw(walletRequestBO);
        logger.warn("=====================bbbbb==========result:{}", JSON.toJSONString(result));
        /*CoinVO coin = walletSupport.getCoinByType(coinType.getValue());
        if(coin == null) return;
        RechargeAddressBO rechargeAddress = RechargeAddressBO.newBuilder().type(coinType.getValue()).build();
        ResponseData<List<RechargeAddressVO>> result = userCoinService.rechargeAddressList(rechargeAddress);
        if(result != null) {
            List<RechargeAddressVO> rechargeAddressList = result.getData();
            rechargeAddressList = rechargeAddressList.stream()
                    .filter(address -> StringUtils.isNoneBlank(address.getAddress()))
                    .collect(Collectors.toList());
            for (RechargeAddressVO vo : rechargeAddressList) {
                if(StringUtils.equals(vo.getAddress(),coin.getMainAddress())) continue;
                WalletRequestBO walletRequest = new WalletRequestBO();
                walletRequest.setAddress(vo.getAddress());
                BigDecimal balance = this.getBalance(walletRequest);
                BigDecimal fee = new BigDecimal("0");
                if (balance.compareTo(coin.getOutQtyToMainAddress()) > 0) {
                    Map<String, Object> param = new HashMap<String, Object>();
                    param.put("from", coin.getMainAddress());
                    param.put("to", vo.getAddress());
                    param.put("value", "0x5");
                    String gasLimit = this.estimateGas(param);
                    int gasPrice = this.ethGasPrice();
                    fee = fee.add(new BigDecimal(gasPrice).multiply(new BigDecimal(Numeric.toBigInt(gasLimit)).multiply(new BigDecimal(2))));
                    if(balance.compareTo(fee)< 0 ){
                        BigDecimal qty = fee.subtract(balance);
                        if (qty.compareTo(new BigDecimal("0")) > 0) {
                            String password = Base64Utils.getFromBase64(Base64Utils.getFromBase64(coin.getMainAddressPassword())) ;
                            WalletRequestBO walletRequestBO = new WalletRequestBO();
                            walletRequestBO.setFromAddress(coin.getMainAddress());
                            walletRequestBO.setToAddress(vo.getAddress());
                            walletRequestBO.setAmount(qty);
                            walletRequestBO.setPwd(password);
                            WalletRespResultVO respResult = this.personalSendTransaction(walletRequestBO);
                            BigDecimal txFee = new BigDecimal(gasPrice).divide(new BigDecimal(10).pow(18)).multiply(new BigDecimal(gasLimit));
                            walletSupport.insertTransferRecord(coinType.toString(),coin.getMainAddress(),vo.getAddress(),
                                    TransferTypeEnum.APPLY_FEE.getValue(), TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue(),
                                    qty,txFee,respResult.getTxHashId(),new BigDecimal(gasLimit),
                                    new BigDecimal(gasPrice).divide(new BigDecimal(10).pow(18)));
                        }
                    }
                }
            }
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
        if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
            for (WalletTransfer walletTransfer : list) {
                WalletRequestBO walletRequestBO = new WalletRequestBO();
                walletRequestBO.setFromAddress(coin.getMainAddress());
                walletRequestBO.setAddress(walletTransfer.getToAddress());
                walletRequestBO.setToAddress(walletTransfer.getToAddress());
                walletRequestBO.setAmount(walletTransfer.getQty());
                WalletRespResultVO withdraw = this.withDraw(walletRequestBO);
                if (withdraw == null) continue;
                if (StringUtils.isBlank(withdraw.getTxHashId())) continue;
                try {
                    WalletTransfer walletTransferParam = new WalletTransfer();
                    walletTransferParam.setCoinType(coinType.toString());
                    walletTransferParam.setTransferNo(walletTransfer.getTransferNo());
                    walletTransferParam.setTxId(withdraw.getTxHashId());
                    walletTransferParam.setStatus(TransferStatusEnum.TRANSFER_INTO_TX_POOL.getValue());
                    walletTransferMapper.updateStatusByTransferNo(walletTransferParam);
                    withdraw.setWithdrawOrderNo(walletTransfer.getWithdrawOrderNo());
                    Message sendMessage = RMQMessageBuilder.of(withdraw).topic(Constant.ACCOUNT_CENTER_TOPIC_WITHDRAW_RESEND).build();
                    walletProducer.syncSend(sendMessage);
                    logger.warn("重新提ETH币成功，walletTransfer：{}, sendMessage{}", JSON.toJSONString(walletTransfer), JSON.toJSONString(sendMessage));
                } catch (Exception e) {
                    logger.error("", e);
                }
            }
        }
    }
}
