package com.jingdianbao.http;

import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class HttpClientFactory {

    /**
     * @return
     * @description
     * @author 美刀
     * @date 2016年6月21日
     */
    public static CloseableHttpClient getHttpClient() {
        return HttpClients.custom().setSSLSocketFactory(getSSLConnectionSocketFactory()).build();
    }

    public static CloseableHttpClient getHttpClient(CookieStore cookieStore) {
        return HttpClients.custom().setSSLSocketFactory(getSSLConnectionSocketFactory()).setDefaultCookieStore(cookieStore).build();
    }

    public static CloseableHttpClient getHttpClient(String proxy) {
        String[] s = proxy.split(":");
        return HttpClients.custom().setProxy(new HttpHost(s[0], Integer.parseInt(s[1]))).setSSLSocketFactory(getSSLConnectionSocketFactory()).build();
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
}
