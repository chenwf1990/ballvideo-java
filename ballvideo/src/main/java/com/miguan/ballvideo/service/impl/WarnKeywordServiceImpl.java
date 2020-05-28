package com.miguan.ballvideo.service.impl;

import com.miguan.ballvideo.common.util.sensitive.SensitiveWordUtil;
import com.miguan.ballvideo.mapper.WarnKeywordMapper;
import com.miguan.ballvideo.service.WarnKeywordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Set;

/**
 * @Author shixh
 * @Date 2019/9/23
 **/
@Slf4j
@Service
public class WarnKeywordServiceImpl implements WarnKeywordService {

    @Autowired
    private WarnKeywordMapper warnKeywordMapper;

    @Override
    public void initWarnKeyword() {
        try {
            Set<String> fonts = warnKeywordMapper.findAllWarnKey();
            if (fonts != null && !fonts.isEmpty()) {
                SensitiveWordUtil.initSensitiveWordMap(fonts);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}
