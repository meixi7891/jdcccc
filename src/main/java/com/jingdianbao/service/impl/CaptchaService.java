package com.jingdianbao.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.captcha.Api;
import com.jingdianbao.captcha.Util;
import com.jingdianbao.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CaptchaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaptchaService.class);

    private String appKey = "bc0db473afd911d202274f9f6753fda5";

    public String getCode(File file) {
        try {
            Api api = new Api();
            String app_id = "302419";
            String app_key = "Qs34MwMuBOGuL58t0jAYKmKlcPXXUFst";
            String pd_id = "102219";
            String pd_key = "jXBzWGlQmBXlC9vLL2/mwtudTowoYQIW";
            // 对象生成之后，在任何操作之前，需要先调用初始化接口
            api.Init(app_id, app_key, pd_id, pd_key);
            // 查询余额
            Util.HttpResp resp = api.QueryBalc();
            System.out.printf("query balc!ret: %d cust: %f err: %s reqid: %s pred: %s\n", resp.ret_code, resp.cust_val, resp.err_msg, resp.req_id, resp.pred_resl);
            //
            String pred_type = "30400";
            // 通过文件进行验证码识别
            resp = api.PredictFromFile(pred_type, file.getName());
            JSONObject jsonObject = JSONObject.parseObject(resp.rsp_data);
            return jsonObject.getString("result");
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return "";
    }

    public String getCode(String base64Str) {
        try {
            Api api = new Api();
            String app_id = "302419";
            String app_key = "Qs34MwMuBOGuL58t0jAYKmKlcPXXUFst";
            String pd_id = "102219";
            String pd_key = "jXBzWGlQmBXlC9vLL2/mwtudTowoYQIW";
            // 对象生成之后，在任何操作之前，需要先调用初始化接口
            api.Init(app_id, app_key, pd_id, pd_key);
            String pred_type = "30400";
            // 通过文件进行验证码识别
            Util.HttpResp resp = api.PredictFromBase64(pred_type, base64Str);
            LOGGER.error("code result : " + resp.rsp_data);
            JSONObject jsonObject = JSONObject.parseObject(resp.rsp_data);

            return jsonObject.getString("result");
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return "";
    }

//    public static void main(String[] args) {
//        try {
//            Api api = new Api();
//            String app_id = "302419";
//            String app_key = "Qs34MwMuBOGuL58t0jAYKmKlcPXXUFst";
//            String pd_id = "102219";
//            String pd_key = "jXBzWGlQmBXlC9vLL2/mwtudTowoYQIW";
//            // 对象生成之后，在任何操作之前，需要先调用初始化接口
//            api.Init(app_id, app_key, pd_id, pd_key);
//            // 查询余额
//            String pred_type = "30400";
//            String[] files = {"E:\\screenShot\\screenshot7123961308211313944.png"};
//            for (int i = 0; i < files.length; i++) {
//                long start = System.nanoTime();
//                Util.HttpResp resp = api.PredictFromFile(pred_type, files[i]);
//                System.out.println("time cost :" + (System.nanoTime() - start));
//                System.out.println("file name :" + files[i] + " ,  result : " + resp.rsp_data);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
