package com.miguan.ballvideo.service.impl;

import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.constants.VideoContant;
import com.miguan.ballvideo.common.util.*;
import com.miguan.ballvideo.common.util.adv.AdvUtils;
import com.miguan.ballvideo.common.util.video.VideoSQLUtils;
import com.miguan.ballvideo.common.util.video.VideoUtils;
import com.miguan.ballvideo.dto.VideoParamsDto;
import com.miguan.ballvideo.dynamicquery.DynamicQuery;
import com.miguan.ballvideo.entity.MarketAudit;
import com.miguan.ballvideo.entity.UserLabel;
import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import com.miguan.ballvideo.mapper.FirstVideosMapper;
import com.miguan.ballvideo.mapper.VideosCatMapper;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.service.*;
import com.miguan.ballvideo.vo.AdvertCodeVo;
import com.miguan.ballvideo.vo.AdvertVo;
import com.miguan.ballvideo.vo.video.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页视频源列表ServiceImpl
 * 1.6.1新接口放这里迁移
 */
@Service("firstVideosService")
@Slf4j
public class FirstVideosServiceImpl implements FirstVideosService {

    @Resource
    private FirstVideosMapper firstVideosMapper;

    @Resource
    private AdvertOldService advertOldService;

    @Resource
    private VideoCacheService videoCacheService;

    @Resource(name="redisDB8Service")
    private RedisDB8Service redisDB8Service;

    @Resource
    private VideosCatMapper videosCatMapper;

    @Resource
    private UserLabelService userLabelService;

    @Resource
    private VideoGatherService videoGatherService;

    @Resource
    private DynamicQuery dynamicQuery;

    @Resource
    private MarketAuditService marketAuditService;

    @Resource
    private VideoEsService videoEsService;

    @Resource
    private AdvertService  advertService;

    private List<Videos161Vo> getRecommendVideos(Map<String, Object> params, Long catId1, Long catId2) {
        final List<Videos161Vo> firstVideosAll = new ArrayList<Videos161Vo>();
        List<Videos161Vo> firstVideosList1;
        List<Videos161Vo> firstVideosList2;
        List<Videos161Vo> firstVideosList3;
        final String userId = MapUtils.getString(params, "userId");
        //获取第一标签随机视频
        final int firstLableValue = Global.getInt("first_label_value");//用户第一标签条数
        params.put("catId", catId1);
        if (userId == null || userId.equals("0")) {
            firstVideosList1 = videoCacheService.getFirstVideos161(params, firstLableValue);
        } else {
            params.put("queryNumber", firstLableValue);
            firstVideosList1 = firstVideosMapper.findFirstVideosListByUserId161(params);
        }
        firstVideosAll.addAll(firstVideosList1);

        //获取第二标签随机视频
        final int secondLableValue = Global.getInt("second_label_value");//用户第二标签条数
        params.remove("queryNumber");
        params.put("catId", catId2);
        if (userId == null || userId.equals("0")) {
            firstVideosList2 = videoCacheService.getFirstVideos161(params, secondLableValue);
        } else {
            params.put("queryNumber", secondLableValue);
            firstVideosList2 = firstVideosMapper.findFirstVideosListByUserId161(params);
        }
        firstVideosAll.addAll(firstVideosList2);

        //获取其他类型随机视频
        final int otherLableValue = Global.getInt("other_label_value");//用户第三标签条数
        params.remove("queryNumber");
        params.remove("catId");
        final List<Long> list = new ArrayList<>();
        list.add(catId1);
        list.add(catId2);

        //1.华为魅族过滤channelId  不展示美女，生活 2.ios不展示美女
        final String marketChannelId = MapUtils.getString(params, "marketChannelId");
        String videosMarketScreen = Global.getValue("videos_market_screen");
        String[] split = videosMarketScreen.split(",");
        for (int i = 0; i < split.length; i++) {
            if (split[i].equals(marketChannelId)) {
                list.add(Long.valueOf("3"));
                list.add(Long.valueOf("251"));
                break;
            }
        }
        params.put("otherCatIds", list);

        if (userId == null || userId.equals("0")) {
            firstVideosList3 = videoCacheService.getFirstVideos161(params, otherLableValue);
        } else {
            params.put("queryNumber", otherLableValue);
            firstVideosList3 = firstVideosMapper.findFirstVideosListByUserId161(params);
        }
        firstVideosAll.addAll(firstVideosList3);
        return firstVideosAll;
    }

    @Override
    public FirstVideos161Vo firstVideosList161(Map<String, Object> params) {
        int state = Global.getInt("use_EsSearch_state2");//Global.getInt("use_EsSearch_state");//0-使用DB查询，1-使用ES查询
        final FirstVideos161Vo firstVideosNewVo = new FirstVideos161Vo();
        List<Videos161Vo> firstVideos;
        final int pageNumber = Global.getInt("page_number");
        final String userId = MapUtils.getString(params, "userId");
        //V2.5.6 根据市场审核开关,屏蔽合集
        String appVersion = MapUtils.getString(params, "appVersion");
        String channelId = MapUtils.getString(params, "channelId");
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(channelId, appVersion);
        if(marketAudit!=null && StringUtils.isNotBlank(marketAudit.getGatherIds())){
            params.put("gatherIds", marketAudit.getGatherIds());
        }
        //未登录用户使用ES查询 add shixh 0331
        if (userId == null || userId.equals("0")) {
            if (state == 1) {
                log.info("use es search");
                firstVideos = videoEsService.query(params, pageNumber);
            } else {
                firstVideos = videoCacheService.getFirstVideos161(params, pageNumber);
            }
        } else {
            params.put("queryNumber", pageNumber);
            firstVideos = firstVideosMapper.findFirstVideosListByUserId161(params);
        }
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            videoCacheService.fillParams(firstVideos);
            //V2.1.0
            //如果大于2.5版本，则不返回集合其余信息
            if(VersionUtil.isHigh(VersionUtil.GATHER_VERSION,params.get("appVersion").toString())){
                //2.1.0新增视频合集展示
                fillGatherVideos(firstVideos);
            }
        }
        //是否返回广告
        if (VersionUtil.isBetween(Constant.APPVERSION_253, Constant.APPVERSION_257, params.get("appVersion").toString()) && Constant.ANDROID.equals(params.get("mobileType").toString())) {
            List<FirstVideosVo> firstVideosVos = VideoUtils.getFirstVideosVos(firstVideos);
            firstVideosNewVo.setFirstVideosVos(firstVideosVos);
        } else {
            params.remove("state");
            boolean newFlag = VersionUtil.isHigh(appVersion, Constant.COMPARE_APPVERSION);
            if (newFlag) {
                //V2.5.0走新广告逻辑
                packagNewAdvertAndVideos(firstVideosNewVo, firstVideos, params);
            } else {
                List<AdvertVo> advertList = advertOldService.getAdvertsBySection(params);
                VideoUtils.packagAdvertAndVideos(firstVideosNewVo, firstVideos, advertList, appVersion);
            }
        }
        return firstVideosNewVo;
    }

    @Override
    public FirstVideos161Vo firstRecommendVideosList161(Map<String, Object> params) {
        final FirstVideos161Vo firstVideosNewVo = new FirstVideos161Vo();
        final String deviceId = MapUtils.getString(params, "deviceId");
        //通过设备ID查询标签信息

        final UserLabel userLabel = userLabelService.getUserLabelByDeviceId(deviceId);
        //获取第一第二标签信息
        final Long catId1 = userLabel == null ? 0L : userLabel.getCatId1();
        final Long catId2 = userLabel == null ? 0L : userLabel.getCatId2();
        //首页推荐根据标签获取随机视频
        final List<Videos161Vo> firstVideos = getRecommendVideos(params, catId1, catId2);
        //首页视频计算点赞和观看总数
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            videoCacheService.fillParams(firstVideos);
        }
        //随机2个广告
        params.remove("queryNumber");//移除随机视频的参数，避免影响广告查询
        final List<AdvertVo> advertVos = advertOldService.queryByRandom(params, 2);
        final List<FirstVideosVo> firstVideosVos = VideoUtils.packaging(advertVos, firstVideos);
        firstVideosNewVo.setFirstVideosVos(firstVideosVos);
        return firstVideosNewVo;
    }

    public String[] getDuty(UserLabelVo userLabelVo, VideoParamsDto params) {
        UserLabel userLabel = userLabelVo.getUserLabel();
        String[] duty = userLabelVo.getDuty().split(",");
        if (StringUtils.isNotEmpty(params.getVideoDuty())) {
            duty = params.getVideoDuty().split(",");
            if (StringUtils.isNotEmpty(params.getLastCatId())) {
                String catIdsSort = userLabelVo.getUserLabel().getCatIdsSort();
                catIdsSort = VideoUtils.appendCatIds(params.getLastCatId(), catIdsSort);
                userLabel.setCatIdsSort(catIdsSort);
                duty = VideoUtils.getDefaultDuty().split(",");
            } else if ("0".equals(duty[0])) {
                //如果主标签变成0，进行主从标签切换
                userLabelService.calculateCatIdsSort(userLabel);
                duty = VideoUtils.getDefaultDuty().split(",");
            }
            userLabelService.updateDBAndRedis(userLabel);
        }
        return duty;
    }

    /**
     * 首页推荐视频1.8
     *
     * @param params
     * @return
     */
    @Override
    public FirstVideos161Vo firstRecommendVideosList18(VideoParamsDto params) {
        FirstVideos161Vo firstVideosNewVo = new FirstVideos161Vo();
        //获取用户标签
        UserLabelVo userLabelVo = userLabelService.getUserLabelVoByDeviceId(params.getDeviceId());
        //获取占比和用户标签集合
        String[] duty = getDuty(userLabelVo, params);
        String catIdsSorts = userLabelVo.getUserLabel().getCatIdsSort();
        if (StringUtil.isBlank(catIdsSorts)) {
            return firstVideosNewVo;
        }
        String[] catIdsSort = catIdsSorts.split(",");
        //查询视频数据和广告数据, 记录已经浏览ID
        List<Videos161Vo> firstVideos = getFirstRecommendVideos(params, duty, catIdsSort);
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            videoCacheService.fillParams(firstVideos);
        }
        //是否返回广告
        if (VersionUtil.isBetween(Constant.APPVERSION_253, Constant.APPVERSION_257, params.getAppVersion()) && Constant.ANDROID.equals(params.getMobileType())) {
            List<FirstVideosVo> firstVideosVos = VideoUtils.getFirstVideosVos(firstVideos);
            firstVideosNewVo.setFirstVideosVos(firstVideosVos);
        } else {
            //随机2个广告
            String channelId = params.getChannelId();
            params.setChannelId(ChannelUtil.filter(channelId));
            Map<String, Object> adverMap = new HashMap<>();
            String appVersion = params.getAppVersion();
            adverMap.put("marketChannelId", params.getChannelId());
            adverMap.put("channelId", params.getChannelId());
            adverMap.put("appVersion", appVersion);
            adverMap.put("positionType", params.getPositionType());
            adverMap.put("mobileType", params.getMobileType());
            adverMap.put("permission", params.getPermission());
            adverMap.put("appPackage", PackageUtil.getAppPackage(params.getAppPackage(), params.getMobileType()));

            boolean newFlag = VersionUtil.isHigh(appVersion, Constant.COMPARE_APPVERSION);
            if (newFlag) {
                //V2.5.0走新广告逻辑
                packagNewAdvertAndVideos(firstVideosNewVo, firstVideos, adverMap);
            } else {
                List<AdvertVo> advertList = advertOldService.getAdvertsBySection(adverMap);
                VideoUtils.packagAdvertAndVideos(firstVideosNewVo, firstVideos, advertList, appVersion);
            }
        }
        //重新返回占比给前端
        firstVideosNewVo.setVideoDuty(StringUtils.join(duty, ","));
        return firstVideosNewVo;
    }

    //V2.5.0走新广告与视频组合
    private void packagNewAdvertAndVideos(FirstVideos161Vo firstVideosNewVo, List<Videos161Vo> firstVideos, Map<String, Object> adverMap) {
        List<AdvertCodeVo> advertCodeVos = advertService.commonSearch(adverMap);
        List<FirstVideosVo> firstVideosVos = VideoUtils.packagingNewAdvert(advertCodeVos, firstVideos);
        firstVideosNewVo.setFirstVideosVos(firstVideosVos);
    }

    /**
     * 获取第二标签的分类ID，不够需要随机补齐
     * 2个情况：
     * a.用户标签数>=视频占比，直接返回，例如 用户第二标签：cat1，cat2，cat3，cat4，num=4
     * b.用户标签数<视频占比，需要补齐，例如 用户第二标签：cat1，cat2，num=4，需要补齐2个
     *
     * @param catIdsSort
     * @param num
     * @return
     */
    public List getCatIds(List<String> catIdsSort, int num) {
        List<String> bak = new ArrayList(catIdsSort);
        int size = bak.size() - 1;//去掉主标签的catID
        bak.remove(0);
        if (size >= num) {
            return bak.subList(0, num);
        } else {
            int less = num - size;
            List<String> catIds = videosCatMapper.findCatIdsNotIn(bak);
            Collections.shuffle(catIds);
            return catIds.subList(0, less);
        }
    }

    public String getCatIdsByNum(List<String> catIdsSort, int num) {
        List<String> catIds = catIdsSort.subList(0, num);
        String join = String.join(",", catIds);
        return join;
    }

    /**
     * 根据用户权重和视频占比查询视频
     *
     * @param params     查询参数
     * @param duty       视频占比，例如4:3:2 表示第一标签显示4个，第二标签显示3个分类各一条，第三标签显示最近3天视频，视频分类有可能包含前面二标签
     * @param catIdsSort 用户标签权重，有序，例如"catID1，catID2，catID3"，权重关系catID1>catID2>catID3
     * @return
     */
    private List<Videos161Vo> getFirstRecommendVideos(
            VideoParamsDto params, String[] duty, String[] catIdsSort) {
        List<Videos161Vo> firstVideosAll = new ArrayList<Videos161Vo>();
        boolean isOpen = false;//市场审核开关是否开启；
        boolean isLogin = StringUtils.isNotBlank(params.getUserId()) && !"0".equals(params.getUserId());
        int state = Global.getInt("use_EsSearch_state");//0-使用DB查询，1-使用ES查询
        //第三标签视频
        //先过滤市场审核开关屏蔽的分类
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(params.getChannelId(), params.getAppVersion());
        String otherCatIds = marketAudit == null || StringUtils.isEmpty(marketAudit.getCatIds()) ? "" : marketAudit.getCatIds();
        //V2.5.6 推荐列表合集屏蔽
        if(marketAudit!=null && StringUtils.isNotBlank(marketAudit.getGatherIds())){
            params.setGatherIds(marketAudit.getGatherIds());
        }
        //审核开关开启并且和用户标签有重叠
        if (StringUtils.isNotEmpty(otherCatIds) && VideoUtils.containsHiddenCatId(otherCatIds,catIdsSort)) {
            isOpen = true;
            duty = new String[]{"0", "0", "8"};
        }
        //屏蔽关闭的分类 v2.1.0
        List<String> catIds = videosCatMapper.findCatIdsByState(2);
        if (CollectionUtils.isNotEmpty(catIds)) {
            if (StringUtils.isNotEmpty(otherCatIds)) {
                otherCatIds += "," + String.join(",", catIds);
            } else {
                otherCatIds = String.join(",", catIds);
            }
        }
        params.setOtherCatIds(otherCatIds);
        Map<String, Object> mapParams = EntityUtils.entityToMap(params);
        List<Videos161Vo> firstVideosList3 = getFirstVideosByParams(mapParams, RedisKeyConstant.CREATED3DAY, Integer.parseInt(duty[2]),state);
        if (CollectionUtils.isNotEmpty(firstVideosList3)) {
            VideoUtils.setLabel(firstVideosList3, 3);
            firstVideosAll.addAll(firstVideosList3);
        }
        //市场审核开关开启-不查询第一二标签数据
        if (!isOpen) {
            //ES开启，并且用户未登录，使用ES查询
            if (state == 1 && !isLogin) {
                //第一标签视频
                String cat1 = catIdsSort[0];
                List<Videos161Vo> firstVideosList1 = getFirstVideosByParams(mapParams, cat1, Integer.parseInt(duty[0]),state);
                if (CollectionUtils.isNotEmpty(firstVideosList1)) {
                    VideoUtils.setLabel(firstVideosList1, 1);
                    firstVideosAll.addAll(firstVideosList1);
                }
                //第二标签视频
                List<String> cats = Arrays.asList(catIdsSort);
                List<String> cat2 = getCatIds(cats, Integer.parseInt(duty[1]));
                for (String catId : cat2) {
                    List<Videos161Vo> firstVideosList2 = getFirstVideosByParams(mapParams, catId, 1,state);
                    if (CollectionUtils.isNotEmpty(firstVideosList2)) {
                        VideoUtils.setLabel(firstVideosList2, 2);
                        firstVideosAll.addAll(firstVideosList2);
                    }
                }
            } else {
                long catId1 = Long.parseLong(catIdsSort[0]);
                String sql = VideoSQLUtils.getSQL(redisDB8Service, params, catIdsSort, duty);
                List<Videos161Vo> videos1And2 = dynamicQuery.nativeQueryList(Videos161Vo.class, sql);
                VideoUtils.setLabel(videos1And2, catId1);
                firstVideosAll.addAll(videos1And2);
                //已曝光ID进行缓存
                saveShowedIds(firstVideosAll, params, catId1 + "");
            }
        }
        //如果大于2.5版本，则不返回集合其余信息
        if(VersionUtil.isHigh(VersionUtil.GATHER_VERSION,params.getAppVersion())){
            //2.1.0新增视频合集展示
            fillGatherVideos(firstVideosAll);
        }
        Collections.shuffle(firstVideosAll);
        return firstVideosAll;
    }

    /**
     * 填充合集视频子集合
     *
     * @param videos
     */
    private void fillGatherVideos(List<Videos161Vo> videos) {
        for (Videos161Vo vo : videos) {
            if (vo.getGatherId() != null && vo.getGatherId() > 0) {
                VideoGatherVo videoGatherVo = videoGatherService.getVideoGatherVo(vo);
                vo.setVideoGatherVo(videoGatherVo);
            }
        }
    }

    public List<Videos161Vo> searchVideos(Map<String, Object> params,int state) {
        String userId = params.get("userId")+"";
        if (StringUtils.isNotBlank(userId) && !"0".equals(userId)) {
            return firstVideosMapper.findFirstVideosListByUserId18(params);
        } else {
            if (state == 1) {
                log.info("use es search");
                return videoEsService.query(params);
            } else {
                return firstVideosMapper.findFirstVideosList18(params);
            }
        }
    }

    private List<Videos161Vo> getFirstVideosByParams(Map<String, Object> params, String catId, int num,int state) {
        if (num == 0) return null;
        String key = RedisKeyConstant.SHOWEDIDS_KEY + params.get("deviceId") + ":" + catId;
        String showedIds = redisDB8Service.get(key);
        if (showedIds != null) {
            if (showedIds.split(",").length > 100) {
                redisDB8Service.del(key);
                showedIds = null;
            }
        }
        params.put("showedIds",VideoUtils.appendShowedIds(showedIds,params.get("id")+""));//视频详情接口要过滤当前id
        params.put("catId",catId);
        params.put("num",num);
        List<Videos161Vo> videos161Vos = searchVideos(params,state);
        //未曝光视频不能满足返回条件，清空曝光记录
        if (videos161Vos == null || videos161Vos.size() < num) {
            params.put("showedIds",null);
            videos161Vos = searchVideos(params,state);
            redisDB8Service.del(key);
        }
        if (CollectionUtils.isNotEmpty(videos161Vos)) {
            List<String> ids_list = videos161Vos.stream().map(p -> p.getId() + "").collect(Collectors.toList());
            String newIds = VideoUtils.getVideoIds(ids_list, showedIds);
            redisDB8Service.set(key, newIds, RedisKeyConstant.SHOWEDIDS_SECONDS);
        }
        return videos161Vos;
    }


    private void saveShowedIds(List<Videos161Vo> videos, VideoParamsDto params, String catId1) {
        List<String> ids_label1_list = videos.stream().filter(v -> v.getLabel() == 1).map(p -> p.getId() + "").collect(Collectors.toList());
        List<Videos161Vo> label2_list = videos.stream().filter(v -> v.getLabel() == 2).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ids_label1_list)) {
            updateRedis(params.getDeviceId(), ids_label1_list, catId1);
        }
        if (CollectionUtils.isNotEmpty(label2_list)) {
            Map<Long, Long> catMap = label2_list.stream().collect(Collectors.toMap(Videos161Vo::getCatId, Videos161Vo::getCatId));
            Iterator it = catMap.keySet().iterator();
            while (it.hasNext()) {
                long catId = Long.parseLong(it.next().toString());
                List<String> cat_ids_List = label2_list.stream().filter(v -> v.getCatId() == catId).map(p -> p.getId() + "").collect(Collectors.toList());
                updateRedis(params.getDeviceId(), cat_ids_List, catId + "");
            }
        }
    }

    public void updateRedis(String deviceId, List<String> ids, String catId) {
        String key = RedisKeyConstant.SHOWEDIDS_KEY + deviceId + ":" + catId;
        String showedIds = redisDB8Service.get(key);
        String newIds = VideoUtils.getVideoIds(ids, showedIds);
        redisDB8Service.set(key, newIds, RedisKeyConstant.SHOWEDIDS_SECONDS);
    }

    @Override
    public FirstVideos161Vo findRecommendByTeenager(Map<String, Object> params) {
        int pageNums = VideoUtils.getPageNums();
        String channelId = params.get("channelId").toString();
        String appVersion = params.get("appVersion").toString();
        params.put("queryNumber", pageNums);
        //如果开启青少年模式，需要获取屏蔽分类ID
        String otherCatIds = marketAuditService.getCatIdsByChannelIdAndAppVersionFromTeenager(channelId, appVersion);
        if (StringUtils.isNotEmpty(otherCatIds)) {
            List catIds = Arrays.asList(otherCatIds.split(","));
            params.put("otherCatIds", catIds);
        }
        List<Videos161Vo> videos = query(params);
        return VideoUtils.packaging(videos);
    }

    @Override
    public FirstVideos161Vo findNoRecommendByTeenager(Map<String, Object> params) {
        int pageNums = VideoUtils.getPageNums();
        params.put("queryNumber", pageNums);
        List<Videos161Vo> videos = query(params);
        return VideoUtils.packaging(videos);
    }

    @Override
    public FirstVideoDetailVo firstVideosDetailList25(Map<String, Object> params) {
        FirstVideoDetailVo firstVideosNewVo = new FirstVideoDetailVo();
        List<Videos161Vo> firstVideosVos;
        params.put("queryNumber","1");//随机标识
        int state = Global.getInt("use_EsSearch_state3");//0-使用DB查询，1-使用ES查询
        //根据渠道和版本号进行市场屏蔽
        String appVersion = MapUtils.getString(params, "appVersion");
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(MapUtils.getString(params, "marketChannelId"), appVersion);
        String otherCatIds = marketAudit == null || StringUtils.isEmpty(marketAudit.getCatIds()) ? "" : marketAudit.getCatIds();
        params.put("otherCatIds", otherCatIds);
        firstVideosVos = getFirstVideosByParams(params,params.get("catId")+"",Global.getInt("page_number"),state);
        if (CollectionUtils.isNotEmpty(firstVideosVos)) {
            videoCacheService.fillParams(firstVideosVos);
        }
        long id = Long.parseLong(params.get("id").toString());
        if(id>0){
            FirstVideoEsVo firstVideoEsVo = videoEsService.getById(id);
//            //V2.5.6 屏蔽合集
//            if(marketAudit!=null && StringUtils.isNotBlank(marketAudit.getGatherIds())){
//                String gatherIds = marketAudit.getGatherIds();
//                List<String> list = Arrays.asList(gatherIds.split(","));
//                if (list.contains(String.valueOf(firstVideoEsVo.getGatherId()))){
//                    firstVideoEsVo = null;
//                }
//            }
            if(firstVideoEsVo!=null){
                Videos161Vo videos161Vo = VideoUtils.packaging(firstVideoEsVo);
                VideoGatherVo videoGatherVo = videoGatherService.getVideoGatherVo(videos161Vo);
                firstVideosNewVo.setVideoGatherVo(videoGatherVo);
            }
        }
        boolean newFlag = VersionUtil.isHigh(appVersion, Constant.COMPARE_APPVERSION);
        if (newFlag){
            //V2.5.0走新广告逻辑
            List<AdvertCodeVo> advertCodeVos = advertService.commonSearch(params);
            firstVideosNewVo.setAdvertCodeVos(advertCodeVos);
        }else {
            //随机5广告
            List<AdvertVo> advertList = advertOldService.getAdvertsBySection(params);
            if (CollectionUtils.isNotEmpty(advertList)){
                AdvertVo advertVo = advertList.get(0);
                int type = advertVo.getType();
                if (type == 0){
                    List<AdvertVo> list = AdvUtils.computer(advertList, 5);
                    firstVideosNewVo.setAdvers(list);
                }else {
                    firstVideosNewVo.setAdvers(advertList);
                }
            }
        }
        firstVideosNewVo.setVideos(firstVideosVos);
        return firstVideosNewVo;
    }

    public List<Videos161Vo> query(Map<String, Object> params) {
        String userId = params.get("userId").toString();
        List<Videos161Vo> firstVideos;
        if (null == userId || "0".equals(userId)) {
            firstVideos = firstVideosMapper.findFirstVideosList161(params);
        } else {
            firstVideos = firstVideosMapper.findFirstVideosListByUserId161(params);
        }
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            VideoUtils.setLoveAndWatchNum(firstVideos);
            //赋值分类名称，神策埋点需要
            VideoUtils.setCatName(firstVideos, null, videoCacheService.getVideosCatMap(VideoContant.FIRST_VIDEO_CODE));
        }
        return firstVideos;
    }

    //V1.8.0市场屏蔽功能
    private void marketAudit18(String marketChannelId, String appVersion, Map<String, Object> params) {
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(marketChannelId, appVersion);
        if (marketAudit != null) {
            if (StringUtils.isNotEmpty(marketAudit.getCatIds())) {
                String[] split = marketAudit.getCatIds().split(",");
                List<Long> list = new ArrayList<>();
                for (int i = 0; i < split.length; i++) {
                    list.add(Long.valueOf(split[i]));
                }
                params.put("otherCatIds", list);
            }
            //v2.1.0
            if (StringUtils.isNotEmpty(marketAudit.getGatherIds())) {
                params.put("gatherIds", marketAudit.getGatherIds());
            }
        }
    }

}