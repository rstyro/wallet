package com.hereblock.wallet.api.enums;

/**
 * 交易状态枚举
 */
public enum TransferStatusEnum {
    //进入交易池
    TRANSFER_INTO_TX_POOL(1),
    //离开交易池
    TRANSFER_OUT_TX_POOL(2),
    //交易已确认
    TRANSFER_CONFIRM(3),
    //常规交易失败
    TRANSFER_FAILURE(9);

    private Integer value;

    TransferStatusEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
