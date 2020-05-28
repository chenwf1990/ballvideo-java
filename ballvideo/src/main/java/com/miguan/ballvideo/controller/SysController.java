package com.miguan.ballvideo.controller;

import com.miguan.ballvideo.common.aop.CommonParams;
import com.miguan.ballvideo.common.interceptor.argument.params.CommonParamsVo;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.common.util.VersionUtil;
import com.miguan.ballvideo.common.util.adv.AdvGlobal;
import com.miguan.ballvideo.rabbitMQ.util.RabbitMQConstant;
import com.miguan.ballvideo.redis.util.CacheConstant;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.service.*;
import com.miguan.ballvideo.service.impl.ToolMofangServiceImpl;
import com.miguan.ballvideo.vo.AboutUsVo;
import com.miguan.ballvideo.vo.queue.SystemQueueVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统参数Controller
 *
 * @author xy.chen
 * @version 1.0.0
 * @date 2019-06-20 10:48:24
 */

@Api(value = "系统配置Api",tags={"系统配置"})
@RestController
public class SysController {

    @Resource
    private AboutUsService aboutUsService;

    @Resource
    private ClMenuConfigService clMenuConfigService;

    @Resource
    private WarnKeywordService warnKeywordService;

    @Resource
    private ToolMofangServiceImpl toolMofangServiceImpl;

    @Resource
    private SysConfigService sysConfigService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private CachePrefetchService cachePrefetchService;

    @Resource
    private  SysService sysService;

    @Resource
    private RedisTemplate shangbaoRedisTemplate;


    /**
     * PHP开关配置点击刷新缓存调用
     *
     * @return
     */
    @ApiOperation(value = "更新服务器的缓存", notes = "更新分布式服务的每个服务器的缓存", httpMethod = "GET")
    @GetMapping("/api/system/config/reload/all")
    public ResultMap reloadAll() {
        sysConfigService.reloadAll();
        return ResultMap.success();
    }

    @ApiOperation(value = "获取系统版本配置信息")
    @PostMapping("/api/app/findSysVersionInfo.htm")
    public ResultMap findSysVersionInfo(@ApiParam("公共参数") @CommonParams CommonParamsVo commonParams) {
        String appPackage = commonParams.getAppPackage();
        String appVersion = commonParams.getAppVersion();
        String channelId = commonParams.getChannelId() == null ? "all" : commonParams.getChannelId();
        return ResultMap.success(sysConfigService.findSysVersionInfo(appPackage, appVersion,channelId));
    }

    @ApiOperation(value = "上报版本更新")
    @PostMapping("/api/app/reportSysVersionInfo.htm")
    public ResultMap reportSysVersionInfo(@ApiParam("公共参数") @CommonParams CommonParamsVo commonParams) {
        int i = sysConfigService.reportSysVersionInfo(commonParams);
        if(i == 0){
            ResultMap.error();
        }
        return ResultMap.success();
    }

    /**
     * 获取关于我们的信息
     */
    @PostMapping(value = "/api/app/findAboutUsInfo.htm")
    public ResultMap findAboutUsInfo() {
        Map<String, Object> params = new HashMap<>();
        List<AboutUsVo> aboutUsList = aboutUsService.findAboutUsList(params);
        if (aboutUsList != null && aboutUsList.size() > 0) {
            return ResultMap.success(aboutUsList.get(0));
        }
        return ResultMap.success();
    }

    //@ServiceLock
    @ApiOperation(value = "菜单栏配置信息（5分钟缓存）")
    @PostMapping(value = "/api/app/findMenuConfigInfo.htm")
    public ResultMap findMenuConfigInfo(@ApiParam("渠道ID") String channelId,
                                        @ApiParam("设备ID") String deviceId,
                                        @ApiParam("app版本") String appVersion,
                                        @ApiParam("包名") String appPackage) {
        //查询菜单栏配置信息
        return clMenuConfigService.findClMenuByAppPackageOrFilterChannel(channelId, deviceId, appVersion, appPackage);
    }

    /**
     * 敏感词配置初始化，全量更新
     */
    @PostMapping(value = "/api/app/updateSensitiveWord.htm")
    public ResultMap updateSensitiveWord() {
        warnKeywordService.initWarnKeyword();
        return ResultMap.success();
    }


    /**
     * 渠道初始化，全量更新
     */
    @ApiOperation("渠道初始化")
    @PostMapping(value = "/api/app/updateChannelId")
    public ResultMap updateChannelId() {
        toolMofangServiceImpl.ChannelInit();
        return ResultMap.success();
    }

    @ApiOperation("获取开关配置参数公共接口")
    @GetMapping(value = "/api/system/findSysConfigInfo")
    public ResultMap findSysConfigInfo(@CommonParams CommonParamsVo commonParamsVo) {
        return ResultMap.success(sysConfigService.findSysConfigInfo(commonParamsVo));
    }


    @ApiOperation("初始化视频评论信息（PHP使用）")
    @PostMapping("/api/app/updateVideosInitInfo")
    public ResultMap updateVideosInitInfo(@ApiParam("批量视频Id") String ids,
                                          @ApiParam("视频类型 10首页视频 20 小视频") String videoType) {
        if (StringUtils.isBlank(ids) || StringUtils.isBlank(videoType)) {
            return ResultMap.error("参数异常！");
        }
        //往评论服务videosComment发送消息
        rabbitTemplate.convertAndSend(RabbitMQConstant.VIDEOS_COMMENT_EXCHANGE, RabbitMQConstant.VIDEOS_COMMENT_KEY, ids + RabbitMQConstant._MQ_ + videoType);
        return ResultMap.success("正在初始化，稍后可查询结果。");
    }

    @ApiOperation("刷新市场审核开关缓存（PHP使用）")
    @PostMapping("/api/app/marketAudit/delRedis")
    public ResultMap updateMarketAuditRedis() {
        sysService.delRedis(CacheConstant.GET_CATIDS_BY_CHANNELID_AND_APPVERSION);
        sysService.delRedis(CacheConstant.GET_CATIDS_BY_CHANNELID_AND_APPVERSION_FromTeenager);
        return ResultMap.success();
    }

    @ApiOperation("刷新缓存（PHP使用）")
    @PostMapping("/api/app/delRedis")
    public ResultMap updateRedis(String PHPkey) {
        sysService.delRedis(PHPkey);
        return ResultMap.success();
    }

    @ApiOperation("文件预热接口")
    @GetMapping("/api/app/preheatBsyVideo")
    public ResultMap preheatBsyVideo() {
        return cachePrefetchService.videoCachePrefetch();
    }

    @ApiOperation("刷新广告位置配置信息缓存（PHP使用）")
    @GetMapping(value = "/api/app/updateAdConfigCache")
    public ResultMap updateAdConfigRedis() {
        sysConfigService.reloadByKey(SystemQueueVo.AdConfig_Cache);
        return ResultMap.success();
    }

    @ApiOperation("刷新价格阶梯广告位置缓存（PHP使用）")
    @GetMapping(value = "/api/app/updateAdLadderCache")
    public ResultMap updateAdLadderCache() {
        sysConfigService.reloadByKey(SystemQueueVo.AdLadderCache);
        sysService.delRedis(CacheConstant.QUERY_LADDER_ADERT_LIST);
        return ResultMap.success();
    }

    @ApiOperation("获取内存数据")
    @GetMapping(value = "/api/app/getCache")
    public ResultMap getCacheTest() {
        Map<String,Object> a = new HashMap<>();
        a.put("config",AdvGlobal.positionConfigMap);
        a.put("ladder", AdvGlobal.priceLadderMap.values());
        return ResultMap.success(a);
    }

    @ApiOperation("获取切片概率")
    @GetMapping(value = "/api/app/section/rate")
    public ResultMap getSectionTest(String adPositionId,String appVersion,String appPackage) {
        Map<String,Object> result = new HashMap<>();
        //阶梯广告的缓存清除，由updateAdLadderCache实现
        //sysService.delRedis(CacheConstant.QUERY_LADDER_ADERT_LIST);//删除阶梯广告缓存
        boolean high = VersionUtil.isHigh(appVersion, 2.4);//判断是否大于2.4版本
        String keyPrefix;
        if (high){
            keyPrefix = RedisKeyConstant.KEY_AD_RATE + RedisKeyConstant.KEY_AD_HIGH + appPackage;
        }else {
            keyPrefix = RedisKeyConstant.KEY_AD_RATE + RedisKeyConstant.KEY_AD_LOW + appPackage;
        }
        Object m = shangbaoRedisTemplate.opsForValue().get(keyPrefix+ 1 + adPositionId);//穿山甲
        Object n = shangbaoRedisTemplate.opsForValue().get(keyPrefix + 0 + adPositionId);//广点通
        Object k = shangbaoRedisTemplate.opsForValue().get(keyPrefix + 2 + adPositionId);//98度广告
        result.put("穿山甲概率", m);
        result.put("广点通概率", n);
        result.put("98度广告概率", k);
        return ResultMap.success(result);
    }

    @ApiOperation("清除所有广告的缓存（协助测试调用）")
    @GetMapping(value = "/api/app/clearAdvCache")
    public ResultMap clearAdvCache() {
        sysService.delRedis(CacheConstant.QUERY_ADERT_LIST);
        sysService.delRedis(CacheConstant.QUERY_ADERT_LIST_ALL);
        sysService.delRedis(CacheConstant.POSITION_TYPE_GAME);
        return ResultMap.success();
    }
}
