package com.miguan.ballvideo.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.miguan.ballvideo.mapper.ClUserOpinionMapper;
import com.miguan.ballvideo.service.ClUserOpinionService;
import com.miguan.ballvideo.vo.ClUserOpinionVo;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 用户意见反馈表ServiceImpl
 * @author xy.chen
 * @date 2019-08-09
 **/

@Service("clUserOpinionService")
public class ClUserOpinionServiceImpl implements ClUserOpinionService {

	@Resource
	private ClUserOpinionMapper clUserOpinionMapper;

	@Override
	@Transactional
	public ClUserOpinionVo getClUserOpinionById(String id) {
		ClUserOpinionVo clUserOpinionById = clUserOpinionMapper.getClUserOpinionById(id);
		clUserOpinionMapper.updateUserOpinionState(id);
		return clUserOpinionById;
	}

	@Override
	public List<ClUserOpinionVo>  findClUserOpinionList(Map<String, Object> params){
		return clUserOpinionMapper.findClUserOpinionList(params);
	}

	@Override
	public Page<ClUserOpinionVo> findClUserOpinionList(ClUserOpinionVo clUserOpinionVo, int currentPage, int pageSize) {
		Map<String, Object> params = new HashedMap();
		params.put("userId",clUserOpinionVo.getUserId());
		params.put("state",ClUserOpinionVo.PROCESSED);
		PageHelper.startPage(currentPage,pageSize);
		List<ClUserOpinionVo> clUserOpinionList = clUserOpinionMapper.findClUserOpinionList(params);
		return (Page<ClUserOpinionVo>) clUserOpinionList;
	}

	@Override
	public int saveClUserOpinion(ClUserOpinionVo clUserOpinionVo){
		return clUserOpinionMapper.saveClUserOpinion(clUserOpinionVo);
	}
}