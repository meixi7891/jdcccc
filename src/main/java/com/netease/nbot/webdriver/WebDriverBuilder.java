package com.netease.nbot.webdriver;

import com.netease.nbot.service.impl.HttpCrawlerService;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebDriverBuilder {

    @Value("${webdriver.path}")
    private String chromeDriverPath;
    @Value("${webdriver.chrome.bin}")
    private String chromeBinPath;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebDriverBuilder.class);

    public ChromeDriver getWebDriver() {
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


            String proxy = "";

//        if (proxy != null) {
//            options.setProxy(new Proxy().setHttpProxy(proxy).setSocksProxy(proxy));
//        }
            ChromeDriver driver = new ChromeDriver(options);
            driver.manage().window().maximize();
            driver.get("https://www.baidu.com");
            LOGGER.info("======================= chrome window size" + driver.manage().window().getSize() + " ===========================");
            return driver;
        } catch (Exception e) {
            LOGGER.error("======================start chrome error", e);
            return null;
        }

    }
}
