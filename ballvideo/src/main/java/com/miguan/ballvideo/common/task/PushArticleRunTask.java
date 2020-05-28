package com.miguan.ballvideo.common.task;

import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.service.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.cgcg.redis.core.entity.RedisLock;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;


@Slf4j
@Data
public class PushArticleRunTask implements Runnable {

    private PushArticle pushArticle;

    private PushArticleConfigService pushArticleConfigService;

    private PushArticleSendResultService pushArticleSendResultService;

    private ClUserService userService;

    private Map<String, ScheduledFuture<?>> futuresMap;

    private PushArticleService pushArticleService;

    private PushSevice pushSevice;

    @Override
    public void run() {

        RedisLock redisLock = new RedisLock(RedisKeyConstant.PUSH_TASK+pushArticle.getId(), RedisKeyConstant.PUSH_TASK_SECONDS);
        if (redisLock.lock()) {
            log.info(Thread.currentThread().getName() + "|schedule PushArticleRunTask" + "|" + pushArticle.getTitle() + "|" + pushArticle.getPushTime());
            String appPackage = pushArticle.getAppPackage();
            if (Constant.IOSPACKAGE.equals(appPackage)) {
                //IOS推送
                pushSevice.sendInfoToIOS(pushArticle);
            } else {
                try {
                    //Android推送
                    pushSevice.sendInfoToAndroid(pushArticle);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("发送失败，请联系管理员");
                }
            }
            futuresMap.remove(String.valueOf(pushArticle.getId()));
        }
    }
}