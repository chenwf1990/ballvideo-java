package com.miguan.ballvideo.service.impl;

import com.alibaba.fastjson.JSON;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.enums.PushChannel;
import com.miguan.ballvideo.common.util.StringUtil;
import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.entity.PushArticleConfig;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.repositories.PushArticleConfigJpaRepository;
import com.miguan.ballvideo.repositories.PushArticleDao;
import com.miguan.ballvideo.service.*;
import com.miguan.message.push.model.PushMessageBody;
import com.miguan.message.push.utils.oppo.OppoPush;
import com.miguan.message.push.utils.vivo.VivoPush;
import com.miguan.message.push.utils.xiaomi.XiaomiPush;
import com.vivo.push.sdk.notofication.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 消息推送
 * Created by shixh on 2019/9/10.
 *
 */
@Slf4j
@Service
public class PushArticleServiceImpl implements PushArticleService {

    public final int pushType = 1; //1 定时 2立即推送

    @Resource
    private PushArticleDao pushArticleDao;

    @Resource
    private RedisService redisService;

    @Resource
    private PushArticleSendResultService pushArticleSendResultService;

    @Resource
    private PushArticleConfigJpaRepository pushArticleConfigJpaRepository;

    @Resource
    private PushArticleConfigService pushArticleConfigService;

    @Resource
    private ClUserService userService;

    @Override
    public PushArticle getOneToPush() {
        List<PushArticle> pushArticles = pushArticleDao.findByState(Constant.open);
        if(CollectionUtils.isEmpty(pushArticles))return null;
        int nums = pushArticles.size();
        int index = getRandomBetween(0,nums);
        return pushArticles.get(index);
    }

    @Override
    public PushArticle findOneToPush(Long id) {
        Optional<PushArticle> optional = pushArticleDao.findById(id);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    public static int getRandomBetween(int first,int second){
        return (int)((second-first)* Math.random()+first);
    }

    @Override
    public List<PushArticle> findFixedTimeListToPush() {
        List<PushArticle> fixedTimeList = new ArrayList<>();
        List<PushArticle> list = pushArticleDao.findAllByStateAndPushType(Constant.open, pushType);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        for (PushArticle pushArticle : list) {
            String pushTime = pushArticle.getPushTime();
            if (StringUtils.isEmpty(pushTime)){
                continue;
            }
            //推送时间大于当前时间
            Date parse = null;
            try {
                parse = sdf.parse(pushTime);
            } catch (ParseException e) {
               log.error("[{}],时间转换异常", pushTime);
            }
            if (parse.after(date)){
                fixedTimeList.add(pushArticle);
            }
        }
        return fixedTimeList;
    }

    @Override
    public void vivoPushByRegIds(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception {
        PushMessageBody pushMessageBody = getVivoPushMessageBody(pushArticle, param, expireTime);
        List<String> regIdsList =new ArrayList<String>(0);
        StringUtil.splitStringArray(pushArticle.getVivoTokens(), ",", 1000, regIdsList);
        for (String str : regIdsList) {
            Result result = null;
            if (str.contains(",")){
                //vivo批量推送（regId个数大于等于2，小于等于1000）
                String[] split = str.split(",");
                pushMessageBody.setRegIds(Arrays.asList(split));
                pushMessageBody.setTaskId(VivoPush.saveListPayload(pushMessageBody));
                result = VivoPush.pushMulti(pushMessageBody);
            }else {
                //vivo单推
                List<String> list = new ArrayList<>();
                list.add(str);
                pushMessageBody.setRegIds(list);
                result = VivoPush.pushSingle(pushMessageBody);
            }
            try {
                if (result!=null&&result.getResult()==0){
                    //保存推送信息
                    pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.VIVO.name(), result.getTaskId(), Constant.ANDROIDPACKAGE);
                }
            }catch (Exception e){
                log.error("推送ID:"+pushArticle.getId()+",保存vivo推送信息异常:[{}]", e.getMessage());
            }

        }
    }

    @Override
    public void vivoPushAll(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception{
        PushMessageBody pushMessageBody = getVivoPushMessageBody(pushArticle, param, expireTime);
        Result result = VivoPush.pushAll(pushMessageBody);
        try {
            if (result!=null&&result.getResult()==0){
                //保存推送信息
                pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.VIVO.name(), result.getTaskId(), Constant.ANDROIDPACKAGE);
            }
        }catch (Exception e){
            log.error("推送ID:"+pushArticle.getId()+",保存vivo推送信息异常:[{}]", e.getMessage());
        }
    }

    //vivo组装PushMessageBody信息
    private PushMessageBody getVivoPushMessageBody(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception {
        PushArticleConfig pushArticleConfig = pushArticleConfigJpaRepository.findByPushChannelAndMobileTypeAndAppPackage(PushChannel.VIVO.name(), Constant.ANDROID, Constant.ANDROIDPACKAGE);
        PushMessageBody pushMessageBody = new PushMessageBody();
        pushMessageBody.setMasterSecret(pushArticleConfig.getAppSecret());
        String vivoToken = redisService.get(RedisKeyConstant.VIVO_TOKEN, String.class);
        if (StringUtils.isEmpty(vivoToken)){
            pushMessageBody.setAppId(pushArticleConfig.getAppId());
            pushMessageBody.setAppKey(pushArticleConfig.getAppKey());
            vivoToken = VivoPush.getToken(pushMessageBody);
            //vivio鉴权有效期默认为1天，过期后无法使用,1小时重新请求存放redis
            redisService.set(RedisKeyConstant.VIVO_TOKEN, vivoToken, 3600);
        }
        pushMessageBody.setToken(vivoToken);
        pushMessageBody.setTitle(pushArticle.getTitle());
        pushMessageBody.setMessagePayload(pushArticle.getNoteContent());
        pushMessageBody.setExpireTime(Integer.valueOf(expireTime));
        pushMessageBody.setParams(param);
        return pushMessageBody;
    }

    @Override
    public void oppoPushByRegIds(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception {
        PushMessageBody pushMessageBody = getOppoPushMessageBody(pushArticle, param, expireTime);
        List<String> regIdsList =new ArrayList<String>(0);
        StringUtil.splitStringArray(pushArticle.getOppoTokens(), ",", 1000, regIdsList);
        com.oppo.push.server.Result result = null;
        for (String str : regIdsList) {
            if (str.contains(",")){
                //oppo批量推送
                String[] split = str.split(",");
                pushMessageBody.setRegIds(Arrays.asList(split));
                result = OppoPush.pushMulti(pushMessageBody);
            }else {
                //oppo单推
                List<String> list = new ArrayList<>();
                list.add(str);
                pushMessageBody.setRegIds(list);
                result = OppoPush.pushSingle(pushMessageBody);
            }
            try {
                if (result!=null){
                    log.info("oppo推送:" + result);
                    //保存推送信息
                    pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.OPPO.name(), result.getMessageId(), Constant.ANDROIDPACKAGE);
                }
            }catch (Exception e){
                log.error("推送ID:"+pushArticle.getId()+",保存oppo推送信息异常:[{}]", e.getMessage());
            }
        }
    }

    @Override
    public void oppoPushAll(PushArticle pushArticle, Map<String, String> param, String expireTime) throws Exception{
        PushMessageBody pushMessageBody = getOppoPushMessageBody(pushArticle, param, expireTime);
        List<String> regIds = userService.findAllOppoToken();
        pushMessageBody.setRegIds(regIds);
        com.oppo.push.server.Result result = OppoPush.pushAll(pushMessageBody);
        try {
            if (result!=null){
                //保存推送信息
                pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.OPPO.name(), result.getTaskId(), Constant.ANDROIDPACKAGE);
            }
        }catch (Exception e){
            log.error("推送ID:"+pushArticle.getId()+",保存oppo推送信息异常:[{}]", e.getMessage());
        }
    }

    //oppo组装PushMessageBody信息
    private PushMessageBody getOppoPushMessageBody(PushArticle pushArticle, Map<String, String> param, String expireTime) {
        PushArticleConfig pushArticleConfig = pushArticleConfigJpaRepository.findByPushChannelAndMobileTypeAndAppPackage(PushChannel.OPPO.name(), Constant.ANDROID, Constant.ANDROIDPACKAGE);
        PushMessageBody pushMessageBody = new PushMessageBody();
        pushMessageBody.setMasterSecret(pushArticleConfig.getAppSecret());
        pushMessageBody.setAppId(pushArticleConfig.getAppId());
        pushMessageBody.setAppKey(pushArticleConfig.getAppKey());
        pushMessageBody.setTitle(pushArticle.getTitle());
        pushMessageBody.setMessagePayload(pushArticle.getNoteContent());
        pushMessageBody.setExpireTime(Integer.parseInt(expireTime));
        pushMessageBody.setParams(param);
        return pushMessageBody;
    }

    @Override
    public void xiaomiPushAll(PushArticle pushArticle, Map<String, String> params, String mobileType, int expireTime, String type) throws Exception{
        List<String> regIds;
        if ("1".equals(type)) {
            regIds = new ArrayList<>(0);
            StringUtil.splitStringArray(pushArticle.getXiaomiTokens(), ",", 1000, regIds);
        } else {
            regIds = userService.findAllXiaoMiTotken(pushArticle);
        }
        if (regIds != null) {
            PushMessageBody pushMessageBody = getXiaomiPushMessageBody(pushArticle, regIds, params, expireTime, mobileType);
            com.xiaomi.xmpush.server.Result result = XiaomiPush.push(pushMessageBody);
            try {
                if (result != null) {
                    //保存推送信息
                    pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.XiaoMi.name(), result.getMessageId(), pushArticle.getAppPackage());
                }
            } catch (Exception e) {
                log.error("推送ID:" + pushArticle.getId() + ",保存xiaomi推送信息异常:[{}]", e.getMessage());
            }
        }
    }

    //xiaomi组装PushMessageBody信息
    private PushMessageBody getXiaomiPushMessageBody(PushArticle pushArticle, List<String> regIds, Map<String, String> params, int expireTime, String mobileType) {
        String[] appNames = {pushArticle.getAppPackage()};
        PushArticleConfig pushArticleConfig = pushArticleConfigService.findPushArticleConfig(PushChannel.XiaoMi.name(), mobileType, pushArticle.getAppPackage());
        PushMessageBody pushMessageBody = new PushMessageBody();
        pushMessageBody.setRegIds(regIds);
        pushMessageBody.setMasterSecret(pushArticleConfig.getAppSecret());
        pushMessageBody.setTitle(pushArticle.getTitle());
        pushMessageBody.setDescription(pushArticle.getNoteContent());
        pushMessageBody.setMessagePayload(JSON.toJSON(params).toString());
        pushMessageBody.setMobileType(mobileType);
        pushMessageBody.setAppNames(appNames);
        pushMessageBody.setAppId(pushArticleConfig.getAppId());
        pushMessageBody.setExpireTime(expireTime);
        return pushMessageBody;
    }

}