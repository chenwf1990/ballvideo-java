package com.miguan.ballvideo.service.impl;

import com.alibaba.fastjson.JSON;
import com.miguan.ballvideo.entity.AdvertErrorCountLog;
import com.miguan.ballvideo.repositories.AdvertErrorCountLogRepository;
import com.miguan.ballvideo.service.AdvertErrorCountLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AdvertErrorCountLogServiceImpl implements AdvertErrorCountLogService {

    @Resource
    private AdvertErrorCountLogRepository advertErrorCountLogRepository;

    @Override
    public void save(List<AdvertErrorCountLog> datas) {
        for(AdvertErrorCountLog advertErrorCountLog:datas){
            AdvertErrorCountLog advertErrorCountLogDB = advertErrorCountLogRepository
                    .findFirstByAdIdAndCreatTimeAndDeviceIdAndAppPackageAndAppVersion(
                            advertErrorCountLog.getAdId(),
                            advertErrorCountLog.getCreatTime(),
                            advertErrorCountLog.getDeviceId(),
                            advertErrorCountLog.getAppPackage(),
                            advertErrorCountLog.getAppVersion());
            try {
                if (advertErrorCountLogDB == null) {
                    advertErrorCountLogRepository.save(advertErrorCountLog);
                } else {
                    advertErrorCountLogDB.setRenderFailed(advertErrorCountLogDB.getRenderFailed() + advertErrorCountLog.getRenderFailed());
                    advertErrorCountLogDB.setRenderSuccess(advertErrorCountLogDB.getRenderSuccess() + advertErrorCountLog.getRenderSuccess());
                    advertErrorCountLogDB.setRequestFailed(advertErrorCountLogDB.getRequestFailed() + advertErrorCountLog.getRequestFailed());
                    advertErrorCountLogDB.setRequestSuccess(advertErrorCountLogDB.getRequestSuccess() + advertErrorCountLog.getRequestSuccess());
                    advertErrorCountLogDB.setShowFailed(advertErrorCountLogDB.getShowFailed() + advertErrorCountLog.getShowFailed());
                    advertErrorCountLogDB.setShowSuccess(advertErrorCountLogDB.getShowSuccess() + advertErrorCountLog.getShowSuccess());
                    advertErrorCountLogDB.setTotalNum(advertErrorCountLogDB.getTotalNum() + advertErrorCountLog.getTotalNum());
                    advertErrorCountLogRepository.save(advertErrorCountLogDB);
                }
            } catch (Exception e) {
                log.error("AdvertErrorCountLogSave_error："+ JSON.toJSONString(advertErrorCountLog));
            }
        }
    }


}
