package com.jingdianbao.http;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class HttpClientFactory {

    private static HttpClientConnectionManager connectionManager;

    /**
     * @return
     * @description
     * @author 美刀
     * @date 2016年6月21日
     */
    public static CloseableHttpClient getHttpClient() {

        // 请求重试处理
        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception,
                                        int executionCount, HttpContext context) {
                if (executionCount >= 5) {// 如果已经重试了5次，就放弃
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
                    return false;
                }
                if (exception instanceof InterruptedIOException) {// 超时
                    return false;
                }
                if (exception instanceof UnknownHostException) {// 目标服务器不可达
                    return false;
                }
                if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
                    return false;
                }
                if (exception instanceof SSLException) {// SSL握手异常
                    return false;
                }
                HttpClientContext clientContext = HttpClientContext
                        .adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    return true;
                }
                return false;
            }
        };
        return HttpClients.custom().setSSLSocketFactory(getSSLConnectionSocketFactory()).setRetryHandler(httpRequestRetryHandler).build();
    }

    /**
     * @return
     * @description
     * @author 美刀
     * @date 2016年6月21日
     */
    @SuppressWarnings("deprecation")
    private static LayeredConnectionSocketFactory getSSLConnectionSocketFactory() {
        X509TrustManager x509mgr = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        };
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance(SSLConnectionSocketFactory.SSL);
            sslContext.init(null, new TrustManager[]{x509mgr}, null);
        } catch (NoSuchAlgorithmException e) {

        } catch (KeyManagementException e) {

        }
        return new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    private static class Client {
        private static CloseableHttpClient client;

        static {
            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                    .getSocketFactory();
            LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
                    .getSocketFactory();
            Registry<ConnectionSocketFactory> registry = RegistryBuilder
                    .<ConnectionSocketFactory>create().register("http", plainsf)
                    .register("https", sslsf).build();
            connectionManager = new PoolingHttpClientConnectionManager(
                    registry);
            // 将最大连接数增加
            ((PoolingHttpClientConnectionManager) connectionManager).setMaxTotal(1000);
            // 将每个路由基础的连接增加
            ((PoolingHttpClientConnectionManager) connectionManager).setDefaultMaxPerRoute(1000);
            // 请求重试处理
            client = HttpClients.custom().setSSLSocketFactory(getSSLConnectionSocketFactory()).setConnectionManager(connectionManager).build();
        }
    }
}
