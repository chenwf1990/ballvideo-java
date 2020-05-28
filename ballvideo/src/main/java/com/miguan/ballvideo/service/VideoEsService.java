package com.miguan.ballvideo.service;

import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.dto.VideoGatherParamsDto;
import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import com.miguan.ballvideo.vo.video.Videos161Vo;

import java.util.List;
import java.util.Map;

/**
 * Created by shixh on 2020/1/7.
 */
public interface VideoEsService {

    ResultMap deleteIndex(String index);

    Object init();

    void init(String sqlBuffer);

    Object search(String title,String userId,VideoGatherParamsDto params);

    Object update(String videoIds, String options);

    Object updateByGatherId(Long gatherId,String videoIds);

    void deleteOrCloseGather(long gatherId, int state);

    void deleteDueVideos();

    FirstVideoEsVo getById(Long id);

    void save(FirstVideoEsVo vo);

    Object updateByGatgherId(Long gatherId);

    Object getMyGatherVidesoById(Long videoId);

    //首页视频推荐接口/视频详情
    List<Videos161Vo> query(Map<String, Object> params);

    //首页视频非推荐接口
    List<Videos161Vo> query(Map<String, Object> params, int num);

}
