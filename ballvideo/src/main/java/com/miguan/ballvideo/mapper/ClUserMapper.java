package com.miguan.ballvideo.mapper;

import com.miguan.ballvideo.redis.util.CacheConstant;
import com.miguan.ballvideo.vo.ClUserVo;
import org.apache.ibatis.annotations.Select;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;

/**
 * 用户表Mapper
 * @author xy.chen
 * @date 2019-08-09
 **/

public interface ClUserMapper{

	/**
	 * 
	 * 通过条件查询用户列表
	 * 
	 **/
	List<ClUserVo>  findClUserList(Map<String, Object> params);

	/**
	 * 
	 * 新增用户信息
	 * 
	 **/
	int saveClUser(ClUserVo clUserVo);

	/**
	 * 
	 * 修改用户信息
	 * 
	 **/
	int updateClUser(ClUserVo clUserVo);

	@Select("select huawei_token from cl_user where state = '10' and huawei_token is not null and huawei_token!='' ")
	List<String> findAllHuaweiToken();

	@Select("select xiaomi_token from cl_user where state = '10' and xiaomi_token is not null and xiaomi_token!='' ")
	List<String> findAllXiaoMiToken();

	@Select("select oppo_token from cl_user where state = '10' and oppo_token is not null and oppo_token!='' ")
	List<String> findAllOppoToken();

	/**
	 * 查询所有用户的token信息
	 * @return
	 */
	@Cacheable(value = CacheConstant.FIND_All_Tokens, unless = "#result == null || #result.size()==0")
	List<ClUserVo> findAllTokens();
}