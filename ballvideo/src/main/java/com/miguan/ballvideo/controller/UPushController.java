package com.miguan.ballvideo.controller;

import com.miguan.ballvideo.common.util.DateUtil;
import com.miguan.ballvideo.common.util.Global;
import com.miguan.ballvideo.common.util.ResultMap;
import com.miguan.ballvideo.entity.PushArticle;
import com.miguan.ballvideo.service.*;
import com.miguan.message.push.utils.UPushSendMessageUtil;
import com.miguan.message.push.utils.huawei.HuaweiPush;
import com.miguan.message.push.utils.huawei.MessageBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 友盟消息推送联调接口
 * <p>
 * 友盟消息推送接口修改（单个推送，固定PHP传入 推送id  进行推送）   2019年9月25日15:46:14     HYL
 *
 * @Author shixh
 * @Date 2019/9/10
 **/
@Slf4j
@Api(value="消息推送接口",tags={"消息推送接口"})
@RequestMapping("/api/uPush")
@RestController
public class UPushController {

    @Resource
    private PushArticleService pushArticleService;
    @Resource
    private ClUserService userService;
    @Resource
    private PushSevice pushSevice;

    @ApiOperation("安卓推送测试")
    @PostMapping("/sendToIOS")
    public ResultMap sendToIOS() {
        try {
            PushArticle pushArticle = pushArticleService.getOneToPush();
            if (pushArticle == null || StringUtils.isBlank(pushArticle.getTitle())
                    || StringUtils.isBlank(pushArticle.getNoteContent())) {
                log.info("当前没有符合条件的推送数据");
            }
            Map<String, String> param = new HashMap<String, String>();
            param.put("xy_type", pushArticle.getType() + "");
            param.put("xy_typeValue", pushArticle.getTypeValue() == null ? "" : pushArticle.getTypeValue());

            String appkey_ios = Global.getValue("appKey_uPush_ios");
            String appMasterSecret_ios = Global.getValue("secret_uPush_ios");
            String mode = Global.getValue("mode_uPush");
            UPushSendMessageUtil.sendToIOS(pushArticle.getTitle(), "", pushArticle.getNoteContent(), param, appkey_ios, appMasterSecret_ios, mode);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultMap.error("发送失败，请联系管理员");
        }
        return ResultMap.success();
    }

    @ApiOperation("苹果推送测试")
    @PostMapping("/sendToAndriod")
    public ResultMap sendToAndriod() {
        try {
            PushArticle pushArticle = pushArticleService.getOneToPush();
            if (pushArticle == null || StringUtils.isBlank(pushArticle.getTitle())
                    || StringUtils.isBlank(pushArticle.getNoteContent())) {
                log.info("当前没有符合条件的推送数据");
            }
            Map<String, String> param = new HashMap<String, String>();
            param.put("xy_type", pushArticle.getType() + "");
            param.put("xy_typeValue", pushArticle.getTypeValue() == null ? "" : pushArticle.getTypeValue());


            String mode = Global.getValue("mode_uPush");
            String appkey_android = Global.getValue("appKey_uPush_android");
            String appMasterSecret_android = Global.getValue("secret_uPush_android");

            String appId = Global.getValue("huawei_app_id");
            String appSecret = Global.getValue("huawei_app_secret");

            String upush_expireTime = Global.getValue("uPush_expireTime");
            String expireTime = DateUtil.parseDateToStr(new Date(System.currentTimeMillis() + 1000 * 60 * Integer.parseInt(upush_expireTime)), "yyyy-MM-dd HH:mm:ss");
            UPushSendMessageUtil.sendToAndriod(pushArticle.getTitle(), pushArticle.getNoteContent(), param, appkey_android, appMasterSecret_android, mode, expireTime, "com.mg.xyvideo.module.main.MainActivity");

            final List<String> allHuaweiTotken = userService.findAllHuaweiTotken(pushArticle);
            HuaweiPush.push(new MessageBody(pushArticle.getTitle(), pushArticle.getNoteContent(), param), pushArticle.getExpireTime(), allHuaweiTotken,appId,appSecret);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultMap.error("发送失败，请联系管理员");
        }
        return ResultMap.success();
    }

    @ApiOperation("立即推送")
    @PostMapping("/realTimeSendInfo")
    public ResultMap realTimeSendInfo(Long id) {
        return pushSevice.realTimeSendInfo(id);
    }

    /**
     * 1、能根据推送ID，推送厂商进行单独厂商推送测试；
     * 2、能根据推送token，推送厂商进行制定用户单独推送测试；
     *
     * @param id          推送ID
     * @param tokens      推送tokens(参数有token，根据token来发送；无，则取相对应表字段值) 多个用逗号隔开
     * @param pushChannel 推送厂商 YouMeng,HuaWei,VIVO,OPPO,XiaoMi;
     * @return
     */
    @ApiOperation("立即推送测试接口")
    @PostMapping("/realTimePushTest")
    public ResultMap realTimePushTest(Long id, String tokens, String pushChannel) {
        return pushSevice.realTimePushTest(id, tokens, pushChannel);
    }
}
