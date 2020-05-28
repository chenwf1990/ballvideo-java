package com.miguan.ballvideo.service;

import java.util.List;

public interface HotWordService {

    /**
     * 获取百度网址的当日前10热词
     * @param editor
     */
    void getBaiduHotWord(String editor);

    /**
     * 获取权重值前10的热词
     * @return
     */
    List<String> findHotWordInfo();

    /**
     * 获刷新redis热词缓存
     */
    void freshHotWordInfo();
}
