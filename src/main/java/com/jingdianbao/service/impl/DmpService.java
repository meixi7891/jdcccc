package com.jingdianbao.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.DmpRequest;
import com.jingdianbao.entity.DmpResult;
import com.jingdianbao.http.HttpClientFactory;
import com.jingdianbao.redis.RedisClient;
import com.jingdianbao.util.CookieUtil;
import com.jingdianbao.webdriver.WebDriverActionDelegate;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class DmpService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmpService.class);

    @Autowired
    private WebDriverBuilder webDriverBuilder;
    @Autowired
    private WebDriverActionDelegate webDriverActionDelegate;
    @Autowired
    private CaptchaService captchaService;
    @Value("${webdriver.screenShot.path}")
    private String PREFIX_FILE_PATH;

    public DmpResult crawl(DmpRequest request) {
        DmpResult dmpResult = new DmpResult();
        ChromeDriver webDriver = webDriverBuilder.getWebDriver();
        CookieUtil.loadCookie(request.getUserName(), request.getPassword(), webDriver);
        webDriver.get("https://jzt.jd.com/dmp/newtag/list");
        Point frameLocation = new Point(0, 0);
        sleep(1000);

        String tagId = null;
        try {
            if (isElementDisplayed("//iframe", webDriver)) {
                WebElement frame = webDriver.findElementByXPath("//iframe");
                frameLocation = frame.getLocation();
                webDriver.switchTo().frame(0);
                webDriver.findElementById("loginname").sendKeys(request.getUserName());
                sleep(1000);
                webDriver.findElementById("nloginpwd").sendKeys(request.getPassword());
                sleep(1000);
                webDriver.findElementById("paipaiLoginSubmit").click();
                if (waitElementDisplayed("//img[@id='JD_Verification1']", webDriver, 10)) {
                    LOGGER.info("================ verifyCode needed !====================");
                    for (int i = 0; i < 3; i++) {
                        verifyCode("//img[@id='JD_Verification1']", webDriver, frameLocation);
                        if (i == 2 && isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            dmpResult.setErrorMessage("打码3次失败");
                            return dmpResult;
                        }
                        if (isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (isElementDisplayed("//label[@id='loginpwd_error']", webDriver)) {
                    dmpResult.setErrorMessage(webDriver.findElementByXPath("//label[@id='loginpwd_error']").getText());
                    return dmpResult;
                }
            }
            if (!waitElementDisplayed("//i[@class='icon-user']", webDriver, 5)) {
                dmpResult.setErrorMessage("登录失败");
                return dmpResult;
            } else {
                CookieUtil.saveCookie(request.getUserName(), request.getPassword(), webDriver);
                sleep(500);
                webDriver.findElementByXPath("//div[@id='000100020001']//div[@class='new-label']/a").click();
                sleep(500);
                String tagName = uuid();
                webDriver.findElementById("purchaseTagName").sendKeys(tagName);
                sleep(500);
                webDriver.findElementById("purchaseSkus").sendKeys(request.getSku());
                sleep(500);
                webDriver.findElementById("noexpandcro").click();
                sleep(500);
                webDriver.findElementById("zdyliulanaction").click();
                sleep(500);
                webDriver.executeScript("chooseCycle(30,\"zdygoumaiaction\",\"zdygoumaicycle\")");
                sleep(500);
                webDriver.executeScript("addPurchaseSku()");
                sleep(500);
                List<WebElement> elements = webDriver.findElementsByXPath("//div[@id='skuDetail_" + request.getSku() + "']/div/div");
                dmpResult.setSku(elements.get(0).getText());
                dmpResult.setName(elements.get(1).getText());
                dmpResult.setBrand(elements.get(2).getText());
                dmpResult.setShop(elements.get(3).getText());
                dmpResult.setCategory(elements.get(4).getText());
                Document doc = Jsoup.connect(dmpResult.getUrl()).timeout(30000).get();
                dmpResult.setImg(doc.select("#spec-img").attr("data-origin"));
                sleep(500);
                webDriver.executeScript("createTag('purchaseWindow')");
                if (webDriverActionDelegate.isAlertPresent(webDriver)) {
                    webDriverActionDelegate.acceptAlert(webDriver);
                }
                sleep(500);
                tagId = webDriver.findElementByXPath("//p[contains(text(),'" + tagName + "')]").getAttribute("pk");
                String js = "showTagPortrait('000100020001','" + tagId + "')";
                webDriver.executeScript(js);
                sleep(2000);
                doc = Jsoup.parse(webDriver.getPageSource());
                dmpResult.setCoverCount(Integer.parseInt(doc.select("span[id=totalUV]").text().replaceAll(",", "")));
//                dmpResult.setCoverCount(Integer.parseInt(webDriver.findElementById("totalUV").getText()));
                webDriver.findElementByXPath("//div[@id='tagportrait']//span[@class='close']/img").click();
//                sleep(500);


            }
        } catch (Exception e) {
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
            } catch (IOException e1) {

            }
            dmpResult.setErrorMessage("抓取失败");
        } finally {
            if (tagId != null) {
                try {
                    webDriver.executeScript("deleteTag(" + tagId + ")");
                    sleep(500);
                    if (webDriverActionDelegate.isConfirmPresent(webDriver)) {
                        webDriverActionDelegate.acceptConfirm(webDriver);
                    }
                    sleep(500);
                    if (webDriverActionDelegate.isAlertPresent(webDriver)) {
                        webDriverActionDelegate.acceptAlert(webDriver);
                    }
                } catch (Exception e) {
                    LOGGER.error("", e);
                } finally {
                    webDriver.quit();
                }
            }
        }
        return dmpResult;
    }

    public DmpResult crawlHttp(DmpRequest request) {
        DmpResult dmpResult = new DmpResult();
        CookieStore cookieStore = new BasicCookieStore();
        CookieUtil.loadCookie(request.getUserName(), request.getPassword(), cookieStore);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                .setSocketTimeout(5000).build();

        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpPost httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/skuinfo/list");
        httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
        httpPost.addHeader("Cookie", CookieUtil.getCookieStr(cookieStore));
        httpPost.setConfig(requestConfig);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("skuId", request.getSku()));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            JSONObject jsonObject = readJSONResponse(response);
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
            httpPost.addHeader("Cookie", CookieUtil.getCookieStr(cookieStore));
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
            jsonObject = readJSONResponse(response);
            String tagId = jsonObject.getString("tagId");
            HttpGet httpGet = new HttpGet("https://jzt.jd.com/dmp/newtag/showPortrait?tag_id=" + tagId + "&_=" + System.currentTimeMillis());
            httpGet.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
            httpGet.addHeader("Cookie", CookieUtil.getCookieStr(cookieStore));
            httpGet.setConfig(requestConfig);
            httpClient = HttpClientFactory.getHttpClient();
            response = httpClient.execute(httpGet);
            jsonObject = readJSONResponse(response);
            dmpResult.setCoverCount(jsonObject.getIntValue("totalUV"));

            httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/deltag");
            httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
            httpPost.addHeader("Cookie", CookieUtil.getCookieStr(cookieStore));
            httpPost.setConfig(requestConfig);
            nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("tagId", tagId));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            httpClient = HttpClientFactory.getHttpClient();
            response = httpClient.execute(httpPost);
            jsonObject = readJSONResponse(response);
            if (jsonObject.getIntValue("code") != 1) {
                dmpResult.setErrorMessage("删除失败");
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return dmpResult;
    }

    public boolean loginHttp(DmpRequest request) {
        CookieStore cookieStore = new BasicCookieStore();
        try {
            if (CookieUtil.hasCookie(request.getUserName(), request.getPassword())) {
                CookieUtil.loadCookie(request.getUserName(), request.getPassword(), cookieStore);
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                        .setSocketTimeout(5000).build();
                CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
                HttpPost httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/skuinfo/list");
                httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
                httpPost.addHeader("Cookie", CookieUtil.getCookieStr(cookieStore));
                httpPost.setConfig(requestConfig);
                List<NameValuePair> nvps = new ArrayList<>();
                nvps.add(new BasicNameValuePair("skuId", "23536794365"));

                httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
                CloseableHttpResponse response = httpClient.execute(httpPost);
                JSONObject jsonObject = readJSONResponse(response);
                if (jsonObject == null) {
                    return login(request);
                }else {
                    return true;
                }
            }else {
                return login(request);
            }
        } catch (Exception e) {
            LOGGER.error("", e);
            return false;
        }
    }

    public boolean login(DmpRequest request) {
        DmpResult dmpResult = new DmpResult();
        ChromeDriver webDriver = webDriverBuilder.getWebDriver();
        CookieUtil.loadCookie(request.getUserName(), request.getPassword(), webDriver);
        webDriver.get("https://jzt.jd.com/dmp/newtag/list");
        if (waitElementDisplayed("//i[@class='icon-user']", webDriver, 5)) {
            webDriver.quit();
            return true;
        }
        Point frameLocation = new Point(0, 0);
        sleep(1000);
        try {
            if (isElementDisplayed("//iframe", webDriver)) {
                WebElement frame = webDriver.findElementByXPath("//iframe");
                frameLocation = frame.getLocation();
                webDriver.switchTo().frame(0);
                webDriver.findElementById("loginname").sendKeys(request.getUserName());
                sleep(1000);
                webDriver.findElementById("nloginpwd").sendKeys(request.getPassword());
                sleep(1000);
                webDriver.findElementById("paipaiLoginSubmit").click();
                if (waitElementDisplayed("//img[@id='JD_Verification1']", webDriver, 10)) {
                    LOGGER.info("================ verifyCode needed !====================");
                    for (int i = 0; i < 3; i++) {
                        verifyCode("//img[@id='JD_Verification1']", webDriver, frameLocation);
                        if (i == 2 && isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            dmpResult.setErrorMessage("打码3次失败");
                            return false;
                        }
                        if (isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (isElementDisplayed("//label[@id='loginpwd_error']", webDriver)) {
                    dmpResult.setErrorMessage(webDriver.findElementByXPath("//label[@id='loginpwd_error']").getText());
                    return false;
                }
            }
            if (!waitElementDisplayed("//i[@class='icon-user']", webDriver, 5)) {
                dmpResult.setErrorMessage("登录失败");
                return false;
            } else {
                CookieUtil.saveCookie(request.getUserName(), request.getPassword(), webDriver);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
            } catch (IOException e1) {
                LOGGER.error("", e);
                return false;
            }
        } finally {
            webDriver.quit();
        }
        return false;
    }


    private void verifyCode(String verifyCodeXpath, ChromeDriver webDriver, Point frameLocation) {
        String code = captchaService.getCode(webDriverActionDelegate.takeScreenShotInFrame(webDriver, webDriver.findElementByXPath(verifyCodeXpath), "png", frameLocation));
        LOGGER.info("==================verify code is : " + code + " ================");
        webDriver.findElementById("authcode").sendKeys(code);
        sleepRandom(500);
        webDriver.findElementById("paipaiLoginSubmit").click();
    }


    private JSONObject readJSONResponse(CloseableHttpResponse response) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject jsonObject = JSONObject.parseObject(sb.toString());
            return jsonObject;
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }

    }

    private String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "").substring(20);
    }

    private boolean isElementDisplayed(String xpath, WebDriver webDriver) {
        try {
            WebElement webElement = webDriver.findElement(By.xpath(xpath));
            return webElement.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean waitElementDisplayed(String xpath, WebDriver webDriver, int seconds) {
        for (int i = 0; i < seconds; i++) {
            try {
                WebElement webElement = webDriver.findElement(By.xpath(xpath));
                return webElement.isDisplayed();
            } catch (NoSuchElementException e) {
                sleep(1000);
            }
        }
        return false;
    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {

        }
    }

    private void sleepRandom(long mills) {
        try {
            Random random = new Random();
            mills = random.nextInt(1000);
            Thread.sleep(mills);
        } catch (InterruptedException e) {

        }
    }
}
