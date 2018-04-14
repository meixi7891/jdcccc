package com.jingdianbao.service.impl;

import com.jingdianbao.entity.DmpResult;
import com.jingdianbao.entity.LoginResult;
import com.jingdianbao.http.HttpClientFactory;
import com.jingdianbao.util.CookieTool;
import com.jingdianbao.util.HttpUtil;
import com.jingdianbao.webdriver.WebDriverActionDelegate;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class LoginService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private WebDriverBuilder webDriverBuilder;
    @Autowired
    private WebDriverActionDelegate webDriverActionDelegate;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private CookieTool cookieTool;
    @Value("${webdriver.screenShot.path}")
    private String PREFIX_FILE_PATH;

    public boolean testLogin(String userName, String password) {
        CookieStore cookieStore = new BasicCookieStore();
        try {
            if (cookieTool.hasCookie(userName, password)) {
                cookieTool.loadCookie(userName, password, cookieStore);
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                        .setSocketTimeout(5000).build();
                CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
                HttpPost httpPost = new HttpPost("https://jzt.jd.com/dmp/tag/skuinfo/list");
                httpPost.addHeader("Referer", "https://jzt.jd.com/dmp/newtag/list");
                httpPost.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
                httpPost.setConfig(requestConfig);
                List<NameValuePair> nvps = new ArrayList<>();
                nvps.add(new BasicNameValuePair("skuId", "23536794365"));
                httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
                CloseableHttpResponse response = httpClient.execute(httpPost);
                int code = response.getStatusLine().getStatusCode();
                String result = HttpUtil.readResponse(response);
                if (result == null || result.isEmpty()) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("", e);
            return false;
        }
    }


    public boolean loginDmp(String userName, String password) {
        ChromeDriver webDriver = null;
        try {
            LOGGER.error("================ start login by web driver !====================");
            DmpResult dmpResult = new DmpResult();
            webDriver = webDriverBuilder.getWebDriver();
            if (webDriver == null) {
                LOGGER.error("================ start login by web driver failed !====================");
                return false;
            }
            cookieTool.loadCookie(userName, password, webDriver);
            webDriver.get("https://jzt.jd.com/dmp/newtag/list");
            if (webDriverActionDelegate.waitElementDisplayed("//i[@class='icon-user']", webDriver, 5)) {
                webDriver.quit();
                return true;
            }
            Point frameLocation = new Point(0, 0);
            webDriverActionDelegate.sleep(1000);
            if (webDriverActionDelegate.isElementDisplayed("//iframe", webDriver)) {
                WebElement frame = webDriver.findElementByXPath("//iframe");
                frameLocation = frame.getLocation();
                webDriver.switchTo().frame(0);
                webDriver.findElementById("loginname").sendKeys(userName);
                webDriverActionDelegate.sleep(1000);
                webDriver.findElementById("nloginpwd").sendKeys(password);
                webDriverActionDelegate.sleep(1000);
                webDriver.findElementById("paipaiLoginSubmit").click();
                if (webDriverActionDelegate.waitElementDisplayed("//img[@id='JD_Verification1']", webDriver, 10)) {
                    LOGGER.error("================ verifyCode needed !====================");
                    for (int i = 0; i < 3; i++) {
                        verifyCode("//img[@id='JD_Verification1']", webDriver, frameLocation);
                        if (i == 2 && webDriverActionDelegate.isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            LOGGER.error("================ 打码3次失败 !====================");
                            dmpResult.setErrorMessage("打码3次失败");
                            return false;
                        }
                        if (webDriverActionDelegate.isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                            continue;
                        } else {
                            break;
                        }
                    }
                }
                if (webDriverActionDelegate.isElementDisplayed("//label[@id='loginpwd_error']", webDriver)) {
                    dmpResult.setErrorMessage(webDriver.findElementByXPath("//label[@id='loginpwd_error']").getText());
                    LOGGER.error("================ 密码错误 ====================");
                    return false;
                }
            }
            if (!webDriverActionDelegate.waitElementDisplayed("//i[@class='icon-user']", webDriver, 5)) {
                LOGGER.error("================ 登录失败 ====================");
                webDriverActionDelegate.takeFullScreenShot(webDriver);
                try {
                    FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
                } catch (IOException e) {
                    LOGGER.error("", e);
                    return false;
                }
                dmpResult.setErrorMessage("登录失败");
                return false;
            } else {
                LOGGER.error("================ 登录成功 ====================");
                cookieTool.saveCookie(userName, password, webDriver);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "gbk");
            } catch (IOException e1) {
                LOGGER.error("", e);
                return false;
            }
        } finally {
            if (webDriver != null) {
                webDriverBuilder.returnDriver(webDriver);
            }
        }
        return false;
    }


    private void verifyCode(String verifyCodeXpath, ChromeDriver webDriver, Point frameLocation) {
        String code = captchaService.getCode(webDriverActionDelegate.takeScreenShotInFrameBase64(webDriver, webDriver.findElementByXPath(verifyCodeXpath), "png", frameLocation));
        LOGGER.error("==================verify code is : " + code + " ================");
        webDriver.findElementById("authcode").sendKeys(code);
        webDriverActionDelegate.sleepRandom(500);
        webDriver.findElementById("paipaiLoginSubmit").click();
    }


    public LoginResult loginSellerBackend(String userName, String password) {
        LoginResult loginResult = new LoginResult();
        ChromeDriver webDriver = null;
        try {
            LOGGER.error("================ start login by web driver !====================");
            webDriver = webDriverBuilder.getWebDriver();
            if (webDriver == null) {
                LOGGER.error("================ start login by web driver failed !====================");
                loginResult.setStatus(-3);
                loginResult.setMessage("chrome资源不足");
            }
            webDriver.get("http://passport.jd.com/common/loginPage?from=pop_vender&regTag=2&ReturnUrl=https://shop.jd.com");
            Point frameLocation = new Point(0, 0);
            webDriverActionDelegate.sleep(1000);
            webDriver.findElementById("loginname").sendKeys(userName);
            webDriverActionDelegate.sleep(1000);
            webDriver.findElementById("nloginpwd").sendKeys(password);
            webDriverActionDelegate.sleep(1000);
            webDriver.findElementById("paipaiLoginSubmit").click();
            if (webDriverActionDelegate.waitElementDisplayed("//img[@id='JD_Verification1']", webDriver, 5)) {
                LOGGER.error("================ verifyCode needed !====================");
                for (int i = 0; i < 3; i++) {
                    webDriver.findElementById("JD_Verification1").click();
                    verifyCode("//img[@id='JD_Verification1']", webDriver, frameLocation);
                    if (i == 2 && webDriverActionDelegate.isElementDisplayed("//label[@id='authcode_error']", webDriver)) {
                        LOGGER.error("================ 打码3次失败 !====================");
                        loginResult.setMessage("打码3次失败");
                        loginResult.setStatus(-1);
                        return loginResult;
                    }
                    if (webDriverActionDelegate.waitElementDisplayed("//img[@id='JD_Verification1']", webDriver, 3)) {
                        continue;
                    } else {
                        break;
                    }
                }
            }
            if (webDriverActionDelegate.isElementDisplayed("//label[@id='loginpwd_error']", webDriver)) {
                loginResult.setStatus(-1);
                loginResult.setMessage(webDriver.findElementByXPath("//label[@id='loginpwd_error']").getText());
                LOGGER.error("================ 密码错误 ====================");
                return loginResult;
            }

            if (webDriverActionDelegate.waitElementDisplayed("//div[@class='wb-per']", webDriver, 5)) {
                loginResult.setStatus(-2);
                String message = webDriver.findElementByXPath("//div[@class='wb-per']").getText();
                message = message.replace(">", "");
                loginResult.setMessage(message);
                return loginResult;
            }
            if (webDriverActionDelegate.waitElementDisplayed("//div[@id='tab_phoneV']", webDriver, 5)) {
                cookieTool.saveSellerCookie(userName, password, webDriver);
                LOGGER.error("================ 登录商家后台成功 ====================");
                loginResult.setStatus(0);
                loginResult.setMessage("");
                return loginResult;

            } else {
                LOGGER.error("================ 登录失败 ====================");
                webDriverActionDelegate.takeFullScreenShot(webDriver);
                try {
                    FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
                loginResult.setStatus(-1);
                loginResult.setMessage("登录失败");
            }
        } catch (Exception e) {
            loginResult.setStatus(-1);
            loginResult.setMessage("登录失败");
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "gbk");
            } catch (IOException e1) {
                LOGGER.error("", e);
            }
        } finally {
            if (webDriver != null) {
                webDriverBuilder.returnDriver(webDriver);
            }
        }
        return loginResult;
    }

}
