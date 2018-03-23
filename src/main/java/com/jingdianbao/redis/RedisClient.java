package com.jingdianbao.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisClient.class);
    private static RedisClient redisClient = null;
    private static String ADDR = "120.79.225.173";
    //Redis的端口号
    private static int PORT = 6379;

    //可用连接实例的最大数目，默认值为8；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    private static int MAX_ACTIVE = -1;

    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    private static int MAX_WAIT = 10000;

    private static int TIMEOUT = 10000;

    //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;

    private static String PWD = "Yushu@redis";


    private JedisPool jedisPool;

    private RedisClient() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT,PWD);
        } catch (Exception e) {
            LOGGER.error("init redis connection pool throws exception :", e);
        }
    }

    public static RedisClient getRedesClient() {
        if (redisClient == null) {
            synchronized (RedisClient.class) {
                if (redisClient == null) {
                    redisClient = new RedisClient();
                }
            }
        }
        return redisClient;
    }

    public Jedis getJedis(){
        return jedisPool.getResource();
    }

    public void returnResource(Jedis jedis){
        if(jedis!=null){
            jedis.close();
        }
    }
}
