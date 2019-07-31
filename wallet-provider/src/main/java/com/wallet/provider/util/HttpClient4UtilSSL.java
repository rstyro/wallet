package com.hereblock.wallet.provider.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class HttpClient4UtilSSL {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient4UtilSSL.class);

    private static CloseableHttpClient httpClient;


    private static RequestConfig defaultRequestConfig;

    /**
     * 最大连接数
     */
    private final static int MAX_TOTAL_CONNECTIONS = 60;

    /**
     * 从连接管理器中获取连接的最大等待时间
     * timeout in milliseconds used when requesting a connection from the connection manager.
     */
    private final static int WAIT_TIMEOUT = 100;

    /**
     * 每个路由最大连接数
     */
    private final static int MAX_ROUTE_CONNECTIONS = 10;

    /**
     * 连接超时时间
     * Determines the timeout in milliseconds until a connection is established.
     */
    private final static int CONNECT_TIMEOUT = 5000;

    /**
     * 读取超时时间
     * Defines the socket timeout in milliseconds,
     * which is the timeout for waiting for data  or, put differently,
     * a maximum period inactivity between two consecutive data packets).
     */
    private final static int READ_TIMEOUT = 10000;

    /**
     * UTF-8编码
     */
    private final static String ENCODING_UTF_8 = "UTF-8";

    public static boolean ubqsOverLoad = false;

    // 创建HttpClient上下文
    static HttpClientContext context;

    static {

        // SSL context for secure connections can be created either based on
        // system or application specific properties.
        // protocol schemes.
        // Create global request configuration
        SSLContext sslcontext = createIgnoreVerifySSL();

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
                .build();
        defaultRequestConfig = RequestConfig.custom().setConnectionRequestTimeout(WAIT_TIMEOUT).setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT).setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);
        // Create an HttpClient with the given custom dependencies and configuration.
        httpClient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(defaultRequestConfig).build();
    }

    public static CloseableHttpClient getHttpClient() {
        return HttpClients.custom().setSSLSocketFactory(createSSLConnSocketFactory()).
                setDefaultRequestConfig(defaultRequestConfig).build();
    }

    public static String PostForXml(String url, String content, String coding) throws Exception {
        Long startTimeMills = System.currentTimeMillis();
        String returnMsg = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            StringEntity entityParams = new StringEntity(content, coding);
            httpPost.setEntity(entityParams);
            CloseableHttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(httpPost, HttpClientContext.create());
                int code = httpResponse.getStatusLine().getStatusCode();
                if (code != 200) {
                    throw new Exception("服务器响应失败！");
                }
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity);
                    EntityUtils.consume(httpEntity);
                }
                httpPost.abort();
            } catch (Exception e) {
                throw e;
            } finally {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            }
        } finally {
            Long endTimeMills = System.currentTimeMillis();
            Long dotimes = endTimeMills - startTimeMills;
            if (dotimes > 1000) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(dotimes)
                        .append(" Mills! httpclient call times more than 1000Millis, url = ")
                        .append(url)
                        .append(", param = ")
                        .append(content)
                ;
                logger.warn(buffer.toString());
            }
        }
        return returnMsg;
    }

    /**
     * post获取数据
     *
     * @param url        接口URL
     * @param requestMap NameValuePair参数
     * @param coding     编码
     * @return
     * @throws Exception
     */
    public static String Post(String url, Map<String, Object> requestMap, String coding, String contentType) throws Exception {
        Long startTimeMills = System.currentTimeMillis();
        String returnMsg = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            if ("JSON".equals(contentType)) {
                httpPost.addHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(JSON.toJSONString(requestMap), coding));

            } else {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                Set<Entry<String, Object>> entrys = requestMap.entrySet();
                for (Entry<String, Object> entry : entrys) {
                    nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, coding));
            }

            CloseableHttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(httpPost, HttpClientContext.create());
                int code = httpResponse.getStatusLine().getStatusCode();
                if (code != 200) {
                    throw new Exception("服务器响应失败！");
                }
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity);
                    EntityUtils.consume(httpEntity);
                }
                httpPost.abort();
            } catch (Exception e) {
                throw e;
            } finally {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            }
        } finally {
            Long endTimeMills = System.currentTimeMillis();
            Long dotimes = endTimeMills - startTimeMills;
            if (dotimes > 1000) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(dotimes)
                        .append(" Mills! httpclient call times more than 1000Millis, url = ")
                        .append(url)
                        .append(", param = ")
                        .append(requestMap.toString())
                ;
                logger.warn(buffer.toString());
            }
        }
        return returnMsg;
    }

    public static String PostHeader(String url, Map<String, Object> requestMap, String coding, String contentType, Map<String, Object> headerMap) throws Exception {
        Long startTimeMills = System.currentTimeMillis();
        String returnMsg = "";
        try {
            HttpPost httpPost = new HttpPost(url);
            if ("JSON".equals(contentType)) {
                Set<Entry<String, Object>> entrys = headerMap.entrySet();
                for (Entry<String, Object> entry : entrys) {
                    httpPost.addHeader(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
                }
                httpPost.setEntity(new StringEntity(JSON.toJSONString(requestMap), coding));
            } else {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                Set<Entry<String, Object>> entrys = requestMap.entrySet();
                for (Entry<String, Object> entry : entrys) {
                    nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, coding));
            }

            CloseableHttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(httpPost);
//                int code = httpResponse.getStatusLine().getStatusCode();
//                if(code != 200){
//                    throw new ManLeException(code,"服务器响应失败！");
//                }
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity);
                    EntityUtils.consume(httpEntity);
                }
                httpPost.abort();
            } catch (Exception e) {
                throw e;
            } finally {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            }
        } finally {
            Long endTimeMills = System.currentTimeMillis();
            Long dotimes = endTimeMills - startTimeMills;
            if (dotimes > 1000) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(dotimes)
                        .append(" Mills! httpclient call times more than 1000Millis, url = ")
                        .append(url)
                        .append(", param = ")
                        .append(requestMap.toString())
                ;
                logger.warn(buffer.toString());
            }
        }
        return returnMsg;
    }

    public static String Get(String url, Map<String, String> requestMap, String coding) throws Exception {
        Long startTimeMills = System.currentTimeMillis();
        String returnMsg = "";
        try {
            Set<Entry<String, String>> entrys = requestMap.entrySet();
            String params = "";
            boolean first = true;
            for (Entry<String, String> entry : entrys) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                if (first) {
                    params = params + key + "=" + value;
                    first = false;
                } else {
                    params = params + "&" + key + "=" + value;
                }
            }
            if (StringUtils.isNotBlank(params)) {
                url = url + "?" + params;
            }
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(httpGet, HttpClientContext.create());
                int code = httpResponse.getStatusLine().getStatusCode();
                if (code != 200) {
                    throw new Exception("服务器响应失败！");
                }
                HttpEntity httpEntity = httpResponse.getEntity();
                if (httpEntity != null) {
                    returnMsg = EntityUtils.toString(httpEntity, "UTF-8");
                    EntityUtils.consume(httpEntity);
                }
                httpGet.abort();
            } catch (Exception e) {
                throw e;
            } finally {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            }
        } finally {
            Long endTimeMills = System.currentTimeMillis();
            Long dotimes = endTimeMills - startTimeMills;
            if (dotimes > 1000) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(dotimes)
                        .append(" Mills! httpclient call times more than 1000Millis, url = ")
                        .append(url)
                        .append(", param = ")
                        .append(requestMap.toString())
                ;
                logger.warn(buffer.toString());
            }
        }
        return returnMsg;
    }

    public static Boolean Get(String url, String path, Map<String, String> requestMap) throws Exception {
        System.out.println(path);
        Long startTimeMills = System.currentTimeMillis();
        String returnMsg = null;
        try {
            Set<Entry<String, String>> entrys = requestMap.entrySet();
            String params = "";
            boolean first = true;
            for (Entry<String, String> entry : entrys) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                if (first) {
                    params = params + key + "=" + value;
                    first = false;
                } else {
                    params = params + "&" + key + "=" + value;
                }
            }
            if (StringUtils.isNotBlank(params)) {
                url = url + "?" + params;
            }

            url = url + "?_random=" + Math.random();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse httpResponse = null;
            try {
                httpResponse = httpClient.execute(httpGet, HttpClientContext.create());
                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    File storeFile = new File(path);
                    HttpEntity entity = httpResponse.getEntity();
                    InputStream is = entity.getContent();
                    FileOutputStream fos = new FileOutputStream(storeFile);
                    byte[] b = new byte[1024];
                    while ((is.read(b)) != -1) {
                        fos.write(b);
                    }
                    is.close();
                    fos.close();
                    return true;
                } else {
                    returnMsg = httpResponse.getStatusLine().toString();
                    logger.error("请求失败，返回：" + returnMsg);
                }
            } catch (SocketTimeoutException e) {
                throw new Exception("查询信息超时!");
            } finally {
                if (httpResponse != null) {
                    httpResponse.close();
                }
            }
        } finally {
            Long endTimeMills = System.currentTimeMillis();
            Long dotimes = endTimeMills - startTimeMills;
            if (dotimes > 1000) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(dotimes)
                        .append(" Mills! httpclient call times more than 1000Millis, url = ")
                        .append(url)
                ;
                logger.warn(buffer.toString());
            }
        }
        return false;
    }

    public static String PostForJSON(String url, String content, String encodingRule) {

        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Content-Type", "application/json; charset=utf-8");

        StringEntity se = null;
        try {
            se = new StringEntity(content, "utf-8");
            httppost.setEntity(se);

            HttpResponse response = httpClient.execute(httppost);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity(), "utf-8");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    public static String doPostWithXml(String url, String xmlStr) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        logger.info("API，POST过去的数据是：");
        logger.info(xmlStr);
        //得指明使用UTF-8编码，否则到API服务器XML的中文不能被成功识别
        StringEntity postEntity = new StringEntity(xmlStr, "UTF-8");
        httpPost.addHeader("Content-Type", "text/xml");
        httpPost.setEntity(postEntity);
        //设置请求器的配置
        logger.info("executing request" + httpPost.getRequestLine());
        String returnMsg = "";
        CloseableHttpResponse httpResponse = null;
        httpResponse = httpClient.execute(httpPost);
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            returnMsg = EntityUtils.toString(httpEntity);
            EntityUtils.consume(httpEntity);
        }
        if (httpResponse != null) {
            httpResponse.close();
        }
        return returnMsg;
    }

    public static String Get(String url, Map<String, String> requestMap) throws Exception {
        return Get(url, requestMap, ENCODING_UTF_8);
    }

    public static String Post(String url, Map<String, Object> requestMap, String contentType) throws Exception {
        return Post(url, requestMap, ENCODING_UTF_8, contentType);
    }

    public static String PostHeader(String url, Map<String, Object> requestMap, String contentType, Map<String, Object> hearderMap) throws Exception {
        return PostHeader(url, requestMap, ENCODING_UTF_8, contentType, hearderMap);
    }

    public static String PostForString(String url, Map<String, String> requestMap, String contentType) throws Exception {

        Map<String, Object> params = new HashedMap();
        params.putAll(requestMap);
        return Post(url, params, ENCODING_UTF_8, contentType);
    }


    /**
     * 创建SSL安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory() {
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {

                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return sslsf;
    }

    /**
     * 绕过验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() {

        try {
            SSLContext sc = SSLContext.getInstance("SSLv3");

            // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        X509Certificate[] paramArrayOfX509Certificate,
                        String paramString) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        X509Certificate[] paramArrayOfX509Certificate,
                        String paramString) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sc.init(null, new TrustManager[]{trustManager}, null);
            return sc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
