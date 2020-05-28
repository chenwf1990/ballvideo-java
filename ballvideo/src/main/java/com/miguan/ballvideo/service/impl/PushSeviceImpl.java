package com.miguan.ballvideo.service.impl;

import com.alibaba.fastjson.JSON;
import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.enums.PushChannel;
import com.miguan.ballvideo.common.util.Global;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.common.util.push.PushUtil;
import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.entity.PushArticleConfig;
import com.miguan.ballvideo.service.*;
import com.miguan.message.push.utils.huawei.messaging.SendResponce;
import com.vivo.push.sdk.notofication.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class PushSeviceImpl implements PushSevice {
    @Resource
    private PushArticleService pushArticleService;

    @Resource
    private PushArticleConfigService pushArticleConfigService;

    @Resource
    private ClUserService userService;

    @Resource
    private PushArticleMobileService pushArticleMobileService;

    @Resource
    private PushArticleSendResultService pushArticleSendResultService;

    @Override
    public ResultMap realTimeSendInfo (Long id) {
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
            return ResultMap.error("发送失败，请联系管理员");
        }
        return ResultMap.success();
    }

    @Override
    public ResultMap sendInfoToIOS(PushArticle pushArticle) {
        Map<String, String> param = PushUtil.getParaMap(pushArticle);
        Map<String, Object> pushParams = PushUtil.getExpireTime(pushArticle.getExpireTime());
        pushParams.put("pushArticle", pushArticle);
        return youMengPush(pushParams, param);
    }

    @Override
    public ResultMap sendInfoToAndroid(PushArticle pushArticle) {
        Map<String, String> param = PushUtil.getParaMap(pushArticle);
        Map<String, Object> pushParams = PushUtil.getExpireTime(pushArticle.getExpireTime());
        Map<String, List<String>> tokensMap;
        if ("20".equals(pushArticle.getUserType())) {
            tokensMap = PushUtil.getTokensMap(pushArticle);
        } else {
            tokensMap = PushUtil.getTokensMap(userService.findAllTokens());
        }
        pushParams.put("pushArticle", pushArticle);
        pushParams.put("packageName","com.mg.xyvideo.module.main.MainActivity");
        //友盟广播推送
        this.youMengPush(pushParams, param);
        //华为广播推送
        this.huaweiPush(pushParams, param, tokensMap);
        //vivo广播推送
        this.vivoPush(pushParams, param, tokensMap);
        //oppo广播推送
        this.oppoPush(pushParams, param, tokensMap);
        //小米广播推送
        this.xiaomiPush(pushParams, param, tokensMap);
        return null;
    }

    @Override
    public ResultMap realTimePushTest(Long id, String tokens, String pushChannel) {
        PushArticle pushArticle = pushArticleService.findOneToPush(id);
        if (pushArticle == null || StringUtils.isBlank(pushArticle.getTitle())
                || StringUtils.isBlank(pushArticle.getNoteContent())) {
            return ResultMap.error("当前没有符合条件的推送数据");
        }
        Map<String, String> param = PushUtil.getParaMap(pushArticle);
        //获取各个厂商的tokens,tokens有值时，取tokens值，否则取表里的值
        Map<String, List<String>> tokensMap = new HashMap<>();
        if (StringUtils.isNotEmpty(tokens)) {
            tokensMap.put(pushChannel, Arrays.asList(tokens.split(",")));
        } else {
            if ("20".equals(pushArticle.getUserType())) {
                tokensMap = PushUtil.getTokensMap(pushArticle);
            } else {
                tokensMap = PushUtil.getTokensMap(userService.findAllTokens());
            }
        }
        PushChannel channel = PushChannel.val(pushChannel);
        Map<String, Object> pushParams = PushUtil.getExpireTime(pushArticle.getExpireTime());
        pushParams.put("pushArticle", pushArticle);
        pushParams.put("packageName","com.mg.xyvideo.module.main.MainActivity");
        //根据厂商调用相对应的推送接口
        switch (channel) {
            case YouMeng:
                return this.youMengPush(pushParams, param);//youMeng广播推送
            case OPPO:
                return this.oppoPush(pushParams, param, tokensMap);//oppo广播推送
            case XiaoMi:
                return this.xiaomiPush(pushParams, param, tokensMap);//小米广播推送
            case VIVO:
                return this.vivoPush(pushParams, param, tokensMap);//vivo指定用户推送
            case HuaWei:
                return this.huaweiPush(pushParams, param, tokensMap);//华为广播推送
            default:
                return ResultMap.error("channel=" + channel + "未配置推送。");
        }
    }

    //友盟推送
    private ResultMap youMengPush(Map<String, Object> pushParams, Map<String, String> param) {
        PushArticle pushArticle = (PushArticle) pushParams.get("pushArticle");
        if ("20".equals(pushArticle.getUserType()) && StringUtils.isBlank(pushArticle.getDeviceTokens())) {
            return null;
        }
        String mobileType = Constant.ANDROID;
        if (Constant.IOSPACKAGE.equals(pushArticle.getAppPackage())) {
            mobileType = Constant.IOS;
        }
        PushArticleConfig youMeng = pushArticleConfigService.findPushArticleConfig(PushChannel.YouMeng.name(), mobileType, pushArticle.getAppPackage());
        if (youMeng == null) return ResultMap.error("友盟推送参数未配置！");
        pushParams.put(PushChannel.YouMeng.name(), youMeng);
        List<Map> resultList;
        if (Constant.IOSPACKAGE.equals(pushArticle.getAppPackage())) {
            resultList = pushArticleMobileService.youMengIosPushInfo(pushParams, param);
        } else {
            resultList = pushArticleMobileService.youMengAndroidPushInfo(pushParams, param);
        }
        if (CollectionUtils.isEmpty(resultList)) {
            return ResultMap.error("友盟推送失败！");
        }
        for (Map mapData : resultList) {
            String taskId = mapData.get("task_id").toString();
            if ("20".equals(pushArticle.getUserType())) {
                taskId = mapData.get("msg_id").toString();
            }
            saveSendResultInfo(pushArticle, taskId, PushChannel.YouMeng.name());
        }
        return null;
    }

    //华为推送
    private ResultMap huaweiPush(Map<String, Object> pushParams, Map<String, String> param,
                                 Map<String, List<String>> tokensMap) {
        PushArticle pushArticle = (PushArticle) pushParams.get("pushArticle");
        PushArticleConfig huaWei = pushArticleConfigService.findPushArticleConfig(PushChannel.HuaWei.name(), Constant.ANDROID, pushArticle.getAppPackage());
        if (huaWei == null) return ResultMap.error("华为推送参数未配置！");
        pushParams.put(PushChannel.HuaWei.name(), huaWei);
        List<String> list = tokensMap.get(PushChannel.HuaWei.name());
        if (CollectionUtils.isEmpty(list)) {
            log.error("huaWeiTokens为空，不调用推送接口");
            return null;
        }
        List<SendResponce> sendResponceList = pushArticleMobileService.huaweiPushInfo(pushParams, param, list);
        if (CollectionUtils.isEmpty(sendResponceList)) {
            return ResultMap.error("华为推送失败！");
        }
        for (SendResponce sendResponce : sendResponceList) {
            if (sendResponce != null) {
                saveSendResultInfo(pushArticle, sendResponce.getRequestId(), PushChannel.HuaWei.name());
            }
        }
        return null;
    }

    //vivo推送
    private ResultMap vivoPush(Map<String, Object> pushParams, Map<String, String> param,
                               Map<String, List<String>> tokensMap) {
        PushArticle pushArticle = (PushArticle) pushParams.get("pushArticle");
        if ("20".equals(pushArticle.getUserType()) && StringUtils.isBlank(pushArticle.getDeviceTokens())) {
            return null;
        }
        PushArticleConfig vivo = pushArticleConfigService.findPushArticleConfig(PushChannel.VIVO.name(), Constant.ANDROID, pushArticle.getAppPackage());
        if (vivo == null) return ResultMap.error("vivo推送参数未配置！");
        pushParams.put(PushChannel.VIVO.name(), vivo);
        List<Result> resultList = new ArrayList<>();
        if ("20".equals(pushArticle.getUserType())) {
            //指定用户推送
            List<String> list = tokensMap.get(PushChannel.VIVO.name());
            resultList = pushArticleMobileService.vivoPushByRegIds(pushParams, param, list);
        } else {
            //全部用户推送 vivo没有测试账号,测试广播推送会影响到正式服用户
            String appEnvironment = Global.getValue("app_environment");
            if ("prod".equals(appEnvironment)) {
                Result result = pushArticleMobileService.vivoPushAll(pushParams, param);
                if (result != null && result.getResult() == 0) {
                    resultList.add(result);
                }
            }
        }
        if (CollectionUtils.isEmpty(resultList)) {
            return ResultMap.error("vivo推送失败");
        }
        for (Result result : resultList) {
            if (result != null && result.getResult() == 0) {
                saveSendResultInfo(pushArticle, result.getTaskId(), PushChannel.VIVO.name());
            }
        }
        return null;
    }

    //oppo推送
    private ResultMap oppoPush(Map<String, Object> pushParams, Map<String, String> param,
                               Map<String, List<String>> tokensMap) {
        List<String> list = tokensMap.get(PushChannel.OPPO.name());
        if (CollectionUtils.isEmpty(list)) {
            log.info("oppoTokens为空，不调用推送接口");
            return null;
        }
        PushArticle pushArticle = (PushArticle) pushParams.get("pushArticle");
        PushArticleConfig oppo = pushArticleConfigService.findPushArticleConfig(PushChannel.OPPO.name(), Constant.ANDROID, pushArticle.getAppPackage());
        if (oppo == null) return ResultMap.error("oppo推送参数未配置！");
        pushParams.put(PushChannel.OPPO.name(), oppo);
        List<com.oppo.push.server.Result> resultList = pushArticleMobileService.oppoPushByRegIds(pushParams, param, list);
        if (CollectionUtils.isEmpty(resultList)) {
            return ResultMap.error("OPPO推送失败");
        }
        for (com.oppo.push.server.Result result : resultList) {
            if (result != null) {
                String taskId = result.getTaskId() == null ? result.getMessageId() : result.getTaskId();
                saveSendResultInfo(pushArticle, taskId, PushChannel.OPPO.name());
            }
        }
        return null;
    }

    //小米推送
    private ResultMap xiaomiPush(Map<String, Object> pushParams, Map<String, String> param,
                                 Map<String, List<String>> tokensMap) {
        List<String> list = tokensMap.get(PushChannel.XiaoMi.name());
        if (CollectionUtils.isEmpty(list)) {
            log.info("xiaomiTokens为空，不调用推送接口");
            return null;
        }
        PushArticle pushArticle = (PushArticle) pushParams.get("pushArticle");
        PushArticleConfig xiaomi = pushArticleConfigService.findPushArticleConfig(PushChannel.XiaoMi.name(), Constant.ANDROID, pushArticle.getAppPackage());
        if (xiaomi == null) return ResultMap.error("xiaomi推送参数未配置！");
        pushParams.put(PushChannel.XiaoMi.name(), xiaomi);
        List<com.xiaomi.xmpush.server.Result> resultList = pushArticleMobileService.xiaomiPushInfo(pushParams, param, list);
        if (CollectionUtils.isEmpty(resultList)) {
            return ResultMap.error("小米推送失败");
        }
        for (com.xiaomi.xmpush.server.Result result : resultList) {
            if (result != null) {
                saveSendResultInfo(pushArticle, result.getMessageId(), PushChannel.XiaoMi.name());
            }
        }
        return null;
    }

    /**
     * 保存推送信息结果
     *
     * @param pushArticle
     * @param businessId
     * @param pushChannel
     */
    private void saveSendResultInfo(PushArticle pushArticle, String businessId, String pushChannel) {
        try {
            pushArticleSendResultService.saveSendResult(pushArticle.getId(), pushChannel, businessId, pushArticle.getAppPackage());
        } catch (Exception e) {
            log.error("保存推送参数:" + JSON.toJSONString(pushArticle));
            log.error("保存推送参数:" + "businessId=" + businessId + ",pushChannel=" + pushChannel);
            log.error("保存推送息异常:" + e.getMessage(), e);
        }
    }
}
