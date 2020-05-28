package com.miguan.ballvideo.rabbitMQ.listener;

import com.miguan.ballvideo.common.enums.VideoESOptions;
import com.miguan.ballvideo.rabbitMQ.util.RabbitMQConstant;
import com.miguan.ballvideo.service.VideoEsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 索引数据操作MQ
 * @author shixh
 * @date 2020-09-01
 */
@Slf4j
@EnableRabbit
@Component
public class VideoEsMqProcessor {

    @Resource
    private VideoEsService firstVideoEsItemService;

    @RabbitListener(autoStartup="#{environment['spring.rabbitmq.open']}",bindings = {
            @QueueBinding(value = @Queue(value = RabbitMQConstant.VIDEOS_ES_SEARCH_QUEUE),
                    exchange = @Exchange(value = RabbitMQConstant.VIDEOS_ES_SEARCH_EXCHANGE, autoDelete = "true"),
                    key = RabbitMQConstant.VIDEOS_ES_SEARCH_KEY)
    })
    public void processor(String jsonMsg) {
        String[] params = jsonMsg.split(RabbitMQConstant._MQ_);
        String options = params[0];
        if(VideoESOptions.videoAdd.name().equals(options)||VideoESOptions.videoDelete.name().equals(options)){
            String videoIds = params[1];
            firstVideoEsItemService.update(videoIds,options);
        }else if(VideoESOptions.gatherAddOrDelete.name().equals(options)){
            String gatherId = params[1];
            String videoIds = params[2];
            firstVideoEsItemService.updateByGatherId(Long.parseLong(gatherId),videoIds);
        }else if(VideoESOptions.gatherDeleteOrClose.name().equals(options)){
            String gatherId = params[1];
            String state = params[2];//0-关闭,1-删除
            firstVideoEsItemService.deleteOrCloseGather(Long.parseLong(gatherId),Integer.parseInt(state));
        }else if(VideoESOptions.deleteDueVideos.name().equals(options)){
            firstVideoEsItemService.deleteDueVideos();
        }else if(VideoESOptions.initVideo.name().equals(options)){
            String sqlBuffer = params[1];
            firstVideoEsItemService.init(sqlBuffer);
        }
    }
}
