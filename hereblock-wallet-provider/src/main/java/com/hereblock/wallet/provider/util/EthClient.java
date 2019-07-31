package com.hereblock.wallet.provider.util;

import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.http.HttpService;

public class EthClient {
    private EthClient() {
    }

    private static class SingletonInstance {
        private static final EthClient INSTANCE = new EthClient();
    }

    public static EthClient getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public Admin getAdminWeb3j(String url) {
        return Admin.build(new HttpService(url));
    }
}
