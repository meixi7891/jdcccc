package com.jingdianbao.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.*;
import com.jingdianbao.http.HttpClientFactory;

import com.jingdianbao.util.CookieTool;
import com.jingdianbao.util.HttpUtil;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DmpService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmpService.class);

    @Autowired
    private AccountService accountService;
    @Autowired
    private LoginService loginService;
    @Autowired
    HttpCrawlerService httpCrawlerService;
    @Autowired
    private LoginTask loginTask;
    @Autowired
    private WebDriverBuilder webDriverBuilder;
    @Autowired
    private CookieTool cookieTool;

    @Autowired
    private ProxyService proxyService;

    public DmpResult crawlHttp(DmpRequest request) {
        DmpResult dmpResult = new DmpResult();
        CookieStore cookieStore = new BasicCookieStore();
        LoginAccount loginAccount = accountService.loadRandomDmpAccount();
        if (loginAccount != null) {
            while (!loginService.testLogin(loginAccount.getUserName(), loginAccount.getPassword())) {
                loginTask.addLoginTask(loginAccount);
                if (accountService.accountCount() == loginTask.getTaskSize()) {
                    return null;
                }
                loginAccount = accountService.loadRandomDmpAccount();
            }
        }
        cookieTool.loadCookie(request.getUserName(), request.getPassword(), cookieStore);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000).setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000).build();

        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpPost httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/skuinfo/list");
        httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
        httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
        httpPost.setConfig(requestConfig);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("skuId", request.getSku()));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            JSONObject jsonObject = HttpUtil.readJSONResponse(response);
            JSONArray array = jsonObject.getJSONArray("skuInfoList");
            for (int i = 0; i < array.size(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (object.getString("sku").equals(request.getSku())) {
                    dmpResult.setSku(object.getString("sku"));
                    dmpResult.setName(object.getString("itemName"));
                    dmpResult.setBrand(object.getString("itemBrand"));
                    dmpResult.setShop(object.getString("itemShop"));
                    dmpResult.setCategory(object.getString("itemCategory"));
                    Document doc = Jsoup.connect(dmpResult.getUrl()).timeout(30000).get();
                    dmpResult.setImg(doc.select("#spec-img").attr("data-origin"));
                }
            }
            String tagName = uuid();
            httpClient = HttpClientFactory.getHttpClient();
            httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/createtag");
            httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
            httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpPost.setConfig(requestConfig);
            nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("tagTitle", tagName));
            nvps.add(new BasicNameValuePair("interetsType", "0"));
            nvps.add(new BasicNameValuePair("interets", ""));
            nvps.add(new BasicNameValuePair("skus", request.getSku()));
            nvps.add(new BasicNameValuePair("is_extend", "false"));
            nvps.add(new BasicNameValuePair("priceRangType", "0"));
            nvps.add(new BasicNameValuePair("goumaiCycle", "30"));
            nvps.add(new BasicNameValuePair("tagType", "1005"));
            nvps.add(new BasicNameValuePair("dimen_code", "000100020001"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            response = httpClient.execute(httpPost);
            jsonObject = HttpUtil.readJSONResponse(response);
            String tagId = jsonObject.getString("tagId");
            HttpGet httpGet = new HttpGet("https://jzt.jd.com/dmp/newtag/showPortrait?tag_id=" + tagId + "&_=" + System.currentTimeMillis());
            httpGet.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
            httpGet.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpGet.setConfig(requestConfig);
            httpClient = HttpClientFactory.getHttpClient();
            response = httpClient.execute(httpGet);
            jsonObject = HttpUtil.readJSONResponse(response);
            dmpResult.setCoverCount(jsonObject.getIntValue("totalUV"));

            httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/deltag");
            httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
            httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpPost.setConfig(requestConfig);
            nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("tagId", tagId));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            httpClient = HttpClientFactory.getHttpClient();
            response = httpClient.execute(httpPost);
            jsonObject = HttpUtil.readJSONResponse(response);
            if (jsonObject.getIntValue("code") != 1) {
                dmpResult.setErrorMessage("删除失败");
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return dmpResult;
    }

    public DmpResult crawlHttpNew(DmpRequest request) {
        SearchResult searchResult = new SearchResult();
        searchResult.setSku(request.getSku());
        httpCrawlerService.crawlSkuDetail(searchResult);
        DmpResult dmpResult = new DmpResult();
        dmpResult.setSku(request.getSku());
        dmpResult.setName(searchResult.getTitle());
        dmpResult.setBrand(searchResult.getBrand());
        dmpResult.setShop(searchResult.getShop());
        dmpResult.setCategory(searchResult.getCategory().getLevel3());
        dmpResult.setImg(searchResult.getImg());
        CookieStore cookieStore = new BasicCookieStore();
        LoginAccount loginAccount = accountService.loadRandomDmpAccount();
        if (loginAccount != null) {
            while (!loginService.testLogin(loginAccount.getUserName(), loginAccount.getPassword())) {
                loginTask.addLoginTask(loginAccount);
                if (accountService.accountCount() == loginTask.getTaskSize()) {
                    return null;
                }
                loginAccount = accountService.loadRandomDmpAccount();
            }
        }
        cookieTool.loadCookie(request.getUserName(), request.getPassword(), cookieStore);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000).setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000).build();

        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        try {
            HttpPost httpPost = new HttpPost("https://jzt.jd.com/dmp/new/tag/portrait/and/estimate");
            httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/labelManage.html");
            httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpPost.setConfig(requestConfig);
            JSONObject postData = new JSONObject();
            postData.put("tagId", 294);
            JSONArray commitAttribute = new JSONArray();
            commitAttribute.add("skus");
            JSONArray skus = new JSONArray();
            skus.add(request.getSku());
            postData.put("commitAttribute", commitAttribute);
            postData.put("crowdId", -1);
            postData.put("skus", skus);
            httpPost.setEntity(new StringEntity(postData.toJSONString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String result = HttpUtil.readResponse(response);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject==null){
                LOGGER.error(result);
                return dmpResult;
            }
            if (jsonObject.getIntValue("code") == 1) {
                dmpResult.setCoverCount(jsonObject.getJSONObject("data").getIntValue("totalUV"));
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return dmpResult;
    }

    public List<KuaicheResult> crawlKuaicheRank(String equipment, String area, String areaValue, String keyword, int position) {
        List<KuaicheResult> results = new ArrayList<>();
        CookieStore cookieStore = new BasicCookieStore();
        LoginAccount loginAccount = accountService.loadRandomDmpAccount();
        if (loginAccount != null) {
            while (!loginService.testLogin(loginAccount.getUserName(), loginAccount.getPassword())) {
                loginTask.addLoginTask(loginAccount);
                if (accountService.accountCount() == loginTask.getTaskSize()) {
                    return results;
                }
                loginAccount = accountService.loadRandomDmpAccount();
            }
        }
        cookieTool.loadCookie(loginAccount.getUserName(), loginAccount.getPassword(), cookieStore);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000).setConnectionRequestTimeout(10000)
                .setSocketTimeout(10000).build();
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient(proxyService.getProxy());
        try {
            HttpPost httpPost = new HttpPost("https://jzt.jd.com/kc/generalizeCondition/genCondSearch");
            httpPost.addHeader("Referer", "https://jzt.jd.com/kc/generalizeCondition/genCondSearch");
            httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpPost.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            httpPost.addHeader("Upgrade-Insecure-Requests", "1");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setConfig(requestConfig);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("equipment", equipment));
            nvps.add(new BasicNameValuePair("area", area));
            nvps.add(new BasicNameValuePair("areaValue", areaValue));
            nvps.add(new BasicNameValuePair("keyValue", keyword));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String result = HttpUtil.readResponse(response, "utf-8");
            Document doc = Jsoup.parse(result);
            if ("1".equals(equipment)) {
                switch (position) {
                    //商品精选（京东左侧）
                    case 1: {
                        Elements elements = doc.select("div[id=promotion_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //商品精选（京东底部）
                    case 2: {
                        Elements elements = doc.select("div[id=selection_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //信息流原生
                    case 3: {
                        Elements elements = doc.select("div[id=infomation_native] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //>商家精选
                    case 4: {
                        Elements elements = doc.select("div[id=merchant_selection] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    default:
                        return results;
                }
            } else {
                switch (position) {
                    //APP搜索页
                    case 1: {
                        Elements elements = doc.select("div[id=app_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //微信搜索页
                    case 2: {
                        Elements elements = doc.select("div[id=wei_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //手Q搜索页
                    case 3: {
                        Elements elements = doc.select("div[id=q_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    //京东M端搜索页
                    case 4: {
                        Elements elements = doc.select("div[id=m_goods] > ul > li");
                        for (Element e : elements) {
                            results.add(parseKuaicheResult(e));
                        }
                        return results;
                    }
                    default:
                        return results;
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return results;
    }

    private KuaicheResult parseKuaicheResult(Element e) {
        KuaicheResult kuaicheResult = new KuaicheResult();
        kuaicheResult.setImg(e.getElementsByTag("img").attr("src"));
        kuaicheResult.setTitle(e.getElementsByAttributeValue("class", "price_tit").text());
        kuaicheResult.setRank(Integer.parseInt(e.getElementsByTag("em").text()));
        kuaicheResult.setPrice(e.getElementsByAttributeValue("class", "red").text());
        kuaicheResult.setComment(Integer.parseInt(e.getElementsByAttributeValue("class", "blue").text()));
        return kuaicheResult;
    }


    private String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "").substring(20);
    }
}
