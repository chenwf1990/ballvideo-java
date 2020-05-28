package com.miguan.ballvideo.service;

import com.miguan.ballvideo.entity.PushArticle;

import java.util.List;
import java.util.Map;

/**
 * Created by shixh on 2019/9/10.
 */
public interface PushArticleService {
    PushArticle getOneToPush();

    PushArticle findOneToPush(Long id);

    //获取定时推送信息
    List<PushArticle> findFixedTimeListToPush();

    //vivo指定用户推送
    void vivoPushByRegIds(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception;

    //vivo广播推送
    void vivoPushAll(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception;

    //oppo指定用户推送
    void oppoPushByRegIds(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception;

    //oppo广播推送
    void oppoPushAll(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception;

    //xiaomi指定用户和广播推送,type:1指定用户；2广播推送
    void xiaomiPushAll(PushArticle pushArticle, Map<String, String> params, String mobileType, int expireTime, String type) throws Exception;
}
