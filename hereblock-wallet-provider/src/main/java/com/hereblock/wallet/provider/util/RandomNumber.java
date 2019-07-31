package com.hereblock.wallet.provider.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class RandomNumber {
    /**
     * 生成随机文件名：当前年月日时分秒毫秒+五位随机数
     *
     * @return
     */
    public static String getRandomNumberId() {
        String str = new SimpleDateFormat("yyyyMMddhhmmssSSS").format(new Date());
        int ranNumber = (int) (new Random().nextDouble() * (9999 - 1000 + 1)) + 1000;// 获取4位随机数
        return str + ranNumber;
    }

}
