package com.hereblock.wallet.api.enums;

/**
 * 交易类型枚举
 */
public enum TransferTypeEnum {
    //申请手续费
    APPLY_FEE(1),
    //客户端充币
    CLIENT_RECHARGE(2),
    //客户端提币
    CLIENT_WITHDRAW(3),
    //提币到主地址
    WITHDRAW_TO_MAIN_ADDRESS(4),
    //提币到冷地址
    WITHDRAW_TO_COLD_ADDRESS(5);

    private Integer value;

    TransferTypeEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
