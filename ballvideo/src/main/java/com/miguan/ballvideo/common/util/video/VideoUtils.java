package com.miguan.ballvideo.common.util.video;

import com.miguan.ballvideo.common.util.Global;
import com.miguan.ballvideo.common.util.StringUtil;
import com.miguan.ballvideo.common.util.VersionUtil;
import com.miguan.ballvideo.common.util.adv.AdvUtils;
import com.miguan.ballvideo.entity.es.FirstVideoEsVo;
import com.miguan.ballvideo.vo.*;
import com.miguan.ballvideo.vo.video.Adv161Vo;
import com.miguan.ballvideo.vo.video.FirstVideos161Vo;
import com.miguan.ballvideo.vo.video.FirstVideosVo;
import com.miguan.ballvideo.vo.video.Videos161Vo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.*;

/**
 * @Author shixh
 * @Date 2019/9/25
 **/
public class VideoUtils {
    /**
     * 安卓这边不能根据currentPage设置是否首次加载，另外增加flag判断；
     *
     * @param advertVo
     * @param currentPage
     * @param default_pageSize
     * @param flag
     * @return
     */
    public static int getPageSize(
            AdvertVo advertVo, int currentPage, int default_pageSize, int flag) {
        int pageSize = default_pageSize;
        if (advertVo == null) {
            return pageSize;
        } else {
            int first_position = advertVo.getFirstLoadPosition() == null ? 0 : advertVo.getFirstLoadPosition().intValue();
            int second_position = advertVo.getSecondLoadPosition() == null ? 0 : advertVo.getSecondLoadPosition().intValue();
            if (currentPage == 1 && first_position == 0) {
                return pageSize;
            } else if (currentPage > 1 && second_position == 0) {
                return pageSize;
            } else {
                if (flag == 1) {
                    if (first_position < default_pageSize) {
                        return pageSize;
                    } else if (first_position >= default_pageSize) {
                        return first_position;
                    }
                } else {
                    if (currentPage == 1 && first_position < default_pageSize) {
                        pageSize = default_pageSize;
                    } else if (currentPage == 1 && first_position >= default_pageSize) {
                        pageSize = first_position;
                    } else if (currentPage > 1 && second_position < default_pageSize) {
                        pageSize = default_pageSize;
                    } else if (currentPage > 1 && second_position > default_pageSize) {
                        pageSize = second_position;
                    }
                }

            }
            return pageSize;
        }
    }

    /**
     * 首页视频计算点赞和观看总数
     *
     * @param firstVideosList
     * @return
     */
    public static void cntFirstVideoLoveAndWatchNum(List<FirstVideos> firstVideosList) {
        firstVideosList.stream().forEach(e -> {
            //收藏数，评论数，点赞数转换单位为‘万’
            final String collectionCount = StringUtil.formatNum(e.getCollectionCount());
            final String commentCount = StringUtil.formatNum(e.getCommentCount());
            final String loveCount = StringUtil.formatNum(e.getLoveCount());
            e.setCollectionCount(collectionCount);
            e.setCommentCount(commentCount);
            e.setLoveCount(loveCount);
        });
    }

    public static void setLoveAndWatchNum(List<Videos161Vo> firstVideosList) {
        firstVideosList.stream().forEach(e -> {
            //收藏数，评论数，点赞数转换单位为‘万’
            final String collectionCount = StringUtil.formatNum(e.getCollectionCount());
            final String commentCount = StringUtil.formatNum(e.getCommentCount());
            final String loveCount = StringUtil.formatNum(e.getLoveCount());
            e.setCollectionCount(collectionCount);
            e.setCommentCount(commentCount);
            e.setLoveCount(loveCount);
        });
    }

    /**
     * 小视频计算点赞和观看总数
     *
     * @param smallVideosList
     * @return
     */
    public static void cntSmallVideoLoveAndWatchNum(List<SmallVideosVo> smallVideosList) {

        smallVideosList.stream().forEach(e -> {
            //收藏数，评论数，点赞数转换单位为‘万’
            final String collectionCount = StringUtil.formatNum(e.getCollectionCount());
            final String commentCount = StringUtil.formatNum(e.getCommentCount());
            final String loveCount = StringUtil.formatNum(e.getLoveCount());
            e.setCollectionCount(collectionCount);
            e.setCommentCount(commentCount);
            e.setLoveCount(loveCount);
        });
    }

    public static FirstVideosVo packagingAdv(AdvertVo advertVo) {
        FirstVideosVo firstVideosVo = new FirstVideosVo();
        firstVideosVo.setType(FirstVideosVo.ADV);
        Adv161Vo adv161Vo = new Adv161Vo();
        BeanUtils.copyProperties(advertVo, adv161Vo);
        firstVideosVo.setAdv(adv161Vo);
        return firstVideosVo;
    }

    public static FirstVideosVo packagingAdvList(List<AdvertVo> list) {
        FirstVideosVo firstVideosVo = new FirstVideosVo();
        firstVideosVo.setType(FirstVideosVo.ADV);
        List<Adv161Vo> adv161VoList = new ArrayList<>();
        for (AdvertVo advertVo : list) {
            Adv161Vo adv161Vo = new Adv161Vo();
            BeanUtils.copyProperties(advertVo, adv161Vo);
            adv161VoList.add(adv161Vo);
        }
        firstVideosVo.setAdvList(adv161VoList);
        return firstVideosVo;
    }

    public static FirstVideosVo packagingnNewAdvList(List<AdvertCodeVo> advertCodeVos) {
        FirstVideosVo firstVideosVo = new FirstVideosVo();
        firstVideosVo.setType(FirstVideosVo.ADV);
        firstVideosVo.setAdvertCodeVos(advertCodeVos);
        return firstVideosVo;
    }

    /**
     * v1.6.1新增,视频和广告后台组装一页显示；
     *
     * @param advertVos
     * @param firstVideos
     */
    public static List<FirstVideosVo> packaging(List<AdvertVo> advertVos, List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = getFirstVideosVos(firstVideos);
        if (!firstVideosVos.isEmpty() && CollectionUtils.isNotEmpty(advertVos)) {
            //设置广告在视频列表的位置
            final int video_first_position = Global.getInt("video_first_position");
            final int video_second_position = Global.getInt("video_second_position");
            final int videosSize = firstVideosVos.size();
            final int advSize = advertVos.size();
            if (video_first_position <= videosSize + 1 && advSize > 0) {
                FirstVideosVo firstVideosVo = packagingAdv(advertVos.get(0));
                firstVideosVos.add(video_first_position - 1, firstVideosVo);
                if (video_second_position <= videosSize + 1 && advSize == 2) {
                    firstVideosVo = packagingAdv(advertVos.get(1));
                    firstVideosVos.add(video_second_position - 1, firstVideosVo);
                }
            }
        } else if (CollectionUtils.isNotEmpty(advertVos)) {
            //不存在视频的时候
            FirstVideosVo firstVideosVo = packagingAdv(advertVos.get(0));
            firstVideosVos.add(firstVideosVo);
        }
        return firstVideosVos;
    }

    /**
     * v2.3.0新增,视频和广告后台组装一页显示；
     *
     * @param advertVos
     * @param firstVideos
     */
    public static List<FirstVideosVo> packagingNew(List<AdvertVo> advertVos, List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = getFirstVideosVos(firstVideos);
        if (!firstVideosVos.isEmpty() && CollectionUtils.isNotEmpty(advertVos)) {
            //设置广告在视频列表的位置
            final int video_first_position = Global.getInt("video_first_position");
            final int video_second_position = Global.getInt("video_second_position");
            final int videosSize = firstVideosVos.size();
            if (video_first_position <= videosSize + 1) {
                FirstVideosVo advVo = packagingAdvList(advertVos);
                firstVideosVos.add(video_first_position - 1, advVo);
                if (video_second_position <= videosSize + 1) {
                    firstVideosVos.add(video_second_position - 1, advVo);
                }
            }
        } else if (CollectionUtils.isNotEmpty(advertVos)) {
            //不存在视频的时候
            FirstVideosVo firstVideosVo = packagingAdvList(advertVos);
            firstVideosVos.add(firstVideosVo);
        }
        return firstVideosVos;
    }

    /**
     * v2.5.0新增,视频和广告后台组装一页显示；
     *
     * @param advertCodeVos
     * @param firstVideos
     */
    public static List<FirstVideosVo> packagingNewAdvert(List<AdvertCodeVo> advertCodeVos, List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = getFirstVideosVos(firstVideos);
        if (!firstVideosVos.isEmpty() && CollectionUtils.isNotEmpty(advertCodeVos)) {
            //设置广告在视频列表的位置
            final int video_first_position = Global.getInt("video_first_position");
            final int video_second_position = Global.getInt("video_second_position");
            final int videosSize = firstVideosVos.size();
            if (video_first_position <= videosSize + 1) {
                FirstVideosVo advVo = packagingnNewAdvList(advertCodeVos);
                firstVideosVos.add(video_first_position - 1, advVo);
                if (video_second_position <= videosSize + 1) {
                    firstVideosVos.add(video_second_position - 1, advVo);
                }
            }
        } else if (CollectionUtils.isNotEmpty(advertCodeVos)) {
            //不存在视频的时候
            FirstVideosVo firstVideosVo = packagingnNewAdvList(advertCodeVos);
            firstVideosVos.add(firstVideosVo);
        }
        return firstVideosVos;
    }

    //组装视频方法
    public static List<FirstVideosVo> getFirstVideosVos(List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            FirstVideosVo firstVideosVo = null;
            for (Videos161Vo firstVideo : firstVideos) {
                firstVideosVo = new FirstVideosVo();
                firstVideosVo.setType(FirstVideosVo.VIDEO);
                firstVideosVo.setVideo(firstVideo);
                firstVideosVos.add(firstVideosVo);
            }
        }
        return firstVideosVos;
    }

    public static String getIdsByVideos(List<Videos161Vo> videos161Vos) {
        StringBuffer sb = new StringBuffer("");
        for (Videos161Vo vo : videos161Vos) {
            sb.append(vo.getId()).append(",");
        }
        return sb.substring(0, sb.length() - 1).toString();
    }

    /**
     * 给每个视频标识是第几标签的视频，方便前端判断重新组装占比；
     *
     * @param videos161Vos
     * @param label        第几标签的视频
     */
    public static void setLabel(List<Videos161Vo> videos161Vos, int label) {
        videos161Vos.forEach(a -> {
            a.setLabel(label);
        });
    }

    public static void setLabel(List<Videos161Vo> videos161Vos, long catID1) {
        videos161Vos.forEach(a -> {
           if(a.getCatId()==catID1){
               a.setLabel(1);
           }else{
               a.setLabel(2);
           }
        });
    }

    /**
     * 获取配置表默认的视频占比
     *
     * @return
     */
    public static String getDefaultDuty() {
        int firstLableValue = Global.getInt("first_label_value");//用户第一标签条数
        int secondLableValue = Global.getInt("second_label_value");//用户第二标签条数
        int otherLableValue = Global.getInt("other_label_value");//用户第三标签条数
        String duty = firstLableValue + "," + secondLableValue + "," + otherLableValue;
        return duty;
    }

    public static int getPageNums() {
        int firstLableValue = Global.getInt("first_label_value");//用户第一标签条数
        int secondLableValue = Global.getInt("second_label_value");//用户第二标签条数
        int otherLableValue = Global.getInt("other_label_value");//用户第三标签条数
        return firstLableValue+secondLableValue+otherLableValue;
    }

    /**
     * 随机获取指定广告
     *
     * @param advertVos
     * @param num
     * @return
     */
    public static List<AdvertVo> randomAdvers(List<AdvertVo> advertVos, int num) {
        if (advertVos.size() <= num) return advertVos;
        Collections.shuffle(advertVos);
        return advertVos.subList(0, num);
    }

//    /**
//     * 筛选指定广告返回
//     * 1 permission=1 的话，优先获取广点通广告；
//     * 2 permission=0，不返回广点通广告；
//     *
//     * @param advers
//     * @param num
//     * @param permission
//     * @return
//     */
//    public static List<AdvertVo> toChoseAdvers(List<AdvertVo> advers, int num, String permission) {
//        if (CollectionUtils.isEmpty(advers)) return advers;
//        List<AdvertVo> gdtAdvs = new ArrayList<AdvertVo>();
//        List<AdvertVo> others = new ArrayList<AdvertVo>();
//        for (AdvertVo adver : advers) {
//            int probability = adver.getProbability();
//            if (probability == 0){
//                continue;
//            }
//            String adCode = adver.getAdCode();
//            if ("2".equals(adCode)||"10".equals(adCode)) {
//                gdtAdvs.add(adver);
//            } else {
//                others.add(adver);
//            }
//        }
//        if (!"1".equals(permission) || gdtAdvs.isEmpty()) {
//            return computer(others, num);
//        }
//        if (gdtAdvs.size() >= num) {
//            return computer(gdtAdvs, num);
//        } else {
//            int gdt = gdtAdvs.size();
//            int less = num - gdt;//差多少个非广点通广告
//            if (less < others.size()) {
//                others = computer(others, less);
//            }
//            gdtAdvs.addAll(others);
//            return gdtAdvs;
//        }
//    }

    /**
     * 根据指定CatID作为主标签，先不考虑重复问题（APP判断）
     *
     * @param lastCatId
     * @param catIdsSort
     * @return
     */
    public static String appendCatIds(String lastCatId, String catIdsSort) {
        //catIdsSort总数:用户第一标签条数+用户第二标签条数 - 1
        int nums = Global.getInt("first_label_value") + Global.getInt("second_label_value");
        StringBuffer sb = new StringBuffer("");
        sb.append(lastCatId);
        String[] catIdSortStr = catIdsSort.split(",");
        int addNum = 0;
        for (int i = 0; i < catIdSortStr.length; i++) {
            if (!lastCatId.equals(catIdSortStr[i]) && addNum < nums) {
                sb.append(",").append(catIdSortStr[i]);
                addNum++;
            }
        }
        return sb.toString();
    }

    /**
     * 首页视频设置分类名称
     *
     * @param firstVideosList
     * @param catMap
     */
    public static void setCatName(List<Videos161Vo> firstVideos16List, List<FirstVideos> firstVideosList, Map<Long, VideosCatVo> catMap) {
        if (catMap == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(firstVideos16List)) {
            firstVideos16List.stream().forEach(e -> {
                final VideosCatVo videosCatVo = catMap.get(e.getCatId());
                if (videosCatVo != null) {
                    e.setCatName(videosCatVo.getName());
                }
            });
        }
        if (CollectionUtils.isNotEmpty(firstVideosList)) {
            firstVideosList.stream().forEach(e -> {
                final VideosCatVo videosCatVo = catMap.get(e.getCatId());
                if (videosCatVo != null) {
                    e.setCatName(videosCatVo.getName());
                }
            });
        }
    }

    /**
     * 如果第三标签的分类ID属于第一、二标签，重新设置label
     *
     * @param firstVideos
     * @param duty
     * @param catIdsSort
     */
    public static void setThirdLabel(List<Videos161Vo> firstVideos, String[] duty, String[] catIdsSort) {
        String labelCats1 = catIdsSort[0];
        int nums = Integer.parseInt(duty[1]);
        if (catIdsSort.length < nums) nums = catIdsSort.length;//测试数据有可能小于nums
        List<String> catIds = new ArrayList<String>();
        for (int i = 1; i < nums; i++) {
            catIds.add(catIdsSort[i]);
        }
        for (Videos161Vo videos161Vo : firstVideos) {
            if (videos161Vo.getLabel() != 3) continue;
            String catId = videos161Vo.getCatId() + "";
            if (labelCats1.equals(catId)) {
                videos161Vo.setLabel(1);
            } else if (catIds.contains(catId)) {
                videos161Vo.setLabel(2);
            } else {
                videos161Vo.setLabel(3);
            }
        }
    }

    public static FirstVideos161Vo packaging(List<Videos161Vo> videos){
        FirstVideos161Vo firstVideos161Vo = new FirstVideos161Vo();
        List<FirstVideosVo> firstVideosVos = new ArrayList<FirstVideosVo>();
        if(CollectionUtils.isNotEmpty(videos)){
            for(Videos161Vo videos161Vo:videos){
                FirstVideosVo v = new FirstVideosVo();
                v.setVideo(videos161Vo);
                v.setType(FirstVideosVo.VIDEO);
                firstVideosVos.add(v);
            }
        }
        firstVideos161Vo.setFirstVideosVos(firstVideosVos);
        return firstVideos161Vo;
    }

    public static List<Videos161Vo> packagingByEsVideos(List<FirstVideoEsVo> esVideos){
        List<Videos161Vo> videos = new ArrayList<Videos161Vo>();
        for(FirstVideoEsVo firstVideoEsVo :esVideos){
            Videos161Vo v = new Videos161Vo();
            BeanUtils.copyProperties(firstVideoEsVo,v);
            videos.add(v);
        }
        return videos;
    }

  /**
   * 合并视频ID并去重
   * @param ids_list
   * @param showedIds
   * @return
   */
    public static String getVideoIds(List<String> ids_list, String showedIds) {
        if (StringUtils.isNotBlank(showedIds)){
            String [] strArray = showedIds.split(",");
            List<String> list = Arrays.asList(strArray);
            ids_list.addAll(list);
        }
        return String.join(",",distinct(ids_list));
    }

    public static List distinct(List<String> list){
        HashSet set = new HashSet(list);
        list.clear();
        list.addAll(set);
        return list;
    }

    //组装首页视频广告数据
    public static void packagAdvertAndVideos(FirstVideos161Vo firstVideosNewVo, List<Videos161Vo> firstVideos, List<AdvertVo> advertList,String appVersion) {
        boolean flag = VersionUtil.isHigh(appVersion, 2.2);
        if (flag){
            if (CollectionUtils.isNotEmpty(advertList)) {
                int type = advertList.get(0).getType();
                if (type == 0) {
                    advertList = AdvUtils.computer(advertList, advertList.size());
                }
            }
            List<FirstVideosVo> firstVideosVos = packagingNew(advertList, firstVideos);
            firstVideosNewVo.setFirstVideosVos(firstVideosVos);
        }else {
            List<AdvertVo> list = AdvUtils.computer(advertList, 2);
            List<FirstVideosVo> firstVideosVos = packaging(list, firstVideos);
            firstVideosNewVo.setFirstVideosVos(firstVideosVos);
        }
    }

  /**
   * 广告根据首次加载p1，再次加载显示p2，多余广告的数据根据p2间隔展示
   * @param advertVos
   * @param firstVideos
   * @return
   */
  public static List<FirstVideosVo> packagingBySearch(
      List<AdvertVo> advertVos, List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = new ArrayList<FirstVideosVo>();
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            boolean showAdvs = true;
            if(CollectionUtils.isEmpty(advertVos)
                    || advertVos.get(0).getFirstLoadPosition()==null
                    || advertVos.get(0).getSecondLoadPosition()==null){
                showAdvs = false;
            }
            FirstVideosVo firstVideosVo = null;
            int firstLoadPosition = showAdvs?advertVos.get(0).getFirstLoadPosition():0;
            int secondLoadPosition = showAdvs?advertVos.get(0).getSecondLoadPosition():0;
            for (int i = 1;i<firstVideos.size()+1;i++) {
                firstVideosVo = new FirstVideosVo();
                firstVideosVo.setType(FirstVideosVo.VIDEO);
                firstVideosVo.setVideo(firstVideos.get(i-1));
                firstVideosVos.add(firstVideosVo);
                if(showAdvs){
                    if(i==firstLoadPosition && !advertVos.isEmpty()){
                        firstVideosVo = packagingAdv(advertVos.get(0));
                        firstVideosVos.add(firstVideosVo);
                        advertVos.remove(advertVos.get(0));
                    }else if(i==secondLoadPosition && !advertVos.isEmpty()){
                        firstVideosVo = packagingAdv(advertVos.get(0));
                        firstVideosVos.add(firstVideosVo);
                        advertVos.remove(advertVos.get(0));
                    }
                }
            }
        }
        return firstVideosVos;
    }

    /**
     * V 2.5.0
     * 广告根据首次加载p1，再次加载显示p2，多余广告的数据根据p2间隔展示
     * @param advertCodeVos
     * @param firstVideos
     * @return
     */
    public static List<FirstVideosVo> packagingNewBySearch(
            List<AdvertCodeVo> advertCodeVos, List<Videos161Vo> firstVideos) {
        List<FirstVideosVo> firstVideosVos = new ArrayList<FirstVideosVo>();
        if (CollectionUtils.isNotEmpty(firstVideos)) {
            boolean showAdvs = true;
            if(CollectionUtils.isEmpty(advertCodeVos)
                    || advertCodeVos.get(0).getFirstLoadPosition()==null
                    || advertCodeVos.get(0).getSecondLoadPosition()==null){
                showAdvs = false;
            }
            FirstVideosVo firstVideosVo = null;
            int firstLoadPosition = showAdvs?advertCodeVos.get(0).getFirstLoadPosition():0;
            int secondLoadPosition = showAdvs?advertCodeVos.get(0).getSecondLoadPosition():0;
            for (int i = 1;i<firstVideos.size()+1;i++) {
                firstVideosVo = new FirstVideosVo();
                firstVideosVo.setType(FirstVideosVo.VIDEO);
                firstVideosVo.setVideo(firstVideos.get(i-1));
                firstVideosVos.add(firstVideosVo);
                if(showAdvs){
                    if(i==firstLoadPosition && CollectionUtils.isNotEmpty(advertCodeVos)){
                        firstVideosVo = packagingNewAdv(advertCodeVos);
                        firstVideosVos.add(firstVideosVo);
                    }else if(i==secondLoadPosition && CollectionUtils.isNotEmpty(advertCodeVos)){
                        firstVideosVo = packagingNewAdv(advertCodeVos);
                        firstVideosVos.add(firstVideosVo);
                    }
                }
            }
        }
        return firstVideosVos;
    }

    public static FirstVideosVo packagingNewAdv(List<AdvertCodeVo> advertCodeVos) {
        FirstVideosVo firstVideosVo = new FirstVideosVo();
        firstVideosVo.setType(FirstVideosVo.ADV);
        firstVideosVo.setAdvertCodeVos(advertCodeVos);
        return firstVideosVo;
    }

    public static String appendShowedIds(String showedIds,String id){
        if(showedIds==null)return id;
        if(id==null || "null".equals(id))return showedIds;
        return showedIds+","+id;
    }

    public static Videos161Vo packaging(FirstVideoEsVo firstVideoEsVo) {
        Videos161Vo videos161Vo = new Videos161Vo();
        BeanUtils.copyProperties(firstVideoEsVo,videos161Vo);
        return videos161Vo;
    }

    public static boolean containsHiddenCatId(String hiddenCatIds, String [] catIds_user) {
        if(StringUtils.isEmpty(hiddenCatIds))return false;
        String [] catIds_hidden = hiddenCatIds.split(",");
        for(String catId:catIds_hidden){
            for(String catId_user:catIds_user){
                if(catId.contains(catId_user)){
                    return true;
                }
            }
        }
        return false;
    }
}
