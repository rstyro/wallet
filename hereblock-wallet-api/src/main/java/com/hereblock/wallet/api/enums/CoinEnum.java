package com.hereblock.wallet.api.enums;

import com.hereblock.common.exception.AppException;
import com.hereblock.framework.api.model.ResponseCode;

public enum CoinEnum {

    ETH((byte) 1), BTC((byte) 2), USDT((byte) 3);

    private byte value;

    CoinEnum(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }


    public static CoinEnum getCoinType(Integer coinType) {
        for (CoinEnum coinEnum : CoinEnum.values()) {
            if (coinEnum.value == coinType.byteValue()) {
                return coinEnum;
            }
        }
        throw new AppException(ResponseCode.ILLEGAL_REQUESTS);
    }

}
