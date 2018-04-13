package com.jingdianbao.controller;

import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.*;
import com.jingdianbao.service.impl.DmpService;
import com.jingdianbao.service.impl.HttpCrawlerService;

import com.jingdianbao.service.impl.LoginService;
import com.jingdianbao.util.CookieTool;
import com.jingdianbao.webdriver.WebDriverBuilder;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/crawler")
public class CrawlerController {

    @Autowired
    private HttpCrawlerService httpCrawlerService;

    @Autowired
    private DmpService dmpService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private WebDriverBuilder webDriverBuilder;

    @Autowired
    private CookieTool cookieTool;

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerController.class);

    @RequestMapping("/search")
    @ResponseBody
    public JSONObject search(@RequestParam(value = "source", required = false, defaultValue = "") String source,
                             @RequestParam(value = "type", required = false, defaultValue = "") String type,
                             @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                             @RequestParam(value = "sku", required = false, defaultValue = "") String sku,
                             @RequestParam(value = "sortType", required = false, defaultValue = "") String sortType,
                             @RequestParam(value = "shop", required = false, defaultValue = "") String shop,
                             @RequestParam(value = "priceStart", required = false, defaultValue = "") String priceStart,
                             @RequestParam(value = "priceEnd", required = false, defaultValue = "") String priceEnd) {

        SearchRequest request = new SearchRequest(type, source, keyword, sku, sortType, shop, priceStart, priceEnd);
        List<SearchResult> resultList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        try {
            resultList = httpCrawlerService.search(request);
            jsonObject.put("code", 0);
            jsonObject.put("message", "抓取成功");
        } catch (Exception e) {
            LOGGER.error("", e);
            jsonObject.put("code", -1);
            jsonObject.put("message", "抓取失败");
        }
        jsonObject.put("result", resultList);
        return jsonObject;
    }

    @RequestMapping("/searchAll")
    @ResponseBody
    public JSONObject searchAll(
            @RequestParam(value = "type", required = false, defaultValue = "") String type,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "sku", required = false, defaultValue = "") String sku,
            @RequestParam(value = "sortType", required = false, defaultValue = "") String sortType,
            @RequestParam(value = "shop", required = false, defaultValue = "") String shop,
            @RequestParam(value = "priceStart", required = false, defaultValue = "") String priceStart,
            @RequestParam(value = "priceEnd", required = false, defaultValue = "") String priceEnd) {
        JSONObject jsonObject = new JSONObject();
        SearchRequest request = new SearchRequest(type, "PC", keyword, sku, sortType, shop, priceStart, priceEnd);
        List<SearchResult> resultList = new ArrayList<>();
        List<SearchMergedResult> result = new ArrayList<>();
        try {
            resultList = httpCrawlerService.search(request);
            for (SearchResult searchResult : resultList) {
                result.add(new SearchMergedResult(searchResult));
            }
            request.setSource("H5");
            resultList = httpCrawlerService.search(request);
            for (SearchMergedResult searchMergedResult : result) {
                for (SearchResult searchResult : resultList) {
                    if (searchMergedResult.getSku().equals(searchResult.getSku())) {
                        searchMergedResult.merge(searchResult);
                    }
                }
            }
            jsonObject.put("code", 0);
            jsonObject.put("message", "抓取成功");
        } catch (Exception e) {
            jsonObject.put("code", -1);
            jsonObject.put("message", "抓取失败");
        }
        jsonObject.put("result", result);
        return jsonObject;
    }


    @RequestMapping("/jobList")
    @ResponseBody
    public JSONObject jobList() {
        JSONObject jsonObject = new JSONObject();
        return jsonObject;
    }

    @RequestMapping("/dmp")
    @ResponseBody
    public JSONObject dmp(@RequestParam(value = "userName", required = false, defaultValue = "") String userName,
                          @RequestParam(value = "password", required = false, defaultValue = "") String password,
                          @RequestParam(value = "sku", required = false, defaultValue = "") String sku) {
        DmpRequest dmpRequest = new DmpRequest(userName, password, sku);
        JSONObject jsonObject = new JSONObject();
        DmpResult dmpResult = dmpService.crawlHttpNew(dmpRequest);
        if (dmpRequest == null) {
            jsonObject.put("code", -1);
            jsonObject.put("message", "请稍后重试");
        } else {
            jsonObject.put("code", 0);
            jsonObject.put("result", dmpResult);
        }
        return jsonObject;
    }

    @RequestMapping("/loginDmp")
    @ResponseBody
    public JSONObject loginDmp(@RequestParam(value = "userName", required = false, defaultValue = "") String userName,
                               @RequestParam(value = "password", required = false, defaultValue = "") String password) {
        JSONObject jsonObject = new JSONObject();
        if (cookieTool.hasSellerCookie(userName, password)) {
            jsonObject.put("code", 0);
            jsonObject.put("message", "");
        } else {
            LoginResult loginResult = loginService.loginSellerBackend(userName, password);
            jsonObject.put("code", loginResult.getStatus());
            jsonObject.put("message", loginResult.getMessage());
        }
        return jsonObject;
    }

    @RequestMapping("/testProxy")
    @ResponseBody
    public JSONObject testProxy(@RequestParam(value = "ip", required = false, defaultValue = "") String ip,
                                @RequestParam(value = "port", required = false, defaultValue = "") String port) {
        JSONObject jsonObject = new JSONObject();
        ChromeDriver chromeDriver = webDriverBuilder.getWebDriverWithProxy(ip, port);
        if (chromeDriver != null) {
            chromeDriver.get("http://httpbin.org/ip");
            jsonObject.put("result", chromeDriver.getPageSource());
        }
        return jsonObject;
    }
}
