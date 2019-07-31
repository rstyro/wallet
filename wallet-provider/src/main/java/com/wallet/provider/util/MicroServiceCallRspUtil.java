package com.hereblock.wallet.provider.util;


import com.hereblock.common.exception.AppException;
import com.hereblock.common.model.ResponseData;
import com.hereblock.wallet.provider.constant.CustomResponseCode;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Author Hugo.Wwg
 * @Since 2019-04-19
 */
public class MicroServiceCallRspUtil {

    private static final Logger logger =
            LoggerFactory.getLogger(MicroServiceCallRspUtil.class);


    /**
     * 调用其它服务获取数据，抛出异常信息
     *
     * @param supplier             调用函数
     * @param responseCodeForNull  如果调用被熔断，或者，如果返回数据报错，抛出异常 responseCodeForNull
     * @param responseCodeForEmpty 如果返回数据为空，抛出异常，如果不提供此参数，则忽略此异常
     * @return ResponseData.getData()
     * @see ResponseData#getData
     */
    public static <T> T getDataFromService(Supplier<ResponseData<T>> supplier,
                                           CustomResponseCode responseCodeForNull,
                                           CustomResponseCode responseCodeForEmpty) {
        return processDataFromService(supplier,
                resp -> {
                    throw new AppException(responseCodeForNull);
                },
                resp -> {
                    throw new AppException(responseCodeForNull);
                },
                resp -> {
                    throw new AppException(responseCodeForEmpty);
                }
        );
    }


    /**
     * 调用其它服务获取数据,打印 WARN LOG
     *
     * @param supplier             调用函数
     * @param responseCodeForNull  如果调用被熔断，或者，如果返回数据报错，记录日志
     * @param responseCodeForEmpty 如果返回数据为空，抛出异常，如果不提供此参数，则忽略此异常
     * @return ResponseData.getData()
     * @see ResponseData#getData
     */
    public static <T> T getDataFromServiceWarn(Supplier<ResponseData<T>> supplier,
                                               CustomResponseCode responseCodeForNull,
                                               CustomResponseCode responseCodeForEmpty) {
        return processDataFromService(supplier,
                resp -> {
                    if (responseCodeForNull != null) {
                        logger.warn(responseCodeForNull.getMessage());
                    }
                },
                resp -> {
                    if (responseCodeForNull != null) {
                        logger.warn(responseCodeForNull.getMessage());
                    }
                },
                resp -> {
                    if (responseCodeForEmpty != null) {
                        logger.warn(responseCodeForEmpty.getMessage());
                    }
                }
        );
    }

    /**
     * 调用其它服务获取数据
     *
     * @param supplier             调用函数
     * @param responseCodeForNull  如果调用被熔断，或者，如果返回数据报错，抛出异常 responseCodeForNull
     * @param showResponseError    如果返回数据报错，调用的处理函数
     * @param responseCodeForEmpty 如果返回数据为空，抛出异常，如果不提供此参数，则忽略此异常
     * @return ResponseData.getData()
     * @see ResponseData#getData
     */
    public static <T> T getDataFromService(Supplier<ResponseData<T>> supplier,
                                           CustomResponseCode responseCodeForNull,
                                           boolean showResponseError,
                                           CustomResponseCode responseCodeForEmpty) {
        return processDataFromService(supplier,
                resp -> {
                    if (responseCodeForNull != null) {
                        throw new AppException(responseCodeForNull);
                    }
                },
                resp -> {
                    if (showResponseError) {
                        throw new AppException(resp.getCode(), resp.getMessage());
                    }
                },
                resp -> {
                    if (responseCodeForEmpty != null) {
                        throw new AppException(responseCodeForEmpty);
                    }
                }
        );
    }


    /**
     * 调用其它服务获取数据，并对数据进行处理
     *
     * @param supplier            调用函数
     * @param processForHystrix   如果调用被熔断，调用的处理函数
     * @param processForError     如果返回数据报错，调用的处理函数
     * @param processForDataEmpty 如果返回数据为空，调用的处理函数
     * @return ResponseData.getData()
     * @see ResponseData#getData
     */
    public static <T> T processDataFromService(Supplier<ResponseData<T>> supplier,
                                               Consumer<ResponseData<T>> processForHystrix,
                                               Consumer<ResponseData<T>> processForError,
                                               Consumer<ResponseData<T>> processForDataEmpty) {
        ResponseData<T> resp = supplier.get();
        if (resp == null) {
            logger.error("请求被熔断");
            processForHystrix.accept(resp);
            return null;
        }
        if (!resp.isSuccess()) {
            logger.warn("请求数据出错，错误码:{}，提示:{}", resp.getCode(), resp.getMessage());
            processForError.accept(resp);
            return null;
        }
        if (resp.getData() == null) {
            logger.info("响应数据为空");
            processForDataEmpty.accept(resp);
        }
        T data = resp.getData();
        if (data instanceof List) {
            List dataList = (List) data;
            if (CollectionUtils.isEmpty(dataList)) {
                logger.info("响应数据为空");
                processForDataEmpty.accept(resp);
            }
        }
        return resp.getData();
    }

}
