package com.miguan.ballvideo.service.impl;

import com.miguan.ballvideo.entity.MarketAudit;
import com.miguan.ballvideo.mapper.VideosCatMapper;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.service.MarketAuditService;
import com.miguan.ballvideo.service.RedisService;
import com.miguan.ballvideo.service.VideosCatService;
import com.miguan.ballvideo.vo.VideosCatVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 首页视频分类表ServiceImpl
 *
 * @author xy.chen
 * @date 2019-08-09
 **/

@Service("videosCatService")
public class VideosCatServiceImpl implements VideosCatService {

	@Resource
	private VideosCatMapper videosCatMapper;

    @Resource
    private MarketAuditService  marketAuditService;

    @Resource
    private RedisService redisService;

    @Override
    public List<VideosCatVo> findFirstVideosCatList(String channelId, String appVersion) {
        Map<String, Object> params = new HashMap<>();
        params.put("state", "1");//状态 2关闭 1开启
        params.put("type", "10");//类型 10首页视频 20 小视频
        marketAudit(channelId, appVersion, params,"");
        return videosCatMapper.findFirstVideosCatList(params);
    }

    @Override
    public Map<String, Object> findFirstVideosCatList18(String channelId, String appVersion,String teenagerModle) {
        Map<String, Object> params = new HashMap<>();
        params.put("state", "1");//状态 2关闭 1开启
        params.put("type", "10");//类型 10首页视频 20 小视频
        String excludeCatIds = marketAudit(channelId, appVersion, params,teenagerModle);
        List<VideosCatVo> firstVideosCatList = videosCatMapper.findFirstVideosCatList(params);
        Map<String, Object> result = new HashMap<>();
        result.put("firstVideosCatList", firstVideosCatList);
        result.put("excludeCatIds", excludeCatIds);
        return result;
    }

    @Override
    public List<String> getCatIdsByStateAndType(int state, String type) {
        String key = RedisKeyConstant.CAT_IDS+state+"_"+type;
        String catIds = redisService.get(key);
        if(StringUtils.isNotEmpty(catIds))return new ArrayList<>(Arrays.asList(catIds.split(",")));
        List<String> catIds_db = videosCatMapper.getCatIdsByStateAndType(state,type);
        if(CollectionUtils.isNotEmpty(catIds_db)){
            redisService.set(key,String.join(",",catIds_db),RedisKeyConstant.CAT_IDS_SECONDS);
        }
        return catIds_db;
    }

    //市场屏蔽分类标签+青少年模式屏蔽分类标签
    private String marketAudit(String channelId, String appVersion, Map<String, Object> params,String teenagerModle) {
        //2.0.0新增青少年模式，如果用户开启&&后台也开启，则过滤
        if("1".equals(teenagerModle)){
            String otherCatIds = marketAuditService.getCatIdsByChannelIdAndAppVersionFromTeenager(channelId, appVersion);
            if(StringUtils.isNotEmpty(otherCatIds)){
                params.put("excludeCatIds", Arrays.asList(otherCatIds.split(",")));
                return otherCatIds;
            }
        }
        String catIds="";
        //根据渠道和版本号进行市场屏蔽
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(channelId, appVersion);
        if(marketAudit!=null && StringUtils.isNotEmpty(marketAudit.getCatIds())){
            catIds = marketAudit.getCatIds();
            params.put("excludeCatIds", Arrays.asList(catIds.split(",")));
        }
        return catIds;
    }

}