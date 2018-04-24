package com.jingdianbao.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.http.HttpClientFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCrawlerService.class);

    private CopyOnWriteArrayList<String> proxyPool = new CopyOnWriteArrayList();

    private String url = "http://webapi.http.zhimacangku.com/getip?num=12&type=2&pro=&city=0&yys=0&port=1&pack=19287&ts=0&ys=0&cs=0&lb=1&sb=0&pb=4&mr=1&regions=";

    private static final int PROXY_POLL_SIZE = 12;

    public String getRandomProxy() {
        Random random = new Random();
        int index = random.nextInt(proxyPool.size());
        if (index < proxyPool.size()) {
            return proxyPool.get(index);
        } else {
            return null;
        }
    }

    public String getProxy() {
        try {
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String result = sb.toString();
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.getIntValue("code") == 0) {
                JSONArray array = jsonObject.getJSONArray("data");
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    String ip = object.getString("ip");
                    String port = object.getString("port");
                    return ip + ":" + port;
                }
            }
        } catch (Exception e) {
            LOGGER.error("load proxy error", e);
        }
        return null;
    }


    @PostConstruct
    @Scheduled(cron = "0 0 */3 * * ?")
    private void loadProxy() {
        try {
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String result = sb.toString();
            JSONObject jsonObject = JSONObject.parseObject(result);
            if (jsonObject.getIntValue("code") == 0) {
                JSONArray array = jsonObject.getJSONArray("data");
                for (int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    String ip = object.getString("ip");
                    String port = object.getString("port");
                    proxyPool.add(0, ip + ":" + port);
                    LOGGER.error("add proxy into proxy pool : " + ip + ":" + port);
                }
            }
            int size = proxyPool.size() - PROXY_POLL_SIZE;
            for (int i = 0; i < size; i++) {
                proxyPool.remove(PROXY_POLL_SIZE);
            }
            LOGGER.error("proxy pool size : " + proxyPool.size());
        } catch (Exception e) {
            LOGGER.error("load proxy error", e);
        }

    }

}
