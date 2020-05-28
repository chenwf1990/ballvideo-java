package com.miguan.ballvideo.service;


import java.util.Map;

/**
 * 视频分享Service
 *
 * @author xy.chen
 * @date 2019-09-09
 **/
public interface VideoShareService {

    //获取分享视频相关信息
    Map<String, Object> getShareVideos(String type, String videoId, String userId,String videoType,String catId);
}
