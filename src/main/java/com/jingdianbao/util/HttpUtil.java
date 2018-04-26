package com.jingdianbao.util;

import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.http.HttpClientFactory;
import com.jingdianbao.service.impl.AccountService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class HttpUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);


    public static String readResponse(CloseableHttpResponse response) {
        return readResponse(response, "utf-8");
    }

    public static String readResponse(CloseableHttpResponse response, String charSetName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charSetName));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.getProperty("line.separator"));
            }
            reader.close();
            String result = sb.toString();
            return result;
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return null;
    }


    public static CloseableHttpResponse doGet(String url, Map<String, String> header, RequestConfig requestConfig) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        httpGet.setConfig(requestConfig);
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        CloseableHttpResponse response = httpClient.execute(httpGet);
        return response;
    }

    public static CloseableHttpResponse doGet(String url, Map<String, String> header, RequestConfig requestConfig,String proxy) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        httpGet.setConfig(requestConfig);
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient(proxy);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        return response;
    }

    public static JSONObject readJSONResponse(CloseableHttpResponse response) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonObject = JSONObject.parseObject(sb.toString());
            return jsonObject;
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }
}
