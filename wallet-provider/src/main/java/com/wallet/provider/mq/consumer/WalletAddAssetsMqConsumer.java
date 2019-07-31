package com.hereblock.wallet.provider.mq.consumer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hereblock.framework.mq.annotition.RMQConsumer;
import com.hereblock.framework.mq.base.AbstractRMQPushConsumer;
import com.hereblock.wallet.api.model.WalletAssetsRecordReqBO;
import com.hereblock.wallet.provider.support.WalletSupport;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@RMQConsumer(topic = "account-center-topic-change-user-coin", consumerGroup = "hereblock-wallet-producer-mq")
public class WalletAddAssetsMqConsumer extends AbstractRMQPushConsumer {
    @Autowired
    private WalletSupport walletSupport;

    @Override
    public boolean process(Object message, Map map) {
        JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(message));
        WalletAssetsRecordReqBO walletAssetsRecordReqBO = JSONObject.toJavaObject(jsonObject, WalletAssetsRecordReqBO.class);
        walletSupport.recordAssetsData(walletAssetsRecordReqBO.getAccount(), walletAssetsRecordReqBO.getAddress(),
                walletAssetsRecordReqBO.getCoinType().toString(), walletAssetsRecordReqBO.getTransferType());
        return true;
    }
}
