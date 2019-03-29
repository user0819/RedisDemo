package com.wang;

import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by Mr.Wang on 2019/2/17.
 * Jedis 测试用例
 */
public class JedisDemo {

    @Test
    public void demo1(){
        //设置ip和端口
        Jedis jedis = new Jedis("192.168.222.128",6379);
        //保存数据
        jedis.set("name","wangxiang");
        //获取数据
        String name = jedis.get("name");
        System.out.print(name);
        //释放资源
        jedis.close();
    }

    @Test
    public void demo2(){
        //连接池配置
        JedisPoolConfig config = new JedisPoolConfig();
        //最大空闲连接数
        config.setMaxIdle(10);
        //最大连接数
        config.setMaxTotal(3);

        //连接池
        JedisPool jedisPool = new JedisPool(config,"192.168.222.128",6379);

        //创建Jedis对象
        Jedis jedis = jedisPool.getResource();
        jedis.set("another","张三");
        String another = jedis.get("wife");
        System.out.print(another);
        jedis.close();
        jedisPool.close();
    }

}
