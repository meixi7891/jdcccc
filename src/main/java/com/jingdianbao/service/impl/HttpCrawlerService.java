package com.jingdianbao.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jingdianbao.entity.SearchRequest;
import com.jingdianbao.entity.SearchResult;
import com.jingdianbao.http.HttpClientFactory;
import com.jingdianbao.service.CrawlerService;
import com.jingdianbao.webdriver.WebDriverBuilder;
import com.jingdianbao.entity.Category;
import com.jingdianbao.webdriver.WebDriverActionDelegate;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
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
        try {
            String pvid = uuid();
            String keyword = URLEncoder.encode(request.getKeyword(), "utf-8");
            String refer = "https://search.jd.com/Search?keyword=" + keyword + "&enc=utf-8&pvid=" + pvid;
            String url = "https://search.jd.com/s_new.php?keyword=" + keyword + "&enc=utf-8&qrst=1&rt=1&stop=1&vt=2&scrolling=y&click=0";
            int s = 1;
            String sku = request.getSku();
            String pa = "<li\\s+class=\"gl-item(\\s+gl-item-presell)?\"\\s+data-sku=\"\\d+\"\\s+data-spu=\"\\d+\" data-pid=\"\\d+\">|<li\\s+data-sku=\"\\d+\"\\s+class=\"gl-item(\\s+gl-item-presell)?\">";
            Pattern p = Pattern.compile(pa);
            Pattern skuPattern = Pattern.compile("data-sku=\"(\\d+)\"");
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                    .setSocketTimeout(5000).build();
            CookieStore cookieStore = new BasicCookieStore();
            CloseableHttpClient httpClient = HttpClientFactory.getHttpClient(cookieStore);
            int pageTotal = 0;
            try {
                HttpGet httpGet = new HttpGet(refer);
                httpGet.setHeader("Referer", refer);
                httpGet.setConfig(requestConfig);
                CloseableHttpResponse response = httpClient.execute(httpGet);
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                String result = sb.toString();
                Document document = Jsoup.parse(result);
                String pageStr = document.select("div[id=J_topPage]>span>i").text();
                pageTotal = Integer.parseInt(pageStr);
            } catch (Exception e) {
                LOGGER.error("", e);
            }
            int rank = 0;
            out:
            for (int i = 0; i < pageTotal; i++) {
                int page = i;
                int position = 0;
                for (int j = 1; j <= 2; j++) {
                    httpClient = HttpClientFactory.getHttpClient(cookieStore);
                    String urlStr = url + "&page=" + (i * 2 + j) + "&s=" + s;
                    String result = get(urlStr, refer, requestConfig, httpClient);
                    Document document = Jsoup.parse(result);
                    httpClient.close();
                    String[] ss = result.split(pa, -1);
                    Set<Integer> adIndexSet = new HashSet<>();
                    for (int m = 0; m < ss.length; m++) {
                        if (ss[m].isEmpty()) {
                            continue;
                        }
                        System.out.println(ss[m]);
                        if (ss[m].contains("广告</span>")) {
                            adIndexSet.add(m);
                        } else {
                            s++;
                        }
                    }
                    int index = 0;
                    Matcher matcher = p.matcher(result);
                    while (matcher.find()) {
                        index++;
                        position++;
                        if (!adIndexSet.contains(index)) {
                            rank++;
                        }
                        String skuLine = matcher.group();
                        if (request.getType().equals("goods") && skuLine.contains("data-sku=\"" + sku + "\"")) {
                            SearchResult searchResult = new SearchResult();
                            searchResult.setPage(page + 1);
                            searchResult.setPos(position);
                            searchResult.setRank(rank);
                            searchResult.setType(request.getType());
                            searchResult.setSku(String.valueOf(sku));
                            searchResult.setKeyword(request.getKeyword());
                            if (request.getSource().equals("PC")) {
                                searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                            } else {
                                searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                            }
                            Elements e = document.select("img[data-sku=" + searchResult.getSku() + "]");
                            if (e.attr("src") != null && !e.attr("src").isEmpty()) {
                                searchResult.setImg(e.attr("src"));
                            } else {
                                searchResult.setImg(e.attr("data-lazy-img"));
                            }
                            resultList.add(searchResult);
                            if (request.getType().equals("goods") || resultList.size() == 10) {
                                break out;
                            }
                        } else if (request.getType().equals("shop")) {
                            Matcher skuMatcher = skuPattern.matcher(skuLine);
                            if (skuMatcher.find()) {
                                String currentSku = skuMatcher.group(1);
                                Elements e = document.select("li[data-sku=" + currentSku + "] div[class=p-shop]");
                                if (e.text().contains(request.getKeyword())) {
                                    SearchResult searchResult = new SearchResult();
                                    searchResult.setPage(page + 1);
                                    searchResult.setPos(position);
                                    searchResult.setRank(rank);
                                    searchResult.setType(request.getType());
                                    searchResult.setSku(String.valueOf(currentSku));
                                    searchResult.setKeyword(request.getKeyword());
                                    if (request.getSource().equals("PC")) {
                                        searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
                                    } else {
                                        searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
                                    }
                                    Elements imageNode = document.select("img[data-sku=" + searchResult.getSku() + "]");
                                    if (imageNode.attr("src") != null && !imageNode.attr("src").isEmpty()) {
                                        searchResult.setImg(imageNode.attr("src"));
                                    } else {
                                        searchResult.setImg(imageNode.attr("data-lazy-img"));
                                    }
                                    resultList.add(searchResult);
                                    if (resultList.size() == 10) {
                                        break out;
                                    }
                                }
                            }
                        }
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        resultList.forEach(searchResult -> {
            crawlSkuDetail(searchResult);
            crawlComment(searchResult);
            crawlFoldComments(searchResult);
        });
        return resultList;
    }

//    private List<SearchResult> searchGoods(SearchRequest request) {
//        List<SearchResult> resultList = new ArrayList<>();
//        ChromeDriver webDriver = webDriverBuilder.getWebDriver();
//        try {
//            String pvid = uuid();
//            String keyword = URLEncoder.encode(request.getKeyword(), "utf-8");
//            String refer = "https://search.jd.com/Search?keyword=" + keyword + "&enc=utf-8&pvid=" + pvid;
//
//            webDriver.get(refer);
//            if (request.getSortType() != null && !"0".equals(request.getSortType())) {
//                webDriver.executeScript("SEARCH.sort('" + request.getSortType() + "')");
//            }
//            String sku = request.getSku();
//            int rank = 0;
//            WebElement element = webDriver.findElementByXPath("//span[@class='p-skip']/input[@class='input-txt']");
//            int elementPosition = element.getLocation().getY() - 100;
//            String js = String.format("window.scroll(0, %s)", elementPosition);
//            webDriver.executeScript(js);
//            sleep(1000);
//
//            int page = 1;
//            List<WebElement> elementList = webDriver.findElementsByCssSelector("li[class~=gl-item]");
//            for (int i = 0; i < elementList.size(); i++) {
//                WebElement e = elementList.get(i);
//                if (e.findElements(By.cssSelector("span[class=p-promo-flag]")).isEmpty()) {
//                    rank++;
//                }
//                if ((request.getType().equals("goods") && sku.equals(e.getAttribute("data-sku"))) || (request.getType().equals("shop") && !e.findElements(By.cssSelector("a[class~=curr-shop]")).isEmpty() && e.findElement(By.cssSelector("a[class~=curr-shop]")).getText().contains(request.getShop()))) {
//                    SearchResult searchResult = new SearchResult();
//                    searchResult.setPage(page);
//                    searchResult.setPos(i + 1);
//                    searchResult.setRank(rank);
//                    searchResult.setType(request.getType());
//                    searchResult.setKeyword(request.getKeyword());
//                    searchResult.setSku(e.getAttribute("data-sku"));
//                    if (request.getSource().equals("PC")) {
//                        searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
//                    } else {
//                        searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
//                    }
//                    WebElement imgNode = e.findElement(By.tagName("img"));
//                    if (imgNode.getAttribute("src") != null && !imgNode.getAttribute("src").isEmpty()) {
//                        searchResult.setImg(imgNode.getAttribute("src"));
//                    } else {
//                        searchResult.setImg(imgNode.getAttribute("data-lazy-img"));
//                    }
//                    resultList.add(searchResult);
//                    if (request.getType().equals("goods") || resultList.size() == 10) {
//                        break;
//                    }
//                }
//            }
//
//            String pageStr = webDriver.findElementByCssSelector("#J_topPage i").getText();
//            int pageTotal = Integer.parseInt(pageStr);
//            out:
//            for (page = 2; page <= pageTotal; page++) {
//                webDriver.findElementByXPath("//a[@class='pn-next']").click();
//                sleep(1000);
//                webDriver.executeScript(js);
//                sleep(100);
//                elementList = webDriver.findElementsByCssSelector("li[class~=gl-item]");
//                for (int i = 0; i < elementList.size(); i++) {
//                    WebElement e = elementList.get(i);
//                    if (e.findElements(By.cssSelector("span[class=p-promo-flag]")).isEmpty()) {
//                        rank++;
//                    }
//                    if ((request.getType().equals("goods") && sku.equals(e.getAttribute("data-sku"))) || (request.getType().equals("shop") && !e.findElements(By.cssSelector("a[class~=curr-shop]")).isEmpty() && e.findElement(By.cssSelector("a[class~=curr-shop]")).getText().contains(request.getShop()))) {
//                        SearchResult searchResult = new SearchResult();
//                        searchResult.setPage(page);
//                        searchResult.setPos(i + 1);
//                        searchResult.setRank(rank);
//                        searchResult.setType(request.getType());
//                        searchResult.setKeyword(request.getKeyword());
//                        searchResult.setSku(e.getAttribute("data-sku"));
//                        if (request.getSource().equals("PC")) {
//                            searchResult.setUrl("https://item.jd.com/" + searchResult.getSku() + ".html");
//                        } else {
//                            searchResult.setUrl("https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
//                        }
//                        WebElement imgNode = e.findElement(By.tagName("img"));
//                        if (imgNode.getAttribute("src") != null && !imgNode.getAttribute("src").isEmpty()) {
//                            searchResult.setImg(imgNode.getAttribute("src"));
//                        } else {
//                            searchResult.setImg(imgNode.getAttribute("data-lazy-img"));
//                        }
//                        resultList.add(searchResult);
//                        if (request.getType().equals("goods") || resultList.size() == 10) {
//                            break out;
//                        }
//                    }
//                }
//                pageTotal = Integer.parseInt(webDriver.findElementByXPath("//span[@class='p-skip']/em/b").getText());
//            }
//        } catch (IOException e) {
//            LOGGER.error("", e);
//            webDriverActionDelegate.takeFullScreenShot(webDriver);
//            try {
//                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
//            } catch (IOException e1) {
//
//            }
//        } catch (Exception e) {
//            LOGGER.error("", e);
//            webDriverActionDelegate.takeFullScreenShot(webDriver);
//            try {
//                FileUtils.write(new File(PREFIX_FILE_PATH + "/" + System.currentTimeMillis() + ".html"), webDriver.getPageSource(), "utf-8");
//            } catch (IOException e1) {
//
//            }
//        }finally {
//            webDriver.quit();
//        }
//        resultList.forEach(searchResult -> {
//            crawlSkuDetail(searchResult);
//            crawlCommentH5(searchResult);
//            crawlFoldComments(searchResult);
//        });
//        return resultList;
//    }


    private List<SearchResult> searchCategory(SearchRequest request) {
        List<SearchResult> resultList = new ArrayList<>();
        SearchResult searchResult = new SearchResult();
        searchResult.setSku(request.getSku());
        searchResult.setType(request.getType());
        crawlSkuDetail(searchResult);
        crawlCommentH5(searchResult);
        crawlFoldComments(searchResult);
        crawlCategory(request, searchResult);
        resultList.add(searchResult);
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
                    } else {
                        System.out.println(e.select("span[class=p-promo-flag]").text());
                    }
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
            System.out.println(doc);
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


    private void crawlSkuDetail(final SearchResult searchResult) {
        String sku = searchResult.getSku();
        String url = "https://item.jd.com/" + sku + ".html";
        try {
            Document doc = Jsoup.connect(url).timeout(30000).get();
            String title = doc.select("div[class=sku-name]").text().trim();
            searchResult.setTitle(title);
            String shop = doc.select("div[class=J-hove-wrap EDropdown fr]>div[class=item]>div[class=name]").text();
            searchResult.setShop(shop);
            Elements elements = doc.select("#crumb-wrap>div[class=w]>div[class=crumb fl clearfix] a");
            String firstCategory = elements.get(0).text();
            String secondCategory = elements.get(1).text();
            String thirdCategory = elements.get(2).text();
            Category category = new Category();
            category.setLevel1(firstCategory);
            category.setLevel2(secondCategory);
            category.setLevel3(thirdCategory);
            searchResult.setCategory(category);
            System.out.println(doc);
        } catch (IOException e) {
            LOGGER.error("", e);
        }
    }

    private void crawlComment(final SearchResult searchResult) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        String url = "https://sclub.jd.com/comment/productPageComments.action?callback=fetchJSON_comment98vv225&productId=" + searchResult.getSku() + "&score=0&sortType=5&page=0&pageSize=10&isShadowSku=0&rid=0&fold=2";
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Referer", "https://item.jd.com/" + searchResult.getSku() + ".html");
            httpGet.setConfig(requestConfig);
            System.out.println(url);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String result = sb.toString();
            System.out.println(result);
            Pattern p = Pattern.compile("\\(.*\\);");
            Matcher matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(");", "");
                JSONObject jsonObject = JSONObject.parseObject(json);
                int commentCount = jsonObject.getJSONObject("productCommentSummary").getIntValue("commentCount");
                searchResult.setComment(commentCount);
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void crawlCommentH5(final SearchResult searchResult) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        try {
            HttpPost httpPost = new HttpPost("https://item.m.jd.com/newComments/newCommentsDetail.json");
            httpPost.setHeader("Referer", "https://item.m.jd.com/product/" + searchResult.getSku() + ".html");
            httpPost.setHeader("user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1");
            httpPost.setHeader("cookie", "abtest=20180228202800418_35; subAbTest=20180228202800418_63; mobilev=html5; mba_muid=15102037407391075657148; M_Identification=3dbf82145dafa232_7e49c7a6168021190e99155b7a54294b; M_Identification_abtest=20180228203521392_79417497; user-key=4f824d2f-c787-4c0a-a0fe-47bbc16ffd61; __jdu=15102037407391075657148; areaId=15; cn=0; ipLocation=%u6D59%u6C5F; ipLoc-djd=15-1213-3411-52667; _jrda=2; PCSYCityID=1213; m_uuid_new=7581434D080F0F9CACB87FDAB8802090; mhome=1; mt_xid=V2_52007VwMWUlxbU1gZTBhaB28DE1RZX1ZcH0wQbAFkURIBDw8CRkhLSlQZYgMUUUFRUlkcVRBfVWcBGgFcX1BfSHkaXQVuHxNQQVhaSx9OElgNbAASYl9oUmofTBFcAGUFE1VtWFFZHQ%3D%3D; TrackID=1WO7I-jO3o5eG2aje7Biq7Dqyd4kOT0HgfKR7eAj9jrGa86tG7DW0Lp425GSS7fyYmepwQIEoe51DTEHhIxAxI2l-60YrURdfh7KxpC4tt6s; pinId=tv7lg4K603eYEzFXmTd7PA; pin=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; unick=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; _tp=YNgb%2BpVBeuPdmEIO5nCqm4Wjzdm1b2MJ72aMs%2BZnuBD3RmRRkUBZ6bgSCC8Vnd5Q; _pst=%E8%AF%BA%E9%82%A6%E6%A8%A1%E5%9E%8B%E8%B1%B9; __jda=122270672.15102037407391075657148.1510203741.1521601290.1521614989.40; __jdc=122270672; __jdv=122270672|direct|-|none|-|1521614989388; 3AB9D23F7A4B3C9B=6J3PSQXXMA7QOEN4FBQJM5OJ5KRJULMKFO3UTQJXCVKAWZD7O6SHBBE2CWRO56FA4IVYK74R2YOKB4GNQZEPMTWGEU; sid=e32c30c27c268986e1bf3e9388314352; USER_FLAG_CHECK=2dc68f02e6c15b183027396c0cc8c798; autoOpenApp_downCloseDate_auto=1521616324011_21600000; warehistory=\"2600242,25568515204,20099469725,5001175,25868561553,14143954650,25797029156,12141800119,5716985,5789585,5495676,11229407797,\"; __jdb=122270672.7.15102037407391075657148|40.1521614989; mba_sid=15216163238624899707965548601.2");
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject jsonObject = JSONObject.parseObject(sb.toString());
            int commentCount = jsonObject.getJSONObject("wareDetailComment").getIntValue("allCnt");
            searchResult.setComment(commentCount);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private void crawlFoldComments(final SearchResult searchResult) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000).setConnectionRequestTimeout(1000)
                .setSocketTimeout(5000).build();
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
        String url = "https://club.jd.com/comment/getProductPageFoldComments.action?callback=jQuery1552332&productId=" + searchResult.getSku() + "&score=0&sortType=5&page=0&pageSize=5&_=1521114318952";
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Referer", "https://item.jd.com/" + searchResult.getSku() + ".html");
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "gbk"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            String result = sb.toString();
            System.out.println(result);
            Pattern p = Pattern.compile("\\(.*\\);");
            Matcher matcher = p.matcher(result);
            if (matcher.find()) {
                String json = matcher.group().replace("(", "").replace(");", "");
                JSONObject jsonObject = JSONObject.parseObject(json);
                int page = jsonObject.getIntValue("maxPage");
                if (page <= 1) {
                    searchResult.setDiscard(jsonObject.getJSONArray("comments").size());
                } else {
                    url = "https://club.jd.com/comment/getProductPageFoldComments.action?callback=jQuery1552332&productId=" + searchResult.getSku() + "&score=0&sortType=5&page=" + (page - 1) + "&pageSize=5&_=1521114318952";
                    httpClient = HttpClientFactory.getHttpClient();
                    httpGet = new HttpGet(url);
                    httpGet.setHeader("Referer", "https://item.jd.com/" + searchResult.getSku() + ".html");
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
                        searchResult.setDiscard((page - 1) * 5 + jsonObject.getJSONArray("comments").size());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    private String get(String url, String refer, RequestConfig requestConfig, CloseableHttpClient httpClient) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Referer", refer);
        httpGet.setHeader("Host", "search.jd.com");
        httpGet.setConfig(requestConfig);
        System.out.println(url);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private String get(String url, String refer, RequestConfig requestConfig, CloseableHttpClient httpClient, Map<String, String> header) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Referer", refer);
        httpGet.setHeader("Host", "search.jd.com");
        for (Map.Entry<String, String> entry : header.entrySet()) {
            httpGet.setHeader(entry.getKey(), entry.getValue());
        }
        httpGet.setConfig(requestConfig);
        System.out.println(url);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        response.getAllHeaders();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
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
        try {
            int adCount = 0;
            out:
            for (int i = 1; i <= 100; i++) {
                JSONObject jsonObject = JSONObject.parseObject(searchH5(request.getKeyword(), i));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                for (int j = 0; j < array.size(); j++) {
                    JSONObject record = array.getJSONObject(j);
                    if (record.getString("catid") == null) {
                        adCount++;
                    }
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
                LOGGER.info(result.toJSONString());
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
            for (int i = 1; i <= 100; i++) {
                JSONObject jsonObject = JSONObject.parseObject(searchH5(request.getKeyword(), i));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                for (int j = 0; j < array.size(); j++) {
                    JSONObject record = array.getJSONObject(j);
                    if (record.getString("catid") == null) {
                        adCount++;
                    }
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
                            if (resultList.size() == 10) {
                                break;
                            }
                        }
                    }
                }
                LOGGER.info(result.toJSONString());
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

    private String searchH5(String keyword, int page) {
        CloseableHttpClient httpClient = HttpClientFactory.getHttpClient();
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

        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
            CloseableHttpResponse response = httpClient.execute(post);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            LOGGER.error("", e);
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
                JSONObject jsonObject = JSONObject.parseObject(searchH5(searchResult.getKeyword(), i));
                String value = jsonObject.getString("value");
                JSONObject result = JSONObject.parseObject(value);
                JSONArray array = result.getJSONObject("wareList").getJSONArray("wareList");
                for (int j = 0; j < array.size(); j++) {
                    JSONObject record = array.getJSONObject(j);
                    if (record.getString("catid") == null) {
                        adCount++;
                    }
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
        } catch (Exception e) {
            LOGGER.error("", e);
        }
        return resultList;
    }

    private String searchCategoryUrl(Category category) {
        String url = "https://so.m.jd.com/category/all.html?searchFrom=home";
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
                        httpGet.setHeader("referer", "https://m.jd.com/");
                        httpGet.setConfig(requestConfig);
                        CloseableHttpResponse response = httpClient.execute(httpGet);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        reader.close();
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
        }
        return "";
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