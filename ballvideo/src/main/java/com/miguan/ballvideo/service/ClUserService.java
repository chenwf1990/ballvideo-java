package com.miguan.ballvideo.service;

import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.vo.ClUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户表Service
 * @author xy.chen
 * @date 2019-08-09
 **/

public interface ClUserService {

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

	/**
	 * 用户登录
	 * @param request
	 * @param clUserVo 用户实体
	 * @param vcode 短信验证码
	 * @return
	 */
	Map<String, Object>  login(HttpServletRequest request, ClUserVo clUserVo, String vcode);

	/**
	 * 获取全部华为推送的tokens
	 * @param pushArticle
	 */
	List<String> findAllHuaweiTotken(PushArticle pushArticle);

	/**
	 * 获取全部小米推送的tokens
	 * @param pushArticle
	 */
	List<String> findAllXiaoMiTotken(PushArticle pushArticle);

    void deleteByUserId(Long userId);

	public List<String> findAllOppoToken();

	/**
	 * 获取全部推送用户的tokens
	 */
	List<ClUserVo> findAllTokens();
}