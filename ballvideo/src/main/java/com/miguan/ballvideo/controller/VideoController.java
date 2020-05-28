package com.miguan.ballvideo.controller;

import com.github.pagehelper.Page;
import com.miguan.ballvideo.common.aop.AccessLimit;
import com.miguan.ballvideo.common.aop.CommonParams;
import com.miguan.ballvideo.common.aop.RequestCache;
import com.miguan.ballvideo.common.aop.ServiceLock;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.constants.VideoContant;
import com.miguan.ballvideo.common.interceptor.argument.params.CommonParamsVo;
import com.miguan.ballvideo.common.util.*;
import com.miguan.ballvideo.dto.VideoDetailDto;
import com.miguan.ballvideo.dto.VideoParamsDto;
import com.miguan.ballvideo.service.*;
import com.miguan.ballvideo.vo.FirstVideos;
import com.miguan.ballvideo.vo.SmallVideosVo;
import com.miguan.ballvideo.vo.VideosCatVo;
import com.miguan.ballvideo.vo.video.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频Controller
 *
 * @Author xy.chen
 * @Date 2019/8/9
 **/
@Slf4j
@Api(value="视频接口",tags={"视频接口"})
@RestController
public class VideoController {

    @Resource
    private VideosCatService videosCatService;

    @Resource
    private FirstVideosService firstVideosService;

    @Resource
    private FirstVideosOldService firstVideosOldService;

    @Resource
    private SmallVideosService smallVideosService;

    @Resource
    private VideoShareService videoShareService;

    /**
     * 首页视频分类
     *
     * @return
     */
    @ApiOperation(value = "首页视频分类")
    @PostMapping("/api/video/firstVideosCatList")
    public ResultMap firstVideosCatList(@ApiParam("渠道ID")String channelId,
                                        @ApiParam("版本号")String appVersion) {
        List<VideosCatVo> videosCatList = videosCatService.findFirstVideosCatList(channelId,appVersion);
        return ResultMap.success(videosCatList);
    }

    /**
     * 首页视频分类
     *
     * @return
     */
    @ApiOperation(value = "首页视频分类V1.8.0")
    @PostMapping("/api/video/firstVideosCatList18")
    public ResultMap firstVideosCatList18(@ApiParam("渠道ID")String channelId,
                                          @ApiParam("版本号")String appVersion,
                                          @ApiParam("是否开启青少年模式：0-未开启，1-开启")String teenagerModle) {
        return ResultMap.success(videosCatService.findFirstVideosCatList18(channelId, appVersion,teenagerModle));
    }

    /**
     * 首页视频源列表
     *
     * @param catId
     * @param videoType
     * @param currentPage
     * @param pageSize
     * @return
     */
    @RequestCache
    @ApiOperation(value = "首页视频源列表")
    @PostMapping("/api/video/firstVideosList")
    public ResultMap firstVideosList(HttpServletRequest request,
                                     @ApiParam("视频分类ID") String catId,
                                     @ApiParam("用户ID") String userId,
                                     @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                     @ApiParam("当前页面") int currentPage,
                                     @ApiParam("每页条数") int pageSize,
                                     @ApiParam("广告位置类型,空则查询全部")String positionType,
                                     @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                     @ApiParam("渠道ID")String channelId,
                                     @ApiParam("作用包") String appPackage) {
        Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("userId", userId);
        params.put("state", "1");//状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType == null ? "2" : mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        Page<FirstVideos> page = firstVideosOldService.findFirstVideosPage(params, currentPage, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(page));
        result.put("data", page);
        return ResultMap.success(result);
    }

    /**
     * 首页视频播放详情
     *
     * @param request
     * @param id
     * @return
     */
    @ApiOperation(value = "首页视频播放详情")
    @PostMapping("/api/video/firstVideosDetail")
    public ResultMap firstVideosDetail(HttpServletRequest request,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("视频ID") String id,
                                       @ApiParam("广告位置类型,空则查询全部")String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                       @ApiParam("渠道ID")String channelId,
                                       @ApiParam(value = "app版本号") String appVersion,
                                       @ApiParam(value = "0没有开启，1开启")String permission,
                                       @ApiParam(value = "作用包") String appPackage) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("userId", userId);
        params.put("state", "1");//状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.getChannelId(channelId, mobileType));
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("appVersion", VersionUtil.getVersion(appVersion));
        params.put("permission", permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        List<FirstVideos> firstVideosList = firstVideosOldService.findFirstVideosList(params);
        if(firstVideosList.size()>0) {
            return ResultMap.success(firstVideosList.get(0));
        } else {
            return ResultMap.error("视频查询为空。");
        }
    }

    /**
     * 首页视频详情列表
     *
     * @param request
     * @param id
     * @param currentPage
     * @param pageSize
     * @return
     */
    @RequestCache
    @ApiOperation(value = "首页视频详情列表")
    @PostMapping("/api/video/firstVideosDetailList")
    public ResultMap firstVideosDetailList(HttpServletRequest request,
                                           @ApiParam("视频ID") String id,
                                           @ApiParam("用户ID") String userId,
                                           @ApiParam("视频分类ID") String catId,
                                           @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                           @ApiParam("当前页面") int currentPage,
                                           @ApiParam("每页条数") int pageSize,
                                           @ApiParam("广告位置类型,空则查询全部")String positionType,
                                           @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                           @ApiParam("渠道ID")String channelId,
                                           @ApiParam("作用包") String appPackage) {
        Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("userId", userId);
        params.put("excludeId", id);//排除播放视频
        params.put("state", "1");//状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        Page<FirstVideos> page = firstVideosOldService.findFirstVideosPage(params, currentPage, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(page));
        result.put("data", page);
        return ResultMap.success(result);
    }

    /**
     * 小视频源列表
     *
     * @param request
     * @param currentPage
     * @param pageSize
     * @return
     */
    @RequestCache
    @ApiOperation(value = "小视频源列表")
    @PostMapping("/api/video/smallVideosList")
    public ResultMap smallVideosList(HttpServletRequest request,
                                     @ApiParam("用户ID") String userId,
                                     @ApiParam("当前页面") int currentPage,
                                     @ApiParam("每页条数") int pageSize,
                                     @ApiParam("广告位置类型,空则查询全部")String positionType,
                                     @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                     @ApiParam("渠道ID")String channelId,
                                     @ApiParam("作用包") String appPackage) {
        Map<String, Object> params = new HashMap<>();
        params.put("state", Constant.open);//状态 1开启 2关闭
        params.put("userId", userId);
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        Page<SmallVideosVo> page = smallVideosService.findSmallVideosList(params, currentPage, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(page));
        result.put("data", page);
        return ResultMap.success(result);
    }

    /**
     * 小视频播放详情
     *
     * @param request
     * @param id
     * @return
     */
    @ApiOperation(value = "小视频播放详情")
    @PostMapping("/api/video/smallVideosDetail")
    public ResultMap smallVideosDetail(HttpServletRequest request,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("视频ID") String id,
                                       @ApiParam("广告位置类型,空则查询全部")String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                       @ApiParam("渠道ID")String channelId,
                                       @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启") String permission,
                                       @ApiParam(value = "app版本号") String appVersion,
                                       @ApiParam("作用包") String appPackage) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("state", "1");//状态 1开启 2关闭
        params.put("userId", userId);
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.getChannelId(channelId, mobileType));
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("appVersion", appVersion);
        params.put("permission", permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        List<SmallVideosVo> smallVideosVoList = smallVideosService.findSmallVideosList(params);
        if(smallVideosVoList.size()>0) {
            return ResultMap.success(smallVideosVoList.get(0));
        } else {
            return ResultMap.error("视频查询为空。");
        }
    }

    /**
     * 更新用户收藏数、点赞数、观看数、是否感兴趣、举报
     *
     * @param id
     * @param userId
     * @param type
     * @param opType
     * @return
     */
    @ApiOperation(value = "更新用户收藏数、点赞数、观看数、是否感兴趣、举报")
    @PostMapping("/api/video/updateCount")
    public ResultMap updateCount(@ApiParam("视频ID") @RequestParam("id") String id, @ApiParam("用户ID") String userId,
                                 @ApiParam("视频类型：10--首页视频 20--小视频") String type,
                                 @ApiParam("操作类型：10--收藏 20--点赞 30--观看 40--取消收藏 50--取消点赞 60-- 不感兴趣 70--举报 80--分享") String opType) {
        if(StringUtils.isBlank(id))return ResultMap.error("视频ID为空");
        Map<String, Object> params = new HashMap<>();
        params.put("id",id);
        params.put("opType", opType);
        params.put("userId", StringUtil.isBlank(userId)?"0":userId);
        params.put("type", type);
        boolean success = firstVideosOldService.updateVideosCountSendMQ(params);
        if (success) return ResultMap.success();
        return ResultMap.error();
    }

    @ApiOperation(value = "批量删除用户收藏")
    @PostMapping("/api/video/batchDelCollections")
    public ResultMap batchDelCollections(@ApiParam("收藏ID 逗号隔开") String collectionIds) {
        if (StringUtils.isBlank(collectionIds)) return ResultMap.error("操作失败，缺少参数！");
        String[] split = collectionIds.split(",");
        int num = firstVideosOldService.batchDelCollections(split);
        if (num > 0) {
            return ResultMap.success();
        } else {
            return ResultMap.error();
        }
    }

    /**
     * 查看我的收藏
     *
     * @param userId
     * @param
     * @return
     */
    @ApiOperation(value = "查看我的收藏")
    @PostMapping("/api/video/findMyCollection")
    public ResultMap findMyCollection(@ApiParam("用户ID") String userId,
                                      @ApiParam("当前页面") int currentPage,
                                      @ApiParam("每页条数") int pageSize) {
        Page<FirstVideos> page = firstVideosOldService.findMyCollection(userId, currentPage, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(page));
        result.put("data", page);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "分享内容")
    @PostMapping("/api/video/shareInfo")
    public ResultMap shareInfo(@ApiParam("视频类型：10--首页视频 20--小视频") String type,
                               @ApiParam("视频id") String videoId,
                               @ApiParam("视频标题") String title,
                               @ApiParam("视频图片地址）") String bsyImgUrl,
                               @ApiParam("用户ID") String userId,
                               @ApiParam("首页视频类型：10--推荐 20--其他分类") String videoType,
                               @ApiParam("首页视频其他分类ID") String catId,
                               @ApiParam("包名") String appPackage) {
        Map<String, Object> result = new HashMap<>();
        result.put("shareTitle", title);
        result.put("imgUrl", bsyImgUrl);
        result.put("shareContent", Global.getValue("share_content"));
        String shareH5 = Global.getValue(appPackage+"_shareH5");
        if ("10".equals(type)) {
            if (StringUtils.isNotEmpty(shareH5)) {
                result.put("shareUrl", shareH5 + "?type=" + type + "&videoId=" + videoId + "&userId=" + userId
                        + "&videoType=" + videoType + "&catId=" + catId+"&appPackage="+appPackage);
            } else {
                shareH5 = Global.getValue("com.mg.xyvideo_shareH5");//原来西柚默认地址
                result.put("shareUrl", shareH5 + "?type=" + type + "&videoId=" + videoId + "&userId=" + userId
                        + "&videoType=" + videoType + "&catId=" + catId+"&appPackage="+appPackage);
            }
        }else{
            result.put("shareUrl", Global.getValue("share_url") + "/share/share.html?type=" + type + "&videoId=" + videoId + "&userId=" + userId+"&appPackage="+appPackage);
        }

        return ResultMap.success(result);
    }

    @ApiOperation(value = "获取分享视频相关信息")
    @AccessLimit(seconds = 60,maxCount = 40)
    @PostMapping("/api/video/getShareVideos")
    public ResultMap getShareVideos(@ApiParam("视频类型：10--首页视频 20--小视频") String type,
                                    @ApiParam("视频id") String videoId, @ApiParam("用户ID") String userId,
                                    @ApiParam("首页视频类型：10--推荐 20--其他分类") String videoType,
                                    @ApiParam("首页视频其他分类ID") String catId) {
        Map<String, Object> result = videoShareService.getShareVideos(type, videoId, userId, videoType, catId);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "随机获取视频和广告")
    @PostMapping("/api/video/getRandomVideosAndAdvert")
    public ResultMap getRandomVideosAndAdvert(@ApiParam("视频类型：10--首页视频 20--小视频") String type,
                                              @ApiParam("首页视频其他分类ID") String catId,
                                              @ApiParam("查询数量") String queryNumber,
                                              @ApiParam("广告位置类型,空则查询全部")String positionType,
                                              @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                              @ApiParam("手机类型：1-ios，2：安卓")String mobileType,
                                              @ApiParam("渠道ID")String channelId,
                                              @ApiParam("0没有开启，1开启")String permission,
                                              @ApiParam("app版本号") String appVersion,
                                              @ApiParam("作用包") String appPackage,
                                              @ApiParam("合集ID,v2.1.0") String gatherId) {
        final Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("catId", catId);
        params.put("queryNumber", queryNumber);
        params.put("positionType", positionType);
        params.put("videoType", videoType);
        params.put("mobileType", mobileType);
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("marketChannelId", channelId);
        params.put("permission", permission);
        params.put("appVersion", VersionUtil.getVersion(appVersion));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        params.put("gatherId",gatherId);
        Map<String, Object> result = firstVideosOldService.getRandomVideosAndAdvert(params);
        return ResultMap.success(result);
    }

    /**
     * 小视频源列表
     *
     * @param request
     * @param currentPage
     * @return
     */
    @RequestCache
    @ApiOperation(value = "小视频源列表(1.3.0新增)")
    @PostMapping("/api/video/smallVideosList/1.3")
    public ResultMap smallVideosList13(HttpServletRequest request,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("当前页面") int currentPage,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("flag(1-首次，0-不是首次)") int flag,
                                       @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启 ") String permission,
                                       @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        params.put("state", Constant.open);//状态 1开启 2关闭
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("permission", permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final SmallVideosNewVo smallVideosNewVo = smallVideosService.findSmallVideosList13(params, currentPage,flag);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(smallVideosNewVo.getPage()));
        result.put("data", smallVideosNewVo);
        return ResultMap.success(result);
    }

    @RequestCache
    @ApiOperation(value = "首页视频源列表(1.3.0新增)")
    @PostMapping("/api/video/firstVideosList/1.3")
    public ResultMap firstVideosList13(HttpServletRequest request,
                                       @ApiParam("视频分类ID") String catId,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                       @ApiParam("当前页面") int currentPage,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("flag(1-首次，0-不是首次)") int flag,
                                       @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("videoType", videoType);
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("state", Constant.open); // 状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideosNewVo firstVideosNewVo = firstVideosOldService.firstVideosList13(params, currentPage, flag);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(firstVideosNewVo.getPage()));
        result.put("data", firstVideosNewVo);
        return ResultMap.success(result);
    }

    @RequestCache
    @ApiOperation(value = "首页视频详情列表(1.3.0新增)")
    @PostMapping("/api/video/firstVideosDetailList/1.3")
    public ResultMap firstVideosDetailList13(HttpServletRequest request,
                                             @ApiParam("视频ID") String id,
                                             @ApiParam("用户ID") String userId,
                                             @ApiParam("视频分类ID") String catId,
                                             @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                             @ApiParam("当前页面") int currentPage,
                                             @ApiParam("广告位置类型,空则查询全部") String positionType,
                                             @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                             @ApiParam("渠道ID") String channelId,
                                             @ApiParam("flag(1-首次，0-不是首次)") int flag,
                                             @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("videoType", videoType);
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        if (StringUtils.isNotBlank(id)) {
            params.put("excludeId", Integer.parseInt(id));//排除播放视频
        }
        params.put("state", Constant.open);//状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideosNewVo firstVideosNewVo = firstVideosOldService.findFirstVideosPage13(params, currentPage, flag);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(firstVideosNewVo.getPage()));
        result.put("data", firstVideosNewVo);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "首页视频列表(1.6.0新增)")
    @PostMapping("/api/video/firstVideosList/1.6")
    public ResultMap firstVideosList16(@ApiParam("视频分类ID") String catId,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("videoType", videoType);
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("state", Constant.open); // 状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideos16Vo firstVideosNewVo = firstVideosOldService.firstVideosList16(params);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(firstVideosNewVo.getPage()));
        result.put("data", firstVideosNewVo);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "首页视频推荐列表(1.6.0新增)")
    @PostMapping("/api/video/firstRecommendVideosList/1.6")
    public ResultMap firstRecommendVideosList16(@ApiParam("用户ID") String userId,
                                                @ApiParam("设备ID") String deviceId,
                                                @ApiParam("广告位置类型,空则查询全部") String positionType,
                                                @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                                @ApiParam("渠道ID") String channelId,
                                                @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("deviceId", deviceId);
        params.put("state", Constant.open); // 状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("channelId", ChannelUtil.filter(channelId));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideos16Vo firstVideosNewVo = firstVideosOldService.firstRecommendVideosList16(params);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(firstVideosNewVo.getPage()));
        result.put("data", firstVideosNewVo);
        return ResultMap.success(result);
    }

    @RequestCache
    @ApiOperation(value = "首页视频详情列表(1.6.0新增)")
    @PostMapping("/api/video/firstVideosDetailList/1.6")
    public ResultMap firstVideosDetailList16(HttpServletRequest request,
                                             @ApiParam("视频ID") String id,
                                             @ApiParam("用户ID") String userId,
                                             @ApiParam("视频分类ID") String catId,
                                             @ApiParam("类型：10--推荐 20--其他分类") String videoType,
                                             @ApiParam("当前页面") int currentPage,
                                             @ApiParam("广告位置类型,空则查询全部") String positionType,
                                             @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                             @ApiParam("渠道ID") String channelId,
                                             @ApiParam("flag(1-首次，0-不是首次)") int flag,
                                             @ApiParam("app版本号") String appVersion,
                                             @ApiParam("permission") String permission,
                                             @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if ("20".equals(videoType)) {
            params.put("catId", catId);
        }
        params.put("videoType", videoType);
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        if (StringUtils.isNotBlank(id)) {
            params.put("excludeId", Integer.parseInt(id));//排除播放视频
        }
        params.put("state", Constant.open);//状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId",channelId);
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("appVersion",VersionUtil.getVersion(appVersion));
        params.put("permission",permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideos16Vo firstVideosNewVo = firstVideosOldService.findFirstVideosPage16(params, currentPage, flag);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(firstVideosNewVo.getPage()));
        result.put("data", firstVideosNewVo);
        return ResultMap.success(result);
    }

    /**
     * 小视频源列表
     *
     * @param request
     * @param currentPage
     * @return
     */
    @RequestCache
    @ApiOperation(value = "小视频源列表(1.6.0新增)")
    @PostMapping("/api/video/smallVideosList/1.6")
    public ResultMap smallVideosList16(HttpServletRequest request,
                                       @ApiParam("用户ID") String userId,
                                       @ApiParam("当前页面") int currentPage,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("是否是推送") String pushType,
                                       @ApiParam("视频id") String videoId,
                                       @ApiParam("flag(1-首次，0-不是首次)") int flag,
                                       @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启") String permission,
                                       @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        params.put("state", Constant.open);//状态 1开启 2关闭
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        if (StringUtils.isNotBlank(pushType)) {
            params.put("pushType", Integer.parseInt(pushType));
        }
        if (StringUtils.isNotBlank(videoId)) {
            params.put("videoId", Integer.parseInt(videoId));
        }
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        if(permission==null){
            permission = "1";
        }
        params.put("permission", permission);
        params.put("marketChannelId", channelId);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final SmallVideos16Vo smallVideosNewVo = smallVideosService.findSmallVideosList16(params, currentPage, flag);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(smallVideosNewVo.getPage()));
        result.put("data", smallVideosNewVo);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "首页视频非推荐列表(1.6.1新增)")
    @PostMapping("/api/video/firstVideosList/1.6.1")
    public ResultMap firstVideosList161(@ApiParam("视频分类ID") @RequestParam String catId,
                                        @ApiParam("设备ID") String deviceId,
                                        @ApiParam("用户ID") String userId,
                                        @ApiParam("广告位置类型,空则查询全部") String positionType,
                                        @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                        @ApiParam("渠道ID") String channelId,
                                        @ApiParam("版本号") String appVersion,
                                        @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启 ") String permission,
                                        @ApiParam("作用包") String appPackage) {
        if(StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(appPackage) || StringUtils.isEmpty(appVersion) || StringUtils.isEmpty(catId)){
            return ResultMap.error("参数异常");
        }
        final Map<String, Object> params = getParamsMap(catId, deviceId, userId, positionType, mobileType, channelId, appVersion, permission, appPackage);
        final FirstVideos161Vo firstVideosNewVo = firstVideosService.firstVideosList161(params);
        return ResultMap.success(firstVideosNewVo);
    }

    @ApiOperation(value = "首页视频推荐列表(1.6.1新增)")
    @PostMapping("/api/video/firstRecommendVideosList/1.6.1")
    public ResultMap firstRecommendVideosList161(@ApiParam("用户ID") String userId,
                                                 @ApiParam("设备ID") String deviceId,
                                                 @ApiParam("广告位置类型,空则查询全部") String positionType,
                                                 @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                                 @ApiParam("渠道ID") String channelId,
                                                 @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启 ") String permission,
                                                 @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("deviceId", deviceId);
        params.put("state", Constant.open); // 状态 1开启 2关闭
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("marketChannelId", ChannelUtil.filterChannelId(channelId));
        params.put("permission", permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final FirstVideos161Vo firstVideosNewVo = firstVideosService.firstRecommendVideosList161(params);
        return ResultMap.success(firstVideosNewVo);
    }


    /**
     * 小视频源列表
     *
     */
    @ApiOperation(value = "小视频源列表(1.7.0新增)")
    @PostMapping("/api/video/smallVideosList/1.7")
    public ResultMap smallVideosList17(@ApiParam("用户ID") String userId,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("排除已浏览视频ID") String excludeIds,
                                       @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启") String permission,
                                       @ApiParam("作用包") String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        params.put("state", Constant.open);//状态 1开启 2关闭
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        //权限判断只针对安卓
        if ("2".equals(mobileType)){
            params.put("permission", permission);
        }
        params.put("excludeIds", excludeIds);
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", channelId);
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        final SmallVideos16Vo smallVideosNewVo = smallVideosService.findSmallVideosList17(params);
        final Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(smallVideosNewVo.getPage()));
        result.put("data", smallVideosNewVo);
        return ResultMap.success(result);
    }

    //@ServiceLock
    //@ConcurrentLock(lockTime=5000)
    @ApiOperation(value = "首页视频推荐列表(1.8新增)")
    @PostMapping("/api/video/firstRecommendVideosList/1.8")
    public FirstVideos161Vo firstRecommendVideosList18(@Validated VideoParamsDto dto) {
        FirstVideos161Vo firstVideos161Vo = firstVideosService.firstRecommendVideosList18(dto);
        return firstVideos161Vo;
    }

    /**
     * 小视频源列表
     *
     */
    @ApiOperation(value = "小视频源列表(1.8.0新增)")
    @PostMapping("/api/video/smallVideosList/1.8")
    public ResultMap smallVideosList18(@ApiParam("用户ID") String userId,
                                       @ApiParam("广告位置类型,空则查询全部") String positionType,
                                       @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                       @ApiParam("渠道ID") String channelId,
                                       @ApiParam("排除已浏览视频ID") String excludeIds,
                                       @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启") String permission,
                                       @ApiParam("app版本号") String appVersion,
                                       @ApiParam("作用包") String appPackage,
                                       @ApiParam("马甲包类型") String appType) {
        final Map<String, Object> params = new HashMap<>();
        params.put("state", Constant.open);//状态 1开启 2关闭
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        //权限判断只针对安卓
        if ("2".equals(mobileType)){
            params.put("permission", permission);
        }
        params.put("excludeIds", excludeIds);
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("marketChannelId", channelId);
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("appVersion", VersionUtil.getVersion(appVersion));
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        params.put("appType",appType);
        SmallVideos16Vo smallVideosNewVo = smallVideosService.findSmallVideosList17(params);
        return ResultMap.success(smallVideosNewVo);
    }

    @ServiceLock
    @ApiOperation(value = "青少年模式首页推荐接口")
    @PostMapping("/api/video/findRecommendByTeenager")
    public ResultMap findRecommendByTeenager(@CommonParams CommonParamsVo params) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("userId",params.getUserId());
        map.put("appPackage",params.getAppPackage());
        map.put("appVersion",params.getAppVersion());
        map.put("channelId",ChannelUtil.filterChannelId(params.getChannelId()));
        FirstVideos161Vo video = firstVideosService.findRecommendByTeenager(map);
        return ResultMap.success(video);
    }

    @ServiceLock
    @ApiOperation(value = "青少年模式首页非推荐接口")
    @PostMapping("/api/video/findNoRecommendByTeenager")
    public ResultMap findNoRecommendByTeenager(@CommonParams CommonParamsVo params,@ApiParam("分类ID") String catId) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("userId",params.getUserId());
        map.put("appPackage",params.getAppPackage());
        map.put("appVersion",params.getAppVersion());
        map.put("channelId",ChannelUtil.filterChannelId(params.getChannelId()));
        map.put("catId",catId);
        FirstVideos161Vo video = firstVideosService.findNoRecommendByTeenager(map);
        return ResultMap.success(video);
    }

  /**
   * 如果是合集视频进入，先返回合集视频数据
   * @param videoDetailDto
   * @return
   */
  @ApiOperation(value = "首页视频详情列表(2.5.0新增)")
  @PostMapping("/api/video/firstVideosDetailList/2.5")
  public FirstVideoDetailVo firstVideosDetailList25(@Validated VideoDetailDto videoDetailDto) {
      if(StringUtil.isEmpty(videoDetailDto.getAppVersion())) {
          return null;
      }
        videoDetailDto.setMarketChannelId(videoDetailDto.getChannelId());
        videoDetailDto.setChannelId( ChannelUtil.filter(videoDetailDto.getChannelId(), videoDetailDto.getMobileType()));
        final Map<String, Object> params = EntityUtils.entityToMap(videoDetailDto);
        FirstVideoDetailVo firstVideosNewVo = firstVideosService.firstVideosDetailList25(params);
        return firstVideosNewVo;
  }

    @ApiOperation(value = "首页视频推荐列表(2.6.0新增)")
    @PostMapping("/api/video/firstRecommendVideosList/2.6.0")
    public FirstVideos161Vo firstRecommendVideosList260(@Validated VideoParamsDto dto) {
        FirstVideos161Vo firstVideos161Vo = firstVideosService.firstRecommendVideosList18(dto);
        return firstVideos161Vo;
    }

    @ApiOperation(value = "首页视频非推荐列表(2.6.0新增)")
    @PostMapping("/api/video/firstVideosList/2.6.0")
    public ResultMap firstVideosList260(@ApiParam("视频分类ID") String catId,
                                        @ApiParam("设备ID") String deviceId,
                                        @ApiParam("用户ID") String userId,
                                        @ApiParam("广告位置类型,空则查询全部") String positionType,
                                        @ApiParam("手机类型：1-ios，2：安卓") String mobileType,
                                        @ApiParam("渠道ID") String channelId,
                                        @ApiParam("版本号") String appVersion,
                                        @ApiParam("是否开启储存、IMEI权限：0-未开启，1-开启 ") String permission,
                                        @ApiParam("作用包") String appPackage) {
        if(StringUtils.isEmpty(deviceId) || StringUtils.isEmpty(appPackage) || StringUtils.isEmpty(appVersion)){
            return ResultMap.error("参数异常");
        }
        final Map<String, Object> params = getParamsMap(catId, deviceId, userId, positionType, mobileType, channelId, appVersion, permission, appPackage);
        final FirstVideos161Vo firstVideosNewVo = firstVideosService.firstVideosList161(params);
        return ResultMap.success(firstVideosNewVo);
    }

    private Map<String, Object> getParamsMap(String catId, String deviceId, String userId, String positionType, String mobileType,
                                             String channelId, String appVersion, String permission, String appPackage) {
        final Map<String, Object> params = new HashMap<>();
        params.put("deviceId", deviceId);
        params.put("catId", catId);
        params.put("videoType", VideoContant.SMALL_VIDEO_CODE);
        if (StringUtils.isNotBlank(userId)) {
            params.put("userId", Integer.parseInt(userId));
        }
        params.put("state", Constant.open);
        params.put("positionType", positionType);
        params.put("mobileType", mobileType);
        params.put("appVersion", VersionUtil.getVersion(appVersion));
        params.put("marketChannelId", ChannelUtil.getChannelId(channelId, mobileType));
        params.put("channelId", ChannelUtil.filter(channelId, mobileType));
        params.put("permission", permission);
        params.put("appPackage", PackageUtil.getAppPackage(appPackage, mobileType));
        return params;
    }

}
