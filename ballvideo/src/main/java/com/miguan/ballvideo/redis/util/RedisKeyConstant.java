package com.miguan.ballvideo.redis.util;


import com.miguan.ballvideo.common.util.Global;

public interface RedisKeyConstant {

    String defalut = "ballVideos:";//公共前缀KEY
    int defalut_seconds = 300;//默认5分钟
    //user token
    String USER_TOKEN = defalut + "token:user:";
    int USER_TOKEN_SECONDS = 60 * 60 * 24 * 30;

    //push task
    String PUSH_TASK = defalut + "initPushTask";
    int PUSH_TASK_SECONDS = 1000 * 60 * 10;

    //@RequestCache
    String REQUEST_CACHE_KEY = defalut + "requestCache:";

    //@CacheAble
    String CACHE_ABLE_KEY = defalut + "cacheAble:";

    //首页推荐1.8-用户标签
    String USERLABEL_KEY = defalut + "userLabe:";
    int USERLABELKEY_SECONDS = 604800;//有效期一周

    //首页推荐1.8-浏览记录ID
    String SHOWEDBYTEIDS_KEY = defalut + "showedByteIds:";
    String SHOWEDIDS_KEY = defalut + "showedByIds:";
    int SHOWEDIDS_SECONDS = Global.getInt("user_showedIds_expiredTime");//有效期动态配置

    //首页推荐1.8-最近3天创建
    String CREATED3DAY = "created3Day";

    //视频缓存时间
    String VIDEO_CACHE = "videoCache";

    //待整合
    String INTERFACE = defalut + "interface:";

    String NEWFIRSTVIDEO161_KEY = INTERFACE + "newfirstVideo161:";
    int NEWFIRSTVIDEO161_SECONDS = 14400;//4h

    String NEWFIRSTVIDEO_KEY = INTERFACE + "newfirstVideo:";
    int NEWFIRSTVIDEO_SECONDS = 14400;//4h

    String ADVERTLIST161_KEY = INTERFACE + "advertList161:";
    int ADVERTLIST161_SECONDS = 120;//2m

    String NEWSMALLVIDEO17_KEY = INTERFACE + "newsmallVideo1.7:";
    int NEWSMALLVIDEO17_SECONDS = 14400;//4h

    String DEVICEID_KEY = CacheConstant.BURYINGPOINT_USER + "::ballVideos:cacheAble:findUserBuryingPointIsNew:";

    //vivo鉴权
    String VIVO_TOKEN = defalut + "vivo:token";

    //getBaiduHotWord
    String HOT_WORD = defalut + "getBaiduHotWord";
    int HOT_WORD_SECONDS = 300;

    //真实权重key前缀
    String REAL_WEIGHT_KEY= defalut+"realWeightVideoIds";
    //真实权重key前缀(锁)
    String REAL_WEIGHT_LOCK_KEY= defalut+"realWeight:redisLock";
    //真实权重锁时间（毫秒）
    int REAL_WEIGHT_LOCK_SECONDS = 3000;
    int REAL_WEIGHT_SECONDS = 60000;

    //文件预热
    String VIDEO_CACHE_PREFETCH = defalut + "videoCachePrefetch";
    int VIDEO_CACHE_PREFETCH_TIME = 300000;//5分钟

    //删除用户标签数据
    String USER_LABEL_DELETE = defalut + "userLabelDelete";
    int USER_LABEL_DELETE_SECONDS = 30000 * 60;

    //切片广告key
    String shangbaoKey = "shangbao:";
    String KEY_AD_RATE = shangbaoKey+"ad_rate";
    String KEY_AD_HIGH = "high";//V2.5.0新增
    String KEY_AD_LOW = "LOW";//V2.5.0新增

    //广告错误redis保存
    String ADVERT_ERROR_LOG = defalut + "AdvertErrorLog:";

    String EMPTY_VALUE = "empty";//防止缓存击穿，设置value为empty

    int EMPTY_VALUE_SECONDS = 300;

    String CAT_IDS= "catIds:";//视频分类IDS

    int CAT_IDS_SECONDS = 14400;//4H

    //处理保存异常错误日志
    String SEARCH_AND_CLEAR_WRONG_DATAS_LOCK = defalut + "search_and_clear_wrong_datas_lock:";
    int SEARCH_AND_CLEAR_WRONG_DATAS_SECONDS = 1000 * 60 * 10;

    //删除广告错误日志数据
    String ADVERT_ERROR_LOG_DELETE = defalut + "advertErrorLogDelete";
    int ADVERT_ERROR_LOG_SECONDS = 30000 * 60;
}
