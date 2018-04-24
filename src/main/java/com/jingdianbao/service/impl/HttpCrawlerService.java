package com.jingdianbao.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.*;
import com.jingdianbao.http.HttpClientFactory;
import com.jingdianbao.service.CrawlerService;
import com.jingdianbao.util.CookieTool;
import com.jingdianbao.util.HttpUtil;
import com.jingdianbao.webdriver.WebDriverBuilder;
import com.jingdianbao.webdriver.WebDriverActionDelegate;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HttpCrawlerService implements CrawlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCrawlerService.class);

    @Autowired
    private WebDriverBuilder webDriverBuilder;

    @Autowired
    private WebDriverActionDelegate webDriverActionDelegate;

    @Value("${webdriver.screenShot.path}")
    private String PREFIX_FILE_PATH;
    @Autowired
    private ProxyService proxyService;
    @Autowired
    private CookieTool cookieTool;

    private RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
            .setSocketTimeout(5000).build();

    @Override
    public List<SearchResult> search(SearchRequest request) throws Exception {
        if (request.getSource().equals("PC")) {
            return searchPC(request);
                       } else if (request.getSource().equals("H5")) {
            return searchH5(request);
        } else if (request.getSource().equals("app")) {
            return searchApp(request);
        } else if (request.getSource().equals("wqs")) {
            return searchWqs(request);
        }
        return searchPC(request);
    }

    private List<SearchResult> searchPC(SearchRequest request) {
        if (request.getType().equals("shop") || request.getType().equals("goods")) {
            return searchGoods(request);
        } else {
            return searchCategory(request);
        }
    }

    private List<SearchResult> searchGoods(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            String pvid = uuid();
            String keyword = URLEncoder.encode(request.getKeyword(), "utf-8");
            String refer = "https://search.jd.com/Search?keyword=" + keyword + "&enc=utf-8&pvid=" + pvid;
            String url = "https://search.jd.com/s_new.php?keyword=" + keyword + "&enc=utf-8&qrst=1&rt=1&stop=1&vt=2&scrolling=y&click=0";
            int s = 1;
            if (request.getSortType() != null && !"0".equals(request.getSortType())) {
                refer = refer + "&psort=" + request.getSortType();
                url = url + "&psort=" + request.getSortType();
            }
            if (!request.getPriceLow().isEmpty() || !request.getPriceHigh().isEmpty()) {
                if (!request.getPriceLow().isEmpty() && request.getPriceHigh().isEmpty()) {
                    refer = refer + "&ev=exprice_" + request.getPriceLow() + "gt%5E";
                    url = url + "&ev=exprice_" + request.getPriceLow() + "gt%5E";
                } else if (request.getPriceLow().isEmpty() && !request.getPriceHigh().isEmpty()) {
                    refer = refer + "&ev=exprice_0-" + request.getPriceHigh() + "%5E";
                    url = url + "&ev=exprice_0-" + request.getPriceHigh() + "%5E";
                } else {
                    refer = refer + "&ev=exprice_" + request.getPriceLow() + "-" + request.getPriceHigh() + "%5E";
                    url = url + "&ev=exprice_" + request.getPriceLow() + "-" + request.getPriceHigh() + "%5E";
                }
            }
            String sku = request.getSku();
            String pa = "<li\\s+class=\"gl-item(\\s+gl-item-presell)?\"\\s+data-sku=\"\\d+\"\\s+data-spu=\"\\d+\" data-pid=\"\\d+\">|<li\\s+data-sku=\"\\d+\"\\s+class=\"gl-item(\\s+gl-item-presell)?\">";
            Pattern p = Pattern.compile(pa);
            Pattern skuPattern = Pattern.compile("data-sku=\"(\\d+)\"");
            Pattern pidPattern = Pattern.compile("data-pid=\"(\\d+)\"");
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                    .setSocketTimeout(5000).build();
            CookieStore cookieStore = new BasicCookieStore();
            cookieTool.loadCookie("search", "PC", cookieStore);
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
            int pageTotal = 0;
            HttpGet httpGet = new HttpGet(refer);
            httpGet.addHeader("Referer", refer);
            httpGet.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            httpGet.addHeader("Host", "search.jd.com");
            httpGet.addHeader("Upgrade-Insecure-Requests", "1");
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String result = sb.toString();
            Document document = Jsoup.parse(result);
            String pageStr = document.select("div[id=J_topPage]>span>i").text();
            pageTotal = Integer.parseInt(pageStr);
            int rank = 0;
            int position = 0;
            Elements elements = document.select("li[class~=gl-item]");
            sb = new StringBuilder();
            //第一页第一屏
            for (Element e : elements) {
                position++;
                sb.append(e.attr("data-pid")).append(",");
                if (e.select("span[class=p-promo-flag]").isEmpty()) {
                    s++;
                    rank++;
                    if ((request.getType().equals("goods") && sku.equals(e.attr("data-sku"))) || (request.getType().equals("shop") && !e.select("a[class~=curr-shop]").isEmpty() && e.select("a[class~=curr-shop]").text().contains(request.getShop()))) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setPage(1);
                        searchResult.setPos(position);
                        searchResult.setRank(rank);
                        searchResult.setType(request.getType());
                        searchResult.setKeyword(request.getKeyword());
                        searchResult.setSku(e.attr("data-sku"));
                        if (request.getSource().equals("PC")) {
                            searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                        } else {
                            searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                        }
                        Elements e1 = e.select("div[class=p-img] img");
                        if (e1.attr("src") != null && !e1.attr("src").isEmpty()) {
                            searchResult.setImg(e1.attr("src"));
                        } else {
                            searchResult.setImg(e1.attr("data-lazy-img"));
                        }
                        resultList.add(searchResult);
                        if (request.getType().equals("goods") || resultList.size() >= 3) {
                            resultList.forEach(sr -> {
                                crawlSkuDetail(sr);
                                crawlComment(sr);
                                crawlFoldComments(sr);
                            });
                            return resultList;
                        }
                    }
                }
            }
            //第一页第二屏
            httpClient = HttpClientFactory.getHttpClient();
            Map<String, String> header = new HashMap<>();
            header.put("Cookie", cookieTool.getCookieStr(cookieStore));
            header.put("Host", "search.jd.com");
            header.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            String urlStr = url + "&page=" + 2 + "&s=" + s;
            if (!(sb.length() == 0)) {
                urlStr = urlStr + "&show_items=" + sb.substring(0, sb.length() - 1);
            }
            result = get(urlStr, refer, requestConfig, httpClient, header);
            document = Jsoup.parse(result);
            sb = new StringBuilder();
            elements = document.select("li[class~=gl-item]");
            for (Element e : elements) {
                position++;
                sb.append(e.attr("data-pid")).append(",");
                if (e.select("span[class=p-promo-flag]").isEmpty()) {
                    s++;
                    rank++;
                    if ((request.getType().equals("goods") && sku.equals(e.attr("data-sku"))) || (request.getType().equals("shop") && !e.select("a[class~=curr-shop]").isEmpty() && e.select("a[class~=curr-shop]").text().contains(request.getShop()))) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setPage(1);
                        searchResult.setPos(position);
                        searchResult.setRank(rank);
                        searchResult.setType(request.getType());
                        searchResult.setKeyword(request.getKeyword());
                        searchResult.setSku(e.attr("data-sku"));
                        if (request.getSource().equals("PC")) {
                            searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                        } else {
                            searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                        }
                        Elements e1 = e.select("div[class=p-img] img");
                        if (e1.attr("src") != null && !e1.attr("src").isEmpty()) {
                            searchResult.setImg(e1.attr("src"));
                        } else {
                            searchResult.setImg(e1.attr("data-lazy-img"));
                        }
                        resultList.add(searchResult);
                        if (request.getType().equals("goods") || resultList.size() >= 3) {
                            resultList.forEach(sr -> {
                                crawlSkuDetail(sr);
                                crawlComment(sr);
                                crawlFoldComments(sr);
                            });
                            return resultList;
                        }
                    }
                }
            }
            //第二页到第n页
            out:
            for (int i = 1; i < pageTotal; i++) {
                int page = i;
                position = 0;
                for (int j = 1; j <= 2; j++) {
                    httpClient = HttpClientFactory.getHttpClient();
                    urlStr = url + "&page=" + (i * 2 + j) + "&s=" + s;
                    if (!(sb.length() == 0)) {
                        urlStr = urlStr + "&show_items=" + sb.substring(0, sb.length() - 1);
                    }
                    result = get(urlStr, refer, requestConfig, httpClient, header);
                    document = Jsoup.parse(result);
                    sb = new StringBuilder();
                    elements = document.select("li[class~=gl-item]");
                    for (Element e : elements) {
                        position++;
                        sb.append(e.attr("data-pid")).append(",");
                        if (e.select("span[class=p-promo-flag]").isEmpty()) {
                            s++;
                            rank++;
                            if ((request.getType().equals("goods") && sku.equals(e.attr("data-sku"))) || (request.getType().equals("shop") && !e.select("a[class~=curr-shop]").isEmpty() && e.select("a[class~=curr-shop]").text().contains(request.getShop()))) {
                                SearchResult searchResult = new SearchResult();
                                searchResult.setPage(page + 1);
                                searchResult.setPos(position);
                                searchResult.setRank(rank);
                                searchResult.setType(request.getType());
                                searchResult.setKeyword(request.getKeyword());
                                searchResult.setSku(e.attr("data-sku"));
                                if (request.getSource().equals("PC")) {
                                    searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                                } else {
                                    searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                }
                                Elements e1 = e.select("div[class=p-img] img");
                                if (e1.attr("src") != null && !e1.attr("src").isEmpty()) {
                                    searchResult.setImg(e1.attr("src"));
                                } else {
                                    searchResult.setImg(e1.attr("data-lazy-img"));
                                }
                                resultList.add(searchResult);
                                if (request.getType().equals("goods") || resultList.size() >= 3 || i == 50) {
                                    resultList.forEach(sr -> {
                                        crawlSkuDetail(sr);
                                        crawlComment(sr);
                                        crawlFoldComments(sr);
                                    });
                                    return resultList;
                                }
                            }
                        }
                    }
//                    String[] ss = result.split(pa, -1);
//                    Set<Integer> adIndexSet = new HashSet<>();
//                    for (int m = 0; m < ss.length; m++) {
//                        if (ss[m].isEmpty()) {
//                            continue;
//                        }
//                        System.out.println(ss[m]);
//                        if (ss[m].contains("广告</span>")) {
//                            adIndexSet.add(m);
//                        } else {
//                            s++;
//                        }
//                    }
//                    int index = 0;
//                    Matcher matcher = p.matcher(result);
//                    while (matcher.find()) {
//                        index++;
//                        position++;
//                        if (!adIndexSet.contains(index)) {
//                            rank++;
//                        }
//                        String skuLine = matcher.group();
//                        Matcher pidMatcher = pidPattern.matcher(skuLine);
//                        if (pidMatcher.find()) {
//                            String currentPid = pidMatcher.group(1);
//                            sb.append(currentPid).append(",");
//                        }
//                        if (request.getType().equals("goods") && skuLine.contains("data-sku=\"" + sku + "\"")) {
//                            SearchResult searchResult = new SearchResult();
//                            searchResult.setPage(page + 1);
//                            searchResult.setPos(position);
//                            searchResult.setRank(rank);
//                            searchResult.setType(request.getType());
//                            searchResult.setSku(String.valueOf(sku));
//                            searchResult.setKeyword(request.getKeyword());
//                            if (request.getSource().equals("PC")) {
//                                searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
//                            } else {
//                                searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
//                            }
//                            Elements e1 = e.select("div[class=p-img] img");
//                            if (e1.attr("src") != null && !e1.attr("src").isEmpty()) {
//                                searchResult.setImg(e1.attr("src"));
//                            } else {
//                                searchResult.setImg(e1.attr("data-lazy-img"));
//                            }
//                            resultList.add(searchResult);
//                            if (request.getType().equals("goods")) {
//                                break out;
//                            }
//                        } else if (request.getType().equals("shop")) {
//                            Matcher skuMatcher = skuPattern.matcher(skuLine);
//                            if (skuMatcher.find()) {
//                                String currentSku = skuMatcher.group(1);
//                                Elements e = document.select("li[data-sku=" + currentSku + "] div[class=p-shop]");
//                                if (e.text().contains(request.getKeyword())) {
//                                    SearchResult searchResult = new SearchResult();
//                                    searchResult.setPage(page + 1);
//                                    searchResult.setPos(position);
//                                    searchResult.setRank(rank);
//                                    searchResult.setType(request.getType());
//                                    searchResult.setSku(String.valueOf(currentSku));
//                                    searchResult.setKeyword(request.getKeyword());
//                                    if (request.getSource().equals("PC")) {
//                                        searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
//                                    } else {
//                                        searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
//                                    }
//                                    Elements imageNode = document.select("img[data-sku=" + searchResult.getSku() + "]");
//                                    if (imageNode.attr("src") != null && !imageNode.attr("src").isEmpty()) {
//                                        searchResult.setImg(imageNode.attr("src"));
//                                    } else {
//                                        searchResult.setImg(imageNode.attr("data-lazy-img"));
//                                    }
//                                    resultList.add(searchResult);
//                                    if (resultList.size() >= 10) {
//                                        break out;
//                                    }
//                                }
//                            }
//                        }
                }


//                    Document doc = Jsoup.parse(result);
//                    Elements elements = doc.select("li[class~=gl-item]");
//                    for (Element e : elements) {
//                        position++;
//                        if (e.select("span[class=p-promo-flag]").isEmpty()) {
//                            s++;
//                            rank++;
//                        }
//                        if ((request.getType().equals("goods") && sku.equals(e.attr("data-sku"))) || (request.getType().equals("shop") && !e.select("a[class~=curr-shop]").isEmpty() && e.select("a[class~=curr-shop]").text().contains(sku))) {
//                            SearchResult searchResult = new SearchResult();
//                            searchResult.setPage(page + 1);
//                            searchResult.setPos(position);
//                            searchResult.setRank(rank);
//                            searchResult.setType(request.getType());
//                            searchResult.setSku(e.attr("data-sku"));
//                            resultList.add(searchResult);
//                            if (request.getType().equals("goods")) {
//                                break out;
//                            }
//                        }
//
//                    }
            }

        } catch (IOException e) {
            LOGGER.error("", e);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        resultList.forEach(sr -> {
            crawlSkuDetail(sr);
            crawlComment(sr);
            crawlFoldComments(sr);
        });
        return resultList;
    }

    private void crawlGoodsPage(SearchCursor cursor, StringBuilder sb, String url, String refer, Map<String, String> header, SearchRequest request, List<SearchResult> resultList) {
        try {
            String sku = request.getSku();
            int position = 0;
            for (int i = 1; i <= 2; i++) {
                CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
                String urlStr = url + "&page=" + (cursor.getPage() * 2 + i) + "&s=" + cursor.getRank();
                if (!(sb.length() == 0)) {
                    urlStr = urlStr + "&show_items=" + sb.substring(0, sb.length() - 1);
                }
                String result = get(urlStr, refer, requestConfig, httpClient, header);
                Document document = Jsoup.parse(result);
                sb = new StringBuilder();
                Elements elements = document.select("li[class~=gl-item]");
                for (Element e : elements) {
                    position++;
                    cursor.setPos(position);
                    sb.append(e.attr("data-pid")).append(",");
                    if (e.select("span[class=p-promo-flag]").isEmpty()) {
                        cursor.setRank(cursor.getRank() + 1);
                    }
                    if ((request.getType().equals("goods") && sku.equals(e.attr("data-sku"))) || (request.getType().equals("shop") && !e.select("a[class~=curr-shop]").isEmpty() && e.select("a[class~=curr-shop]").text().contains(sku))) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setPage(cursor.getPage() + 1);
                        searchResult.setPos(position);
                        searchResult.setRank(cursor.getRank());
                        searchResult.setType(request.getType());
                        searchResult.setSku(e.attr("data-sku"));
                        resultList.add(searchResult);
                        if (request.getType().equals("goods") || resultList.size() >= 10) {
                            return;
                        }
                    }
                }

            }

        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private List<SearchResult> searchGoodsCookie(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        ChromeDriver webDriver = webDriverBuilder.getWebDriver();
//        CookieUtil.saveCookie("search", "PC", webDriver);
//        CookieUtil.loadCookie("search", "PC", webDriver);


        try {
            String pvid = uuid();
            String keyword = URLEncoder.encode(request.getKeyword(), "utf-8");
            String refer = "https://search.jd.com/Search?keyword=" + keyword + "&enc=utf-8&pvid=" + pvid;
            webDriver.get(refer);
            cookieTool.saveCookie("search", "PC", webDriver);
            cookieTool.loadCookie("search", "PC", webDriver);
            if (request.getSortType() != null && !"0".equals(request.getSortType())) {
                webDriver.executeScript("SEARCH.sort('" + request.getSortType() + "')");
            }
            String sku = request.getSku();
            int rank = 0;
            WebElement element = webDriver.findElementByXPath("//span[@class='p-skip']/input[@class='input-txt']");
            int elementPosition = element.getLocation().getY() - 100;
            String js = String.format("window.scroll(0, %s)", elementPosition);
            webDriver.executeScript(js);
            sleep(1000);

            int page = 1;
            List<WebElement> elementList = webDriver.findElementsByCssSelector("li[class~=gl-item]");
            for (int i = 0; i < elementList.size(); i++) {
                WebElement e = elementList.get(i);
                if (e.findElements(By.cssSelector("span[class=p-promo-flag]")).isEmpty()) {
                    rank++;
                }
                if ((request.getType().equals("goods") && sku.equals(e.getAttribute("data-sku"))) || (request.getType().equals("shop") && !e.findElements(By.cssSelector("a[class~=curr-shop]")).isEmpty() && e.findElement(By.cssSelector("a[class~=curr-shop]")).getText().contains(request.getShop()))) {
                    SearchResult searchResult = new SearchResult();
                    searchResult.setPage(page);
                    searchResult.setPos(i + 1);
                    searchResult.setRank(rank);
                    searchResult.setType(request.getType());
                    searchResult.setKeyword(request.getKeyword());
                    searchResult.setSku(e.getAttribute("data-sku"));
                    if (request.getSource().equals("PC")) {
                        searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                    } else {
                        searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                    }
                    WebElement imgNode = e.findElement(By.tagName("img"));
                    if (imgNode.getAttribute("src") != null && !imgNode.getAttribute("src").isEmpty()) {
                        searchResult.setImg(imgNode.getAttribute("src"));
                    } else {
                        searchResult.setImg(imgNode.getAttribute("data-lazy-img"));
                    }
                    resultList.add(searchResult);
                    if (request.getType().equals("goods") || resultList.size() >= 10) {
                        break;
                    }
                }
            }

            String pageStr = webDriver.findElementByCssSelector("#J_topPage i").getText();
            int pageTotal = Integer.parseInt(pageStr);

            out:
            for (page = 2; page <= pageTotal; page++) {
                webDriver.findElementByXPath("//a[@class='pn-next']").click();
                sleep(1000);
                webDriver.executeScript(js);
                sleep(3000);
                elementList = webDriver.findElementsByCssSelector("li[class~=gl-item]");
                for (int i = 0; i < elementList.size(); i++) {
                    WebElement e = elementList.get(i);
                    if (e.findElements(By.cssSelector("span[class=p-promo-flag]")).isEmpty()) {
                        rank++;
                    }
                    if ((request.getType().equals("goods") && sku.equals(e.getAttribute("data-sku"))) || (request.getType().equals("shop") && !e.findElements(By.cssSelector("a[class~=curr-shop]")).isEmpty() && e.findElement(By.cssSelector("a[class~=curr-shop]")).getText().contains(request.getShop()))) {
                        SearchResult searchResult = new SearchResult();
                        searchResult.setPage(page);
                        searchResult.setPos(i + 1);
                        searchResult.setRank(rank);
                        searchResult.setType(request.getType());
                        searchResult.setKeyword(request.getKeyword());
                        searchResult.setSku(e.getAttribute("data-sku"));
                        if (request.getSource().equals("PC")) {
                            searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                        } else {
                            searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                        }
                        WebElement imgNode = e.findElement(By.tagName("img"));
                        if (imgNode.getAttribute("src") != null && !imgNode.getAttribute("src").isEmpty()) {
                            searchResult.setImg(imgNode.getAttribute("src"));
                        } else {
                            searchResult.setImg(imgNode.getAttribute("data-lazy-img"));
                        }
                        resultList.add(searchResult);
                        if (request.getType().equals("goods") || resultList.size() >= 10) {
                            break out;
                        }
                    }
                }
//                CookieUtil.saveCookie("search","PC",webDriver);
                pageTotal = Integer.parseInt(webDriver.findElementByXPath("//span[@class='p-skip']/em/b").getText());
            }
        } catch (IOException e) {
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
            } catch (IOException e1) {

            }
        } catch (Exception e) {
            LOGGER.error("", e);
            webDriverActionDelegate.takeFullScreenShot(webDriver);
            try {
                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
            } catch (IOException e1) {

            }
        } finally {
            webDriverBuilder.returnDriver(webDriver);
        }
        resultList.forEach(searchResult -> {
            crawlSkuDetail(searchResult);
            crawlCommentH5(searchResult);
            crawlFoldComments(searchResult);
        });
        return resultList;
    }


    private List<SearchResult> searchCategory(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        SearchResult searchResult = new SearchResult();
        searchResult.setSku(request.getSku());
        searchResult.setType(request.getType());
        crawlSkuDetail(searchResult);
        crawlCommentH5(searchResult);
        crawlFoldComments(searchResult);
        crawlCategory(request, searchResult);
        if (searchResult.getRank() != 0) {
            resultList.add(searchResult);
        }
        return resultList;
    }

    private void crawlCategory(SearchRequest request, SearchResult searchResult) {
        String sku = searchResult.getSku();
        String url = "https://item.jd.com/" + sku + ".html";
        try {
            Document doc = Jsoup.connect(url).timeout(30000).get();
            String title = doc.select("div[class=sku-name]").text().trim();
            searchResult.setTitle(title);
            String shop = doc.select("div[class=J-hove-wrap EDropdown fr]>div[class=item]>div[class=name]").text();
            searchResult.setShop(shop);
            Elements elements = doc.select("#crumb-wrap>div[class=w]>div[class=crumb fl clearfix] a");
            if (elements.size() > 2) {


                String catUrl = elements.get(2).attr("href");
                Pattern p = Pattern.compile("\\d+,\\d+,\\d+");
                Matcher matcher = p.matcher(catUrl);
                if (matcher.find()) {
                    String cat = matcher.group();
                    int page = 1;
                    int position = 0;
                    int rank = 0;
                    if (request.getSortType() != null) {
                        catUrl = "https://list.jd.com/list.html?cat=" + cat + "&page=" + page + "&sort=" + getCategorySortType(request.getSortType()) + "&trans=1&JL=6_0_0#J_main";
                    } else {
                        catUrl = "https://list.jd.com/list.html?cat=" + cat + "&page=" + page + "&sort=sort_rank_asc&trans=1&JL=6_0_0#J_main";
                    }
                    doc = Jsoup.connect(catUrl).timeout(30000).get();
                    elements = doc.select("li[class~=gl-item]");
                    for (Element e : elements) {
                        position++;
                        if (e.select("span[class=p-promo-flag]").isEmpty()) {
                            rank++;
                            if (sku.equals(e.select("div[data-sku]").attr("data-sku"))) {
                                searchResult.setPage(page);
                                searchResult.setPos(position);
                                searchResult.setRank(rank);
                                searchResult.setSku(sku);
                                searchResult.setKeyword("");
                                if (request.getSource().equals("PC")) {
                                    searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                                } else {
                                    searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                }
                                Element imgNode = e.select("img").first();
                                if (imgNode.attr("src") != null && !imgNode.attr("src").isEmpty()) {
                                    searchResult.setImg(imgNode.attr("src"));
                                } else {
                                    searchResult.setImg(imgNode.attr("data-lazy-img"));
                                }
                                return;
                            }
                        }
                    }
                    int pageTotal = Math.min(Integer.parseInt(doc.select("span[class=p-skip]>em>b").text()), 100);
                    for (int i = 2; i < pageTotal; i++) {
                        position = 0;
                        if (request.getSortType() != null) {
                            catUrl = "https://list.jd.com/list.html?cat=" + cat + "&page=" + i + "&sort=sort_rank_asc&trans=1&JL=6_0_0#J_main";
                        } else {
                            catUrl = "https://list.jd.com/list.html?cat=" + cat + "&page=" + i + "&sort=" + getCategorySortType(request.getSortType()) + "&trans=1&JL=6_0_0#J_main";
                        }
                        doc = Jsoup.connect(catUrl).timeout(30000).get();
                        elements = doc.select("li[class~=gl-item]");
                        for (Element e : elements) {
                            position++;
                            if (e.select("span[class=p-promo-flag]").isEmpty()) {
                                rank++;
                            }
                            if (sku.equals(e.select("div[data-sku]").attr("data-sku"))) {
                                searchResult.setPage(page + 1);
                                searchResult.setPos(position);
                                searchResult.setRank(rank);
                                searchResult.setSku(sku);
                                searchResult.setKeyword("");
                                if (request.getSource().equals("PC")) {
                                    searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                                } else {
                                    searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                }
                                Element imgNode = e.select("img").first();
                                if (imgNode.attr("src") != null && !imgNode.attr("src").isEmpty()) {
                                    searchResult.setImg(imgNode.attr("src"));
                                } else {
                                    searchResult.setImg(imgNode.attr("data-lazy-img"));
                                }
                                return;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private String getCategorySortType(String sortType) {
        switch (sortType) {
            case "1":
                return "sort_dredisprice_desc";
            case "2":
                return "sort_dredisprice_asc";
            case "3":
                return "sort_totalsales15_desc";
            case "4":
                return "sort_commentcount_desc";
            case "5":
                return "sort_winsdate_desc";
            default:
                return "sort_rank_asc";
        }
    }


    public void crawlSkuDetail(final SearchResult searchResult) {
        String sku = searchResult.getSku();
        String url = "https://item.jd.com/" + sku + ".html";
        try {
            Document doc = Jsoup.connect(url).timeout(30000).get();
            String title = doc.select("div[class=sku-name]").text().trim();
            searchResult.setTitle(title);
            String shop = doc.select("div[class=J-hove-wrap EDropdown fr]>div[class=item]>div[class=name]").text();
            if (shop == null || shop.isEmpty()) {
                shop = doc.select("div[class=shopName]").text();
            }
            searchResult.setShop(shop);
            String brand = doc.select("ul[id=parameter-brand]>li>a").text();
            searchResult.setBrand(brand);
            searchResult.setImg(doc.select("img[id=spec-img]").attr("data-origin"));
            Elements elements = doc.select("#crumb-wrap>div[class=w]>div[class=crumb fl clearfix] a");
            if (elements.size() > 2) {
                String firstCategory = elements.get(0).text();
                String secondCategory = elements.get(1).text();
                String thirdCategory = elements.get(2).text();
                Category category = new Category();
                category.setLevel1(firstCategory);
                category.setLevel2(secondCategory);
                category.setLevel3(thirdCategory);
                searchResult.setCategory(category);
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }


    public String crawlSkuPrice(final String sku) {

        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpGet httpGet = new HttpGet("https://p.3.cn/prices/mgets?callback=jQuery2244087&type=1&area=15_1213_3411_52667&pdtk=&pduid=15102037407391075657148&pdpin=meixi7891&pin=meixi7891&pdbp=0&skuIds=J_" + sku + "&ext=11000000&source=item-pc");
        httpGet.addHeader("Referer", "https://item.jd.com/5618804.html");
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
        httpGet.setConfig(requestConfig);
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            Pattern p = Pattern.compile("\\(.*\\);");
            String result = HttpUtil.readResponse(response);
            Matcher matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(");", "");
                JSONArray jsonArray = JSONArray.parseArray(json);
                return jsonArray.getJSONObject(0).getString("p");
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return "";
    }

    private Map<String, String> crawlIds(String sku) {
        Map<String, String> idMap = new HashMap<String, String>();
        String url = "https://item.jd.com/" + sku + ".html";
        try {
            Document doc = Jsoup.connect(url).timeout(30000).get();
            Elements elements = doc.select("#crumb-wrap>div[class=w]>div[class=crumb fl clearfix] a");
            String catId = "";
            Pattern p = Pattern.compile("cat=([\\d+,]+\\d+)");
            if (elements.size() > 2) {
                String catUrl = elements.get(2).attr("href");
                Matcher matcher = p.matcher(catUrl);
                if (matcher.find()) {
                    catId = matcher.group(1);
                }
            }
            String source = doc.toString();
            String venderId = "";
            p = Pattern.compile("venderId:(\\d+),");
            Matcher matcher = p.matcher(source);
            if (matcher.find()) {
                venderId = matcher.group(1);
            }
            String shopId = "";
            p = Pattern.compile("shopId:'(\\d+)',");
            matcher = p.matcher(source);
            if (matcher.find()) {
                shopId = matcher.group(1);
            }
            idMap.put("catId", catId);
            idMap.put("venderId", venderId);
            idMap.put("shopId", shopId);
        } catch (Exception e) {

        }
        return idMap;
    }

    public List<String> crawlSkuAdverts(final String sku, String price) {
        List<String> adverts = new ArrayList<>();
        Map<String, String> idMap = crawlIds(sku);
        try {
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
            HttpGet httpGet = new HttpGet("https://cd.jd.com/promotion/v2?callback=jQuery3149880&skuId=" + sku + "&area=15_1213_3411_52667&shopId=" + idMap.get("shopId") + "&venderId=" + idMap.get("venderId") + "&cat=" + URLEncoder.encode(idMap.get("catId"), "utf-8") + "&isCanUseDQ=isCanUseDQ-1&isCanUseJQ=isCanUseJQ-1&platform=0&orgType=2&jdPrice=" + price + "&_=" + System.currentTimeMillis());
            httpGet.addHeader("Referer", "https://item.jd.com/5618804.html");
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            httpGet.setConfig(requestConfig);
            CookieStore cookieStore = new BasicCookieStore();
            cookieTool.loadCookie("search", "PC", cookieStore);
            httpGet.addHeader("Referer", "https://item.jd.com/5618804.html");
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            httpGet.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String result = HttpUtil.readResponse(response, "gbk");
            Pattern p = Pattern.compile("\\(.*\\)");
            Matcher matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(")", "");
                JSONObject jsonObject = JSONObject.parseObject(json);
                //广告语
                JSONArray ads = jsonObject.getJSONArray("ads");
                for (int i = 0; i < ads.size(); i++) {
                    String ad = ads.getJSONObject(i).getString("ad").replaceAll("(<.*/?>)", "");
                    adverts.add(ad);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return adverts;
    }


    public Promotion crawlSkuPromotion(final String sku, String price) {
        Promotion promotion = new Promotion();
        String url = "https://item.jd.com/" + sku + ".html";
        try {
            Document doc = Jsoup.connect(url).timeout(30000).get();
            Elements elements = doc.select("#crumb-wrap>div[class=w]>div[class=crumb fl clearfix] a");
            String catId = "";
            Pattern p = Pattern.compile("cat=([\\d+,]+\\d+)");
            if (elements.size() > 2) {
                String catUrl = elements.get(2).attr("href");
                Matcher matcher = p.matcher(catUrl);
                if (matcher.find()) {
                    catId = matcher.group(1);
                }
            }
            String source = doc.toString();
            String venderId = "";
            p = Pattern.compile("venderId:(\\d+),");
            Matcher matcher = p.matcher(source);
            if (matcher.find()) {
                venderId = matcher.group(1);
            }
            String shopId = "";
            p = Pattern.compile("shopId:'(\\d+)',");
            matcher = p.matcher(source);
            if (matcher.find()) {
                shopId = matcher.group(1);
            }
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
            HttpGet httpGet = new HttpGet("https://cd.jd.com/promotion/v2?callback=jQuery3149880&skuId=" + sku + "&area=15_1213_3411_52667&shopId=" + shopId + "&venderId=" + venderId + "&cat=" + URLEncoder.encode(catId, "utf-8") + "&isCanUseDQ=isCanUseDQ-1&isCanUseJQ=isCanUseJQ-1&platform=0&orgType=2&jdPrice=" + price + "&_=" + System.currentTimeMillis());
            CookieStore cookieStore = new BasicCookieStore();
            cookieTool.loadCookie("search", "PC", cookieStore);
            httpGet.addHeader("Referer", "https://item.jd.com/5618804.html");
            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
            httpGet.addHeader("Cookie", cookieTool.getCookieStr(cookieStore));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String result = HttpUtil.readResponse(response, "gbk");
            p = Pattern.compile("\\(.*\\)");
            matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(")", "");
                JSONObject jsonObject = JSONObject.parseObject(json);
                //促销
                JSONObject prom = jsonObject.getJSONObject("prom");
                JSONArray tags = prom.getJSONArray("tags");
                for (int i = 0; i < tags.size(); i++) {
                    String name = tags.getJSONObject(i).getString("name");
                    //赠品
                    if ("赠品".equals(name)) {
                        JSONArray gifts = tags.getJSONObject(i).getJSONArray("gifts");
                        for (int j = 0; j < gifts.size(); j++) {
                            Gift gift = new Gift();
                            gift.setName(gifts.getJSONObject(j).getString("nm"));
                            gift.setNumber(gifts.getJSONObject(j).getIntValue("num"));
                            gift.setImg("http://img13.360buyimg.com/n1/s25x25_" + gifts.getJSONObject(j).getString("mp"));
                            promotion.getGiftList().add(gift);
                        }
                    } else {
                        PromotionItem promotionItem = new PromotionItem();
                        promotionItem.setName(name);
                        promotionItem.setContent(tags.getJSONObject(i).getString("content"));
                        promotion.getPromotionItemList().add(promotionItem);
                    }
                }
                JSONArray pickOneTag = prom.getJSONArray("pickOneTag");
                for (int i = 0; i < pickOneTag.size(); i++) {
                    PromotionItem promotionItem = new PromotionItem();
                    promotionItem.setName(pickOneTag.getJSONObject(i).getString("name"));
                    promotionItem.setContent(pickOneTag.getJSONObject(i).getString("content"));
                    promotion.getPromotionItemList().add(promotionItem);
                }
                //满额返券
                JSONArray quan = jsonObject.getJSONArray("quan");
                if (quan != null && !quan.isEmpty()) {
                    PromotionItem promotionItem = new PromotionItem();
                    promotionItem.setName("满额返券");
                    promotionItem.setContent(quan.getJSONObject(0).getString("title"));
                    promotion.getPromotionItemList().add(promotionItem);
                }
                //优惠券
                JSONArray skuCoupons = jsonObject.getJSONArray("skuCoupon");
                for (int i = 0; i < skuCoupons.size(); i++) {
                    JSONObject skuCoupon = skuCoupons.getJSONObject(i);
                    if (skuCoupon.getIntValue("couponKind") == 3) {
                        promotion.getCoupons().add(skuCoupon.getString("allDesc"));
                    } else if (skuCoupon.getIntValue("couponKind") == 2) {
                        promotion.getCoupons().add("满" + skuCoupon.getString("quota") + "减" + skuCoupon.getString("trueDiscount"));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return promotion;
    }


    private void crawlComment(final SearchResult searchResult) {
        crawlCommentH5(searchResult);
//        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
//        String url = "https://sclub.jd.com/comment/productPageComments.action?callback=fetchJSON_comment98vv225&productId=" + searchResult.getSku() + "&score=0&sortType=5&page=0&pageSize=10&isShadowSku=0&rid=0&fold=2";
//        try {
//            HttpGet httpGet = new HttpGet(url);
//            httpGet.addHeader("Referer", "https://item.jd.com/" + searchResult.getSku() + ".html");
//            httpGet.setConfig(requestConfig);
//            System.out.println(url);
//            CloseableHttpResponse response = httpClient.execute(httpGet);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//            reader.close();
//            String result = sb.toString();
//            System.out.println(result);
//            Pattern p = Pattern.compile("\\(.*\\);");
//            Matcher matcher = p.matcher(result);
//            if (matcher.find()) {
//                String json = matcher.group().replace("(", "").replace(");", "");
//                JSONObject jsonObject = JSONObject.parseObject(json);
//                int commentCount = jsonObject.getJSONObject("productCommentSummary").getIntValue("commentCount");
//                searchResult.setComment(commentCount);
//            }
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }
    }

    private void crawlCommentH5(final SearchResult searchResult) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        BufferedReader reader = null;
        try {
            HttpPost httpPost = new HttpPost("https://item.m.jd.com/newComments/newCommentsDetail.json");
            httpPost.addHeader("Referer", "https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
            httpPost.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
            httpPost.addHeader("cookie", "abtest=20180228202800418_35; subAbTest=20180228202800418_63; mobilev=html5; mba_muid=15102037407391075657148; M_Identification=3dbf82145dafa232_7e49c7a6168021190e99155b7a54294b; M_Identification_abtest=20180228203521392_79417497; user-key=4f824d2f-c787-4c0a-a0fe-47bbc16ffd61; __jdu=15102037407391075657148; areaId=15; cn=0; ipLocation=%u6D59%u6C5F; ipLoc-djd=15-1213-3411-52667; _jrda=2; PCSYCityID=1213; m_uuid_new=7581434D080F0F9CACB87FDAB8802090; mhome=1; mt_xid=V2_52007VwMWUlxbU1gZTBhaB28DE1RZX1ZcH0wQbAFkURIBDw8CRkhLSlQZYgMUUUFRUlkcVRBfVWcBGgFcX1BfSHkaXQVuHxNQQVhaSx9OElgNbAASYl9oUmofTBFcAGUFE1VtWFFZHQ%3D%3D; TrackID=1WO7I-jO3o5eG2aje7Biq7Dqyd4kOT0HgfKR7eAj9jrGa86tG7DW0Lp425GSS7fyYmepwQIEoe51DTEHhIxAxI2l-60YrURdfh7KxpC4tt6s; pinId=tv7lg4K603eYEzFXmTd7PA; pin=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; unick=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; _tp=YNgb%2BpVBeuPdmEIO5nCqm4Wjzdm1b2MJ72aMs%2BZnuBD3RmRRkUBZ6bgSCC8Vnd5Q; _pst=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; __jda=122270672.15102037407391075657148.1510203741.1521601290.1521614989.40; __jdc=122270672; __jdv=122270672|direct|-|none|-|1521614989388; 3AB9D23F7A4B3C9B=6J3PSQXXMA7QOEN4FBQJM5OJ5KRJULMKFO3UTQJXCVKAWZD7O6SHBBE2CWRO56FA4IVYK74R2YOKB4GNQZEPMTWGEU; sid=e32c30c27c268986e1bf3e9388314352; USER_FLAG_CHECK=2dc68f02e6c15b183027396c0cc8c798; autoOpenApp_downCloseDate_auto=1521616324011_21600000; warehistory=\"2600242,25568515204,20099469725,5001175,25868561553,14143954650,25797029156,12141800119,5716985,5789585,5495676,11229407797,\"; __jdb=122270672.7.15102037407391075657148|40.1521614989; mba_sid=15216163238624899707965548601.2");
            httpPost.setConfig(requestConfig);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("wareId", searchResult.getSku()));
            nvps.add(new BasicNameValuePair("offset", "1"));
            nvps.add(new BasicNameValuePair("num", "10"));
            nvps.add(new BasicNameValuePair("checkParam", "LUIPPTP"));
            nvps.add(new BasicNameValuePair("isUseMobile", "true"));
            nvps.add(new BasicNameValuePair("type", "0"));
            nvps.add(new BasicNameValuePair("isCurrentSku", "false"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
//            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("wareId", new StringBody(searchResult.getSku(), ContentType.TEXT_PLAIN))
//                    .addPart("offset", new StringBody("1", ContentType.TEXT_PLAIN))
//                    .addPart("num", new StringBody("10", ContentType.TEXT_PLAIN))
//                    .addPart("checkParam", new StringBody("LUIPPTP", ContentType.TEXT_PLAIN))
//                    .addPart("isUseMobile", new StringBody("true", ContentType.TEXT_PLAIN))
//                    .addPart("type", new StringBody("0", ContentType.TEXT_PLAIN))
//                    .addPart("isCurrentSku", new StringBody("false", ContentType.TEXT_PLAIN)).build();
//            httpPost.setEntity(reqEntity);

            CloseableHttpResponse response = httpClient.execute(httpPost);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonObject = JSONObject.parseObject(sb.toString());
            int commentCount = jsonObject.getJSONObject("wareDetailComment").getIntValue("allCnt");
            searchResult.setComment(commentCount);
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    public CommentResult crawlCommentH5(final String sku) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        BufferedReader reader = null;
        CommentResult commentResult = new CommentResult();
        commentResult.setSku(sku);
        try {
            HttpPost httpPost = new HttpPost("https://item.m.jd.com/newComments/newCommentsDetail.json");
            httpPost.addHeader("Referer", "https://item.m.jd.com/product/" + sku + ".html");
            httpPost.addHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
            httpPost.addHeader("cookie", "abtest=20180228202800418_35; subAbTest=20180228202800418_63; mobilev=html5; mba_muid=15102037407391075657148; M_Identification=3dbf82145dafa232_7e49c7a6168021190e99155b7a54294b; M_Identification_abtest=20180228203521392_79417497; user-key=4f824d2f-c787-4c0a-a0fe-47bbc16ffd61; __jdu=15102037407391075657148; areaId=15; cn=0; ipLocation=%u6D59%u6C5F; ipLoc-djd=15-1213-3411-52667; _jrda=2; PCSYCityID=1213; m_uuid_new=7581434D080F0F9CACB87FDAB8802090; mhome=1; mt_xid=V2_52007VwMWUlxbU1gZTBhaB28DE1RZX1ZcH0wQbAFkURIBDw8CRkhLSlQZYgMUUUFRUlkcVRBfVWcBGgFcX1BfSHkaXQVuHxNQQVhaSx9OElgNbAASYl9oUmofTBFcAGUFE1VtWFFZHQ%3D%3D; TrackID=1WO7I-jO3o5eG2aje7Biq7Dqyd4kOT0HgfKR7eAj9jrGa86tG7DW0Lp425GSS7fyYmepwQIEoe51DTEHhIxAxI2l-60YrURdfh7KxpC4tt6s; pinId=tv7lg4K603eYEzFXmTd7PA; pin=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; unick=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; _tp=YNgb%2BpVBeuPdmEIO5nCqm4Wjzdm1b2MJ72aMs%2BZnuBD3RmRRkUBZ6bgSCC8Vnd5Q; _pst=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; __jda=122270672.15102037407391075657148.1510203741.1521601290.1521614989.40; __jdc=122270672; __jdv=122270672|direct|-|none|-|1521614989388; 3AB9D23F7A4B3C9B=6J3PSQXXMA7QOEN4FBQJM5OJ5KRJULMKFO3UTQJXCVKAWZD7O6SHBBE2CWRO56FA4IVYK74R2YOKB4GNQZEPMTWGEU; sid=e32c30c27c268986e1bf3e9388314352; USER_FLAG_CHECK=2dc68f02e6c15b183027396c0cc8c798; autoOpenApp_downCloseDate_auto=1521616324011_21600000; warehistory=\"2600242,25568515204,20099469725,5001175,25868561553,14143954650,25797029156,12141800119,5716985,5789585,5495676,11229407797,\"; __jdb=122270672.7.15102037407391075657148|40.1521614989; mba_sid=15216163238624899707965548601.2");
            httpPost.setConfig(requestConfig);
            List<NameValuePair> nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("wareId", sku));
            nvps.add(new BasicNameValuePair("offset", "1"));
            nvps.add(new BasicNameValuePair("num", "10"));
            nvps.add(new BasicNameValuePair("checkParam", "LUIPPTP"));
            nvps.add(new BasicNameValuePair("isUseMobile", "true"));
            nvps.add(new BasicNameValuePair("type", "0"));
            nvps.add(new BasicNameValuePair("isCurrentSku", "false"));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jsonObject = JSONObject.parseObject(sb.toString());
            int commentCount = jsonObject.getJSONObject("wareDetailComment").getIntValue("allCnt");
            commentResult.setComment(commentCount);
            commentResult.setGoodComment(jsonObject.getJSONObject("wareDetailComment").getIntValue("goodCnt"));
            commentResult.setNormalComment(jsonObject.getJSONObject("wareDetailComment").getIntValue("normalCnt"));
            commentResult.setBadComment(jsonObject.getJSONObject("wareDetailComment").getIntValue("badCnt"));
            commentResult.setPicComment(jsonObject.getJSONObject("wareDetailComment").getIntValue("pictureCnt"));
            commentResult.setDiscard(crawlFoldComments(sku));
            SearchResult searchResult = new SearchResult();
            searchResult.setSku(sku);
            crawlSkuDetail(searchResult);
            commentResult.setTitle(searchResult.getTitle());
            commentResult.setImg(searchResult.getImg());
            commentResult.setUrl("https://item.jd.com/" + sku + ".html");

        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        return commentResult;
    }

    private void crawlFoldComments(final SearchResult searchResult) {
        searchResult.setDiscard(crawlFoldComments(searchResult.getSku()));
    }

    private int crawlFoldComments(String sku) {
        int discard = 0;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                .setSocketTimeout(5000).build();
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        String url = "https://club.jd.com/comment/getProductPageFoldComments.action?callback=jQuery1552332&productId=" + sku + "&score=0&sortType=5&page=0&pageSize=5&_=1521114318952";
        BufferedReader reader = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Referer", "https://item.jd.com/" + sku + ".html");
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String result = sb.toString();
            System.out.println(result);
            Pattern p = Pattern.compile("\\(.*\\);");
            Matcher matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(");", "");
                JSONObject jsonObject = JSONObject.parseObject(json);
                int page = jsonObject.getIntValue("maxPage");
                if (page <= 1) {
                    discard = jsonObject.getJSONArray("comments").size();
                } else {
                    url = "https://club.jd.com/comment/getProductPageFoldComments.action?callback=jQuery1552332&productId=" + sku + "&score=0&sortType=5&page=" + (page - 1) + "&pageSize=5&_=1521114318952";
                    httpClient = HttpClientFactory.getHttpClient();
                    httpGet = new HttpGet(url);
                    httpGet.addHeader("Referer", "https://item.jd.com/" + sku + ".html");
                    httpGet.setConfig(requestConfig);
                    response = httpClient.execute(httpGet);
                    reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
                    sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    result = sb.toString();
                    matcher = p.matcher(result);
                    if (matcher.find()) {
                        json = matcher.group().replace("(", "").replace(");", "");
                        jsonObject = JSONObject.parseObject(json);
                        discard = (page - 1) * 5 + jsonObject.getJSONArray("comments").size();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        return discard;
    }


    private String get(String url, String refer, RequestConfig requestConfig, CloseableHttpClient httpClient) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Referer", refer);
        httpGet.addHeader("Host", "search.jd.com");
        httpGet.setConfig(requestConfig);
        BufferedReader reader = null;
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
    }

    private String get(String url, String refer, RequestConfig requestConfig, CloseableHttpClient httpClient, Map<String, String> header) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Referer", refer);
        httpGet.addHeader("Host", "search.jd.com");
        for (Map.Entry<String, String> entry : header.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        httpGet.setConfig(requestConfig);
        BufferedReader reader = null;
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            response.getAllHeaders();
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private List<SearchResult> searchH5(SearchRequest request) {
        if (request.getType().equals("goods")) {
            return searchGoodsH5(request);
        } else if (request.getType().equals("shop")) {
            return searchShopH5(request);
        } else {
            return searchCategoryH5(request);
        }
    }

    private List<SearchResult> searchGoodsH5(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        int totalPage = 100;
        try {
            int adCount = 0;
            out:
            for (int i = 1; i <= Math.min(100, totalPage); i++) {
                JSONObject jsonObject = JSONObject.parseObject(searchH5(request.getKeyword(), i, request));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                int total = result.getJSONObject("wareList").getIntValue("wareCount");
                totalPage = (total + 9) / 10;
                if (array != null) {
                    for (int j = 0; j < array.size(); j++) {
                        JSONObject record = array.getJSONObject(j);
                        if (record.getString("catid") == null) {
                            adCount++;
                        } else {
                            if (record != null && request.getSku().equals(record.getString("wareId"))) {
                                SearchResult searchResult = new SearchResult();
                                searchResult.setPage(i);
                                searchResult.setPos(j + 1);
                                searchResult.setRank((i - 1) * 10 + j + 1 - adCount);
                                searchResult.setType(request.getType());
                                searchResult.setKeyword(request.getKeyword());
                                searchResult.setSku(record.getString("wareId"));
                                searchResult.setComment(record.getIntValue("totalCount"));
                                searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                searchResult.setImg(record.getString("imageurl"));
                                crawlSkuDetail(searchResult);
                                crawlFoldComments(searchResult);
                                resultList.add(searchResult);
                                break out;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }


    private List<SearchResult> searchShopH5(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        try {
            int adCount = 0;
            int totalPage = 50;
            out:
            for (int i = 1; i <= Math.min(50, totalPage); i++) {
                JSONObject jsonObject = JSONObject.parseObject(searchH5(request.getKeyword(), i, request));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                int total = result.getJSONObject("wareList").getIntValue("wareCount");
                totalPage = (total + 9) / 10;
                for (int j = 0; j < array.size(); j++) {
                    JSONObject record = array.getJSONObject(j);
                    if (record.getString("catid") == null) {
                        adCount++;
                    } else {
                        if (record != null) {
                            SearchResult searchResult = new SearchResult();
                            searchResult.setSku(record.getString("wareId"));
                            crawlSkuDetail(searchResult);
                            if (searchResult.getShop().contains(request.getShop())) {
                                searchResult.setPage(i);
                                searchResult.setPos(j + 1);
                                searchResult.setRank((i - 1) * 10 + j + 1 - adCount);
                                searchResult.setType(request.getType());
                                if (Pattern.matches("(\\d+-\\d+-\\d+)", request.getKeyword())) {
                                    searchResult.setKeyword("");
                                } else {
                                    searchResult.setKeyword(request.getKeyword());
                                }
                                searchResult.setKeyword(request.getKeyword());
                                searchResult.setComment(record.getIntValue("totalCount"));
                                searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                searchResult.setImg(record.getString("imageurl"));
                                crawlSkuDetail(searchResult);
                                crawlFoldComments(searchResult);
                                resultList.add(searchResult);
                                if (resultList.size() >= 3) {
                                    break out;
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }

//    private String searchCateH5(String keyword, int page) {
//        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
//        HttpPost post = new HttpPost("https://so.m.jd.com/ware/searchList.action");
//        List<NameValuePair> nvps = new ArrayList<>();
//        nvps.add(new BasicNameValuePair("_format_", "json"));
//        nvps.add(new BasicNameValuePair("page", String.valueOf(page)));
//        nvps.add(new BasicNameValuePair("keyword", keyword));
//        try {
//            post.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
//            post.setHeader("referer","https://so.m.jd.com/ware/search.action?keyword=%E8%BF%9E%E8%A1%A3%E8%A3%99%E7%A7%8B&PTAG=137886.1.1113&searchFrom=category");
//            post.setHeader("cookie","JAMCookie=true; pinId=g-Husv2FDvHXgZHD7j7zXA; abtest=20180228202800418_35; subAbTest=20180228202800418_63; mobilev=html5; mba_muid=15102037407391075657148; M_Identification=3dbf82145dafa232_7e49c7a6168021190e99155b7a54294b; M_Identification_abtest=20180228203521392_79417497; user-key=4f824d2f-c787-4c0a-a0fe-47bbc16ffd61; __jdu=15102037407391075657148; pin=meixi7891; unick=meixi7891; _tp=WUqTsDff7JH0MbcWiDvFUA%3D%3D; _pst=meixi7891; unpl=V2_ZzNtbUZVSkJ1WxQELEwOAWJTF1kSUxERdQFEUS5ODgNjBEUKclRCFXwUR1ZnGFsUZwEZWEJcQRVFCHZWeR1cB28GF11yZ3MWdThGZHsdXARkAhBbR1ZFF30IR1J%2fHlgDZwYbbXJQcxV1C0RTfRxeNVcFEV9KXkAUfQh2VUsYbFcJ26TrmvLyCaGiyYHzjhEFYwMTXkNVRRB0DkRcexhaAWAHFF1HXnMURQs%3d; __jdv=122270672|ads-union.jd.com|t_335139441_|tuiguang|538f1bcafdc54a45a5c51834dfc756ff-p_788006353|1520314891705; areaId=15; cn=0; ipLocation=%u6D59%u6C5F; ipLoc-djd=15-1213-3411-52667; _jrda=2; TrackID=1rN-EkYJZv4NzhIjY4U0awZxqgsrBLi7NGgIl3G1jV1UwY__eIxsDfvH1iqpX_eSiZ2xTmxMCZjnq0c0ZYoIYlEy2aPXNfdQfpeiA2hcRAyE; PCSYCityID=1213; m_uuid_new=7581434D080F0F9CACB87FDAB8802090; mhome=1; mt_xid=V2_52007VwMWUlxbU1gZTBhaB28DE1RZX1ZcH0wQbAFkURIBDw8CRkhLSlQZYgMUUUFRUlkcVRBfVWcBGgFcX1BfSHkaXQVuHxNQQVhaSx9OElgNbAASYl9oUmofTBFcAGUFE1VtWFFZHQ%3D%3D; autoOpenApp_downCloseDate_auto=1521511670885_21600000; __jda=122270672.15102037407391075657148.1510203741.1521510206.1521515773.35; USER_FLAG_CHECK=d9b7bf739a9bebc7423522fd83a98de3; sid=2ae0f34d154aad84ab0d88a3de0b1c0e; __jdc=122270672; 3AB9D23F7A4B3C9B=Y2YCZF6SJ5YAJCSOUPPMAMEBUZX2M3WOBB45DWQJQFBGKTKFTJ5I7DGSIKZH6CRVK3GFYQAE2J3QKGNL4NZL4UCRBU; warehistory=\"25868561553,14143954650,25797029156,12141800119,20099469725,5716985,5789585,5495676,25568515204,11229407797,\"; __jdb=122270672.43.15102037407391075657148|35.1521515773; mba_sid=15215157733427806727101212234.39; M_Identification=3dbf82145dafa232_7e49c7a6168021190e99155b7a54294b");
//            CloseableHttpResponse response = httpClient.execute(post);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
//            StringBuilder sb = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//            reader.close();
//            return sb.toString();
//        } catch (Exception e) {
//            LOGGER.error("", e);
//        }
//        return "";
//    }

    private String searchH5(String keyword, int page, SearchRequest request) {
        return searchH5(keyword, page, request, null);
    }


    private String searchH5(String keyword, int page, SearchRequest request, String proxy) {
        CloseableHttpClient httpClient = null;
        if (proxy != null) {
            httpClient = HttpClientFactory.getHttpClient(proxy);
        } else {
            httpClient = HttpClientFactory.getHttpClient();
        }
        HttpPost post = new HttpPost("https://so.m.jd.com/ware/searchList.action");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("_format_", "json"));
        nvps.add(new BasicNameValuePair("page", String.valueOf(page)));
        if (Pattern.matches("(\\d+-\\d+-\\d+)", keyword)) {
            String[] ss = keyword.split("-");
            nvps.add(new BasicNameValuePair("c1", ss[0]));
            nvps.add(new BasicNameValuePair("c2", ss[1]));
            nvps.add(new BasicNameValuePair("categoryId", ss[2]));
        } else {
            nvps.add(new BasicNameValuePair("keyword", keyword));
        }
        if (!request.getPriceLow().isEmpty() || !request.getPriceHigh().isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("inputprice", "true");
            if (!request.getPriceLow().isEmpty() && request.getPriceHigh().isEmpty()) {
                jsonObject.put("min", request.getPriceLow());
            } else if (request.getPriceLow().isEmpty() && !request.getPriceHigh().isEmpty()) {
                jsonObject.put("max", request.getPriceHigh());
            } else {
                jsonObject.put("min", request.getPriceLow());
                jsonObject.put("max", request.getPriceHigh());
            }
            jsonArray.add(jsonObject);
            nvps.add(new BasicNameValuePair("price", jsonArray.toJSONString()));
        }
        BufferedReader reader = null;
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(post);
            reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        return "";
    }

    private String searchCategory(String secondCategory) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        HttpGet get = new HttpGet("https://so.m.jd.com/category/all.html?searchFrom=home");
        String catelogyId = "WQ1985";
        try {
            String s = get("https://so.m.jd.com/category/all.html?searchFrom=home", "https://m.jd.com/", requestConfig, httpClient);
            JSONObject jsonObject = JSONObject.parseObject(get("https://so.m.jd.com/category/list.action?_format_=json&catelogyId=" + catelogyId, "https://so.m.jd.com/category/all.html?searchFrom=home", requestConfig, httpClient));
            String value = jsonObject.getString("value");
            JSONObject result = JSONObject.parseObject(value);
            return result.toJSONString();
        } catch (Exception e) {
            LOGGER.error("", e);

        }
        return "";
    }

    private List<SearchResult> searchCategoryH5(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        SearchResult searchResult = new SearchResult();
        searchResult.setSku(request.getSku());
        crawlSkuDetail(searchResult);
        String url = searchCategoryUrl(searchResult.getCategory());
        try {
            Pattern pattern = Pattern.compile("keyword=([\\S]+?)&");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String keyword = URLDecoder.decode(matcher.group(1).trim(), "utf-8");
                searchResult.setKeyword(keyword);
            } else {
                pattern = Pattern.compile("(\\d+-\\d+-\\d+)");
                matcher = pattern.matcher(url);
                if (matcher.find()) {
                    String keyword = matcher.group(1).trim();
                    searchResult.setKeyword(keyword);
                }
            }
            int adCount = 0;
            out:
            for (int i = 1; i <= 100; i++) {
                JSONObject jsonObject = JSONObject.parseObject(searchH5(searchResult.getKeyword(), i, request));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                for (int j = 0; j < array.size(); j++) {
                    JSONObject record = array.getJSONObject(j);
                    if (record.getString("catid") == null) {
                        adCount++;
                    } else {
                        if (record != null && request.getSku().equals(record.getString("wareId"))) {
                            searchResult.setPage(i);
                            searchResult.setPos(j + 1);
                            searchResult.setRank((i - 1) * 10 + j + 1 - adCount);
                            searchResult.setType(request.getType());
                            searchResult.setKeyword(request.getKeyword());
                            searchResult.setComment(record.getIntValue("totalCount"));
                            searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                            searchResult.setImg(record.getString("imageurl"));
                            crawlFoldComments(searchResult);
                            resultList.add(searchResult);
                            break out;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }

    private String searchCategoryUrl(Category category) {
        String url = "https://so.m.jd.com/category/all.html?searchFrom=home";
        BufferedReader reader = null;
        try {
            String cid = "";
            Document doc = Jsoup.connect(url).timeout(30000).get();
            Pattern pattern = Pattern.compile("jsArgs\\['category'\\]\\s+=([\\s\\S]+?)\\s+jsArgs\\['html5_2015'\\]");
            Matcher matcher = pattern.matcher(doc.toString());
            if (matcher.find()) {
                String s = matcher.group(1).trim();
                JSONObject jsonObject = JSON.parseObject(s.substring(0, s.length() - 1));
                JSONArray array = jsonObject.getJSONObject("roorList").getJSONArray("catelogyList");
                for (int i = 0; i < array.size(); i++) {
                    if (array.getJSONObject(i).getString("name").equals("热门推荐")) {
                        continue;
                    }
                    cid = array.getJSONObject(i).getString("cid");
                    if (!cid.isEmpty()) {
                        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
                        HttpGet httpGet = new HttpGet("https://so.m.jd.com/category/list.action?_format_=json&catelogyId=" + cid);
                        httpGet.addHeader("referer", "https://m.jd.com/");
                        httpGet.setConfig(requestConfig);
                        CloseableHttpResponse response = httpClient.execute(httpGet);
                        reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        jsonObject = JSON.parseObject(sb.toString());
                        JSONObject jsonObject1 = jsonObject.getJSONObject("catalogBranch");
                        JSONArray jsonArray = jsonObject1.getJSONArray("data");
                        for (int j = 0; j < jsonArray.size(); j++) {
                            if (jsonArray.getJSONObject(j).getString("name").contains("热卖") || jsonArray.getJSONObject(j).getString("name").contains("热门")) {
                                continue;
                            }
                            JSONArray cateArray = jsonArray.getJSONObject(j).getJSONArray("catelogyList");
                            for (int k = 0; k < cateArray.size(); k++) {
                                if (category.getLevel3().equals(cateArray.getJSONObject(k).getString("name"))) {
                                    return cateArray.getJSONObject(k).getString("action");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("", e);
                }
            }
        }
        return "";
    }


    private List<SearchResult> searchWqs(SearchRequest request) {
        if (request.getType().equals("goods")) {
            return searchGoodsWqs(request);
        } else if (request.getType().equals("shop")) {
            return searchShopWqs(request);
        } else {
            return searchCategoryWqs(request);
        }
    }

    private List<SearchResult> searchGoodsWqs(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        Map<String, String> header = new HashMap<>();
        CookieStore cookieStore = new BasicCookieStore();
        cookieTool.loadCookie("search", "PC", cookieStore);
        header.put("Cookie", cookieTool.getCookieStr(cookieStore));
        int pageSize = 10;
        try {
            CloseableHttpResponse response = HttpUtil.doGet("http://wqsou.jd.com/search/searchn?key=" + URLEncoder.encode(request.getKeyword(), "utf-8") + "&datatype=1&callback=jdSearchResultBkCbB&page=1&pagesize=10&ext_attr=no&brand_col=no&price_col=no&color_col=no&size_col=no&ext_attr_sort=no&merge_sku=yes&multi_suppliers=yes&area_ids=1,72,4137&qp_disable=no&fdesc=%E5%8C%97%E4%BA%AC&t1=1524462928763", header, requestConfig);
            String result = HttpUtil.readResponse(response);
            result = result.replaceAll("^\\w+\\(", "").replaceAll("\\)$", "");
            JSONObject jsonObject = JSON.parseObject(result);
            int total = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONObject("Head").getJSONObject("Summary").getIntValue("ResultCount");
            JSONArray array = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONArray("Paragraph");
            Set<Integer> adPositionSet = new HashSet<>();
            JSONArray adArray = jsonObject.getJSONObject("data").getJSONObject("adpos").getJSONArray("data");
            for (int i = 0; i < adArray.size(); i++) {
                adPositionSet.add(adArray.getJSONObject(i).getIntValue("flow_order"));
            }
            int rank = 0;
            int page = 1;
            int position = 0;
            int rankWithAds = 0;
            for (int i = 0; i < array.size(); i++) {
                rank++;
                position = i + 1;
                JSONObject record = array.getJSONObject(i);
                if (request.getSku().equals(record.getString("wareid"))) {
                    SearchResult searchResult = new SearchResult();
                    for (Integer adPosition : adPositionSet) {
                        if (i + 1 >= adPosition) {
                            position++;
                        }
                    }
                    searchResult.setPage(page);
                    searchResult.setPos(position);
                    searchResult.setRank(rank);
                    searchResult.setType(request.getType());
                    searchResult.setKeyword(request.getKeyword());
                    searchResult.setSku(record.getString("wareid"));
                    searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                    crawlSkuDetail(searchResult);
                    searchResult.setImg("//img14.360buyimg.com/n8/s370x474_" + record.getJSONObject("Content").getString("imageurl"));
                    crawlCommentH5(searchResult);
                    crawlFoldComments(searchResult);
                    resultList.add(searchResult);
                    return resultList;
                }
            }
            rankWithAds = rankWithAds + array.size() + adArray.size();
            int totalPage = (total + pageSize - 1) / pageSize;
            for (int i = 2; i <= Math.min(50, totalPage); i++) {
                response = HttpUtil.doGet("http://wqsou.jd.com/search/searchn?key=" + URLEncoder.encode(request.getKeyword(), "utf-8") + "&datatype=1&callback=jdSearchResultBkCbB&page=" + i + "&pagesize=10&ext_attr=no&brand_col=no&price_col=no&color_col=no&size_col=no&ext_attr_sort=no&merge_sku=yes&multi_suppliers=yes&area_ids=1,72,4137&qp_disable=no&fdesc=%E5%8C%97%E4%BA%AC&t1=1524462928763", header, requestConfig);
                result = HttpUtil.readResponse(response);
                result = result.replaceAll("^\\w+\\(", "").replaceAll("\\)$", "");
                jsonObject = JSON.parseObject(result);
                array = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONArray("Paragraph");
                adArray = jsonObject.getJSONObject("data").getJSONObject("adpos").getJSONArray("data");
                adPositionSet = new HashSet<>();
                for (int j = 0; j < adArray.size(); j++) {
                    adPositionSet.add(adArray.getJSONObject(j).getIntValue("flow_order"));
                }
                for (int j = 0; j < array.size(); j++) {
                    rank++;
                    position = j + 1;
                    JSONObject record = array.getJSONObject(j);
                    if (request.getSku().equals(record.getString("wareid"))) {
                        SearchResult searchResult = new SearchResult();
                        for (Integer adPosition : adPositionSet) {
                            if (j + 1 >= adPosition) {
                                position++;
                            }
                        }
                        int totalPosition = position + rankWithAds;
                        searchResult.setPos(totalPosition % pageSize);
                        searchResult.setPage((totalPosition + pageSize - 1) / pageSize);
                        searchResult.setRank(rank);
                        searchResult.setType(request.getType());
                        searchResult.setKeyword(request.getKeyword());
                        searchResult.setSku(record.getString("wareid"));
                        searchResult.setUrl("https://wqitem.jd.com/item/view?sku=" + searchResult.getSku());
                        crawlSkuDetail(searchResult);
                        searchResult.setImg("//img14.360buyimg.com/n8/s370x474_" + record.getJSONObject("Content").getString("imageurl"));
                        crawlCommentH5(searchResult);
                        crawlFoldComments(searchResult);
                        resultList.add(searchResult);
                        return resultList;
                    }
                }
                rankWithAds = rankWithAds + array.size() + adArray.size();
            }
            LOGGER.error(result);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }

    private List<SearchResult> searchShopWqs(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        Map<String, String> header = new HashMap<>();
        CookieStore cookieStore = new BasicCookieStore();
        cookieTool.loadCookie("search", "PC", cookieStore);
        header.put("Cookie", cookieTool.getCookieStr(cookieStore));
        int pageSize = 10;
        try {
            CloseableHttpResponse response = HttpUtil.doGet("http://wqsou.jd.com/search/searchn?key=" + URLEncoder.encode(request.getKeyword(), "utf-8") + "&datatype=1&callback=jdSearchResultBkCbB&page=1&pagesize=10&ext_attr=no&brand_col=no&price_col=no&color_col=no&size_col=no&ext_attr_sort=no&merge_sku=yes&multi_suppliers=yes&area_ids=1,72,4137&qp_disable=no&fdesc=%E5%8C%97%E4%BA%AC&t1=1524462928763", header, requestConfig);
            String result = HttpUtil.readResponse(response);
            result = result.replaceAll("^\\w+\\(", "").replaceAll("\\)$", "");
            JSONObject jsonObject = JSON.parseObject(result);
            int total = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONObject("Head").getJSONObject("Summary").getIntValue("ResultCount");
            JSONArray array = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONArray("Paragraph");
            JSONArray adArray = jsonObject.getJSONObject("data").getJSONObject("adpos").getJSONArray("data");
            Set<Integer> adPositionSet = new HashSet<>();
            for (int i = 0; i < adArray.size(); i++) {
                adPositionSet.add(adArray.getJSONObject(i).getIntValue("flow_order"));
            }
            int rank = 0;
            int page = 1;
            int position = 0;
            int rankWithAds = 0;
            for (int i = 0; i < array.size(); i++) {
                rank++;
                position = i + 1;
                JSONObject record = array.getJSONObject(i);
                SearchResult searchResult = new SearchResult();
                searchResult.setSku(record.getString("wareid"));
                crawlSkuDetail(searchResult);
                if (searchResult.getShop().contains(request.getShop())) {
                    searchResult.setPage(page);
                    for (Integer adPosition : adPositionSet) {
                        if (i + 1 >= adPosition) {
                            position++;
                        }
                    }
                    searchResult.setPos(position);
                    searchResult.setRank(rank);
                    searchResult.setType(request.getType());
                    searchResult.setKeyword(request.getKeyword());
                    searchResult.setSku(record.getString("wareid"));
                    searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                    crawlSkuDetail(searchResult);
                    searchResult.setImg("//img14.360buyimg.com/n8/s370x474_" + record.getJSONObject("Content").getString("imageurl"));
                    crawlCommentH5(searchResult);
                    crawlFoldComments(searchResult);
                    resultList.add(searchResult);
                    if (resultList.size() >= 3) {
                        return resultList;
                    }
                }
            }
            rankWithAds = rankWithAds + array.size() + adArray.size();
            int totalPage = (total + pageSize - 1) / pageSize;
            for (int i = 2; i <= Math.min(50, totalPage); i++) {
                response = HttpUtil.doGet("http://wqsou.jd.com/search/searchn?key=" + URLEncoder.encode(request.getKeyword(), "utf-8") + "&datatype=1&callback=jdSearchResultBkCbB&page=" + i + "&pagesize=10&ext_attr=no&brand_col=no&price_col=no&color_col=no&size_col=no&ext_attr_sort=no&merge_sku=yes&multi_suppliers=yes&area_ids=1,72,4137&qp_disable=no&fdesc=%E5%8C%97%E4%BA%AC&t1=1524462928763", header, requestConfig);
                result = HttpUtil.readResponse(response);
                result = result.replaceAll("^\\w+\\(", "").replaceAll("\\)$", "");
                jsonObject = JSON.parseObject(result);
                array = jsonObject.getJSONObject("data").getJSONObject("searchm").getJSONArray("Paragraph");
                adArray = jsonObject.getJSONObject("data").getJSONObject("adpos").getJSONArray("data");
                adPositionSet = new HashSet<>();
                for (int j = 0; j < adArray.size(); j++) {
                    adPositionSet.add(adArray.getJSONObject(j).getIntValue("flow_order"));
                }
                for (int j = 0; j < array.size(); j++) {
                    rank++;
                    position = j + 1;
                    JSONObject record = array.getJSONObject(j);
                    SearchResult searchResult = new SearchResult();
                    searchResult.setSku(record.getString("wareid"));
                    crawlSkuDetail(searchResult);
                    if (searchResult.getShop().contains(request.getShop())) {
                        for (Integer adPosition : adPositionSet) {
                            if (j + 1 >= adPosition) {
                                position++;
                            }
                        }
                        int totalPosition = position + rankWithAds;
                        searchResult.setPos(totalPosition % pageSize);
                        searchResult.setPage((totalPosition + pageSize - 1) / pageSize);
                        searchResult.setRank(rank);
                        searchResult.setType(request.getType());
                        searchResult.setKeyword(request.getKeyword());
                        searchResult.setSku(record.getString("wareid"));
                        searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                        crawlSkuDetail(searchResult);
                        searchResult.setImg("//img14.360buyimg.com/n8/s370x474_" + record.getJSONObject("Content").getString("imageurl"));
                        crawlCommentH5(searchResult);
                        crawlFoldComments(searchResult);
                        resultList.add(searchResult);
                        if (resultList.size() >= 3) {
                            return resultList;
                        }
                    }
                }
                rankWithAds = rankWithAds + array.size() + adArray.size();
            }
            LOGGER.error(result);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }

    private List<SearchResult> searchCategoryWqs(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        SearchResult searchResult = new SearchResult();
        searchResult.setSku(request.getSku());
        crawlSkuDetail(searchResult);
        return resultList;
    }


    private List<SearchResult> searchApp(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        return resultList;
    }

    private String uuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "");
    }

    private void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {

        }
    }
}
