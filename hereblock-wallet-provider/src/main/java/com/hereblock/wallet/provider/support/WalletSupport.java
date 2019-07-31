package com.hereblock.wallet.provider.support;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hereblock.account.api.model.CoinBO;
import com.hereblock.account.api.model.RechargeAddressBO;
import com.hereblock.account.api.model.RechargeBO;
import com.hereblock.account.api.model.vo.CoinVO;
import com.hereblock.account.api.model.vo.RechargeAddressVO;
import com.hereblock.account.api.model.vo.RechargeVO;
import com.hereblock.account.rpc.CoinService;
import com.hereblock.account.rpc.UserCoinService;
import com.hereblock.common.model.ResponseData;
import com.hereblock.framework.mq.base.RMQMessageBuilder;
import com.hereblock.framework.service.utils.IDGenerator;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.enums.TransferStatusEnum;
import com.hereblock.wallet.api.enums.TransferTypeEnum;
import com.hereblock.wallet.api.model.WalletAssetsRecordBO;
import com.hereblock.wallet.api.model.WalletAssetsVO;
import com.hereblock.wallet.api.model.WalletTransferBO;
import com.hereblock.wallet.api.model.WalletTransferVO;
import com.hereblock.wallet.provider.constant.Constant;
import com.hereblock.wallet.provider.entity.WalletTransfer;
import com.hereblock.wallet.provider.mapper.WalletTransferMapper;
import com.hereblock.wallet.provider.mq.consumer.producer.WalletProducer;
import com.hereblock.wallet.provider.service.WalletAssetsRecordService;
import com.hereblock.wallet.provider.service.WalletAssetsService;
import com.hereblock.wallet.provider.service.WalletTransferService;
import com.hereblock.wallet.provider.util.CoinRpcClient;
import com.hereblock.wallet.provider.util.RandomNumber;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Scope("singleton")
@Service("walletSupport")
public class WalletSupport {
    private Logger logger = LoggerFactory.getLogger(WalletSupport.class);
    @Value("${btc.server.url}")
    private String url;
    @Value("${btc.server.basicauth}")
    private String basicAuth;
    @Value("${btc.rpc.url}")
    private String rpcUrl;
    @Autowired
    private CoinService coinService;
    @Autowired
    private UserCoinService userCoinService;
    @Autowired
    private CoinRpcClient client;
    @Autowired
    private CloseableHttpClient httpClient;
    @Autowired
    private IDGenerator idGenerator;
    @Autowired
    private WalletTransferService walletTransferService;
    @Autowired
    private WalletAssetsRecordService walletAssetsRecordService;
    @Autowired
    private WalletAssetsService walletAssetsService;
    @Autowired
    private WalletProducer walletProducer;
    @Autowired
    private WalletTransferMapper walletTransferMapper;

    public JSONObject httpGet(String url) {
        JSONObject object = null;
        HttpEntity entity = null;
        try {
            HttpGet get = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(get);
            entity = response.getEntity();
            String content = EntityUtils.toString(entity, Consts.UTF_8);
            //logger.info("http response:{}", content);
            object = JSONObject.parseObject(content);
        } catch (Exception e) {
            logger.error("", e);
        } finally {
            if (null != entity) {
                EntityUtils.consumeQuietly(entity);
            }
        }
        return object;
    }

    public LinkedHashMap getTransaction(String txId) {
        try {
            return client.getClient(url, basicAuth).invoke("gettransaction", new Object[]{txId}, LinkedHashMap.class);
        } catch (Throwable e) {
            logger.error("getTransaction exception===>", e);
        }
        return null;
    }

    public JSONArray listTransactions() {
        try {
            return client.getClient(url, basicAuth).invoke("listtransactions", new Object[]{"*", 999, 0}, JSONArray.class);
        } catch (Throwable e) {
            logger.error("btc listTransactions exception===>", e);
        }
        return null;
    }

    public CoinVO getCoinByType(byte type) {
        CoinBO coinParam = new CoinBO();
        coinParam.setType(type);
        ResponseData<CoinVO> responseData = coinService.get(coinParam);
        CoinVO ethCoin = null;
        if (responseData != null) {
            ethCoin = responseData.getData();
        }
        return ethCoin;
    }

    public String dbNum2EthNum(BigDecimal dbNum) {
        BigDecimal multiply = dbNum.multiply(new BigDecimal(10).pow(18)).setScale(16, BigDecimal.ROUND_HALF_DOWN);
        return toHexString(multiply.toBigInteger());
    }


    public BigInteger toNum(String hexString) {
        hexString = hexString.substring(2);
        return new BigInteger(hexString, 16);
    }

    public int ethGasPrice2DbGasPrice(String ethGasPrice) {
        BigInteger bigInteger = toNum(ethGasPrice);
        BigInteger divide = bigInteger.divide(new BigInteger("10").pow(9));
        return divide.intValue();
    }

    public static String toHexString(BigInteger numString) {
        return "0x" + numString.toString(16);
    }

    public static String dbGasPrice2EthGasPrice(int dbNum) {
        BigDecimal bigDecimal = new BigDecimal(dbNum);
        BigDecimal divide = bigDecimal.multiply(new BigDecimal(10).pow(9));
        return toHexString(divide.toBigInteger());
    }

    public boolean isCanTrans(BigDecimal dbNum, int decimals) {
        // 余额转eth 单位
        BigDecimal multiply = dbNum.multiply(new BigDecimal(10).pow(decimals));
        //gas * gasprice * 1.5
        multiply = multiply.subtract(new BigDecimal(21000 * 1.5).multiply(new BigDecimal(14).pow(9)));
        if (multiply.compareTo(BigDecimal.ZERO) > 0) {
            return true;
        }
        return false;
    }

    public BigDecimal dbGasPrice2EthNum(int dbNum) {
        BigDecimal bigDecimal = new BigDecimal(dbNum);
        BigDecimal multiply = bigDecimal.multiply(new BigDecimal(10).pow(9));
        return multiply;
    }

    public void insertRechargeCoin(CoinEnum type) {
        byte coinType = type.getValue();
        CoinVO coin = this.getCoinByType(coinType);
        JSONArray listtransactions = this.listTransactions();
        //todo 从资产服务获取用户子地址和充值记录 需改成从钱包数据库获取
        RechargeAddressBO rechargeAddress = RechargeAddressBO.newBuilder().type(coinType).build();
        ResponseData<List<RechargeAddressVO>> result = userCoinService.rechargeAddressList(rechargeAddress);
        ResponseData<List<RechargeVO>> allRechargeList = userCoinService.rechargeList();
        if (result != null) {
            List<RechargeAddressVO> rechargeAddressList = result.getData();
            for (RechargeAddressVO vo : rechargeAddressList) {
                List<RechargeVO> dbRechargeList = allRechargeList.getData().stream()
                        .filter(recharge ->
                                StringUtils.equalsIgnoreCase(recharge.getToAddress(), vo.getAddress()))
                        .collect(Collectors.toList());
                if (listtransactions == null) continue;
                List<LinkedHashMap> newRechargeList = listtransactions.stream()
                        .map(e -> (LinkedHashMap) e)
                        //获取 所有对应地址 的到账记录
                        .filter(e -> e.containsKey("address") && StringUtils.equalsIgnoreCase(StringUtils.defaultIfBlank((String) e.get("address"), ""), vo.getAddress()) &&
                                StringUtils.equalsIgnoreCase(StringUtils.defaultIfBlank((String) e.get("category"), ""), "receive"))
                        //过滤数据库有的txid 交易
                        .filter(e -> {
                            Optional<RechargeVO> rechargeOptional = dbRechargeList.stream()
                                    .filter(dbRecharge -> StringUtils.equalsIgnoreCase(dbRecharge.getTxId(), StringUtils.defaultIfBlank((String) e.get("txid"), ""))
                                    ).findFirst();
                            return !rechargeOptional.isPresent();
                        }).collect(Collectors.toList());
                this.recharge(newRechargeList, coin, vo);
            }
        }
    }

    public LinkedHashMap getUsdtTransaction(String txId) {
        try {
            return client.getClient(url, basicAuth).invoke("omni_gettransaction", new Object[]{txId}, LinkedHashMap.class);
        } catch (Throwable e) {
            logger.error("", e);
        }
        return null;
    }

    private void btcRecharge(List<LinkedHashMap> newRechargeList, CoinVO coin, RechargeAddressVO vo) {
        for (LinkedHashMap map : newRechargeList) {
            RechargeBO recharge = new RechargeBO();
            String txid = StringUtils.defaultIfBlank((String) map.get("txid"), "");
            BigDecimal qty = new BigDecimal(StringUtils.defaultIfBlank(map.getOrDefault("amount", "0").toString(), "0"));
            if (qty.compareTo(new BigDecimal("0.000")) < 0) continue;
            recharge.setQty(qty);
            recharge.setTxId(txid);
            recharge.setUserId(vo.getUserId());
            if (coin != null) {
                recharge.setCoinId(coin.getId());
            }
            try {
                LinkedHashMap transaction = this.getTransaction(txid);
                List<LinkedHashMap> details = (List<LinkedHashMap>) transaction.get("details");
                Optional<LinkedHashMap> send = details.stream()
                        .map(e -> (LinkedHashMap) e)
                        .filter(e -> StringUtils.equalsIgnoreCase(StringUtils.defaultIfBlank((String) e.get("category"), ""), "send"))
                        .findFirst();
                String from = "";
                if (send.isPresent()) {
                    from = StringUtils.defaultIfBlank((String) send.get().get("address"), "");
                    recharge.setFromAddress(from);
                }
                recharge.setToAddress(vo.getAddress());
                recharge.setCoinType(coin.getType());
                WalletTransferBO params = new WalletTransferBO();

                params.setTxId(txid);
                WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
                if (walletTransfer != null) continue;

                this.insertTransferRecord("BTC", from, vo.getAddress(),
                        TransferTypeEnum.CLIENT_RECHARGE.getValue(), TransferStatusEnum.TRANSFER_CONFIRM.getValue(),
                        qty, new BigDecimal(transaction.getOrDefault("fee", "0").toString()), txid, new BigDecimal("0"), new BigDecimal("0"));

                //ResponseData resp = userCoinService.rechargeSuccess(recharge);
                Message sendMessage = RMQMessageBuilder.of(recharge).topic(Constant.ACCOUNT_CENTER_TOPIC_RECHARGE_SUCCESS).build();
                walletProducer.syncSend(sendMessage);
                //logger.info("{} 充值记录插入状态：{}，userId:{},address:{}",coin.getType(),resp.isSuccess(),vo.getUserId(),vo.getAddress());
            } catch (Exception e) {
                logger.error("{} insertRechargeCoin 充值记录插入失败====>: ", coin.getType(), e);
            }
        }
    }

    private void usdtRecharge(List<LinkedHashMap> newRechargeList, CoinVO coin, RechargeAddressVO vo) {
        for (LinkedHashMap map : newRechargeList) {
            RechargeBO recharge = new RechargeBO();
            String txid = StringUtils.defaultIfBlank((String) map.get("txid"), "");
            if (StringUtils.isBlank(txid)) continue;
            LinkedHashMap usdtTransactionMap = this.getUsdtTransaction(txid);
            if (CollectionUtils.isEmpty(usdtTransactionMap)) {
                //logger.debug("txid:{},没找到对应的交易记录",txid);
                continue;
            }
            BigDecimal qty = new BigDecimal(StringUtils.defaultIfBlank(usdtTransactionMap.getOrDefault("amount", "0").toString(), "0"));
            if (qty.compareTo(new BigDecimal("0.000")) < 0) continue;
            recharge.setQty(qty);
            recharge.setTxId(txid);
            recharge.setUserId(vo.getUserId());
            if (coin != null) {
                recharge.setCoinId(coin.getId());
            }
            try {
                recharge.setToAddress(vo.getAddress());
                recharge.setCoinType(coin.getType());

                WalletTransferBO params = new WalletTransferBO();
                params.setTxId(txid);
                WalletTransfer walletTransfer = walletTransferService.selectWalletTransferByTxId(params);
                if (walletTransfer != null) continue;

                this.insertTransferRecord("USDT", "", vo.getAddress(),
                        TransferTypeEnum.CLIENT_RECHARGE.getValue(), TransferStatusEnum.TRANSFER_CONFIRM.getValue(),
                        qty, new BigDecimal(usdtTransactionMap.getOrDefault("fee", "0").toString()), txid, new BigDecimal("0"), new BigDecimal("0"));
                //ResponseData resp = userCoinService.rechargeSuccess(recharge);
                Message sendMessage = RMQMessageBuilder.of(recharge).topic(Constant.ACCOUNT_CENTER_TOPIC_RECHARGE_SUCCESS).build();
                walletProducer.syncSend(sendMessage);
                //logger.info("{} 充值记录插入状态：{}，userId:{},address:{}",coin.getType(),resp.isSuccess(),vo.getUserId(),vo.getAddress());
            } catch (Exception e) {
                logger.error("{} insertRechargeCoin 充值记录插入失败====>: ", coin.getType(), e);
            }
        }
    }

    private void recharge(List<LinkedHashMap> newRechargeList, CoinVO coin, RechargeAddressVO vo) {
        if (coin.getType() == 3) {
            this.usdtRecharge(newRechargeList, coin, vo);
        } else if (coin.getType() == 2) {
            this.btcRecharge(newRechargeList, coin, vo);
        }
    }

    public BitcoindRpcClient getBitcoindRpcClient() {
        BitcoindRpcClient btcRpcClient = null;
        try {
            btcRpcClient = new BitcoinJSONRPCClient(rpcUrl);
        } catch (Exception e) {
            logger.error("BitcoindRpcClient MalformedURLException exception", e);
        }
        return btcRpcClient;
    }

    /**
     * 调用创建一个未签名的序列化交易，该交易可以将一个UTXO 转让给指定的P2PKH地址或P2SH地址。
     * 该交易不会存储在钱包里，也不会发送到网络中
     *
     * @param transferInfo
     * @param sendInfo
     * @return
     */
    public Object createRawTransaction(Object transferInfo, Object sendInfo) {
        try {
            logger.warn("createRawTransaction transferInfo:{},sendInfo:{}", JSON.toJSONString(transferInfo), JSON.toJSONString(sendInfo));
            return client.getClient(url, basicAuth).invoke("createrawtransaction", new Object[]{transferInfo, sendInfo}, Object.class);
        } catch (Throwable e) {
            logger.error("createRawTransaction exception===>", e);
        }
        return null;
    }

    /**
     * 调用使用钱包中的私钥对指定的序列化交易字符串进行签名
     *
     * @param hexstring
     * @param transferInfo
     * @return
     */
    public Object signRawTransaction(String hexstring, Object transferInfo) {
        try {
            logger.warn("signRawTransaction hexstring:{},transferInfo:{}", JSON.toJSONString(hexstring), JSON.toJSONString(transferInfo));
            return client.getClient(url, basicAuth).invoke("signrawtransaction", new Object[]{hexstring, transferInfo}, Object.class);
        } catch (Throwable e) {
            logger.error("signRawTransaction exception===>", e);
        }
        return null;
    }

    /**
     * 调用验证指定交易并将其广播到P2P网络中
     *
     * @param hexHash
     * @return
     */
    public Object sendRawTransaction(String hexHash) {
        try {
            return client.getClient(url, basicAuth).invoke("sendrawtransaction", new Object[]{hexHash}, Object.class);
        } catch (Throwable e) {
            logger.error("sendRawTransaction exception===>", e);
        }
        return null;
    }


    public void insertTransferRecord(String coinType, String fromAddress, String toAddress, Integer transferType, Integer status,
                                     BigDecimal qty, BigDecimal fee, String txId, BigDecimal gasPrice, BigDecimal gasLimit) {
        String transferNo = RandomNumber.getRandomNumberId();
        WalletTransferBO walletTransfer = new WalletTransferBO();
        walletTransfer.setTransferNo(transferNo);
        walletTransfer.setCoinType(coinType);
        walletTransfer.setFromAddress(fromAddress);
        walletTransfer.setToAddress(toAddress);
        walletTransfer.setTransferType(transferType);
        walletTransfer.setStatus(status);
        walletTransfer.setQty(qty);
        walletTransfer.setTxFee(fee.abs());
        walletTransfer.setTxId(txId);
        walletTransfer.setGasPrice(gasPrice);
        walletTransfer.setGasLimit(gasLimit);
        walletTransfer.setConfirmTime(new Date());
        walletTransfer.setCreateTime(new Date());
        try {
            WalletTransferBO reqParam = new WalletTransferBO();
            reqParam.setTxId(txId);
            reqParam.setTransferType(transferType);
            List<WalletTransferVO> list = walletTransferService.selectWalletTransfer(reqParam);
            if (!CollectionUtils.isEmpty(list)) {
                return;
            }
            walletTransferService.insertTransferRecord(walletTransfer);
            this.recordAssetsData(qty, toAddress, coinType, transferType);
        } catch (Exception e) {
            logger.error("insertTransferRecord  coinType : {} ,txId : {} , transferType: " +
                    "{} insertTransferRecord exception : ", coinType, txId, transferType, e);
        }
    }


    /**
     * 记录对应钱包地址充币流水及资产变更
     */
    @Transactional
    public Boolean recordAssetsData(BigDecimal amount, String address, String coinType, Integer transferType) {
        WalletAssetsRecordBO walletAssetsRecord = new WalletAssetsRecordBO();
        walletAssetsRecord.setAccount(amount);
        walletAssetsRecord.setAddress(address);
        WalletAssetsVO walletAssetsVO = walletAssetsService.selectWalletAssetsByAddress(address);
        if (walletAssetsVO != null) {
            //钱包地址对应的资产变更
            walletAssetsRecord.setAppId(walletAssetsVO.getAppId());
            WalletAssetsVO walletAssetsTmp = new WalletAssetsVO();
            walletAssetsTmp.setAddress(address);
            walletAssetsTmp.setAppId(walletAssetsVO.getAppId());
            walletAssetsTmp.setCoinType(walletAssetsVO.getCoinType());
            walletAssetsTmp.setOldVersion(walletAssetsVO.getVersion());
            walletAssetsTmp.setVersion(walletAssetsVO.getVersion() + 1);

            if (transferType == TransferTypeEnum.CLIENT_RECHARGE.getValue()) {//充值
                walletAssetsTmp.setTotalAccount(walletAssetsVO.getTotalAccount().add(amount));
            } else if (transferType == TransferTypeEnum.CLIENT_WITHDRAW.getValue()) {//提币
                walletAssetsTmp.setTotalAccount(walletAssetsVO.getTotalAccount().subtract(amount));
            }
            walletAssetsService.updateWalletAssets(walletAssetsTmp);
        }
        //记录钱包资产变更流水
        walletAssetsRecord.setCoinType(coinType);
        walletAssetsRecord.setOrderNo(String.valueOf(idGenerator.generatorId()));
        walletAssetsRecord.setTransferType(transferType);
        walletAssetsRecord.setCreateTime(new Date());
        walletAssetsRecord.setUpdateTime(new Date());
        walletAssetsRecordService.insertWalletAssetsRecord(walletAssetsRecord);
        return true;
    }


    public void updateWalletTransfer(String txId, BigDecimal txFee, String transferNo, Integer status, BigDecimal gasPrice, BigDecimal gasLimit) {
        WalletTransferBO transferParam = new WalletTransferBO();
        transferParam.setTxId(txId);
        if (txFee != null) {
            transferParam.setTxFee(txFee);
            transferParam.setConfirmTime(new Date());
        }
        transferParam.setTransferNo(transferNo);
        transferParam.setStatus(status);
        if (gasPrice != null) {
            transferParam.setGasPrice(gasPrice);
        }
        if (gasLimit != null) {
            transferParam.setGasLimit(gasLimit);
        }
        try {
            walletTransferService.updateWalletTransfer(transferParam);
        } catch (Exception e) {
            logger.error("updateWalletTransfer  transferNo : {} ,txId : {}  exception : ", transferNo, txId, e);
        }
    }

    public List<WalletTransferVO> selectWalletTransfer(String coinType, Integer status) {
        WalletTransferBO reqParam = new WalletTransferBO();
        reqParam.setCoinType(coinType);
        reqParam.setStatus(status);
        return walletTransferService.selectWalletTransfer(reqParam);
    }


    public int updateStatusByTransferNo(WalletTransfer walletTransfer) {
        return walletTransferMapper.updateWalletTransfer(walletTransfer);
    }
}
