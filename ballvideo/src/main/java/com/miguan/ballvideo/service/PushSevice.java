package com.miguan.ballvideo.service;

import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.entity.PushArticle;

public interface PushSevice {
    /**
     * 消息推送
     *
     * @param id
     * @return
     */
    ResultMap realTimeSendInfo(Long id);

    /**
     * IOS推送消息
     * @param pushArticle
     * @return
     */
    ResultMap sendInfoToIOS(PushArticle pushArticle);

    /**
     * Android推送消息
     *
     * @param pushArticle
     * @return
     */
    ResultMap sendInfoToAndroid(PushArticle pushArticle);

    /**
     * 立即推送测试接口
     *
     * @param id
     * @param tokens
     * @param pushChannel
     * @return
     */
    ResultMap realTimePushTest(Long id, String tokens, String pushChannel);
}
