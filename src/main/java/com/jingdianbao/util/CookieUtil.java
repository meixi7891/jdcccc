package com.jingdianbao.util;

import com.jingdianbao.redis.RedisClient;
import com.jingdianbao.service.impl.DmpService;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class CookieUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmpService.class);

    public static void saveCookie(String userName, String pwd, WebDriver webDriver) {
        Set<Cookie> cookieSet = webDriver.manage().getCookies();
        String key = userName + "_" + pwd;

        try {
            StringBuilder sb = new StringBuilder();
            for (Cookie cookie : cookieSet) {
                sb.append(cookie.toString()).append("@__@");
            }
            String str = sb.toString();
            Jedis jedis = RedisClient.getRedesClient().getJedis();
            jedis.hset("cookies", key, str);
            RedisClient.getRedesClient().returnResource(jedis);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public static boolean hasCookie(String userName, String pwd) {
        Jedis jedis = RedisClient.getRedesClient().getJedis();
        String key = userName + "_" + pwd;
        String str = jedis.hget("cookies", key);
        RedisClient.getRedesClient().returnResource(jedis);
        if (str == null || str.isEmpty()) {
            return false;
        }
        return true;
    }


    public static boolean hasSellerCookie(String userName, String pwd) {
        Jedis jedis = RedisClient.getRedesClient().getJedis();
        String key = "seller_cookies_" + userName + "_" + pwd;
        String str = jedis.hget("seller_cookies", key);
        RedisClient.getRedesClient().returnResource(jedis);
        if (str == null || str.isEmpty()) {
            return false;
        }
        return true;
    }

    public static void saveSellerCookie(String userName, String pwd, WebDriver webDriver) {
        Set<Cookie> cookieSet = webDriver.manage().getCookies();
        String key = "seller_cookies_" + userName + "_" + pwd;
        try {
            StringBuilder sb = new StringBuilder();
            for (Cookie cookie : cookieSet) {
                sb.append(cookie.toString()).append("@__@");
            }
            String str = sb.toString();
            Jedis jedis = RedisClient.getRedesClient().getJedis();
            jedis.set(key, str);
            jedis.expire(key, 7 * 24 * 60 * 60);
            RedisClient.getRedesClient().returnResource(jedis);
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }


    public static void loadCookie(String userName, String pwd, WebDriver webDriver) {
        Jedis jedis = RedisClient.getRedesClient().getJedis();
        String key = userName + "_" + pwd;
        try {
            String str = jedis.hget("cookies", key);
            RedisClient.getRedesClient().returnResource(jedis);
            if (str == null || str.isEmpty()) {
                return;
            }
            String[] ss = str.split("@__@");
            for (int i = 0; i < ss.length; i++) {
                Cookie cookie = buildCookie(ss[i]);
                if (cookie != null) {
                    webDriver.manage().addCookie(cookie);
                }
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public static void loadCookie(String userName, String pwd, CookieStore cookieStore) {
        Jedis jedis = RedisClient.getRedesClient().getJedis();
        String key = userName + "_" + pwd;
        try {
            String str = jedis.hget("cookies", key);
            RedisClient.getRedesClient().returnResource(jedis);
            if (str == null || str.isEmpty()) {
                return;
            }
            String[] ss = str.split("@__@");
            for (int i = 0; i < ss.length; i++) {
                Cookie cookie = buildCookie(ss[i]);
                BasicClientCookie basicClientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
                basicClientCookie.setDomain(cookie.getDomain());
                basicClientCookie.setPath(cookie.getPath());
                basicClientCookie.setSecure(cookie.isSecure());
                basicClientCookie.setExpiryDate(cookie.getExpiry());
                cookieStore.addCookie(basicClientCookie);
            }
        } catch (Exception e) {
            LOGGER.error("", e);
        }
    }

    public static String getCookieStr(CookieStore cookieStore) {
        StringBuilder stringBuilder = new StringBuilder();
        for (org.apache.http.cookie.Cookie cookie : cookieStore.getCookies()) {
            stringBuilder.append(cookie.getName()).append("=").append(cookie.getValue()).append(";");
        }
        if (stringBuilder.length() > 0) {
            return stringBuilder.substring(0, stringBuilder.length() - 1);
        } else {
            return "";
        }

    }

    private static Cookie buildCookie(String str) {
        String[] ss = str.split(";");
        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        Date expiry = null;
        boolean isSecure = false;
        try {
            for (int i = 0; i < ss.length; i++) {
                if (ss[i].contains("=")) {
                    String[] sss = ss[i].split("=");
                    if (sss[0].trim().equals("expires")) {
                        expiry = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z").parse(sss[1].trim());
                    } else if (sss[0].trim().equals("path")) {
                        path = sss[1].trim();
                    } else if (sss[0].trim().equals("domain")) {
                        domain = sss[1].trim();
                    } else {
                        name = sss[0].trim();
                        value = sss[1].trim();
                    }
                } else {
                    if (ss[i].trim().equals("secure")) {
                        isSecure = true;
                    }
                }
            }
            if (name == null) {
                return null;
            }
            Cookie cookie = new Cookie(name, value, domain, path, expiry, isSecure);
            return cookie;
        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }
}
