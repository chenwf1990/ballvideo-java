package com.miguan.ballvideo.service.impl;

import com.google.common.collect.Lists;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.enums.VideoESOptions;
import com.miguan.ballvideo.common.util.EntityUtils;
import com.miguan.ballvideo.common.util.Global;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.common.util.VersionUtil;
import com.miguan.ballvideo.common.util.adv.AdvUtils;
import com.miguan.ballvideo.common.util.video.VideoUtils;
import com.miguan.ballvideo.dto.VideoGatherParamsDto;
import com.miguan.ballvideo.dynamicquery.DynamicQuery;
import com.miguan.ballvideo.entity.MarketAudit;
import com.miguan.ballvideo.entity.VideoGather;
import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import com.miguan.ballvideo.mapper.ClUserVideosMapper;
import com.miguan.ballvideo.rabbitMQ.util.RabbitMQConstant;
import com.miguan.ballvideo.repositories.VideoEsRepository;
import com.miguan.ballvideo.service.*;
import com.miguan.ballvideo.vo.AdvertCodeVo;
import com.miguan.ballvideo.vo.AdvertVo;
import com.miguan.ballvideo.vo.video.FirstVideosVo;
import com.miguan.ballvideo.vo.video.VideoGatherVo;
import com.miguan.ballvideo.vo.video.Videos161Vo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * @Author shixh
 * @Date 2020/1/7
 **/
@Slf4j
@Service
public class VideoEsServiceImpl implements VideoEsService {

    @Resource
    private ElasticsearchTemplate esTemplate;

    @Resource
    private VideoEsRepository videoEsRepository;

    @Resource
    private VideoCacheService videoCacheService;

    @Resource
    private DynamicQuery dynamicQuery;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private VideoGatherService videoGatherService;

    @Resource
    private MarketAuditService marketAuditService;

    @Resource
    private AdvertOldService advertOldService;

    @Resource
    private AdvertService advertService;

    @Resource
    private ToolMofangService toolMofangService;

    @Resource
    private ClUserVideosMapper clUserVideosMapper;


    private String video_sql = "select v.id,v.title,v.cat_id,v.url_img,v.bsy_url AS bsyUrl,v.created_at AS createdAt,UNIX_TIMESTAMP(v.created_at) as createDate,v.bsy_img_url AS bsyImgUrl,v.collection_count AS collectionCount,(v.love_count + v.love_count_real) AS loveCount,v.comment_count AS commentCount,\n" +
            " IFNULL(g.id, 0) AS gatherId,g.title as gatherTitle,(v.watch_count + v.watch_count_real) AS watchCount,'0' collection,'0' love,v.bsy_head_url AS bsyHeadUrl,v.video_author AS videoAuthor,v.video_time,v.video_size AS videoSize,v.base_weight+v.real_weight as totalWeight FROM first_videos v left join video_gather g ON g.id = v.gather_id AND g.state = 1 where v.state = 1 ";


    @Override
    public ResultMap deleteIndex(String index) {
        if (esTemplate.indexExists(index)) {
            return ResultMap.success(esTemplate.deleteIndex(index));
        }
        return ResultMap.error("索引不存在，删除失败");
    }

    @Override
    public Object init() {
        if (!esTemplate.indexExists(FirstVideoEsVo.class)) {
            esTemplate.createIndex(FirstVideoEsVo.class);
            esTemplate.putMapping(FirstVideoEsVo.class);
        } else {
            esTemplate.deleteIndex(FirstVideoEsVo.class);
        }
        int day = Global.getInt("init_video_mostRecentDays");
        String countSQL = "select count(*) from first_videos where state = 1 and (TO_DAYS(NOW()) - TO_DAYS(created_at)) <= " + day;
        Object o = dynamicQuery.nativeQueryObject(countSQL);
        if (o == null) return ResultMap.error("无可操作数据");
        int count = Integer.parseInt(o + "");
        int loop = 0;
        while (true) {
            String append = " and (TO_DAYS(NOW()) - TO_DAYS(v.created_at)) <= " + day + " order by v.created_at desc limit " + loop + "," + (loop + 5000);
            String json = VideoESOptions.initVideo.name() + RabbitMQConstant._MQ_ + append;
            rabbitTemplate.convertAndSend(RabbitMQConstant.VIDEOS_ES_SEARCH_EXCHANGE, RabbitMQConstant.VIDEOS_ES_SEARCH_KEY, json);
            if (loop >= count) break;
            loop += 5000;
        }
        log.info("es首页视频初始化开始。。。");
        return ResultMap.success();
    }

    public void init(String sqlBuffer) {
        String sql = video_sql + sqlBuffer;
        List<Videos161Vo> videos = dynamicQuery.nativeQueryList(Videos161Vo.class, sql);
        log.info("初始化视频数据：" + sqlBuffer.split("limit")[1] + "...");
        if (CollectionUtils.isNotEmpty(videos)) {
            saveToEs(videos);
        }
    }

    private void saveToEs(List<Videos161Vo> videos) {
        try {
            videoCacheService.fillParams(videos);
            List<FirstVideoEsVo> csVideos = new ArrayList<FirstVideoEsVo>();
            for (Videos161Vo video : videos) {
                FirstVideoEsVo csVideo = new FirstVideoEsVo();
                BeanUtils.copyProperties(video, csVideo);
                csVideos.add(csVideo);
            }
            videoEsRepository.saveAll(csVideos);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public FirstVideoEsVo getById(Long id) {
        Optional<FirstVideoEsVo> optional = videoEsRepository.findById(id);
        if (optional.isPresent()) {
            FirstVideoEsVo vo = optional.get();
            if (vo.getGatherId() > 0 && StringUtils.isEmpty(vo.getGatherTitle())) {
                //存在合集不为空但是标题为空的问题，临时处理
                update(vo.getId() + "", VideoESOptions.videoAdd.name());
                return getById(id);
            }
            return vo;
        }
        return null;
    }

    @Override
    public void save(FirstVideoEsVo vo) {
        videoEsRepository.save(vo);
    }

    @Override
    public Object updateByGatgherId(Long gatherId) {
        String sql = video_sql + " and v.gather_id =" + gatherId;
        List<Videos161Vo> videos = dynamicQuery.nativeQueryList(Videos161Vo.class, sql);
        if (CollectionUtils.isNotEmpty(videos)) {
            saveToEs(videos);
        }
        return ResultMap.success();
    }

    @Override
    public Object getMyGatherVidesoById(Long videoId) {
        String sql = video_sql + " and v.id = " + videoId;
        List<Videos161Vo> videos = dynamicQuery.nativeQueryList(Videos161Vo.class, sql);
        if (CollectionUtils.isNotEmpty(videos)) {
            Videos161Vo videos161Vo = videos.get(0);
            return videoGatherService.getVideoGatherVo(videos161Vo);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Object search(String title, String userId, VideoGatherParamsDto param) {
        String appVersion = param.getAppVersion();
        boolean isHigh = VersionUtil.isHigh(appVersion, "2.3.1");
        int currentPage = param.getCurrentPage() - 1;//es从0开始
        if (currentPage < 0) {
            currentPage = 0;
        }
        Pageable pageable = PageRequest.of(currentPage, param.getPageSize());
        List<Videos161Vo> esVideos = getVideos(pageable, title, param);
        //根据用户视频关联表判断是否收藏
        videoCacheService.getVideosCollection(esVideos, userId);
        if (!isHigh) {
            Map<String, Object> map = new HashMap<>();
            map.put("searchData", esVideos);
            map.put("page", pageable);
            return map;
        }
        boolean newFlag = VersionUtil.isHigh(appVersion, Constant.COMPARE_APPVERSION);
        List<FirstVideosVo> videos;
        if (newFlag) {
            //V2.5.0
            Map<String, Object> map = EntityUtils.entityToMap(param);
            List<AdvertCodeVo> advertCodeVos = advertService.commonSearch(map);
            videos = VideoUtils.packagingNewBySearch(advertCodeVos, esVideos);
        } else {
            List<AdvertVo> advs = getAdvs(param);
            videos = VideoUtils.packagingBySearch(advs, esVideos);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("searchData", videos);
        map.put("page", pageable);
        return map;
    }

    public List<AdvertVo> getAdvs(VideoGatherParamsDto params) {
        Map<String, Object> map = EntityUtils.entityToMap(params);
        boolean flag = toolMofangService.stoppedByMofang(map);
        if (flag){
            return Lists.newArrayList();
        }
        List<AdvertVo> advs = advertOldService.getBaseAdverts(map);
        if (CollectionUtils.isNotEmpty(advs)) {
            advs = AdvUtils.computer(advs, advs.size());
        }
        return advs;
    }

    public List<Videos161Vo> getVideos(Pageable pageable, String title, VideoGatherParamsDto param) {
        String preTag = "<font color='#FF5765'>";
        String postTag = "</font>";
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(param.getChannelId(), param.getAppVersion());
        //title
        MatchQueryBuilder matchQueryBuilder = matchQuery("title", title);
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //catid
        if (marketAudit != null && StringUtils.isNotEmpty(marketAudit.getCatIds())) {
            TermsQueryBuilder builder = QueryBuilders.termsQuery("catId", marketAudit.getCatIds().split(","));
            queryBuilder.mustNot(builder);
        }
        //V2.5.6 屏蔽合集
        if (marketAudit != null && StringUtils.isNotEmpty(marketAudit.getGatherIds())) {
            TermsQueryBuilder builder = QueryBuilders.termsQuery("gatherId", marketAudit.getGatherIds().split(","));
            queryBuilder.mustNot(builder);
        }
        queryBuilder.must(matchQueryBuilder);
        SearchQuery searchQuery = new NativeSearchQueryBuilder().
                withQuery(queryBuilder).
                withHighlightFields(new HighlightBuilder.Field("title").preTags(preTag).postTags(postTag)).build();
        searchQuery.setPageable(pageable);


        AggregatedPage<FirstVideoEsVo> items = esTemplate.queryForPage(searchQuery, FirstVideoEsVo.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
                List<Videos161Vo> chunk = new ArrayList<Videos161Vo>();
                for (SearchHit searchHit : response.getHits()) {
                    if (response.getHits().getHits().length <= 0) {
                        return null;
                    }
                    Long id = Long.parseLong(searchHit.getId());
                    FirstVideoEsVo firstVideoEsItem = getById(id);
                    HighlightField highlightField = searchHit.getHighlightFields().get("title");
                    if (highlightField != null) {
                        firstVideoEsItem.setColorTitle(highlightField.fragments()[0].toString());
                        Videos161Vo videos161Vo = new Videos161Vo();
                        BeanUtils.copyProperties(firstVideoEsItem, videos161Vo);
                        if (videos161Vo.getGatherId() > 0) {
                            VideoGatherVo videoGatherVo = videoGatherService.getVideoGatherVoByGatherId(videos161Vo.getGatherId(), false);
                            videos161Vo.setVideoGatherVo(videoGatherVo);
                        }
                        chunk.add(videos161Vo);
                    }
                }
                if (chunk.size() > 0) {
                    return new AggregatedPageImpl<>((List<T>) chunk);
                }
                return null;
            }
        });
        if (items == null) return new ArrayList<Videos161Vo>();
        PageImpl<Videos161Vo> page = new PageImpl(items.getContent());
        return items == null ? new ArrayList<Videos161Vo>() : page.getContent();
    }

    @Override
    public Object update(String videoIds, String options) {
        if (VideoESOptions.videoAdd.name().equals(options)) {
            String sql = video_sql + " and v.id in(" + videoIds + ")";
            List<Videos161Vo> videos = dynamicQuery.nativeQueryList(Videos161Vo.class, sql);
            if (CollectionUtils.isNotEmpty(videos)) {
                saveToEs(videos);
            }
        } else if (VideoESOptions.videoDelete.name().equals(options)) {
            String[] ids = videoIds.split(",");
            for (String id : ids) {
                long videoId = Long.parseLong(id);
                videoEsRepository.deleteById(videoId);
            }
        }
        return null;
    }

    @Override
    public Object updateByGatherId(Long gatherId, String videoIds) {
        List<String> vodeoIds_arrA = Arrays.asList(videoIds.split(","));
        for (String strId : vodeoIds_arrA) {
            Long id = Long.parseLong(strId);
            FirstVideoEsVo firstVideoEsVo = getById(id);
            if (firstVideoEsVo != null) {
                firstVideoEsVo.setGatherId(gatherId);
                if (gatherId > 0) {
                    VideoGather gather = videoGatherService.getByGatherId(gatherId);
                    if (gather != null) {
                        firstVideoEsVo.setGatherTitle(gather.getTitle());
                    } else {
                        firstVideoEsVo.setGatherTitle("");
                        firstVideoEsVo.setGatherId(0L);
                    }
                }
                videoEsRepository.save(firstVideoEsVo);
            }
        }
        return ResultMap.success();
    }

    @Override
    public void deleteOrCloseGather(long gatherId, int state) {
        //state:0-关闭,1-删除
        List<FirstVideoEsVo> videos = videoEsRepository.findByGatherId(gatherId);
        if (CollectionUtils.isNotEmpty(videos)) {
            for (FirstVideoEsVo video : videos) {
                if (state == 0 || state == 1) {
                    video.setGatherId(0L);
                    video.setGatherTitle("");
                }
            }
            videoEsRepository.saveAll(videos);
        }
    }

    @Override
    public void deleteDueVideos() {
        int day = Global.getInt("init_video_mostRecentDays");
        long seconds = System.currentTimeMillis()/1000-60*60*24*day;
        List<FirstVideoEsVo> videos = this.videoEsRepository.findByCreateDateLessThanEqual(seconds);
        if(CollectionUtils.isNotEmpty(videos)){
            videoEsRepository.deleteAll(videos);
        }
    }

    @Override
    public List<Videos161Vo> query(Map<String, Object> params) {
        Pageable pageable = PageRequest.of(0, 10);
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder builder = QueryBuilders.boolQuery();        //builder下有must、should以及mustNot 相当于sql中的and、or以及not
        String catId = MapUtils.getString(params, "catId");
        if (StringUtils.isNotEmpty(catId) && !"0".equals(catId) && !"created3Day".equals(catId)) {
            builder.must(matchQuery("catId", params.get("catId") + ""));
        }
        String otherCatIds = MapUtils.getString(params, "otherCatIds");
        if (StringUtils.isNotEmpty(otherCatIds)) {
            TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("catId", params.get("otherCatIds").toString().split(","));
            builder.mustNot(termsQueryBuilder);
        }
        String showedIds = MapUtils.getString(params, "showedIds");
        if (StringUtils.isNotEmpty(showedIds) && !"null".equals(showedIds)) {
            TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("id", params.get("showedIds").toString().split(","));
            builder.mustNot(termsQueryBuilder);
        }
        //V2.5.6ES屏蔽集合
        String gatherIds = MapUtils.getString(params, "gatherIds");
        if (StringUtils.isNotEmpty(gatherIds) && !"null".equals(gatherIds)) {
            TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("gatherId", params.get("gatherIds").toString().split(","));
            builder.mustNot(termsQueryBuilder);
        }
        if ("created3Day".equals(catId)) {
            FieldSortBuilder sort = SortBuilders.fieldSort("createDate").order(SortOrder.DESC);
            nativeSearchQueryBuilder.withSort(sort);
        }
        if (StringUtils.isNotEmpty(catId) && !"created3Day".equals(catId)) {
            FieldSortBuilder sort = SortBuilders.fieldSort("totalWeight").order(SortOrder.DESC);
            nativeSearchQueryBuilder.withSort(sort);
        }
        String numStr = MapUtils.getString(params, "num");
        int num = StringUtils.isEmpty(numStr) ? 0 : Integer.parseInt(numStr);
        if (num > 0) {
            pageable = PageRequest.of(0, num);
        }
        String queryNumber = MapUtils.getString(params, "queryNumber");
        //queryNumber随机标识
        if (StringUtils.isNotEmpty(queryNumber)) {
            Script script = new Script("Math.random()");
            ScriptSortBuilder scriptSortBuilder =
                    SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER).order(SortOrder.DESC);
            nativeSearchQueryBuilder.withQuery(builder).withSort(scriptSortBuilder);
        } else {
            nativeSearchQueryBuilder.withQuery(builder);
        }
        nativeSearchQueryBuilder.withPageable(pageable);
        Iterable it = this.videoEsRepository.search(nativeSearchQueryBuilder.build());
        List<FirstVideoEsVo> esVideos = Lists.newArrayList(it);
        if (CollectionUtils.isNotEmpty(esVideos)) {
            List<Videos161Vo> videos = new ArrayList<Videos161Vo>();
            for (FirstVideoEsVo firstVideoEsVo : esVideos) {
                Videos161Vo videos161Vo = new Videos161Vo();
                BeanUtils.copyProperties(firstVideoEsVo, videos161Vo);
                videos.add(videos161Vo);
            }
            return videos;
        }
        return null;
    }

    @Override
    public List<Videos161Vo> query(Map<String, Object> params, int num) {
        Script script = new Script("Math.random()");
        ScriptSortBuilder scriptSortBuilder =
                SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER).order(SortOrder.DESC);
        String catId = MapUtils.getString(params, "catId");
        Pageable pageable = PageRequest.of(0, num);
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder builder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(catId)) {
            builder.must(matchQuery("catId", catId));
        }
        //V2.5.6ES屏蔽集合
        String channelId = MapUtils.getString(params, "channelId");
        String appVersion = MapUtils.getString(params, "appVersion");
        MarketAudit marketAudit = marketAuditService.getCatIdsByChannelIdAndAppVersion(channelId, appVersion);
        if (marketAudit != null && StringUtils.isNotBlank(marketAudit.getGatherIds())) {
            String gatherIds = marketAudit.getGatherIds();
            if (StringUtils.isNotEmpty(gatherIds) && !"null".equals(gatherIds)) {
                TermsQueryBuilder termsQueryBuilder = QueryBuilders.termsQuery("gatherId", params.get("gatherIds").toString().split(","));
                builder.mustNot(termsQueryBuilder);
            }
        }
        nativeSearchQueryBuilder.withQuery(builder).withSort(scriptSortBuilder);
        nativeSearchQueryBuilder.withPageable(pageable);
        Iterable it = this.videoEsRepository.search(nativeSearchQueryBuilder.build());
        List<FirstVideoEsVo> esVideos = Lists.newArrayList(it);
        if (CollectionUtils.isNotEmpty(esVideos)) {
            List<Videos161Vo> videos = new ArrayList<Videos161Vo>();
            for (FirstVideoEsVo firstVideoEsVo : esVideos) {
                Videos161Vo videos161Vo = new Videos161Vo();
                BeanUtils.copyProperties(firstVideoEsVo, videos161Vo);
                videos.add(videos161Vo);
            }
            return videos;
        }
        return null;
    }

}
