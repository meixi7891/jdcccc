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

//@Component
public class ProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCrawlerService.class);

    private CopyOnWriteArrayList<String> proxyPool = new CopyOnWriteArrayList();

    private String url = "http://tiqu.jiguangip.com/getip?num=10&type=2&pro=0&city=0&yys=0&port=1&pack=122&ts=0&ys=0&cs=0&lb=1&sb=0&pb=4&mr=0&regions=";

    public String getProxy() {
        Random random = new Random();
        int index = random.nextInt(10);
        if (index < proxyPool.size()) {
            return proxyPool.get(index);
        } else {
            return null;
        }
    }

//    @PostConstruct
//    @Scheduled(cron = "0 0/30 * * * ?")
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
                }
            }
            int size = proxyPool.size() - 10;
            for (int i = 0; i < size; i++) {
                proxyPool.remove(10);
            }
        } catch (Exception e) {
            LOGGER.error("load proxy error", e);
        }

    }

}
