package com.miguan.ballvideo.service.impl;

import com.miguan.ballvideo.common.util.WebHotNewsUtil;
import com.miguan.ballvideo.entity.HotWord;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.repositories.HotWordJpaRepository;
import com.miguan.ballvideo.service.HotWordService;
import com.miguan.ballvideo.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.cgcg.redis.core.entity.RedisLock;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("hotWordService")
public class HotWordServiceImpl implements HotWordService {

    @Resource
    private HotWordJpaRepository hotWordJpaRepository;

    @Resource
    private RedisService redisService;

    private static String url="https://tophub.today/n/Jb0vmloB1G";
    private static String className="al";
    private static int number=10;

    @Override
    public void getBaiduHotWord(String editor) {
        List<String> hotWortStrList = WebHotNewsUtil.getWebHotVideos(url, className, number);
        if (!hotWortStrList.isEmpty()) {
            List<HotWord> hotWordList = new ArrayList<>();
            for (String word : hotWortStrList) {
                HotWord hotWord = hotWordJpaRepository.findTopByContent(word);
                if (hotWord == null) {
                    hotWord = new HotWord();
                    hotWord.setUpdateDate(new Date());
                    hotWord.setCreateDate(new Date());
                    hotWord.setContent(word);
                    hotWord.setEditor(editor);
                    hotWordList.add(hotWord);
                } else {
                    hotWord.setUpdateDate(new Date());
                    hotWord.setEditor(editor);
                    hotWordList.add(hotWord);
                }
            }
            if (!hotWordList.isEmpty()) {
                try {
                    hotWordJpaRepository.saveAll(hotWordList);
                } catch (Exception e) {
                    log.error("百度热词保存失败。");
                    e.printStackTrace();
                }
            }
        }
    }

    //@Scheduled(cron = "0 0 */4 * * ?")
    public void getBaiduHotWord() {
        RedisLock redisLock = new RedisLock(RedisKeyConstant.HOT_WORD, RedisKeyConstant.HOT_WORD_SECONDS);
        if (redisLock.lock()) {
            getBaiduHotWord("admin");
            log.info("获取百度当日前10热词--定时任务启动成功！");
        }
    }

    @Override
    public List<String> findHotWordInfo() {
        List<HotWord> hotWordList = hotWordJpaRepository.findHotWordInfo();
        if (hotWordList.isEmpty()) {
            return null;
        }
        List<String> resultList = hotWordList.stream().map(HotWord::getContent).collect(Collectors.toList());
        return resultList;
    }

    @Override
    public void freshHotWordInfo() {
        String key = "findHotWordInfo::ballVideos:cacheAble:findHotWordInfo:";
        if (redisService.exits(key)) {
            redisService.delByByte(key);
        }
    }
}
