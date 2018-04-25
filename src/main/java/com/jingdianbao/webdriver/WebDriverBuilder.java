package com.jingdianbao.webdriver;

import com.jingdianbao.service.impl.ProxyService;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebDriverBuilder {

    @Value("${webdriver.path}")
    private String chromeDriverPath;
    @Value("${webdriver.chrome.bin}")
    private String chromeBinPath;
    @Value("${webdriver.chrome.max.size}")
    private int maxdriversize;

    private AtomicInteger diverCount = new AtomicInteger(0);
    @Autowired
    private ProxyService proxyService;


    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverBuilder.class);

    public ChromeDriver getWebDriver() {
        if (diverCount.get() == maxdriversize) {
            return null;
        }
        try {
            DesiredCapabilities dcaps = new DesiredCapabilities();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("start-maximized");
            options.addArguments("--no-sandbox");
//            System.setProperty("webdriver.chrome.driver", "E:\\chromedriver_win32\\chromedriver.exe");
//            options.setBinary("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            options.setBinary(chromeBinPath);
//        System.setProperty("Importal.xvfb.id", "7");
//        options.setCapability("DISPLAY", "7");
            options.setHeadless(true);
            options.merge(dcaps);

            String proxy = "";

//        if (proxy != null) {
//            options.setProxy(new Proxy().setHttpProxy(proxy).setSocksProxy(proxy));
//        }
            ChromeDriver driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.get("https://www.jd.com/");
            LOGGER.info("======================= chrome window size" + driver.manage().window().getSize() + " ===========================");
            diverCount.addAndGet(1);
            return driver;
        } catch (Exception e) {
            LOGGER.error("======================start chrome error", e);
            return null;
        }

    }

    public ChromeDriver getWebDriverWithProxy(String ip, String port) {
        if (diverCount.get() == maxdriversize) {
            return null;
        }
        try {
            DesiredCapabilities dcaps = new DesiredCapabilities();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("start-maximized");
//            System.setProperty("webdriver.chrome.driver", "E:\\chromedriver_win32\\chromedriver.exe");
//            options.setBinary("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
            System.setProperty("webdriver.chrome.driver", chromeDriverPath);
            options.setBinary(chromeBinPath);
//        System.setProperty("Importal.xvfb.id", "7");
//        options.setCapability("DISPLAY", "7");
            options.setHeadless(true);
            options.merge(dcaps);
            String proxy = ip + ":" + port;
            options.setProxy(new Proxy().setHttpProxy(proxy).setSslProxy(proxy));
            ChromeDriver driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.get("https://www.jd.com/");
            LOGGER.info("======================= chrome window size" + driver.manage().window().getSize() + " ===========================");
            diverCount.addAndGet(1);
            return driver;
        } catch (Exception e) {
            LOGGER.error("======================start chrome error", e);
            return null;
        }
    }

    public ChromeDriver getH5Driver() {
        Map<String, String> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceName", "iPhone X");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        chromeOptions.addArguments("--no-sandbox");
//        System.setProperty("webdriver.chrome.driver", "E:\\chromedriver_win32\\chromedriver.exe");
//        chromeOptions.setBinary("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe");
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        chromeOptions.setBinary(chromeBinPath);
        chromeOptions.setHeadless(true);
        ChromeDriver driver = new ChromeDriver(chromeOptions);
        return driver;
    }

    public void returnDriver(ChromeDriver chromeDriver) {
        chromeDriver.quit();
        diverCount.getAndAdd(-1);
    }
}
