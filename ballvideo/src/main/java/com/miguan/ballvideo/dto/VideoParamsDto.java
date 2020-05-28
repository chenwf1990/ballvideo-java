package com.miguan.ballvideo.dto;

import com.miguan.ballvideo.common.aop.validated.NoContains;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 首页推荐接口参数对象
 * @Author shixh
 * @Date 2019/11/30
 * @version 1.8
 **/
@Data
public class VideoParamsDto {
    String userId;//用户ID
    @NotBlank(message = "'设备ID'不能为空")
    String deviceId;//设备ID
    @NotBlank(message = "'广告位置'不能为空")
    String positionType;//广告位置类型,空则查询全部
    @NotBlank(message = "'手机类型'不能为空")
    String mobileType;//手机类型：1-ios，2：安卓
    @NotBlank(message = "'渠道ID'不能为空")
    String channelId;//渠道ID
    String permission;//是否开启储存、IMEI权限：0-未开启，1-开启，安卓调用参数，IOS不传
    @NoContains(character="-",message ="参数异常，占比不能有负数")
    String videoDuty;//视频占比，例如4:3:1，首次访问为空
    @NotBlank(message = "'版本号'不能为空")
    String appVersion;
    String appPackage;
    String lastCatId;//如果没有点击主标签和其他标签，只点击了最近3日的视频，返回它的分类ID作为主标签

    //version2.1.0新增集合视频
    String gatherIds;//要屏蔽的集合ID

    //非前端传递参数字段
    String catId;
    String catIds;
    String otherCatIds;//需要屏蔽的分类
    String showedIds;
    int num;
    int recentlyDay;//最近多少天
    String limitParam;//分页参数，如limitParam=“0,10”，表示 limit 0,10

}
