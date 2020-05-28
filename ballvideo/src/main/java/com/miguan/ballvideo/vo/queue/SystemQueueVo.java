package com.miguan.ballvideo.vo.queue;

import lombok.Data;

/**
 * 队列消息
 * @Author shixh
 * @Date 2019/12/11
 **/
@Data
public class SystemQueueVo {

    public static String FLASH_CACHE = "flash_Cache";
    public static String AdConfig_Cache = "AdConfig_Cache";//广告切片算法配置
    public static String AdLadderCache = "AdLadder_Cache";//价格阶梯广告配置

    SystemQueueVo(){}
    public SystemQueueVo(String operation){
        this.operation = operation;
    }
    private String operation;//flashCache-更新系统缓存，
    private String desc;
}
