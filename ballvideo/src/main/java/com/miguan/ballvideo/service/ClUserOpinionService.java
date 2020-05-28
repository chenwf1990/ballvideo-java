package com.miguan.ballvideo.service;

import com.miguan.ballvideo.vo.ClUserOpinionVo;
import io.swagger.annotations.ApiParam;
import com.github.pagehelper.Page;

import java.util.List;
import java.util.Map;

/**
 * 用户意见反馈表Service
 * @author xy.chen
 * @date 2019-08-09
 **/

public interface ClUserOpinionService {

	/**
	 *
	 * 通过ID查询用户意见反馈信息
	 *
	 **/
	ClUserOpinionVo  getClUserOpinionById(String id);

	/**
	 *
	 * 通过条件查询用户意见反馈列表
	 *
	 **/
	List<ClUserOpinionVo>  findClUserOpinionList(Map<String, Object> params);

	/**
	 *
	 * 通过条件查询用户意见反馈列表
	 *	Add  方法的重写  HYL  2019年9月25日10:12:54
	 **/
	Page<ClUserOpinionVo>  findClUserOpinionList(ClUserOpinionVo clUserOpinionVo,int currentPage,int pageSize);

	/**
	 *
	 * 新增用户意见反馈信息
	 *
	 **/
	int saveClUserOpinion(ClUserOpinionVo clUserOpinionVo);
}