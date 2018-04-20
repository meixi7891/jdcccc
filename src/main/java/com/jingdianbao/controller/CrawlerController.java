package com.jingdianbao.controller;

import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.*;
import com.jingdianbao.service.impl.AccountService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    @Autowired
    private AccountService accountService;

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
                result.add(new SearchMergedResult(searchResult, request.getSource()));
            }
            request.setSource("H5");
            resultList = httpCrawlerService.search(request);
            Set<String> mergedSkuSet = new HashSet<>();
            for (SearchMergedResult searchMergedResult : result) {
                for (SearchResult searchResult : resultList) {
                    if (searchMergedResult.getSku().equals(searchResult.getSku())) {
                        searchMergedResult.merge(searchResult);
                        mergedSkuSet.add(searchResult.getSku());
                    }
                }
            }
            for (SearchResult searchResult : resultList) {
                if(!mergedSkuSet.contains(searchResult.getSku())){
                    SearchMergedResult h5SearchMergedResult = new SearchMergedResult(searchResult, request.getSource());
                    result.add(h5SearchMergedResult);
                }
            }
            for(SearchMergedResult searchMergedResult : result){
                searchMergedResult.setPrice(httpCrawlerService.crawlSkuPrice(searchMergedResult.getSku()));
                searchMergedResult.setAdverts(httpCrawlerService.crawlSkuAdverts(searchMergedResult.getSku(), searchMergedResult.getPrice()));
                searchMergedResult.setPromotion(httpCrawlerService.crawlSkuPromotion(searchMergedResult.getSku(), searchMergedResult.getPrice()));
                searchMergedResult.setCommentEntity(httpCrawlerService.crawlCommentH5(searchMergedResult.getSku()));
            }
            jsonObject.put("code", 0);
            jsonObject.put("message", "抓取成功");
        } catch (Exception e) {
            LOGGER.error("", e);
            jsonObject.put("code", -1);
            jsonObject.put("message", "抓取失败");
        }
        jsonObject.put("result", result);
        return jsonObject;
    }

    @RequestMapping("/searchComment")
    @ResponseBody
    public JSONObject Comment(@RequestParam(value = "sku", required = false, defaultValue = "") String sku) {
        JSONObject jsonObject = new JSONObject();
        try {
            CommentResult commentResult = httpCrawlerService.crawlCommentH5(sku);
            jsonObject.put("code", 0);
            jsonObject.put("result", commentResult);
            jsonObject.put("message", "抓取成功");
        } catch (Exception e) {
            LOGGER.error("", e);
            jsonObject.put("code", -1);
            jsonObject.put("message", "抓取失败");
        }
        return jsonObject;
    }

    @RequestMapping("/searchPromotion")
    @ResponseBody
    public JSONObject searchPromotion(@RequestParam(value = "sku", required = false, defaultValue = "") String sku) {
        JSONObject jsonObject = new JSONObject();
        try {
            CommentResult commentResult = httpCrawlerService.crawlCommentH5(sku);
            String price = httpCrawlerService.crawlSkuPrice(sku);
            List<String> adverts = httpCrawlerService.crawlSkuAdverts(sku, price);
            Promotion promotion = httpCrawlerService.crawlSkuPromotion(sku, price);
            JSONObject result = new JSONObject();
            result.put("price", price);
            result.put("adverts", adverts);
            result.put("promotion", promotion);
            result.put("comment", commentResult);
            SearchResult searchResult = new SearchResult();
            searchResult.setSku(sku);
            httpCrawlerService.crawlSkuDetail(searchResult);
            if (searchResult.getCategory() != null) {
                result.put("category", searchResult.getCategory());
            }
            jsonObject.put("code", 0);
            jsonObject.put("message", "抓取成功");
            jsonObject.put("result", result);
        } catch (Exception e) {
            LOGGER.error("", e);
            jsonObject.put("code", -1);
            jsonObject.put("message", "抓取失败");
        }
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

    @RequestMapping("/crawlKuaicheRank")
    @ResponseBody
    public JSONObject crawlKuaicheRank(@RequestParam(value = "equipment", required = false, defaultValue = "") String equipment,
                                       @RequestParam(value = "area", required = false, defaultValue = "") String area,
                                       @RequestParam(value = "areaValue", required = false, defaultValue = "") String areaValue,
                                       @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                       @RequestParam(value = "pageNo", required = false, defaultValue = "") String pageNo,
                                       @RequestParam(value = "position", required = false, defaultValue = "") String position) {
        JSONObject jsonObject = new JSONObject();
        if (!Pattern.matches("\\d+", position) || !Pattern.matches("\\d+", pageNo)) {
            jsonObject.put("code", -1);
            jsonObject.put("message", "参数错误");
        }
        List<KuaicheResult> results = dmpService.crawlKuaicheRank(equipment, area, areaValue, keyword, Integer.parseInt(position));
        int pageNum = Integer.parseInt(pageNo) - 1;
        int totalPage = (results.size() / 10) + 1;
        if ("1".equals(equipment)) {
            List<KuaicheResult> subResult = new ArrayList<>();
            if (pageNum < totalPage - 1) {
                subResult = results.subList(0 + pageNum * 10, pageNum * 10 + 10);
            } else {
                subResult = results.subList(0 + pageNum * 10, pageNum * 10 + results.size() % 10);
            }
            jsonObject.put("result", subResult);
        } else {
            jsonObject.put("result", results);
        }
        jsonObject.put("total", results.size());
        jsonObject.put("code", 0);
        jsonObject.put("message", "");
        return jsonObject;
    }

    @RequestMapping("/testLoginDmp")
    @ResponseBody
    public JSONObject testLoginDmp() {
        JSONObject jsonObject = new JSONObject();
        List<LoginAccount> accountList = accountService.allAccounts();
        accountList.stream().forEach(loginAccount -> loginService.doLoginDmp(loginAccount.getUserName(), loginAccount.getPassword()));
        return jsonObject;
    }
}
