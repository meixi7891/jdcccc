package com.netease.nbot.controller;

import com.alibaba.fastjson.JSONObject;
import com.netease.nbot.entity.*;
import com.netease.nbot.service.impl.DmpService;
import com.netease.nbot.service.impl.HttpCrawlerService;
import com.netease.nbot.service.impl.WebDriverCrawlerService;
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

    @RequestMapping("/search")
    @ResponseBody
    public JSONObject search(@RequestParam(value = "source", required = false, defaultValue = "") String source,
                             @RequestParam(value = "type", required = false, defaultValue = "") String type,
                             @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                             @RequestParam(value = "sku", required = false, defaultValue = "") String sku,
                             @RequestParam(value = "sortType", required = false, defaultValue = "") String sortType,
                             @RequestParam(value = "shop", required = false, defaultValue = "") String shop) {

        SearchRequest request = new SearchRequest(type, source, keyword, sku, sortType, shop);
        List<SearchResult> resultList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject();
        try {
            resultList = httpCrawlerService.search(request);
            jsonObject.put("code", 0);
            jsonObject.put("message", "抓取成功");
        } catch (Exception e) {
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
            @RequestParam(value = "shop", required = false, defaultValue = "") String shop) {
        JSONObject jsonObject = new JSONObject();
        SearchRequest request = new SearchRequest(type, "PC", keyword, sku, sortType, shop);
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
        DmpResult dmpResult = dmpService.crawl(dmpRequest);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", dmpResult);
        return jsonObject;
    }
}
