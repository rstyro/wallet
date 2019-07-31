package com.hereblock.wallet.provider.mq.consumer.producer;

import com.alibaba.fastjson.JSON;
import com.hereblock.framework.mq.annotition.RMQProducer;
import com.hereblock.framework.mq.base.AbstractRMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * @Author Hugo.Wwg
 * @Since 2019-04-26
 */
@RMQProducer
public class WalletProducer extends AbstractRMQProducer {

    @Override
    public void doAfterSyncSend(Message message, SendResult sendResult) {
        /** todo 异步给用户发送短信息 IM 信息 */
        System.out.print(" ====SendCodeProducer====" + JSON.toJSONString(message));

    }

}
