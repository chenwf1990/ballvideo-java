package com.miguan.ballvideo.common.enums;

/** 视频以及集合的操作标识*/
public enum VideoESOptions {
    videoAdd("videoAdd"),//视频新增
    videoDelete("videoDelete"),//视频删除
    gatherAddOrDelete("gatherAddOrDelete"),//合集新增/删除视频
    gatherDeleteOrClose("gatherDeleteOrClose"),//合集删除/关闭视频
    deleteDueVideos("deleteDueVideos"),//删除过期视频
    initVideo("initVideo");//初始化视频
    VideoESOptions(String code){
        this.code = code;
    }
    private String code;

}
