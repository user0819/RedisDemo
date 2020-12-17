package com.wang.demo;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

public class Chapter01 {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
    private static final int ARTICLES_PER_PAGE = 25;

    public static void main(String[] args) {
        new Chapter01().run();
    }


    /**
     * redis中存储信息：
     * key          type        desc
     * article:     String      文章ID
     * article:N    Hash        文章信息
     * voted:N      Set         投票人集合
     * score:       ZSet        所有文章的分数
     * time:        ZSet        所有文章的创建时间
     */
    public void run() {
        Jedis conn = new Jedis("119.45.1.19");
        conn.select(15);

        //发布一本书
        String articleId = postArticle(conn, "username", "A title", "http://www.google.com");
        //打印相关信息
        System.out.println("We posted a new article with id: " + articleId);
        System.out.println("Its HASH looks like:");
        Map<String,String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry<String,String> entry : articleData.entrySet()){
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println();

        //再投一票
        articleVote(conn, "other_user", "article:" + articleId);
        String votes = conn.hget("article:" + articleId, "votes");
        System.out.println("We voted for the article, it now has votes: " + votes);
        assert Integer.parseInt(votes) > 1;

        //书列表
        System.out.println("The currently highest-scoring articles are:");
        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(articles);
        assert articles.size() >= 1;

        //把书放到其他分组去
        addGroups(conn, articleId, new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);
        assert articles.size() >= 1;
    }

    public String postArticle(Jedis conn, String user, String title, String link) {
        //article:
        String articleId = String.valueOf(conn.incr("article:"));

        //voted:1
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;
        HashMap<String,String> articleData = new HashMap<String,String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);
        conn.zadd("score:", now + VOTE_SCORE, article);
        conn.zadd("time:", now, article);

        return articleId;
    }

    public void articleVote(Jedis conn, String user, String article) {
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        if (conn.zscore("time:", article) < cutoff){
            return;
        }

        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1) {
            conn.zincrby("score:", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1);
        }
    }


    public List<Map<String,String>> getArticles(Jedis conn, int page) {
        return getArticles(conn, page, "score:");
    }

    public List<Map<String,String>> getArticles(Jedis conn, int page, String ZSetKey) {
        int start = (page - 1) * ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE - 1;

        Set<String> ids = conn.zrevrange(ZSetKey, start, end);
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }

        return articles;
    }

    public void addGroups(Jedis conn, String articleId, String[] toAdd) {
        String article = "article:" + articleId;
        for (String group : toAdd) {
            conn.sadd("group:" + group, article);
        }
    }

    public List<Map<String,String>> getGroupArticles(Jedis conn, String group, int page) {
        return getGroupArticles(conn, group, page, "score:");
    }

    public List<Map<String,String>> getGroupArticles(Jedis conn, String group, int page, String order) {
        String key = order + group;
        if (!conn.exists(key)) {
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }

    private void printArticles(List<Map<String,String>> articles){
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
