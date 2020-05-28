package com.miguan.ballvideo.rabbitMQ.util;

public interface RabbitMQConstant {

  String _MQ_ = "@";

  // 用户标签埋点
  String BURYPOINT_LABEL_QUEUE = "xy.send.burypoint.queue";
  String BURYPOINT_LABEL_EXCHANGE = "xy.send.burypoint.exchange";
  String BURYPOINT_LABEL_KEY = "xy.send.burypoint.rutekey";

  // 用户操作埋点
  String BURYPOINT_EXCHANGE = "xy.new.send.burypoint.exchange";
  String BURYPOINT_RUTE_KEY = "xy.new.send.burypoint.rutekey";

  // 用户标签初始化缓存
  String UserLabel_QUEUE = "xy.userLabel.queue";
  String UserLabel_EXCHANGE = "xy.userLabel.exchange";
  String UserLabel_KEY = "xy.userLabel.key";

  // 用户标签保存
  String UserLabel_SAVE_QUEUE = "xy.userLabel.save.queue";
  String UserLabel_SAVE_EXCHANGE = "xy.userLabel.save.exchange";
  String UserLabel_SAVE_KEY = "xy.userLabel.save.key";

  // 推送埋点
  String BURYPOINT_PUSH_QUEUE = "xy.send.push.burypoint.queue";
  String BURYPOINT_PUSH_EXCHANGE = "xy.send.push.burypoint.exchange";
  String BURYPOINT_PUSH_KEY = "xy.send.push.burypoint.rutekey";

  // 视频评论初始化
  String VIDEOS_COMMENT_QUEUE = "xy.send.videos.comment.queue";
  String VIDEOS_COMMENT_EXCHANGE = "xy.send.videos.comment.exchange";
  String VIDEOS_COMMENT_KEY = "xy.send.videos.comment.rutekey";

  //日志记录埋点
  String OPERATE_LOG_QUEUE = "xy.operateLog.queue";
  String OPERATE_LOG_EXCHANGE = "xy.operateLog.exchange";
  String OPERATE_LOG_KEY = "xy.operateLog.key";

  // 用户标签权重分更新
  String USERLABELGRADE_QUEUE = "xy.userLabelGrade.queue";
  String USERLABELGRADE_EXCHANGE = "xy.userLabelGrade.exchange";
  String USERLABELGRADE_KEY = "xy.userLabelGrade.key";

  //视频生成索引数据
  String VIDEOS_ES_SEARCH_QUEUE = "xy.video.es.queue1";
  String VIDEOS_ES_SEARCH_EXCHANGE = "xy.video.es.exchange1";
  String VIDEOS_ES_SEARCH_KEY = "xy.video.es.key1";

  //修改观看数，点赞数等相关视频操作
  String VIDEO_UPDATECOUNT_QUEUE = "xy.video.updateCount.queue";
  String VIDEO_UPDATECOUNT_EXCHANGE = "xy.video.updateCount.exchange";
  String VIDEO_UPDATECOUNT_KEY = "xy.video.updateCount.key";

  //修改视频真实权重
  String VIDEO_REALWEIGHT_UPDATE_QUEUE = "xy.video.realweight.update.queue";
  String VIDEO_REALWEIGHT_UPDATE_EXCHANGE = "xy.video.realweight.update.exchange";
  String VIDEO_REALWEIGHT_UPDATE_KEY = "xy.video.realweight.update.key";

  //用户标签预期未操作删除操作
  String UserLabel_DELETE_QUEUE = "xy.userLabel.delete.queue";
  String UserLabel_DELETE_EXCHANGE = "xy.userLabel.delete.exchange";
  String UserLabel_DELETE_KEY = "xy.userLabel.delete.key";

  //广告展示错误埋点保存
  String AD_ERROR_QUEUE = "xy.ad.error.queue";
  String AD_ERROR_EXCHANGE = "xy.ad.error.exchange";
  String AD_ERROR_KEY = "xy.ad.error.key";

  //广告展示错误统计埋点保存
  String AD_ERROR_COUNT_QUEUE = "xy.ad.error.count.queue";
  String AD_ERROR_COUNT_EXCHANGE = "xy.ad.error.count.exchange";
  String AD_ERROR_COUNT_KEY = "xy.ad.error.count.key";
}
