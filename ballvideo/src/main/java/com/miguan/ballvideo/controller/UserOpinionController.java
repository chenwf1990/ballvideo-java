package com.miguan.ballvideo.controller;

import com.cgcg.context.thread.ThreadPoolManager;
import com.github.pagehelper.Page;
import com.miguan.ballvideo.common.util.*;
import com.miguan.ballvideo.service.ClUserOpinionService;
import com.miguan.ballvideo.springTask.thread.UploadImgsThread;
import com.miguan.ballvideo.vo.ClUserOpinionVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tool.util.StringUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 意见反馈Controller
 *
 * @author xy.chen
 * @version 1.0.0
 * @date 2019-08-09
 */
@Api(value = "意见反馈ApiController",tags={"意见反馈Api"})
@RestController
public class UserOpinionController {

    @Resource
    private ClUserOpinionService clUserOpinionService;

    @ApiOperation(value = "意见反馈提交")
    @PostMapping("/api/video/userOpinionSubmit")
    public ResultMap userOpinionSubmit(ClUserOpinionVo clUserOpinionVo) {
        int num = 0;
        if (clUserOpinionVo.getId() == null && clUserOpinionVo.getUserId() != null) {
            clUserOpinionVo.setState(ClUserOpinionVo.UNTREATED);//默认未处理状态
            num = clUserOpinionService.saveClUserOpinion(clUserOpinionVo);
        }
        if (num == 1) {
            return ResultMap.success();
        } else {
            return ResultMap.error();
        }
    }


    @ApiOperation(value = "消息中心-系统消息")
    @PostMapping("/api/video/findUserOpinionList")
    public ResultMap findUserOpinionList(@ModelAttribute ClUserOpinionVo clUserOpinionVo, @ApiParam("当前页面") int currentPage, @ApiParam("每页条数") int pageSize) {
        Page<ClUserOpinionVo> clUserOpinionList = clUserOpinionService.findClUserOpinionList(clUserOpinionVo, currentPage, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("page", new RdPage(clUserOpinionList));
        result.put("data", clUserOpinionList);
        return ResultMap.success(result);
    }

    @ApiOperation(value = "意见反馈查看详情")
    @PostMapping("/api/video/findUserOpinionInfo")
    public ResultMap findUserOpinionInfo(@ApiParam("用户反馈id") String id) {
        ClUserOpinionVo clUserOpinionById = clUserOpinionService.getClUserOpinionById(id);
        return ResultMap.success(clUserOpinionById);
    }

    @ApiOperation(value = "上传反馈图片到白山云返回URL")
    @PostMapping("/api/video/getOpinionImgUrl")
    public ResultMap getImgUrl(@RequestParam(value = "opinionImageFile") MultipartFile opinionImageFile,
                               @ApiParam("用户ID") String userId) {
        Map<String, Object> result = new HashMap<>();
        String originalFilename = opinionImageFile.getOriginalFilename();
        String fileType = originalFilename.substring(originalFilename.lastIndexOf(".") + 1, originalFilename.length());
        Date currdate = DateUtil.getNow();
        String prefix = StringUtil.isBlank(userId) ? "" : userId + "_";
        String imgPicName = prefix + DateUtil.dateStr(currdate, DateUtil.DATEFORMAT_STR_016) + "." + fileType;
        String appEnvironment = Global.getValue("app_environment");
        String appfile = "";
        if ("prod".equals(appEnvironment)) {
            appfile = "pro-img-xiyou";
        } else {
            appfile = "dev-img-xiyou";
        }

        String endPoint = YmlUtil.getCommonYml("aws.endPoint");
        String imgfilePath = endPoint +"/" + appfile + "/userOpinion/" + DateUtil.dateStr(currdate, DateUtil.DATEFORMAT_STR_013) + "/" + imgPicName;
        if (opinionImageFile != null) {
            ThreadPoolManager.execute(new UploadImgsThread(opinionImageFile, userId, "userOpinion", "opinion", currdate));
            result.put("bsyOpinionImgUrl", imgfilePath);
            return ResultMap.success(result);
        }
        return ResultMap.error("请上传图片");
    }
}
