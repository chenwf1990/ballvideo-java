package com.miguan.ballvideo.service;


import com.miguan.ballvideo.vo.AdvertCodeVo;

import java.util.List;
import java.util.Map;

/**
 * 广告Service
 * @author laiyudan
 * @date 2020-04-24
 */
public interface AdvertService {

    /**
     * 查询广告信息
     * @param param
     * @return
     */
    List<AdvertCodeVo> commonSearch(Map<String, Object> param);

    /**
     * 游戏广告查询
     *
     * @param param
     * @return
     */
    List<AdvertCodeVo> getAdertsByGame(Map<String, Object> param);

    /**
     * 锁屏广告判断
     *
     * @param param
     * @return
     */
    Map<String, Object> getLockScreenInfo(Map<String, Object> param);

    /**
     * 根据指定N个广告位置返回所有广告
     * @param param
     * @return
     */
    Map<String, List<AdvertCodeVo>> getAdversByPositionTypes(Map<String, Object> param);

    /**
     * 根据指定N个广告位置返回所有广告
     * @param appPackage
     * @return
     */
    Map<String,String> getAppIdByAppPackage(String appPackage);
}
