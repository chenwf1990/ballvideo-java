package com.miguan.ballvideo.vo.queue;

import lombok.Data;

/**
 * 用户标签队列对象
 * @Author shixh
 * @Date 2019/12/12
 **/
@Data
public class UserLabelQueueVo {
    private String deviceId;
    private String channelId;
    private String appVersion;

    UserLabelQueueVo(){}
    public UserLabelQueueVo(String deviceId, String channelId, String appVersion){
        this.appVersion = appVersion;
        this.deviceId = deviceId;
        this.channelId = channelId;
    }
}
