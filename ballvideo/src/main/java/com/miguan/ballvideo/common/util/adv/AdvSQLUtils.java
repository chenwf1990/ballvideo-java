package com.miguan.ballvideo.common.util.adv;

import com.miguan.ballvideo.common.constants.Constant;
import com.miguan.ballvideo.common.util.DateUtil;
import com.miguan.ballvideo.entity.AdvertErrorLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @Author shixh
 * @Date 2020/4/22
 **/
@Slf4j
public class AdvSQLUtils {

    public static final String ERROR_KEY = "AdvertErrorLog_";

    //代码位字段
    public static final String adCode_fields = "acode.id,acode.ad_id as adId,acode.material_key as material,acode.plat_key as plat,acode.render_key as render,acode.sdk_key as sdk,acode.type_key as adType,acode.permission as permission,";
    //广告位字段
    public static final String aPosition_fields= "aposition.id as positionId,aposition.position_type as positionType,aposition.mobile_type as mobileType,aposition.first_load_position as firstLoadPosition,aposition.second_load_position as secondLoadPosition,aposition.max_show_num as maxShowNum,";
    //广告配置代码位字段
    public static final String aConfigCode_fields= "aconfigcode.option_value as optionValue,";
    //广告配置字段
    public static final String aConfig_fields= "aconfig.computer as computer,";
    //98创意广告字段
    public static final String iAdvert_fields= "idea.title,idea.type_url AS url,idea.type AS linkType,idea.image_url AS imgPath,";

    public static final String position_type_fields= "aposition.id as positionId,aposition.position_type as positionType,";

    public static String splicingSQL(List<AdvertErrorLog> collect) {
        StringBuffer sb = new StringBuffer("INSERT INTO `ad_error_1` (`ad_id`, `ad_error`, `type_key`, `plat_key`,`app_package`,`app_version`,`device_id`,`mobile_type`,`creat_time`,`render`,`channel_id`,`position_id`) VALUES ");
        for (AdvertErrorLog adError : collect) {
            if(StringUtils.isNotEmpty(adError.getAdError()) && adError.getAdError().contains("java.net.ProtocolException")){
                log.error(ERROR_KEY+"乱码不保存："+adError.getAdError());
                continue;
            }
            sb.append("(");
            sb.append("'" + adError.getAdId() + "'" + ",");
            sb.append("'" + adError.getAdError() + "'" + ",");
            sb.append("'" + adError.getTypeKey() + "'" + ",");
            sb.append("'" + adError.getPlatKey() + "'" + ",");
            sb.append("'" + adError.getAppPackage() + "'" + ",");
            sb.append("'" + adError.getAppVersion() + "'" + ",");
            sb.append("'" + adError.getDeviceId() + "'" + ",");
            sb.append("'" + adError.getMobileType() + "'" + ",");
            sb.append("'" + DateUtil.parseDateToStr(adError.getCreatTime(), "yyyy-MM-dd HH:mm:ss") + "'" + ",");
            sb.append("'" + adError.getRender() + "'" + ",");
            sb.append("'" + adError.getChannelId() + "'" + ",");
            sb.append(StringUtils.isBlank(adError.getPositionId())?0:adError.getPositionId());//广告位置ID为空，默认0
            sb.append(")");
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(";");
        return sb.toString();
    }

  /**
   * type=0,查询所有的
   * type=1,只查询广告位字段
   * @param type
   * @return
   */
  public static StringBuffer getAdvFields(int type) {
        StringBuffer sb = new StringBuffer("");
        switch (type){
            case 1: sb.append(adCode_fields).append(aPosition_fields).append(aConfigCode_fields).append(aConfig_fields).append(iAdvert_fields);
            break;
            case 2: sb.append(position_type_fields);
            break;
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb;
    }

    public static StringBuffer getSqlString(int fieldType) {
        StringBuffer nativeSql = new StringBuffer("select ");
        nativeSql.append(getAdvFields(fieldType)).append(" ");
        nativeSql.append("from ad_advert_position aposition ");
        nativeSql.append("left join ad_advert_config aconfig on aconfig.position_id = aposition.id ");
        nativeSql.append("left join ad_advert_config_code aconfigcode on aconfigcode.config_id = aconfig.id ");
        nativeSql.append("left join ad_advert_code acode on acode.id = aconfigcode.code_id ");
        nativeSql.append("left join idea_initiative_advert idea on idea.ad_code = acode.ad_id ");
        nativeSql.append("and idea.status = ").append(Constant.open).append(" ");
        nativeSql.append("where aconfig.state = ").append(Constant.open).append(" ");
        return nativeSql;
    }

    public static String getAdverByParams(Map<String, Object> param,int fieldType) {
        String positionType = MapUtils.getString(param, "positionType");
        String mobileType = MapUtils.getString(param, "mobileType");
        String channelId = MapUtils.getString(param, "channelId");
        String permission = MapUtils.getString(param, "permission");
        String appVersion = MapUtils.getString(param, "appVersion");
        String appPackage = MapUtils.getString(param, "appPackage");
        String lockScreen = MapUtils.getString(param, "lockScreen");
        String positionTypes = MapUtils.getString(param, "positionTypes");
        String game = MapUtils.getString(param, "game");
        //查询语句组装
        StringBuffer nativeSql = getSqlString(fieldType);
        nativeSql.append(" and replace('").append(appVersion).append("','.','')+0");
        nativeSql.append(" between replace(acode.version1,'.','')+0");
        nativeSql.append(" and replace(acode.version2,'.','')+0");
        if ("0".equals(permission)) {
            nativeSql.append(" and acode.permission = '").append(permission).append("' ");
        }
        if (StringUtils.isNotEmpty(positionType)) {
            nativeSql.append(" and aposition.position_type = '").append(positionType).append("' ");
        }
        if (StringUtils.isNotEmpty(positionTypes)) {
            nativeSql.append(" and aposition.position_type in (").append(positionTypes).append(") ");
        }
        if (StringUtils.isNotEmpty(lockScreen)) {
            nativeSql.append(" and aposition.position_type in ('lockScreenDeblocking','lockH5ScreenDeblocking','lockAppScreenDeblocking')");
        }
        if (StringUtils.isNotEmpty(game)) {
            nativeSql.append(" and aposition.position_type like concat('Game','%')");
        }
        nativeSql.append(" and aconfig.app_package = '").append(appPackage).append("' ");
        nativeSql.append(" and aposition.mobile_type = '").append(mobileType).append("' ");
        nativeSql.append(" and if(acode.channel_type=2,locate('").append(channelId + ",").append("', concat(acode.channel_ids, ','))>0,1=1 )");
        nativeSql.append(" and if(acode.channel_type=3,locate('").append(channelId + ",").append("', concat(acode.channel_ids, ','))=0,1=1 )");
        return nativeSql.toString();
    }


}
