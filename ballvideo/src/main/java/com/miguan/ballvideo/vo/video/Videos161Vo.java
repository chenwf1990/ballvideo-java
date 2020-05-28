package com.miguan.ballvideo.vo.video;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel("首页视频源VO-V1.6.1")
public class Videos161Vo{

	@ApiModelProperty("视频字段：主键")
	private Long id;

	@ApiModelProperty("视频字段：视频标题")
	private String title;

	@ApiModelProperty("视频字段：分类id")
	private Long catId;

	@ApiModelProperty("视频字段：源图片url")
	private String urlImg;

	@ApiModelProperty("视频字段：白山云视频地址")
	private String bsyUrl;

	@ApiModelProperty("视频字段：白山云图片地址")
	private String bsyImgUrl;

	@ApiModelProperty("视频字段：收藏数")
	private String collectionCount;

	@ApiModelProperty("视频字段：点赞数")
	private String loveCount;

	@ApiModelProperty("视频字段：评论数")
	private String commentCount;

	@ApiModelProperty("视频字段：观看数")
	private String watchCount;

	@ApiModelProperty("视频字段：收藏 0 未收藏 1收藏")
	private String collection;

	@ApiModelProperty("视频字段：点赞 0 未点赞 1点赞")
	private String love;

	@ApiModelProperty("视频字段：视频源头像或者广告url")
	private String urlHeadimg;

	@ApiModelProperty("视频字段：白云山头像地址")
	private String bsyHeadUrl;//用户头像

	@ApiModelProperty("视频字段：视频作者")
	private String videoAuthor;

	@ApiModelProperty("视频字段：时长")
	private String videoTime;

	@ApiModelProperty("视频类型")
	private String videoType;

	@ApiModelProperty("视频字段：分享数")
	private String shareCount;

	@ApiModelProperty("视频字段：完整播放数")
	private String playAllCount;

	@ApiModelProperty("视频字段：视频大小")
	private String videoSize;

	@ApiModelProperty("创建时间")
	private String createdAt;

	//version 1.8
	private int label;//第几标签的视频
	@ApiModelProperty("分类名称")
	private String catName;//神策用到

	//version2.1.0  新增集合视频
	@ApiModelProperty("集合ID")
	private Long gatherId;
	@ApiModelProperty("合集视频")
	private VideoGatherVo videoGatherVo;
	//version2.1.1  新增集合标题
	@ApiModelProperty("集合标题")
	private String gatherTitle;

	private long createDate;//单位是秒
	private long totalWeight;
	private String colorTitle;

}
