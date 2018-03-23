package com.jingdianbao.service.impl;

import com.jingdianbao.entity.SearchRequest;
import com.jingdianbao.entity.SearchResult;
import com.jingdianbao.service.CrawlerService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WebDriverCrawlerService implements CrawlerService {
    @Override
    public List<SearchResult> search(SearchRequest request) throws Exception{
        List<SearchResult> resultList = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", "E:/chromedriver_win32/chromedriver.exe");
        options.setBinary("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
        options.addArguments("start-maximized");
        ChromeDriver driver = new ChromeDriver(options);
        driver.get("https://www.jd.com/");
        String sku = String.valueOf(request.getSku());
        String shop = "迪莎衣品";
        sleep(500);
        driver.findElementById("key").sendKeys(request.getKeyword());
        sleep(500);
        driver.findElementByXPath("//button[@class='button']").click();
        sleep(500);
        int page = 1;

        WebElement element = driver.findElementByXPath("//span[@class='p-skip']/input[@class='input-txt']");
        int elementPosition = element.getLocation().getY() - 50;
        String js = String.format("window.scroll(0, %s)", elementPosition);
        driver.executeScript(js);
        sleep(500);
        List<WebElement> elements = driver.findElementsByXPath("//li[@data-sku]");
        for (int i = 0; i < elements.size(); i++) {
            if (sku.equals(elements.get(i).getAttribute("data-sku"))) {
                System.out.println("sku page : " + page + ", index : " + i);
            }
            String shopElement = elements.get(i).findElement(By.xpath("div/div[@class='p-shop']/span/a")).getAttribute("title");
            if (shopElement.contains(shop)) {
                System.out.println("shop page : " + page + ", index : " + i + ", sku : " + elements.get(i).getAttribute("data-sku") + " , shop : " + shopElement);
            }
        }
        int pageTotal = Integer.parseInt(driver.findElementByXPath("//span[@class='p-skip']/em/b").getText());
        for (page = 2; page <= pageTotal; page++) {
            driver.findElementByXPath("//a[@class='pn-next']").click();
            sleep(500);
            driver.executeScript(js);
            sleep(500);
            elements = driver.findElementsByXPath("//li[@data-sku]");
            for (int i = 0; i < elements.size(); i++) {
                if (sku.equals(elements.get(i).getAttribute("data-sku"))) {
                    System.out.println("sku page : " + page + ", index : " + i);
                }
                String shopElement = elements.get(i).findElement(By.xpath("div/div[@class='p-shop']/span/a")).getAttribute("title");
                if (shopElement.contains(shop)) {
                    System.out.println("shop page : " + page + ", index : " + i + ", sku : " + elements.get(i).getAttribute("data-sku") + " , shop : " + shopElement);
                }
            }
            pageTotal = Integer.parseInt(driver.findElementByXPath("//span[@class='p-skip']/em/b").getText());
        }
        driver.quit();
        return resultList;
    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {

        }
    }
}