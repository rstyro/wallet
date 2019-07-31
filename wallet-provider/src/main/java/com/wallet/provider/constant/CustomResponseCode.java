package com.hereblock.wallet.provider.constant;


import com.hereblock.framework.api.model.BaseCode;

/**
 * @Author Hugo.Wwg
 * @Since 2019-04-19
 */
public enum CustomResponseCode implements BaseCode {
    INVALID_PARAMETER(10001, "无效的输入参数！"),
    STATUS__NULL_(10002, "状态不能为空"),
    INPUT_PARAMS_NULL(10003, "必填参数不能为空");

    private int code;
    private String message;

    private CustomResponseCode(int code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    @Override
    public String toString() {
        return Integer.toString(getCode());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
