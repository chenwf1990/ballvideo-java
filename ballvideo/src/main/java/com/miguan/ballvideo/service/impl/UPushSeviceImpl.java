package com.miguan.ballvideo.service.impl;

import com.alibaba.fastjson.JSON;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.enums.PushChannel;
import com.miguan.ballvideo.common.util.*;
import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.entity.PushArticleConfig;
import com.miguan.ballvideo.service.*;
import com.miguan.message.push.utils.UPushSendMessageUtil;
import com.miguan.message.push.utils.huawei.HuaweiPush;
import com.miguan.message.push.utils.huawei.MessageBody;
import com.miguan.message.push.utils.huawei.messaging.SendResponce;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service("uPushSevice")
public class UPushSeviceImpl implements UPushSevice {
    @Resource
    private PushArticleService pushArticleService;

    @Resource
    private PushArticleConfigService pushArticleConfigService;

    @Resource
    private PushArticleSendResultService pushArticleSendResultService;

    @Resource
    private ClUserService userService;

    @Override
    public ResultMap realTimeSendInfo(Long id) {
        try {
            PushArticle pushArticle = pushArticleService.findOneToPush(id);
            if (pushArticle == null || StringUtils.isBlank(pushArticle.getTitle())
                    || StringUtils.isBlank(pushArticle.getNoteContent())) {
                log.error("当前没有符合条件的推送数据");
                return ResultMap.error("当前没有符合条件的推送数据");
            }
            ResultMap resultMap;
            if (Constant.IOSPACKAGE.equals(pushArticle.getAppPackage())) {
                resultMap = sendInfoToIOS(pushArticle);
            } else {
                resultMap = sendInfoToAndroid(pushArticle);
            }
            if (resultMap != null) {
                return resultMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResultMap.error("发送失败，请联系管理员");
        }
        return ResultMap.success();
    }

    @Override
    public ResultMap sendInfoToIOS(PushArticle pushArticle) {
        //马甲包
        String appPackage = pushArticle.getAppPackage();
        Map<String, String> param = new HashMap<String, String>();
        param.put("xy_id", pushArticle.getId() + "");//推送ID
        param.put("xy_type", pushArticle.getType() + "");
        param.put("xy_typeValue", pushArticle.getTypeValue() == null ? "" : pushArticle.getTypeValue());
        param.put("xy_title", pushArticle.getTitle() + "");//华为推送埋点用
        param.put("xy_noteContent", pushArticle.getNoteContent()== null ? "" : pushArticle.getNoteContent());//华为推送埋点用
        param.put("xy_sendTime", SpringTaskUtil.getMillisecond(pushArticle.getPushTime()) + "");
        param.put("thumbnail_url", pushArticle.getThumbnailUrl() == null ? "" : pushArticle.getThumbnailUrl());//缩略图URL
        PushArticleConfig youMengIOS = pushArticleConfigService.findPushArticleConfig(PushChannel.YouMeng.name(), Constant.IOS, appPackage);
        String appkey_ios = youMengIOS.getAppKey();
        String appMasterSecret_ios = youMengIOS.getAppSecret();
        String mode = youMengIOS.getPushModel();
        String userType = pushArticle.getUserType();//用户类型：10全部用户 20指定用户
        String upush_expireTime = pushArticle.getExpireTime();
        //推送有效期，如果实体类有没有配置有效期，取配置表的有效期
        long l = 0L;
        if (!StringUtils.isEmpty(upush_expireTime)) {
            double v = Double.parseDouble(upush_expireTime);
            l = Math.round(v * 60 * 60 * 1000);
        } else {
            upush_expireTime = Global.getValue("uPush_expireTime");
            double v = Double.parseDouble(upush_expireTime);
            l = Math.round(v * 60 * 1000);
        }
        String expireTime = DateUtil.parseDateToStr(new Date(System.currentTimeMillis() + l), "yyyy-MM-dd HH:mm:ss");
        //指定用户推送
        if ("20".equals(userType)) {
            //listcast-列播，要求不超过500个device_token,手动进行拆分，进行循环发送
            String deviceTokens = pushArticle.getDeviceTokens();
            List<String> result =new ArrayList<String>(0);
            StringUtil.splitStringArray(deviceTokens, ",", 500, result);
            for (String str : result) {
                try {
                    String s = UPushSendMessageUtil.sendToIOSListcast(pushArticle.getTitle(), "", pushArticle.getNoteContent(), param, appkey_ios, appMasterSecret_ios, mode, expireTime, str);
                    if (StringUtils.isNotBlank(s)) {
                        //保存推送信息
                        Map map = (Map) JSON.parse(s);
                        Map mapData = (Map) map.get("data");
                        try {
                            pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.YouMeng.name(), mapData.get("msg_id").toString(), appPackage);
                        } catch (Exception e) {
                            log.error("推送ID:" + pushArticle.getId() + ",保存YouMeng推送信息异常:[{}]", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("列播方式ios推送异常", e);
                }
            }
        }else {
            //全部用户推送
            try {
                String s = UPushSendMessageUtil.sendToIOS(pushArticle.getTitle(), "", pushArticle.getNoteContent(), param, appkey_ios, appMasterSecret_ios, mode, expireTime);
                if (StringUtils.isNotBlank(s)) {
                    //保存推送信息
                    Map map = (Map) JSON.parse(s);
                    Map mapData = (Map) map.get("data");
                    try {
                        pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.YouMeng.name(), mapData.get("task_id").toString(), appPackage);
                    } catch (Exception e) {
                        log.error("推送ID:" + pushArticle.getId() + ",保存YouMeng推送信息异常:[{}]", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("广播方式ios推送异常", e);
            }
        }
        return null;
    }

    @Override
    public ResultMap sendInfoToAndroid(PushArticle pushArticle) throws Exception {
        //马甲包
        String appPackage = pushArticle.getAppPackage() == null ? "com.mg.xyvideo" : pushArticle.getAppPackage();
        Map<String, String> param = new HashMap<String, String>();
        param.put("xy_id", pushArticle.getId() + "");//推送ID
        param.put("xy_type", pushArticle.getType() + "");
        param.put("xy_typeValue", pushArticle.getTypeValue() == null ? "" : pushArticle.getTypeValue());
        param.put("xy_title", pushArticle.getTitle() + "");//华为推送埋点用
        param.put("xy_noteContent", pushArticle.getNoteContent()== null ? "" : pushArticle.getNoteContent());//华为推送埋点用
        param.put("xy_sendTime", SpringTaskUtil.getMillisecond(pushArticle.getPushTime()) + "");
        param.put("thumbnail_url", pushArticle.getThumbnailUrl() == null ? "" : pushArticle.getThumbnailUrl());//缩略图URL
        PushArticleConfig youMeng = pushArticleConfigService.findPushArticleConfig(PushChannel.YouMeng.name(), Constant.ANDROID, appPackage);
        String mode = "";
        String appkey_android = "";
        String appMasterSecret_android = "";
        if (youMeng != null) {
            mode = youMeng.getPushModel();
            appkey_android = youMeng.getAppKey();
            appMasterSecret_android = youMeng.getAppSecret();
        }
        PushArticleConfig huaWei = pushArticleConfigService.findPushArticleConfig(PushChannel.HuaWei.name(), Constant.ANDROID, appPackage);
        String appId = "";
        String appSecret = "";
        if (huaWei != null) {
            appId = huaWei.getAppId();
            appSecret = huaWei.getAppSecret();
        }
        String userType = pushArticle.getUserType();//用户类型：10全部用户 20指定用户
        String upush_expireTime = pushArticle.getExpireTime();
        //推送有效期，如果实体类有没有配置有效期，取配置表的有效期
        long l = 0L;
        if (!StringUtils.isEmpty(upush_expireTime)) {
            double v = Double.parseDouble(upush_expireTime);
            l = Math.round(v * 60 * 60 * 1000);
        } else {
            upush_expireTime = Global.getValue("uPush_expireTime");
            double v = Double.parseDouble(upush_expireTime);
            l = Math.round(v * 60 * 1000);
        }
        String expireTime = DateUtil.parseDateToStr(new Date(System.currentTimeMillis() + l), "yyyy-MM-dd HH:mm:ss");
        if (StringUtils.isNotEmpty(appSecret)) {
            final List<String> allHuaweiTotken = userService.findAllHuaweiTotken(pushArticle);
            List<SendResponce> push = HuaweiPush.push(new MessageBody(pushArticle.getTitle(), pushArticle.getNoteContent(), param), pushArticle.getExpireTime(), allHuaweiTotken, appId, appSecret);
            try {
                if (push != null && push.size() != 0) {
                    for (int i = 0; i < push.size(); i++) {
                        //保存推送信息
                        pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.HuaWei.name(), push.get(i).getRequestId(), appPackage);
                    }
                }
            } catch (Exception e) {
                log.error("推送ID:" + pushArticle.getId() + ",保存huaWei推送信息异常:[{}]", e.getMessage());
            }
        }
        if ("20".equals(userType)) {
            //listcast-列播，要求不超过500个device_token,手动进行拆分，进行循环发送
            String deviceTokens = pushArticle.getDeviceTokens();
            List<String> result =new ArrayList<String>(0);
            StringUtil.splitStringArray(deviceTokens, ",", 500, result);
            if (StringUtils.isNotEmpty(appMasterSecret_android)) {
                for (String str : result) {
                    String s = UPushSendMessageUtil.sendToAndriodListcast(pushArticle.getTitle(), pushArticle.getNoteContent(), param, appkey_android, appMasterSecret_android, mode, expireTime, "com.mg.xyvideo.module.main.MainActivity", str);
                        if (StringUtils.isNotBlank(s)) {
                        //保存推送信息
                        Map map = (Map) JSON.parse(s);
                        Map mapData = (Map) map.get("data");
                        try {
                            pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.YouMeng.name(), mapData.get("msg_id").toString(), appPackage);
                        } catch (Exception e) {
                            log.error("推送ID:" + pushArticle.getId() + ",保存YouMeng推送信息异常:[{}]", e.getMessage());
                        }
                    }
                }
            }
            if (StringUtils.isNotEmpty(pushArticle.getVivoTokens())){
                //vivo指定用户推送
                pushArticleService.vivoPushByRegIds(pushArticle, param,  String.valueOf(l/1000));
            }
            if (StringUtils.isNotEmpty(pushArticle.getOppoTokens())){
                //oppo指定用户推送
                pushArticleService.oppoPushByRegIds(pushArticle, param,  String.valueOf(l/1000));
            }
            //小米指定用户推送
            if (StringUtils.isNotEmpty(pushArticle.getXiaomiTokens())) {
                pushArticleService.xiaomiPushAll(pushArticle, param, Constant.ANDROID, (int)l, "1");
            }
        } else {
            //友盟广播推送
            if (StringUtils.isNotEmpty(appMasterSecret_android)) {
                String s = UPushSendMessageUtil.sendToAndriod(pushArticle.getTitle(), pushArticle.getNoteContent(), param, appkey_android, appMasterSecret_android, mode, expireTime, "com.mg.xyvideo.module.main.MainActivity");
                if (StringUtils.isNotBlank(s)) {
                    //保存推送信息
                    Map map = (Map) JSON.parse(s);
                    Map mapData = (Map) map.get("data");
                    try {
                        pushArticleSendResultService.saveSendResult(pushArticle.getId(), PushChannel.YouMeng.name(), mapData.get("task_id").toString(), appPackage);
                    } catch (Exception e) {
                        log.error("推送ID:" + pushArticle.getId() + ",保存YouMeng推送信息异常:[{}]", e.getMessage());
                    }
                }
            }
            String appEnvironment = Global.getValue("app_environment");
            //vivo和oppo没有测试账号,测试广播推送会影响到正式服用户
            if ("prod".equals(appEnvironment)){
                //vivo广播推送
                pushArticleService.vivoPushAll(pushArticle, param, String.valueOf(l/1000));
                //oppo广播推送
                pushArticleService.oppoPushAll(pushArticle, param, String.valueOf(l/1000));
                //小米广播推送
                pushArticleService.xiaomiPushAll(pushArticle, param, Constant.ANDROID, (int)l,"2");
            }
        }
        return null;
    }
}
