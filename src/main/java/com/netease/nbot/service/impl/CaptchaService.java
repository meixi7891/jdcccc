package com.netease.nbot.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nbot.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CaptchaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);

    private String appKey = "bc0db473afd911d202274f9f6753fda5";

    public String getCode(File file) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        CloseableHttpResponse response = null;
        String result = null;
        try {
            RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(60000).build();
            HttpPost httppost = new HttpPost("http://op.juhe.cn/vercode/index");
            StringBody keyBody = new StringBody(appKey, ContentType.TEXT_PLAIN);
            StringBody typeBody = new StringBody("5004", ContentType.TEXT_PLAIN);
            HttpEntity reqEntity = MultipartEntityBuilder.create().addBinaryBody("image", file, ContentType.create("image/png"), file.getName()).addPart("key", keyBody)
                    .addPart("codeType", typeBody).build();
            httppost.setEntity(reqEntity);
            httppost.setConfig(config);
            response = httpClient.execute(httppost);
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                result = IOUtils.toString(resEntity.getContent(), "UTF-8");
            }
            LOGGER.info("=========================== verify code service return : " + result);
            JSONObject jsonObject = JSON.parseObject(result);
            EntityUtils.consume(resEntity);
            return jsonObject.getString("result");
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return result;
    }
}
