package com.miguan.ballvideo.service.impl;

import com.alibaba.fastjson.JSON;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.controller.SysController;
import com.miguan.ballvideo.entity.ClMenuConfig;
import com.miguan.ballvideo.rabbitMQ.util.RabbitMQConstant;
import com.miguan.ballvideo.repositories.ClMenuConfigDao;
import com.miguan.ballvideo.service.ClMenuConfigService;
import com.miguan.ballvideo.service.MarketAuditService;
import com.miguan.ballvideo.vo.queue.UserLabelQueueVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * ClMenuConfigServiceImpl  查询APP菜单功能
 *
 * @author HYL
 * @date 2019年9月11日10:22:45
 **/
@Service
public class ClMenuConfigServiceImpl implements ClMenuConfigService {

    private Logger log = LoggerFactory.getLogger(SysController.class);

    @Autowired
    private ClMenuConfigDao clMenuConfigDao;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MarketAuditService marketAuditService;

    //默认APP包名，向上兼容
    private final String DEFAULT_APP_PACKAGE = "com.mg.xyvideo";

    /**
     * 首页菜单栏
     *
     * @param channelId  渠道ID
     * @param deviceId   设备ID
     * @param appVersion 版本
     * @param appPackage 马甲包
     * @return
     */
    @Override
    @Transactional
    public ResultMap findClMenuByAppPackageOrFilterChannel(
            String channelId,
            String deviceId,
            String appVersion,
            String appPackage) {
        if (appPackage == null || ("").equals(appPackage)) {
            appPackage = DEFAULT_APP_PACKAGE;
        }
        if (StringUtils.isEmpty(channelId)) {
            channelId = " ";
        }
        //旧版本默认1.6.0
        if (StringUtils.isEmpty(appVersion)) {
            appVersion = "1.6.0";
        }
        if (!StringUtils.isEmpty(deviceId)) {
            // 如果用户标签为空，创建用户标签
            String jsonStr = JSON.toJSONString(new UserLabelQueueVo(deviceId, channelId, appVersion));
            rabbitTemplate.convertAndSend(
                    RabbitMQConstant.UserLabel_EXCHANGE,
                    RabbitMQConstant.UserLabel_KEY, jsonStr);
        }
        List<ClMenuConfig> data = clMenuConfigDao.findClMenuConfigListByAppPackage(appPackage, channelId, appVersion);
        return ResultMap.success(data);
    }


}
