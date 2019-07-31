package com.hereblock.wallet.provider.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;

public class Base64Utils {
    private static final Logger log = LoggerFactory.getLogger((Class) Base64Utils.class);

    public static String getBase64(final String str) {
        byte[] b = null;
        String s = null;
        try {
            b = str.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException ", e);
        }
        if (b != null) {
            s = new BASE64Encoder().encode(b);
        }
        return s;
    }

    public static String getFromBase64(final String s) {
        byte[] b = null;
        String result = null;
        if (s != null) {
            final BASE64Decoder decoder = new BASE64Decoder();
            try {
                b = decoder.decodeBuffer(s);
                result = new String(b, "utf-8");
            } catch (Exception e) {
                log.error("UnsupportedEncodingException ", e);
            }
        }
        return result;
    }
}
