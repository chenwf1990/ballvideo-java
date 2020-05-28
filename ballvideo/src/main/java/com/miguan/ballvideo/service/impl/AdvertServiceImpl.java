package com.miguan.ballvideo.service.impl;

import com.miguan.ballvideo.common.util.Global;
import com.miguan.ballvideo.common.util.adv.AdvFieldType;
import com.miguan.ballvideo.common.util.adv.AdvUtils;
import com.miguan.ballvideo.dynamicquery.Dynamic2Query;
import com.miguan.ballvideo.redis.util.RedisKeyConstant;
import com.miguan.ballvideo.service.AdvertService;
import com.miguan.ballvideo.service.RedisService;
import com.miguan.ballvideo.service.ToolMofangService;
import com.miguan.ballvideo.vo.AdvertCodeVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class AdvertServiceImpl implements AdvertService {

    @Resource
    private Dynamic2Query dynamic2Query;

    @Resource
    private ToolMofangService toolMofangService;

    @Resource
    private RedisService redisService;

    @Override
    public List<AdvertCodeVo> commonSearch(Map<String, Object> param) {
        List<AdvertCodeVo> advertCodeVos = getAdvertsByParams(param,AdvFieldType.All);
        if(CollectionUtils.isEmpty(advertCodeVos))return null;
        if (advertCodeVos.get(0).getComputer() == 1) {
            return AdvUtils.computerAndSort(advertCodeVos);
        }else if(advertCodeVos.get(0).getComputer() == 2){
            return AdvUtils.sort(advertCodeVos);
        }
        return advertCodeVos;
    }

    @Override
    public List<AdvertCodeVo> getAdertsByGame(Map<String, Object> param) {
        param.put("game", "game");
        List<AdvertCodeVo> list = getAdvertsByParams(param,AdvFieldType.All);;
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<AdvertCodeVo> result = new ArrayList<>();
        Map<String, List<AdvertCodeVo>> mapList = list.stream().collect(Collectors.groupingBy(AdvertCodeVo::getPositionType));
        for (Map.Entry<String, List<AdvertCodeVo>> map : mapList.entrySet()) {
            List<AdvertCodeVo> advertCodeVos = map.getValue();
            advertCodeVos = AdvUtils.sortByComputer(advertCodeVos);
            if(CollectionUtils.isEmpty(advertCodeVos))continue;//空指针问题修复 addshixh0809
            //每个广告位置只需返回一个广告给第三方调用
            result.add(advertCodeVos.get(0));
        }
        return result;
    }

  /**
   * 如果开关开启，返回多个广告位，并随机返回一个广告位
   * @param param
   * @return
   */
  @Override
  public Map<String, Object> getLockScreenInfo(Map<String, Object> param) {
        Map<String, Object> result = new HashMap<>();
        String androidLockScreenToken = Global.getValue("android_lock_screen_token");
        result.put("androidLockScreenToken", androidLockScreenToken);
        //锁屏是开的时候才去查广告信息
        if ("10".equals(androidLockScreenToken)) {
            param.put("lockScreen", "1");
            List<AdvertCodeVo> advertVoList = getAdvertsByParams(param,AdvFieldType.PositionType);
            if (CollectionUtils.isNotEmpty(advertVoList)) {
                List<String> list = advertVoList.stream().map(AdvertCodeVo::getPositionType).collect(toList());
                int i = new Random().nextInt(list.size());
                result.put("positionType", list.get(i));
            }
        }
        return result;
    }

    @Override
    public Map<String, List<AdvertCodeVo>> getAdversByPositionTypes(Map<String, Object> param) {
        List<String> positionTypes = (List<String>) param.get("positionTypes");
        param.put("positionTypes", "'"+String.join("','",positionTypes)+"'");
        List<AdvertCodeVo> list = getAdvertsByParams(param,AdvFieldType.All);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, List<AdvertCodeVo>> mapList = list.stream().collect(Collectors.groupingBy(AdvertCodeVo::getPositionType));
        for (Map.Entry<String, List<AdvertCodeVo>> map : mapList.entrySet()) {
            List<AdvertCodeVo> advertCodeVos = map.getValue();
            map.setValue(AdvUtils.sortByComputer(advertCodeVos));
        }
        return mapList;
    }

    public List<AdvertCodeVo> getAdvertsByParams(Map<String, Object> param,int fieldType) {
        //魔方后台-广告总开关:true禁用，false非禁用
        if (toolMofangService.stoppedByMofang(param)) {
            return null;
        }
        String key = AdvUtils.filter(param);
        String json = redisService.get(key);
        if(RedisKeyConstant.EMPTY_VALUE.equals(json)){
            return null;
        }
        List<AdvertCodeVo> sysVersionVos = dynamic2Query.getAdversWithCache(param,fieldType);
        if(CollectionUtils.isEmpty(sysVersionVos)){
            redisService.set(key,RedisKeyConstant.EMPTY_VALUE,RedisKeyConstant.EMPTY_VALUE_SECONDS);
            return null;
        }else{
            return sysVersionVos;
        }
    }

    @Override
    public Map<String,String> getAppIdByAppPackage(String appPackage) {
        try{
            Map<String,String> resultMap = new HashMap<>();
            List<Map<String,String>> resultList = dynamic2Query.nativeQueryListMap("select app_id,plat_key from ad_appId_ios where app_package=? ",appPackage);
            if (CollectionUtils.isNotEmpty(resultList)) {
                for (Map map : resultList) {
                    resultMap.put(map.get("plat_key").toString(),map.get("app_id").toString());
                }
            }
            return resultMap;
        }catch (Exception e){
            return null;
        }
    }
}
