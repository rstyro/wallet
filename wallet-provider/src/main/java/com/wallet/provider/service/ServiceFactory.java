package com.hereblock.wallet.provider.service;

import com.google.common.collect.Maps;
import com.hereblock.wallet.api.enums.CoinEnum;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ServiceFactory implements ApplicationContextAware {
    private static Map<CoinEnum, WalletService> serviceMap = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, WalletService> map = applicationContext.getBeansOfType(WalletService.class);
        serviceMap = Maps.newConcurrentMap();
        map.forEach((key, value) -> serviceMap.put(value.getCode(), value));
    }

    public WalletService getWalletService(CoinEnum coinEnum) {
        return serviceMap.get(coinEnum);
    }
}
