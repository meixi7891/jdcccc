package com.jingdianbao.service.impl;

import com.jingdianbao.entity.DmpRequest;
import com.jingdianbao.entity.DmpResult;
import com.jingdianbao.redis.RedisClient;
import com.jingdianbao.webdriver.WebDriverActionDelegate;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.apache.commons.io.FileUtils;
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
        loadCookie(request.getUserName(), request.getPassword(), webDriver);
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
                saveCookie(request.getUserName(), request.getPassword(), webDriver);
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

    private void verifyCode(String verifyCodeXpath, ChromeDriver webDriver, Point frameLocation) {
        String code = captchaService.getCode(webDriverActionDelegate.takeScreenShotInFrame(webDriver, webDriver.findElementByXPath(verifyCodeXpath), "png", frameLocation));
        LOGGER.info("==================verify code is : " + code + " ================");
        webDriver.findElementById("authcode").sendKeys(code);
        sleepRandom(500);
        webDriver.findElementById("paipaiLoginSubmit").click();
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

    private void saveCookie(String userName, String pwd, WebDriver webDriver) {
        Set<Cookie> cookieSet = webDriver.manage().getCookies();
        String key = userName + "_" + pwd;

        try {
            StringBuilder sb = new StringBuilder();
            for (Cookie cookie : cookieSet) {
                sb.append(cookie.toString()).append("@__@");
            }
            String str = sb.toString();
            Jedis jedis = RedisClient.getRedesClient().getJedis();
            jedis.hset("cookies", key, str);
            RedisClient.getRedesClient().returnResource(jedis);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void loadCookie(String userName, String pwd, WebDriver webDriver) {
        Jedis jedis = RedisClient.getRedesClient().getJedis();
        String key = userName + "_" + pwd;
        try {
            String str = jedis.hget("cookies", key);
            if (str == null || str.isEmpty()) {
                return;
            }
            String[] ss = str.split("@__@");
            for (int i = 0; i < ss.length; i++) {
                Cookie cookie = buildCookie(ss[i]);
                if (cookie != null) {
                    webDriver.manage().addCookie(cookie);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private Cookie buildCookie(String str) {
        String[] ss = str.split(";");
        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        Date expiry = null;
        boolean isSecure = false;
        try {
            for (int i = 0; i < ss.length; i++) {
                if (ss[i].contains("=")) {
                    String[] sss = ss[i].split("=");
                    if (sss[0].trim().equals("expires")) {
                        expiry = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").parse(sss[1].trim());
                    } else if (sss[0].trim().equals("path")) {
                        path = sss[1].trim();
                    } else if (sss[0].trim().equals("domain")) {
                        domain = sss[1].trim();
                    } else {
                        name = sss[0].trim();
                        value = sss[1].trim();
                    }
                } else {
                    if (ss[i].trim().equals("secure")) {
                        isSecure = true;
                    }
                }
            }
            if (name == null) {
                return null;
            }
            Cookie cookie = new Cookie(name, value, domain, path, expiry, isSecure);
            return cookie;
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }
}
