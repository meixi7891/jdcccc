package com.jingdianbao.job;

import com.jingdianbao.service.impl.HttpCrawlerService;
import com.jingdianbao.service.impl.ProxyService;
import com.jingdianbao.util.CookieTool;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.touch.TouchActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class WqsCatUrlLoadJob {

    private volatile List<HttpCrawlerService.CatUrl> catUrlList = new ArrayList<>();

    @Autowired
    private WebDriverBuilder webDriverBuilder;

    private volatile boolean loading = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(WqsCatUrlLoadJob.class);

    @Autowired
    private CookieTool cookieTool;

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * ?")
    private void load() {
        LOGGER.error("=============== load wqs category url start ==================");
        List<HttpCrawlerService.CatUrl> newCatUrlList = new ArrayList<>();
        ChromeDriver webDriver = webDriverBuilder.getH5Driver();
        webDriver.get("http://wqs.jd.com/portal/sq/category_q.shtml?shownav=1&ptag=137652.25.4");
        sleep(1000);
        List<WebElement> elements = webDriver.findElementsByXPath("//ul[@class='category1']/li/span");
        for (WebElement e : elements) {
            TouchActions touchActions = new TouchActions(webDriver);
            touchActions.flick(e, 0, 0, 1).perform();
            sleep(1000);
            String page = webDriver.getPageSource();
            Document doc = Jsoup.parse(page);
            Elements elements1 = doc.select("div[id=category2] > dl > dd > a");
            for (Element e1 : elements1) {
                HttpCrawlerService.CatUrl catUrl = new HttpCrawlerService.CatUrl();
                catUrl.catName = e1.text();
                catUrl.url = e1.attr("target");
                newCatUrlList.add(catUrl);
            }
        }
        webDriver.quit();
        synchronized (this) {
            loading = true;
            catUrlList = newCatUrlList;
            loading = false;
        }
        webDriver = webDriverBuilder.getWebDriver();
        String pvid = uuid();
        String keyword = "2017%E8%BF%9E%E8%A1%A3%E8%A3%99";
        String refer = "https://search.jd.com/Search?keyword=" + keyword + "&enc=utf-8&pvid=" + pvid;
        webDriver.get(refer);
        cookieTool.saveCookie("search", "PC", webDriver);
        webDriver.quit();
        LOGGER.error("=============== load wqs category url end ==================");
    }

    public List<HttpCrawlerService.CatUrl> getCatUrlList() {
        if (loading) {
            synchronized (this) {
                return catUrlList;
            }
        } else {
            return catUrlList;
        }
    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {

        }
    }

    private String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "");
    }
}
