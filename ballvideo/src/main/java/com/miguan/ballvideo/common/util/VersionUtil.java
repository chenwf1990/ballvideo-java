package com.miguan.ballvideo.common.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * @Author shixh
 * @Date 2019/12/1
 **/
public class VersionUtil {

    //如果大于2.5版本，则不返回集合其余信息  V2.5.0
    public static final String GATHER_VERSION = "2.5.0";

    /**
     * 判断2位数的版本，比如2.3
     * @param appVersion
     * @param v
     * @return
     */
    public static boolean isHigh(String appVersion, double v) {
        if(StringUtils.isEmpty(appVersion) || "null".equals(appVersion))return false;//没传版本号默认返回false；
        int appVersionNum = Integer.parseInt(appVersion.replace(".",""));
        BigDecimal bg = new BigDecimal(v * 100);
        double doubleValue = bg.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
        return appVersionNum>(int)doubleValue;
    }

  /**
   * 判断3位数的版本，比如2.3.1
   * @param appVersion
   * @param v
   * @return
   */
  public static boolean isHigh(String appVersion, String v) {
        if(StringUtils.isEmpty(appVersion))return false;//没传版本号默认返回false；
        int appVersionNum = Integer.parseInt(appVersion.replace(".",""));
        int v2 = Integer.parseInt(v.replace(".",""));
        return appVersionNum>v2;
    }

    /**
     * 版本号为空处理
     * 1 生产环境存在appVersion="未知版本"问题，添加条件过滤；
     * @param appVersion
     * @return
     */
    public static String getVersion(String appVersion) {
        if (StringUtils.isBlank(appVersion) || "未知版本".equals(appVersion)) {
            return "1.7.0";
        } else {
            return appVersion;
        }
    }

    public static boolean isBetween(String versionStart,String versionEnd,String version) {
        if(StringUtils.isEmpty(versionStart)
                || StringUtils.isEmpty(versionEnd)
                || StringUtils.isEmpty(version))return false;//没传版本号默认返回false；
        int vStart = Integer.parseInt(versionStart.replace(".",""));
        int vEnd = Integer.parseInt(versionEnd.replace(".",""));
        int v = Integer.parseInt(version.replace(".",""));
        return vStart<=v && v<=vEnd;
    }
}
