package com.miguan.ballvideo.service;

import com.miguan.ballvideo.dto.VideoParamsDto;
import com.miguan.ballvideo.vo.video.FirstVideoDetailVo;
import com.miguan.ballvideo.vo.video.FirstVideos161Vo;

import java.util.Map;

/*
* 1.6.1 后新接口往这里迁移
* */
public interface FirstVideosService {

	/**
	 * 首页非推荐接口V1.6.1
	 *
	 * @param params
	 * @return
	 */
	FirstVideos161Vo firstVideosList161(Map<String, Object> params);

	/**
	 * 首页推荐接口V1.6.1
	 *
	 * @param params
	 * @return
	 */
	FirstVideos161Vo firstRecommendVideosList161(Map<String, Object> params);

	/**
	 * 首页推荐接口V1.8
	 *
	 * @param params
	 * @return
	 */
	FirstVideos161Vo firstRecommendVideosList18(VideoParamsDto params);

	/**
	 * 首页推荐接口（青少年模式）V2.0.0
	 *
	 * @param params
	 * @return
	 */
	FirstVideos161Vo findRecommendByTeenager(Map<String,Object> params);
	/**
	 * 首页非推荐接口（青少年模式）V2.0.0
	 *
	 * @param params
	 * @return
	 */
	FirstVideos161Vo findNoRecommendByTeenager(Map<String,Object> params);

	/**
	 * 视频详情页接口V2.5以上版本
	 * @param params
	 * @return
	 */
	FirstVideoDetailVo firstVideosDetailList25(Map<String, Object> params);
}