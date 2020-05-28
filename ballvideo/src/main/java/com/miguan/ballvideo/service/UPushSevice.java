package com.miguan.ballvideo.service;

import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.entity.PushArticle;

public interface UPushSevice {
    /**
     * IOS立即推送
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
     * @param pushArticle
     * @return
     */
    ResultMap sendInfoToAndroid(PushArticle pushArticle) throws Exception;
}
