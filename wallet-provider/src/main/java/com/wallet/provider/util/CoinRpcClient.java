package com.hereblock.wallet.provider.util;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Component
@Configuration
public class CoinRpcClient {
    private Logger logger = LoggerFactory.getLogger(CoinRpcClient.class);

    /**
     * 获取钱包链接
     *
     * @return
     */
    public JsonRpcHttpClient getClient(String url, String basicAuth) {
        JsonRpcHttpClient client = null;
        try {
            Map<String, String> headers = new HashMap<String, String>(1);
            headers.put("Authorization", "Basic " + basicAuth);
            client = new JsonRpcHttpClient(new URL(url), headers);
        } catch (Exception e) {
            logger.info("CoinRpcClient:{} service client ===>", e.getMessage(), e);
        }
        return client;
    }
}
