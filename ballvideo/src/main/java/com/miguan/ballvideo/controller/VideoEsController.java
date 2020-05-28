package com.miguan.ballvideo.controller;

import cn.jiguang.common.utils.StringUtils;
import com.miguan.ballvideo.common.aop.ConcurrentLock;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.common.enums.VideoESOptions;
import com.miguan.ballvideo.dto.VideoGatherParamsDto;
import com.miguan.ballvideo.rabbitMQ.util.RabbitMQConstant;
import com.miguan.ballvideo.service.VideoEsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@Api(value="全文检索首页视频相关接口",tags={"全文检索首页视频相关接口"})
@RequestMapping("/api/es")
@RestController
public class VideoEsController {

    @Resource
    private VideoEsService videoEsService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @ConcurrentLock(lockTime=60000,message ="操作太频繁，请稍后再试")
    @ApiOperation("全量更新，每次5000条进行更新(PHP调用)")
    @GetMapping("/firstVideo/init")
    public ResultMap init() {
        return (ResultMap) videoEsService.init();
    }

    @ApiOperation("视频查询,返回结果高亮显示")
    @GetMapping("/firstVideo/searchByPage")
    public ResultMap search(@ApiParam("关键字")String title,
                            @ApiParam("关键字")String userId,
                            @ApiParam("公共参数")VideoGatherParamsDto params) {
        if (StringUtils.isEmpty(title)) {
            return ResultMap.error("参数异常");
        }
        Object o = videoEsService.search(title,userId,params);
        return ResultMap.success(o);
    }

    @ApiOperation("批量新增/删除视频（PHP调用）")
    @PostMapping("/firstVideo/addOrDelete")
    public ResultMap addOrDelete(@ApiParam("视频ID，多个逗号隔开")String videoIds, @ApiParam("videoAdd/videoDelete")String options) {
        if (StringUtils.isEmpty(videoIds)
            || StringUtils.isEmpty(options)
            || !VideoESOptions.videoAdd.name().equals(options) && !VideoESOptions.videoDelete.name().equals(options)) {
            return ResultMap.error("参数异常");
        }
        String json = options+RabbitMQConstant._MQ_+videoIds;
        rabbitTemplate.convertAndSend(RabbitMQConstant.VIDEOS_ES_SEARCH_EXCHANGE,RabbitMQConstant.VIDEOS_ES_SEARCH_KEY,json);
        return ResultMap.success();
    }

    @ApiOperation("删除过期视频（PHP调用）")
    @PostMapping("/firstVideo/deleteDueVideos")
    public ResultMap deleteDueVideos() {
        String json = VideoESOptions.deleteDueVideos.name();
        rabbitTemplate.convertAndSend(RabbitMQConstant.VIDEOS_ES_SEARCH_EXCHANGE,RabbitMQConstant.VIDEOS_ES_SEARCH_KEY,json);
        return ResultMap.success();
    }

    //test
    @ApiOperation("根据ID获取视频(测试用到)")
    @GetMapping("/firstVideo/getById")
    public ResultMap getById(Long videoId) {
        if (videoId==null) {
            return ResultMap.error("参数异常");
        }
        Object o = videoEsService.getById(videoId);
        return ResultMap.success(o);
    }

    @ApiOperation("根据index删除索引(测试用到)")
    @GetMapping("/firstVideo/deleteIndex")
    public ResultMap deleteIndex(String index){
        return videoEsService.deleteIndex(index);
    }

    @ApiOperation("更新当前集合下视屏数据(测试用到)")
    @GetMapping("/firstVideo/updateByGatgherId")
    public ResultMap updateByGatgherId(@ApiParam("合集ID")Long gatherId) {
        if(gatherId==null || gatherId<0){
            return ResultMap.error("参数异常");
        }
        return ResultMap.success(videoEsService.updateByGatgherId(gatherId));
    }

    @ApiOperation("根据ID获取合集子集(测试用到)")
    @GetMapping("/firstVideo/getMyGatherVidesoById")
    public ResultMap getMyGatherVidesoById(Long videoId) {
        if (videoId==null) {
            return ResultMap.error("参数异常");
        }
        Object o = videoEsService.getMyGatherVidesoById(videoId);
        return ResultMap.success(o);
    }

}
