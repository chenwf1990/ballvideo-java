package com.miguan.ballvideo.common.constants;


/**
 * 公用常量类定义
 *
 * @author xy.chen
 * @version 1.0.0
 * @date 2019-06-20 10:48:24
 */
public class Constant {

	public static final String RESPONSE_CODE = "code";

	public static final String RESPONSE_CODE_MSG = "message";
	
	public static final int CLIENT_EXCEPTION_CODE_VALUE = 998; // 连接异常（除请求超时）
	
	public static final int TIMEOUT_CODE_VALUE = 999; // 请求超时

	//开关状态
	public static final int open = 1;//开启
	public static final int close = 0;//关闭

	//手机类型iOS
	public static final String IOS_MOBILE = "1";
	//锁屏广告
	public static final String LOCK_SCREEN_POSITION = "lockScreenDeblocking";

	public static final String ANDROIDPACKAGE = "com.mg.xyvideo";

	public static final String IOSPACKAGE = "com.mg.westVideo";

	public static final String IOS = "1";//手机类型：1-ios
	public static final String ANDROID = "2";//手机类型：2：安卓

	//开关状态
	public static final String OPENSTR = "1";//开启

	public static final String GGSPPACKAGE = "com.mg.ggvideo";
	public static final String MTSPPACKAGE = "com.mg.mtvideo";

	public static final String COMPARE_APPVERSION = "2.4.9";//V2.5.0后广告走新逻辑

	public static final double APPVERSION_259 = 2.52;//V2.5.9版本号
	public static final String APPVERSION_253 = "2.53";//V2.5.3版本号
	public static final String APPVERSION_257 = "2.57";//V2.5.7版本号

	public static final int ADV_MAX_NUM = 10;//广告返回个数
}
