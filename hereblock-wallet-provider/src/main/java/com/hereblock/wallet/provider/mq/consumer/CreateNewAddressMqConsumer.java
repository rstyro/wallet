package com.hereblock.wallet.provider.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hereblock.account.api.model.CoinBO;
import com.hereblock.account.rpc.CoinService;
import com.hereblock.common.model.ResponseData;
import com.hereblock.framework.mq.annotition.RMQConsumer;
import com.hereblock.framework.mq.base.AbstractRMQPushConsumer;
import com.hereblock.wallet.api.enums.CoinEnum;
import com.hereblock.wallet.api.model.WalletRequestBO;
import com.hereblock.wallet.api.service.WalletRemoteService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RMQConsumer(topic = "account-center-topic-init-address", consumerGroup = "hereblock-wallet-producer")
public class CreateNewAddressMqConsumer extends AbstractRMQPushConsumer {
    private Logger logger = LoggerFactory.getLogger(CreateNewAddressMqConsumer.class);
    @Autowired
    private WalletRemoteService walletRemoteService;
    @Autowired
    private CoinService coinService;

    @Override
    public boolean process(Object message, Map map) {
        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(message));
        WalletRequestBO walletRequestBO = JSONObject.toJavaObject(jsonObject, WalletRequestBO.class);
        logger.info("start CreateNewAddressMqConsumer walletRequestBO:{}", JSON.toJSONString(walletRequestBO));
        List<String> coinTypeList = Lists.newArrayList();
        List<CoinEnum> coinTypes = walletRequestBO.getCoinTypeList();
        if (CollectionUtils.isEmpty(coinTypes)) {
            ResponseData<List<CoinBO>> responseData = coinService.list();
            if (responseData != null) {
                List<CoinBO> coinList = responseData.getData();
                if (CollectionUtils.isEmpty(coinList)) {
                    coinTypeList = coinList.stream().map(CoinBO::getCoinName).collect(Collectors.toList());
                }
            }
        } else {
            coinTypeList = coinTypes.stream().map(CoinEnum::toString).collect(Collectors.toList());
        }
        for (String coinType : coinTypeList) {
            if (CoinEnum.BTC.toString().equals(coinType)) {
                walletRequestBO.setCoinType(CoinEnum.BTC);
            }
            if (CoinEnum.USDT.toString().equals(coinType)) {
                walletRequestBO.setCoinType(CoinEnum.USDT);
            }
            if (CoinEnum.ETH.toString().equals(coinType)) {
                walletRequestBO.setCoinType(CoinEnum.ETH);
            }
            walletRemoteService.getNewAddress(walletRequestBO);
        }
        logger.info("end CreateNewAddressMqConsumer walletRequestBO:{}", JSON.toJSONString(walletRequestBO));
        return true;
    }
}
