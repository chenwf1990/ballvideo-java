package com.miguan.ballvideo.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.interceptor.argument.params.CommonParamsVo;
import com.miguan.ballvideo.common.util.EntityUtils;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.common.util.VersionUtil;
import com.miguan.ballvideo.common.util.adv.AdvUtils;
import com.miguan.ballvideo.common.util.video.VideoUtils;
import com.miguan.ballvideo.dto.VideoGatherParamsDto;
import com.miguan.ballvideo.dynamicquery.DynamicQuery;
import com.miguan.ballvideo.entity.MarketAudit;
import com.miguan.ballvideo.entity.VideoGather;
import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import com.miguan.ballvideo.repositories.VideoEsRepository;
import com.miguan.ballvideo.repositories.VideoGatherJpaRepository;
import com.miguan.ballvideo.service.*;
import com.miguan.ballvideo.vo.AdvertCodeVo;
import com.miguan.ballvideo.vo.AdvertVo;
import com.miguan.ballvideo.vo.video.VideoGatherVo;
import com.miguan.ballvideo.vo.video.Videos161Vo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @Author shixh
 * @Date 2020/1/10
 **/
@Service
public class VideoGatherServiceImpl implements VideoGatherService {

    private int default_page = 8;

    private Long default_max_weight = 10000L;

    @Resource
    private VideoGatherJpaRepository videoGatherJpaRepository;

    @Resource
    private VideoEsRepository videoEsRepository;

    @Resource
    private VideoEsService videoEsService;

    @Resource
    private SysService sysService;

    @Resource
    private DynamicQuery dynamicQuery;

    @Resource
    private AdvertOldService advertOldService;

    @Resource
    private MarketAuditService marketAuditService;

    @Resource
    private AdvertService advertService;

    @Resource
    private VideoCacheService videoCacheService;

    /**
     * 首页视频合集视频子集查询
     * @param gatherId
     * @param totalWeight
     * @return
     */
    @Override
    public List<Videos161Vo> getVideos(Long gatherId,Long totalWeight) {
        //根据当前权重查询后面的数据
        List<Videos161Vo> videos = getGatherVideosByPage(gatherId,totalWeight,"left");
        //无数据或者数据少于8，继续获取后面的数据返回
        if(CollectionUtils.isEmpty(videos))return getGatherVideosByPage(gatherId,0L,"left");
        if(videos.size()<default_page)videos.addAll(getGatherVideosByPage(gatherId,default_max_weight,"left"));
        return videos;
    }

    public Object getVideos(Long gatherId, CommonParamsVo param) {
        Map<String,Object> map = new HashMap<>();
        int currentPage  = param.getCurrentPage();
        currentPage-=1;//es从0开始
        if (currentPage < 0) {
            currentPage = 0;
        }
        Optional<VideoGather> videoGatherOpt = videoGatherJpaRepository.findById(gatherId);
        if(!videoGatherOpt.isPresent()){
            map.put("searchData","");
            map.put("page","");
            map.put("title","");
            return map;
        }
        VideoGather videoGather = videoGatherOpt.get();
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        builder.must(QueryBuilders.matchPhraseQuery("gatherId", gatherId));
        Pageable pageable = PageRequest.of(currentPage, param.getPageSize(), Sort.Direction.DESC, "totalWeight");
        Page<FirstVideoEsVo> pages = videoEsRepository.search(builder,pageable);
        List<FirstVideoEsVo> content = pages.getContent();
        List<Videos161Vo> videos161Vos = VideoUtils.packagingByEsVideos(content);
        //根据用户视频关联表判断是否收藏
        videoCacheService.getVideosCollection(videos161Vos, param.getUserId());
        map.put("searchData", videos161Vos);
        map.put("page",pageable);
        map.put("title",videoGather.getTitle());
        return map;
    }

    @Override
    public Object getVideos(Long gatherId) {
        //videoEsRepository.deleteByGatherId(gatherId);
        VideoGather gather = this.getByGatherId(gatherId);
        if(gather==null){
            Map map = new HashMap();
            map.put("vo","");
            map.put("data",new ArrayList<FirstVideoEsVo>());
            return map;
        }
        VideoGatherVo vo = this.packing(gather);
        List<FirstVideoEsVo> esdatas = videoEsRepository.findByGatherId(vo.getId());
        Map map = new HashMap();
        map.put("vo",vo);
        map.put("data",esdatas);
        return map;
    }

    public Object getVideos(Long gatherId,Long totalWeight,Long videoId,String step) {
        List<Videos161Vo> videos = getGatherVideosByPage(gatherId,totalWeight,step);
        if(videoId==null && CollectionUtils.isNotEmpty(videos)){
            videos.remove(0);
        }else if(CollectionUtils.isNotEmpty(videos)){
            for(Videos161Vo videos161Vo:videos){
                if(videos161Vo.getId().intValue()==videoId.intValue()){
                    //权重大小判断新增等于判断，要过滤自身
                    videos.remove(videos161Vo);break;
                }
            }
            //往右边滑动，权重排序由大到小
            if("right".equals(step)){
                videos = Lists.reverse(videos);
            }
        }
        Map<String,Object> map = new HashMap<>();
        map.put("searchData",videos);
        return map;
    }

    /**
     * 前翻后翻获取视频数据
     * step-left,获取小于totalWeight的数据
     * step-right,获取大于totalWeight的数据
     * @param gatherId
     * @param totalWeight
     * @param step
     * @return
     */
    private List<Videos161Vo> getGatherVideosByPage(Long gatherId, Long totalWeight, String step) {
        QueryBuilder gteQb = QueryBuilders.rangeQuery("totalWeight").lte(totalWeight);
        Sort.Direction direction =Sort.Direction.DESC;
        if("right".equals(step)){
            gteQb = QueryBuilders.rangeQuery("totalWeight").gte(totalWeight);
            direction =Sort.Direction.ASC;
        }
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        builder.must(QueryBuilders.matchPhraseQuery("gatherId", gatherId));
        builder.must(gteQb);
        Pageable pageable = PageRequest.of(0, default_page,direction, "totalWeight","id");
        List<FirstVideoEsVo> content = videoEsRepository.search(builder,pageable).getContent();
        List<Videos161Vo> videos = VideoUtils.packagingByEsVideos(content);
        return videos;
    }

    @Override
    public Object getDefaultVideos(String userId, VideoGatherParamsDto params) {
        List<VideoGatherVo> videoGatherVos = getGatherVideos(userId,params);
        String appVersion = params.getAppVersion();
        boolean isHigh = VersionUtil.isHigh(appVersion,"2.3.1");
        if(!isHigh){
            return videoGatherVos;
        }
        boolean newFlag = VersionUtil.isHigh(appVersion, Constant.COMPARE_APPVERSION);
        Map<String,Object> map = Maps.newHashMap();
        if (newFlag){
            Map<String, Object> paraMap = EntityUtils.entityToMap(params);
            List<AdvertCodeVo> advertCodeVos = advertService.commonSearch(paraMap);
            map.put("advertCodeVos", advertCodeVos);
        }else {
            List<AdvertVo> advs = getAdvs(params);
            map.put("advs",advs);
        }
        map.put("videos",videoGatherVos);
        return map;
    }

    public List<AdvertVo> getAdvs(VideoGatherParamsDto params){
        boolean isShield = marketAuditService.isShield(params.getChannelId(),params.getAppVersion());
        if(isShield){
            return Lists.newArrayList();
        }
        Map<String, Object> map = EntityUtils.entityToMap(params);
        List<AdvertVo> advs = advertOldService.getBaseAdverts(map);
        if (CollectionUtils.isNotEmpty(advs)){
            advs = AdvUtils.computer(advs, advs.size());
        }
        return advs;
    }

    public List<VideoGatherVo> getGatherVideos(String userId,VideoGatherParamsDto params){
        //通过市场审核开关过滤屏蔽的合集
        List<VideoGather> gathers = getGathers(params);
        if(CollectionUtils.isEmpty(gathers))return null;
        List<VideoGatherVo> videoGatherVos = new ArrayList<VideoGatherVo>();
        for(VideoGather gather:gathers){
            VideoGatherVo vo = getByGather(gather,true);
            //根据用户视频关联表判断是否收藏
            videoCacheService.getVideosCollection(vo.getSearchData(), userId);
            videoGatherVos.add(vo);
        }
        return videoGatherVos;
    }

    public VideoGatherVo packing(VideoGather gather){
        int count = this.countByGatherId(gather.getId());
        VideoGatherVo videoGatherVo = new VideoGatherVo();
        videoGatherVo.setId(gather.getId());
        videoGatherVo.setCount(count);
        videoGatherVo.setTitle(gather.getTitle());
        return videoGatherVo;
    }

    public VideoGatherVo packing(Long gatherId,String title){
        if(gatherId==null || StringUtils.isEmpty(title))return null;
        int count = this.countByGatherId(gatherId);
        VideoGatherVo videoGatherVo = new VideoGatherVo();
        videoGatherVo.setId(gatherId);
        videoGatherVo.setCount(count);
        videoGatherVo.setTitle(title);
        return videoGatherVo;
    }

    public VideoGatherVo getByGather(VideoGather gather,boolean includeSearchData) {
        VideoGatherVo videoGatherVo = packing(gather);
        if(includeSearchData){
            List<Videos161Vo> videos = getGatherVideosByPage(gather.getId(),default_max_weight,"left");
            videoGatherVo.setSearchData(videos);
        }
        return videoGatherVo;
    }

    @Override
    public int countByGatherId(Long gatherId) {
        return videoEsRepository.countByGatherId(gatherId);
    }

    @Override
    public VideoGather getByGatherId(Long gatherId) {
        Optional<VideoGather> optional = videoGatherJpaRepository.findById(gatherId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    @Override
    public VideoGatherVo getVideoGatherVoByGatherId(Long gatherId,boolean includeSearchData) {
        VideoGather gather = this.getByGatherId(gatherId);
        if(gather==null)return null;
        return this.getByGather(gather,includeSearchData);
    }

    /**
     * 根据当前视频获取同合集下其他视频，并且根据当前视频的权重往下排序
     * @param vo
     * @return
     */
    @Override
    public VideoGatherVo getVideoGatherVo(Videos161Vo vo) {
        VideoGatherVo videoGatherVo = packing(vo.getGatherId(),vo.getGatherTitle());
        if(videoGatherVo==null)return null;
        if(videoGatherVo.getCount()==0){
            videoGatherVo.setSearchData(Lists.newArrayList());
            return videoGatherVo;
        }
        List<Videos161Vo> gatherVideos = getVideos(vo.getGatherId(),vo.getTotalWeight());
        if(CollectionUtils.isEmpty(gatherVideos))gatherVideos = new ArrayList<Videos161Vo>();
        //大于或者小于权重排序，包含当前对象本身，需要去重复
        for(Videos161Vo v:gatherVideos){
            if(v.getId().intValue()==vo.getId().intValue()){
                gatherVideos.remove(v);break;
            }
        }
        Videos161Vo newObject = new Videos161Vo();
        BeanUtils.copyProperties(vo,newObject);
        gatherVideos.add(0,newObject);
        videoGatherVo.setSearchData(gatherVideos);
        return videoGatherVo;
    }

    /** 通过市场审核开关过滤屏蔽的合集
     * @param params
     * @return
     */
    private List<VideoGather> getGathers(VideoGatherParamsDto params) {
        StringBuffer sb = new StringBuffer("");
        sb.append("select * from video_gather where state =  ").append(Constant.open);
        sb.append(" and recommend_state=").append(VideoGather.RECOMMEND);
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(params.getChannelId(),params.getAppVersion());
        if(marketAudit!=null && StringUtils.isNotBlank(marketAudit.getGatherIds())){
            sb.append(" and id not in (").append(marketAudit.getGatherIds()).append(")");
        }
        sb.append(" order by base_weight desc ");
        return dynamicQuery.nativeQueryList(VideoGather.class,sb.toString());
    }


    @Override
    public ResultMap refreshVideosByGatherId(Long gatherId) {
        videoEsService.deleteOrCloseGather(gatherId,0);
        videoEsService.updateByGatgherId(gatherId);
        sysService.delRedis("ballVideos:interface:newfirstVideo161:");
        return ResultMap.success();
    }

}
